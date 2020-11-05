package com.ares5k.process.impl;

import com.ares5k.exception.LockException;
import com.ares5k.process.DistributeLockProcess;
import com.ares5k.util.CuratorUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 公平锁, 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * 读锁处理类
 *
 * @author ares5k
 * @since 2020-10-30
 * qq: 16891544
 * email: 16891544@qq.com
 */
public class ReadLockProcess implements DistributeLockProcess {

    /**
     * curator提供的分布式锁
     */
    private final InterProcessReadWriteLock readWriteLock;

    /**
     * 写锁被占用时, 仍可以获取读锁的构造方法
     *
     * @param writeLockProcess 读写锁对象
     * @author ares5k
     */
    public ReadLockProcess(WriteLockProcess writeLockProcess) {
        this.readWriteLock = writeLockProcess.getReadWriteLock();
    }

    /**
     * 构造方法
     *
     * @param businessPath  需要锁住的 zookeeper路径
     * @param curatorClient zookeeper客户端
     * @author ares5k
     */
    public ReadLockProcess(String businessPath, CuratorFramework curatorClient) {
        this.readWriteLock = new InterProcessReadWriteLock(curatorClient, ROOT_PATH + businessPath);
    }

    /**
     * 拿锁
     *
     * @param timeWait 当拿锁失败时的等待时间
     * @return 是否拿到锁
     * @throws LockException 设置锁节点失败异常
     * @author ares5k
     */
    @Override
    public boolean acquire(long timeWait) throws LockException {
        return CuratorUtil.acquire(this.readWriteLock.readLock(), timeWait);
    }

    /**
     * 释放锁
     *
     * @throws LockException 删除锁节点失败异常
     * @author ares5k
     */
    @Override
    public void release() throws LockException {
        CuratorUtil.release(this.readWriteLock.readLock());
    }
}
