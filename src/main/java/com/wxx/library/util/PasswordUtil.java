package com.wxx.library.util;


import org.mindrot.jbcrypt.BCrypt;
import org.springframework.util.StringUtils;

/**
 * 密码加密工具类（BCrypt加密，不可逆，安全可靠）
 */
public class PasswordUtil {

    /**
     * 加密密码（生成随机盐）
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        // BCrypt.gensalt()：生成盐值，默认10轮哈希（轮数越多越安全，性能越低）
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encryptedPassword 加密后的密码
     * @return 验证结果（true-匹配，false-不匹配）
     */
    public static boolean checkPassword(String rawPassword, String encryptedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encryptedPassword)) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encryptedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
