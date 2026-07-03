package com.wxx.library.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.util.StringUtils;

/**
 * 密码加密工具类（BCrypt加密，不可逆）
 */
public class PasswordUtil {

    /**
     * 加密密码
     */
    public static String encryptPassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
    }

    /**
     * 验证密码
     */
    public static boolean checkPassword(String rawPassword, String encryptedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encryptedPassword)) {
            return false;
        }
        try {
            return org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, encryptedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
