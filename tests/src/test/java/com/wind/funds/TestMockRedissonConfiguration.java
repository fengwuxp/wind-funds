package com.wind.funds;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Redisson 测试基础设施。
 *
 * <p>服务层流程测试只替换 Redis 这类外部运行环境，业务内部链路仍由 Spring 注入真实 Bean。</p>
 */
@Configuration(proxyBeanMethods = false)
public class TestMockRedissonConfiguration {

    private static final String REDIS_HOST = "127.0.0.1";

    private static final String REDIS_ADDRESS_TEMPLATE = "redis://" + REDIS_HOST + ":%d";

    private static final int REDIS_CONNECTION_POOL_SIZE = 4;

    private static final int REDIS_CONNECTION_MINIMUM_IDLE_SIZE = 1;

    private static final int REDIS_START_ATTEMPTS = 8;

    private static final RedisServer REDIS_SERVER;

    private static final int REDIS_PORT;

    static {
        StartedRedis startedRedis = startRedisServer();
        REDIS_SERVER = startedRedis.redisServer();
        REDIS_PORT = startedRedis.port();
        Runtime.getRuntime().addShutdownHook(new Thread(TestMockRedissonConfiguration::stopRedisServer));
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

    private static StartedRedis startRedisServer() {
        IOException lastException = null;
        for (int attempt = 0; attempt < REDIS_START_ATTEMPTS; attempt++) {
            RedisServer redisServer = null;
            try {
                int redisPort = findAvailablePort();
                redisServer = buildRedisServer(redisPort);
                redisServer.start();
                return new StartedRedis(redisServer, redisPort);
            } catch (IOException e) {
                suppressStopFailure(redisServer, e);
                lastException = e;
            }
        }
        throw new IllegalStateException("Failed to start embedded Redis for tests", lastException);
    }

    private static RedisServer buildRedisServer(int redisPort) throws IOException {
        return RedisServer.newRedisServer()
                .bind(REDIS_HOST)
                .port(redisPort)
                .setting("save \"\"")
                .setting("appendonly no")
                .onShutdownForceStop(true)
                .build();
    }

    private static int findAvailablePort() throws IOException {
        InetAddress address = InetAddress.getByName(REDIS_HOST);
        try (ServerSocket socket = new ServerSocket(0, 0, address)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void suppressStopFailure(RedisServer redisServer, IOException startException) {
        if (redisServer == null || !redisServer.isActive()) {
            return;
        }
        try {
            redisServer.stop();
        } catch (IOException e) {
            startException.addSuppressed(e);
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

    private record StartedRedis(RedisServer redisServer, int port) {
    }
}
