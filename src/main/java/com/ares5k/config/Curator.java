package com.ares5k.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基于 curator实现的 zookeeper分布式锁 Aop组件(锁类型: 互斥锁, 读锁, 写锁, 可重入锁(同一路径的锁同一线程可重入))
 * <p>
 * zookeeper java客户端-curator 的 Bean配置类
 *
 * @author ares5k
 * @since 2020-10-29
 * qq: 16891544
 * email: 16891544@qq.com
 */
@Configuration
public class Curator {

    /**
     * zookeeper服务器地址
     */
    private static final String ZOOKEEPER_SERVER_ADDRESS = "192.168.3.88:2181";

    /**
     * 连接 zookeeper服务器并返回 curator客户端对象
     *
     * @return zookeeper客户端-curator
     * @author ares5k
     */
    @Bean
    public CuratorFramework curatorClient() {
        //创建 zookeeper 客户端 - curator
        CuratorFramework client = CuratorFrameworkFactory
                .builder()
                //zookeeper服务器地址
                .connectString(ZOOKEEPER_SERVER_ADDRESS)
                //连接超时, 当指定时间内仍未连接服务器成功, 就会抛出连接异常
                .sessionTimeoutMs(5000)
                //断线重试机制
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                //生成客户端
                .build();

        //连接 zookeeper服务器
        client.start();
        //zookeeper客户端-curator
        return client;
    }
}
