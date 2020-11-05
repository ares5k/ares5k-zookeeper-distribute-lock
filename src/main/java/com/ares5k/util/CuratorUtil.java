package com.ares5k.util;

import com.ares5k.exception.LockException;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.concurrent.TimeUnit;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 公平锁, 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * zookeeper java客户端-curator 分布式锁工具类
 *
 * @author ares5k
 * @since 2020-10-30
 * qq: 16891544
 * email: 16891544@qq.com
 */
public class CuratorUtil {

    /**
     * 拿锁
     *
     * @param lock     curator提供的分布式锁
     * @param timeWait 当未能获取锁时的等待时间
     * @return 拿锁结果
     * @throws LockException 设置锁节点失败异常
     * @author ares5k
     */
    public static boolean acquire(InterProcessMutex lock, long timeWait) throws LockException {
        //拿锁结果
        boolean hasLock;
        try {
            //判断拿锁失败是否等待
            if (timeWait >= 0) {
                //拿不到锁等待固定时间, 如果等待时间内, 锁被其他人释放, 重新尝试拿锁
                hasLock = lock.acquire(timeWait, TimeUnit.MILLISECONDS);
            } else {
                //拿不到锁就一直等待
                hasLock = lock.acquire(0, null);
            }
        } catch (Exception e) {
            throw new LockException(SET_LOCK_NODE_ERROR, e);
        }
        //拿锁结果
        return hasLock;
    }

    /**
     * 释放锁
     *
     * @param lock curator提供的分布式锁
     * @throws LockException 删除锁节点失败异常
     * @author ares5k
     */
    public static void release(InterProcessMutex lock) throws LockException {
        try {
            lock.release();
        } catch (Exception e) {
            throw new LockException(DEL_LOCK_NODE_ERROR, e);
        }
    }

    /**
     * 设置锁节点失败
     */
    private static final String SET_LOCK_NODE_ERROR = "设置锁节点失败";

    /**
     * 删除锁节点失败
     */
    private static final String DEL_LOCK_NODE_ERROR = "删除锁节点失败";
}
