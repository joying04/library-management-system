package com.wxx.library.common.constant;

/**
 * Redis 常量
 */
public class RedisConstant {

    /**
     * Token 黑名单 Key 前缀
     */
    public static final String REDIS_TOKEN_BLACKLIST_KEY = "library:token:blacklist";

    /**
     * 刷新 Token Key 前缀
     */
    public static final String REDIS_REFRESH_TOKEN_KEY = "library:refresh:token";

    /**
     * 热门图书 ZSet Key
     */
    public static final String REDIS_HOT_BOOKS_KEY = "library:hot:books";

    /**
     * 借阅分布式锁 Key 前缀
     */
    public static final String REDIS_LOCK_BORROW_KEY = "library:lock:borrow";

    /**
     * 用户信息缓存 Key 前缀
     */
    public static final String REDIS_USER_INFO_KEY = "library:user:info";

    /**
     * 图书信息缓存 Key 前缀
     */
    public static final String REDIS_BOOK_INFO_KEY = "library:book:info";
}
