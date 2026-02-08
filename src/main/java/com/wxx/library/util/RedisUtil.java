package com.wxx.library.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ============================ 通用操作 ============================
    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除key（支持多个）
     */
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
            e.printStackTrace();
            return false;
        }
    }

    // ============================ String操作 ============================
    /**
     * 获取String值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        return value == null ? null : clazz.cast(value);
    }

    /**
     * 设置String值（无过期时间）
     */
    public Boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置String值（带过期时间）
     */
    public Boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================ Hash操作 ============================
    /**
     * 获取Hash中的某个字段值
     */
    public Object hGet(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 设置Hash中的某个字段值
     */
    public Boolean hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================ List操作 ============================
    /**
     * 获取List列表
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置List列表（覆盖原有）
     */
    public Boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 限流核心方法
     * @param key 限流唯一标识（格式：rate_limit:IP:接口路径）
     * @param maxCount 周期内最大请求数（如 100 表示1分钟最多100次）
     * @param period 限流周期（单位：秒，如 60 表示1分钟）
     * @return true-已限流（请求次数超出限制），false-未限流（请求次数在限制内）
     */
    public boolean isRateLimited(String key, int maxCount, long period) {
        try {
            // 1. Redis自增计数（key不存在时自动创建，初始值1）
            Long currentCount = redisTemplate.opsForValue().increment(key, 1);
            if (currentCount == null) {
                log.warn("Redis限流计数失败，key：{}", key);
                return false; // 异常时放行，避免影响正常业务
            }

            // 2. 第一次请求时，设置key过期时间（与限流周期一致）
            if (currentCount == 1) {
                redisTemplate.expire(key, period, TimeUnit.SECONDS);
                log.debug("Redis限流key创建成功，key：{}，过期时间：{}秒", key, period);
            }

            // 3. 判断当前请求数是否超出最大限制
            boolean isLimited = currentCount > maxCount;
            if (isLimited) {
                log.warn("触发限流：key={}，当前请求数={}，最大限制={}，周期={}秒",
                        key, currentCount, maxCount, period);
            } else {
                log.debug("未触发限流：key={}，当前请求数={}，最大限制={}，周期={}秒",
                        key, currentCount, maxCount, period);
            }

            return isLimited;
        } catch (Exception e) {
            log.error("Redis限流判断异常，key：{}", key, e);
            return false; // 异常时放行，避免雪崩
        }
    }

}
