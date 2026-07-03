package com.wxx.library.common.constant;

/**
 * 系统自定义常量类（统一管理常量，避免硬编码）
 */
public class SystemConstant {

    // ============================ Redis缓存Key常量 ============================
    public static final String REDIS_TOKEN_BLACKLIST_KEY = "library:token:blacklist"; // Token黑名单Key
    public static final String REDIS_REFRESH_TOKEN_KEY = "library:refresh:token"; // 刷新Token Key
    public static final String REDIS_HOT_BOOKS_KEY = "library:hot:books";  // 热门图书缓存Key
    public static final String REDIS_LOCK_BORROW_KEY = "library:lock:borrow"; // 借阅分布式锁Key
    public static final String BOOK_CACHE_KEY_PREFIX = "book:detail:";  // 图书详情缓存Key前缀

    // ============================ 分页默认参数 ============================
    public static final Integer DEFAULT_PAGE_NUM = 1;  // 默认页码
    public static final Integer DEFAULT_PAGE_SIZE = 10; // 默认每页条数

    // ============================ 借阅相关参数 ============================
    public static final Integer BORROW_DAYS = 30;  // 默认借阅天数
    public static final Integer MAX_RENEW_COUNT = 1;  // 最大续借次数
    public static final Integer RENEW_DAYS = 30;  // 续借延长天数
    public static final Integer MAX_BORROW_COUNT = 5;  // 默认最大借阅数量

    // ============================ 网关传递的请求头 ============================
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_PHONE = "X-User-Phone";
}
