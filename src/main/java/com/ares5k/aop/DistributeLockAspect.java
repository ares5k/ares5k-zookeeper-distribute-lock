package com.ares5k.aop;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ares5k.annotation.Lock;
import com.ares5k.annotation.ReadLock;
import com.ares5k.annotation.WriteLock;
import com.ares5k.exception.LockException;
import com.ares5k.process.DistributeLockProcess;
import com.ares5k.process.impl.LockProcess;
import com.ares5k.process.impl.ReadLockProcess;
import com.ares5k.process.impl.WriteLockProcess;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 公平锁, 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * aop切面类 - 使用锁注解时的处理
 *
 * @author ares5k
 * @since 2020-10-30
 * qq: 16891544
 * email: 16891544@qq.com
 */
@SuppressWarnings("all")
@Slf4j
@Aspect
@Component
@EnableAspectJAutoProxy(exposeProxy = true)
public class DistributeLockAspect {

    /**
     * zookeeper java客户端-curator
     */
    @Autowired
    private CuratorFramework curatorClient;

    /**
     * 分布式锁注解集合
     */
    private final Class[] annotationClazz = {Lock.class, ReadLock.class, WriteLock.class};

    /**
     * 互斥锁缓存
     */
    private final Map<String, DistributeLockProcess> mutexLockCache = new ConcurrentHashMap<>();

    /**
     * 读锁缓存
     */
    private final Map<String, DistributeLockProcess> readLockCache = new ConcurrentHashMap<>();

    /**
     * 读写锁缓存
     */
    private final Map<String, Map<LockEnum, DistributeLockProcess>> readWriteLockCache = new ConcurrentHashMap<>();

    /**
     * 切入点-使用分布式锁注解时切入
     *
     * @author ares5k
     */
    @Pointcut("@annotation(com.ares5k.annotation.Lock) || @annotation(com.ares5k.annotation.WriteLock) || @annotation(com.ares5k.annotation.ReadLock)")
    public void pointcut() {
    }

    /**
     * 环绕通知
     *
     * @param proceedingJoinPoint 目标方法信息
     * @throws Throwable 处理异常
     * @author ares5k
     */
    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        //方法执行结果
        Object returnVal = null;
        //获取目标方法
        Method targetMethod = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        //获取目标方法元信息
        Method targetMethodDefineInfo = proceedingJoinPoint.getTarget().getClass().getMethod(targetMethod.getName(), targetMethod.getParameterTypes());
        //在目标方法上查找标记的具体锁注解
        Annotation annotation = searchAnnotationOnMethod(targetMethodDefineInfo);

        //没有锁注解直接执行目标方法
        if (annotation == null) {
            //执行目标方法
            proceedingJoinPoint.proceed();
        }
        //判别锁的类型
        AnnotationData annotationData;
        log.info("分布式锁切面处理--开始");

        if (annotation instanceof Lock) {
            //互斥锁处理
            annotationData = lockProcess(annotation);
        } else if (annotation instanceof ReadLock) {
            //读锁处理
            annotationData = readProcess(annotation);
        } else {
            //写锁处理
            annotationData = writeProcess(annotation);
        }
        //判断拿锁是否成功
        log.info("尝试获取分布式锁");
        if (annotationData.distributeLockProcess.acquire(annotationData.waitTime)) {
            try {
                //执行目标方法-并获取返回值
                log.info("获取分布式锁成功, 执行目标临界方法");
                returnVal = proceedingJoinPoint.proceed();
            } finally {
                //关闭分布式锁
                log.info("释放分布式锁");
                annotationData.distributeLockProcess.release();
            }
        } else {
            //指定时间内未能获取分布式锁
            throw new LockException(LockException.CAN_NOT_GET_LOCK_IN_TIME, LockException.LockErrorCode.ERROR_IN_TIME);
        }
        log.info("分布式锁切面处理--结束");
        return returnVal;
    }

    /**
     * 互斥锁处理
     *
     * @param annotation 目标方法上的注解
     * @return 用来包装注解参数和分布式锁处理对象
     * @author ares5k
     */
    private AnnotationData lockProcess(Annotation annotation) {

        //互斥锁处理对象
        DistributeLockProcess lockProcess;
        //互斥锁注解对象
        Lock lockAnnotation = (Lock) annotation;
        //zookeeper节点必须以 '/'开头
        String path = processPath(lockAnnotation.bizPath());

        //线程是否可以重入
        if (lockAnnotation.reentrant()) {
            //缓存中是否有当前节点的互斥锁处理对象
            if (ObjectUtil.isEmpty((lockProcess = mutexLockCache.get(path)))) {
                //防止第一次并发创建
                synchronized (mutexLockCache) {
                    //双重验证
                    if (ObjectUtil.isEmpty((lockProcess = mutexLockCache.get(path)))) {
                        //缓存中没有互斥锁处理对象时, 创建互斥锁处理对象并加入缓存
                        lockProcess = new LockProcess(MUTEX_LOCK_ROOT_PATH + path, curatorClient);
                        mutexLockCache.put(path, lockProcess);
                    }
                }
            }
        } else {
            //线程不可重入时, 创建新的锁处理对象
            lockProcess = new LockProcess(MUTEX_LOCK_ROOT_PATH + path, curatorClient);
        }
        //用来包装注解参数和分布式锁处理对象
        return new AnnotationData()
                .setWaitTime(lockAnnotation.waitTime())
                .setDistributeLockProcess(lockProcess);
    }

    /**
     * 读锁处理
     *
     * @param annotation 目标方法上的注解
     * @return 用来包装注解参数和分布式锁处理对象
     * @author ares5k
     */
    private AnnotationData readProcess(Annotation annotation) {

        //读锁对象
        DistributeLockProcess readLockProcess;
        //读锁注解对象
        ReadLock readLockAnnotation = (ReadLock) annotation;
        //zookeeper节点必须以 '/'开头
        String path = processPath(readLockAnnotation.bizPath());

        //可重入读写锁缓存中是否有读锁对象
        if (CollUtil.isNotEmpty(readWriteLockCache.get(path))) {
            //有读锁对象就从缓存中获取
            readLockProcess = readWriteLockCache.get(path).get(LockEnum.READ);
        } else {
            //可重入读写锁缓存中没有读锁对象, 就尝试从读锁缓存中获取
            //读锁缓存中是否有当前节点的读锁处理对象
            if (ObjectUtil.isEmpty(readLockProcess = readLockCache.get(path))) {
                //防止第一次并发创建
                synchronized (readLockCache) {
                    //双重验证
                    if (ObjectUtil.isEmpty(readLockProcess = readLockCache.get(path))) {
                        //缓存中没有读锁处理对象时, 创建读锁处理对象并加入读锁缓存
                        readLockProcess = new ReadLockProcess(READ_WRITE_LOCK_ROOT_PATH + path, curatorClient);
                        readLockCache.put(path, readLockProcess);
                    }
                }
            }
        }
        //用来包装注解参数和分布式锁处理对象
        return new AnnotationData()
                .setWaitTime(readLockAnnotation.waitTime())
                .setDistributeLockProcess(readLockProcess);
    }

    /**
     * 写锁处理类
     *
     * @param annotation 目标方法上的注解
     * @return 用来包装注解参数和分布式锁处理对象
     * @author ares5k
     */
    private AnnotationData writeProcess(Annotation annotation) {

        //写锁处理对象
        DistributeLockProcess writeLockProcess;
        //写锁注解对象
        WriteLock writeLockAnnotation = (WriteLock) annotation;
        //zookeeper节点必须以 '/'开头
        String path = processPath(writeLockAnnotation.bizPath());

        //线程是否可以重入
        if (writeLockAnnotation.reentrant()) {
            //防止第一次并发创建
            synchronized (readWriteLockCache) {

                //读写锁缓存中是否有当前节点的写锁处理对象
                if (CollUtil.isEmpty(readWriteLockCache.get(path))) {
                    //缓存中没有写锁处理对象时, 创建写锁处理对象
                    writeLockProcess = new WriteLockProcess(READ_WRITE_LOCK_ROOT_PATH + path, curatorClient);

                    //创建读写锁 Map并放入读写锁缓存中
                    Map readWriteMap = new ConcurrentHashMap();
                    readWriteMap.put(LockEnum.READ, new ReadLockProcess((WriteLockProcess) writeLockProcess));
                    readWriteMap.put(LockEnum.WRITE, writeLockProcess);
                    readWriteLockCache.put(path, readWriteMap);
                } else {
                    //缓存中有写锁处理对象就直接获取
                    writeLockProcess = readWriteLockCache.get(path).get(LockEnum.WRITE);
                }
            }
        } else {
            //线程不可重入时, 创建新的写锁处理对象
            writeLockProcess = new WriteLockProcess(READ_WRITE_LOCK_ROOT_PATH + path, curatorClient);
        }
        //用来包装注解参数和分布式锁处理对象
        return new AnnotationData()
                .setWaitTime(writeLockAnnotation.waitTime())
                .setDistributeLockProcess(writeLockProcess);
    }

    /**
     * zookeeper节点必须以 '/'开头
     *
     * @param path 锁路径
     * @return 拼接 '/'后的路径
     */
    private String processPath(String path) {
        if (!StrUtil.startWith(path, "/")) {
            path = "/" + path;
        }
        return path;
    }

    /**
     * 在目标方法上查找标记的具体锁注解
     *
     * @param method 目标方法
     * @return 锁注解
     * @author ares5k
     */
    private Annotation searchAnnotationOnMethod(Method method) {
        for (Class clazz : annotationClazz) {
            Annotation annotation = method.getAnnotation(clazz);
            method.getAnnotations();
            method.getDeclaredAnnotations();
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 用来包装注解参数和分布式锁处理对象
     *
     * @author ares5k
     */
    @Setter
    @Accessors(chain = true)
    private class AnnotationData {
        /**
         * 当未能获取锁时的等待时间
         */
        private long waitTime;
        /**
         * 分布式锁处理类
         */
        private DistributeLockProcess distributeLockProcess;
    }

    /**
     * 分布式锁枚举
     */
    private enum LockEnum {
        READ, WRITE
    }

    /**
     * 互斥锁 zookeeper根路径
     */
    private static final String MUTEX_LOCK_ROOT_PATH = "/mutex";

    /**
     * 读写锁 zookeeper根路径
     */
    private static final String READ_WRITE_LOCK_ROOT_PATH = "/read-write";
}
