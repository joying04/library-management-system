package com.wxx.library.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Slf4j
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey error: {}", key, e);
            return false;
        }
    }

    public Boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis expire error: {}", key, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Boolean delete(String... key) {
        try {
            if (key != null && key.length > 0) {
                if (key.length == 1) {
                    redisTemplate.delete(key[0]);
                } else {
                    redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Redis delete error", e);
            return false;
        }
    }

    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        return value == null ? null : clazz.cast(value);
    }

    public Boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set error: {}", key, e);
            return false;
        }
    }

    public Boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis set with TTL error: {}", key, e);
            return false;
        }
    }

    /**
     * 限流核心方法
     */
    public boolean isRateLimited(String key, int maxCount, long period) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key, 1);
            if (currentCount == null) {
                return false;
            }
            if (currentCount == 1) {
                redisTemplate.expire(key, period, TimeUnit.SECONDS);
            }
            boolean isLimited = currentCount > maxCount;
            if (isLimited) {
                log.warn("触发限流：key={}，当前请求数={}，最大限制={}", key, currentCount, maxCount);
            }
            return isLimited;
        } catch (Exception e) {
            log.error("Redis限流判断异常，key：{}", key, e);
            return false;
        }
    }
}
