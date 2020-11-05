package com.ares5k.exception;

import lombok.Getter;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 公平锁, 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * 分布式锁处理异常类
 *
 * @author ares5k
 * @since 2020-10-30
 * qq: 16891544
 * email: 16891544@qq.com
 */
public class LockException extends Exception {

    /**
     * 指定时间内未能获取分布式锁
     */
    public static final String CAN_NOT_GET_LOCK_IN_TIME = "指定时间内未能获取分布式锁";

    /**
     * 错误编码
     */
    @Getter
    private LockErrorCode lockErrorCode;

    /**
     * 调用父类构造
     *
     * @param message       错误信息
     * @param lockErrorCode 错误编码
     * @author ares5k
     */
    public LockException(String message, LockErrorCode lockErrorCode) {
        super(message);
        this.lockErrorCode = lockErrorCode;
    }

    /**
     * 调用父类构造
     *
     * @param message   错误信息
     * @param throwable 错误原因
     * @author ares5k
     */
    public LockException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * 错误编码枚举类
     *
     * @author ares5k
     */
    public enum LockErrorCode {
        ERROR_IN_TIME
    }
}
