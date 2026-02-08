package com.wxx.library.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置
 * 解决Redis存储对象乱码问题，支持JSON序列化
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);  //绑定Redis连接工厂

        // Jackson2JsonRedisSerializer序列化对象
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化规则：所有字段可见、支持多类型
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jacksonSerializer.setObjectMapper(objectMapper);  //把配置好的ObjectMapper绑定到Jackson序列化器

        // String序列化（key用String，value用Jackson）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        //绑定序列化器到RedisTemplate
        template.setKeySerializer(stringSerializer);  //key用String序列化
        template.setHashKeySerializer(stringSerializer);  //Hash类型的key也用String序列化
        template.setValueSerializer(jacksonSerializer);   //value用JSON序列化
        template.setHashValueSerializer(jacksonSerializer); //Hash类型的value也用JSON序列化

        //触发RedisTemplate的初始化流程
        template.afterPropertiesSet();
        return template;
    }
}