package com.wxx.library.constant;


/**
 * 系统自定义常量类（统一管理常量，避免硬编码）
 */
public class SystemConstant {

    // ============================ Redis缓存Key常量 ============================
    public static final String REDIS_TOKEN_BLACKLIST_KEY = "library:token:blacklist"; // Token黑名单Key（登出时使用）
    public static final String hotBookKey = "library:hot:books";  // 热门图书缓存Key
    public static final String BOOK_CACHE_KEY_PREFIX = "book:detail:";  //缓存key前缀

    // ============================ 分页默认参数 ============================
    public static final Integer DEFAULT_PAGE_NUM = 1;  // 默认页码
    public static final Integer DEFAULT_PAGE_SIZE = 10; // 默认每页条数

    // ============================ 续借相关参数 ============================
    public static final Integer MAX_RENEW_TIMES = 1;  // 最大续借次数
    public static final Integer RENEW_DAYS = 30;  // 续借延长天数

}
