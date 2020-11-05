package com.ares5k.process;

import com.ares5k.exception.LockException;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 公平锁, 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * 分布式锁处理接口
 *
 * @author ares5k
 * @since 2020-10-30
 * qq: 16891544
 * email: 16891544@qq.com
 */
public interface DistributeLockProcess {

    /**
     * 锁节点的根路径
     */
    String ROOT_PATH = "/lock";

    /**
     * 拿锁
     *
     * @param timeWait 当拿锁失败时的等待时间
     * @return 是否拿到锁
     * @throws LockException 设置锁节点失败异常
     * @author ares5k
     */
    boolean acquire(long timeWait) throws LockException;

    /**
     * 释放锁
     *
     * @throws LockException 删除锁节点失败异常
     * @author ares5k
     */
    void release() throws LockException;

}
