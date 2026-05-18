package com.capte.funds;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Redisson 测试基础设施。
 *
 * <p>服务层流程测试只替换 Redis 这类外部运行环境，业务内部链路仍由 Spring 注入真实 Bean。</p>
 */
@Configuration(proxyBeanMethods = false)
public class TestMockRedissonConfiguration {

    private static final String REDIS_ADDRESS_TEMPLATE = "redis://127.0.0.1:%d";

    private static final int REDIS_CONNECTION_POOL_SIZE = 4;

    private static final int REDIS_CONNECTION_MINIMUM_IDLE_SIZE = 1;

    private static final RedisServer REDIS_SERVER;

    private static final int REDIS_PORT;

    static {
        try {
            REDIS_PORT = findAvailablePort();
            REDIS_SERVER = RedisServer.newRedisServer()
                    .bind("127.0.0.1")
                    .port(REDIS_PORT)
                    .setting("save \"\"")
                    .setting("appendonly no")
                    .onShutdownForceStop(true)
                    .build();
            REDIS_SERVER.start();
            Runtime.getRuntime().addShutdownHook(new Thread(TestMockRedissonConfiguration::stopRedisServer));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start embedded Redis for tests", e);
        }
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format(REDIS_ADDRESS_TEMPLATE, REDIS_PORT))
                .setDatabase(0)
                .setConnectionPoolSize(REDIS_CONNECTION_POOL_SIZE)
                .setConnectionMinimumIdleSize(REDIS_CONNECTION_MINIMUM_IDLE_SIZE);
        return Redisson.create(config);
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void stopRedisServer() {
        if (!REDIS_SERVER.isActive()) {
            return;
        }
        try {
            REDIS_SERVER.stop();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stop embedded Redis for tests", e);
        }
    }
}
