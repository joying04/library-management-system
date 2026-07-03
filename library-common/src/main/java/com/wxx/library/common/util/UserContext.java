package com.wxx.library.common.util;

import com.wxx.library.common.enums.UserRoleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户线程上下文（防内存泄漏+防空指针+防复用污染）
 */
public class UserContext {
    private static final Logger log = LoggerFactory.getLogger(UserContext.class);

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> USER_ROLE = new ThreadLocal<>();

    // ==================== Setter ====================
    public static void setUserId(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("尝试设置无效用户ID：{}，忽略该操作", userId);
            return;
        }
        clearUserId();
        USER_ID.set(userId);
    }

    public static void setRole(Integer role) {
        if (role == null || UserRoleEnum.getByCode(role) == null) {
            log.warn("尝试设置无效用户角色：{}，忽略该操作", role);
            return;
        }
        clearRole();
        USER_ROLE.set(role);
    }

    // ==================== Getter ====================
    public static Long getUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            return -1L;
        }
        return userId;
    }

    public static Integer getRole() {
        Integer role = USER_ROLE.get();
        if (role == null) {
            return -1;
        }
        return role;
    }

    // ==================== 清理 ====================
    public static void clearUserId() {
        USER_ID.remove();
    }

    public static void clearRole() {
        USER_ROLE.remove();
    }

    public static void clear() {
        USER_ID.remove();
        USER_ROLE.remove();
    }
}
