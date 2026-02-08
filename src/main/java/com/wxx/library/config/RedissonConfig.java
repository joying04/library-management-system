package com.wxx.library.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;
    /**
     * 配置RedissonClient客户端，提供Redis 高级功能支持
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        // 本地 Redis 配置
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(30) // 连接池大小，优化并发性能
                .setConnectionMinimumIdleSize(10)  //最小空闲连接数
                .setConnectTimeout(3000); // 连接超时时间（毫秒）
        return Redisson.create(config);
    }
}
