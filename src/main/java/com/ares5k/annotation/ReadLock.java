package com.ares5k.annotation;

import java.lang.annotation.*;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 公平锁, 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * 读锁注解
 *
 * @author ares5k
 * @since 2020-10-30
 * qq: 16891544
 * email: 16891544@qq.com
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadLock {

    /**
     * 需要锁住的 zookeeper路径
     *
     * @author ares5k
     */
    String bizPath();

    /**
     * 当未能获取锁时的等待时间
     * 在等待时间内如果持锁节点释放了锁, 则会触发 curator 监听, 然后会重新判断自己是否排到了锁
     * 传值(单位毫秒):
     * 1. 传正数代表等待时长
     * 2. 传负数代表无线等待
     * 3. 传 0代表不等待
     *
     * @author ares5k
     */
    long waitTime() default 0;

}
