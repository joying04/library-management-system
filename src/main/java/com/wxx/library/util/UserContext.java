package com.wxx.library.util;


import com.wxx.library.enums.UserRoleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户线程上下文（防内存泄漏+防空指针+防复用污染）
 */
public class UserContext {
    // 私有静态日志（避免日志打印线程安全问题）
    private static final Logger log = LoggerFactory.getLogger(UserContext.class);

    // 1. 存储用户ID（初始值为null，通过工具方法控制访问）
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    // 2. 存储用户角色（0-普通用户，1-管理员）
    private static final ThreadLocal<Integer> USER_ROLE = new ThreadLocal<>();

    // ==================== Setter ====================
    /**
     * 设置用户ID（线程安全：校验非空，避免无效值注入）
     * @param userId 用户ID（不为null）
     */
    public static void setUserId(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("尝试设置无效用户ID：{}，忽略该操作", userId);
            return;
        }
        // 设值前先清理旧值（防御线程复用导致的旧数据污染）
        clearUserId();
        USER_ID.set(userId);
        log.debug("线程[{}]设置用户ID：{}", Thread.currentThread().getId(), userId);
    }

    /**
     * 设置用户角色（线程安全：校验合法值，避免越界）
     * @param role 用户角色（0-普通用户，1-管理员，不为null）
     */
    public static void setRole(Integer role) {
        if (role == null || UserRoleEnum.getByCode(role) == null) {
            log.warn("尝试设置无效用户角色：{}，忽略该操作（仅支持0-普通用户/1-管理员）", role);
            return;
        }
        // 设值前先清理旧值
        clearRole();
        USER_ROLE.set(role);
        log.debug("线程[{}]设置用户角色：{}", Thread.currentThread().getId(), role);
    }

    // ==================== Getter ====================
    /**
     * 获取用户ID（线程安全：返回默认值，避免空指针）
     * @return 用户ID（未设置时返回-1，而非null）
     */
    public static Long getUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            log.debug("线程[{}]未设置用户ID，返回默认值-1", Thread.currentThread().getId());
            return -1L; // 用-1表示“未登录/未设置”，避免业务层空指针
        }
        return userId;
    }

    /**
     * 获取用户角色（线程安全：返回默认值，避免空指针）
     * @return 用户角色（未设置时返回-1，而非null）
     */
    public static Integer getRole() {
        Integer role = USER_ROLE.get();
        if (role == null) {
            log.debug("线程[{}]未设置用户角色，返回默认值-1", Thread.currentThread().getId());
            return -1; // 用-1表示“未登录/未设置”
        }
        return role;
    }

    // ==================== 防内存泄漏 ====================
    /**
     * 清理用户ID（单独清理，支持部分场景复用）
     */
    public static void clearUserId() {
        Long userId = USER_ID.get();
        if (userId != null) {
            USER_ID.remove();
            log.debug("线程[{}]清理用户ID：{}", Thread.currentThread().getId(), userId);
        }
    }

    /**
     * 清理用户角色（单独清理，支持部分场景复用）
     */
    public static void clearRole() {
        Integer role = USER_ROLE.get();
        if (role != null) {
            USER_ROLE.remove();
            log.debug("线程[{}]清理用户角色：{}", Thread.currentThread().getId(), role);
        }
    }

    /**
     * 全量清理上下文（核心方法：请求结束必须调用，防内存泄漏）
     * 强制清理，无论是否有值，避免遗漏
     */
    public static void clear() {
        log.debug("线程[{}]开始全量清理用户上下文", Thread.currentThread().getId());
        // 强制remove（即使get()为null，remove()也不会报错，更安全）
        USER_ID.remove();
        USER_ROLE.remove();
        log.debug("线程[{}]全量清理用户上下文完成", Thread.currentThread().getId());
    }
}