package com.wxx.library.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.wxx.library.base.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT工具类测试
 */
public class JwtUtilTest extends BaseTest {

    @Autowired
    private JwtUtil jwtUtil;

    private final Long testUserId = 1L;
    private final String testPhone = "13800138000";
    private final Integer testRole = 0; // 普通用户

    // ==================== 核心测试 ====================
    @Test
    void testGenerateAndVerifyToken() {
        String token = jwtUtil.generateToken(testUserId, testPhone, testRole);
        assertNotNull(token);
        assertTrue(jwtUtil.verifyToken(token));

        Long userId = jwtUtil.getUserIdFromToken(token);
        String phone = jwtUtil.getPhoneFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);

        assertEquals(testUserId, userId);
        assertEquals(testPhone, phone);
        assertEquals(testRole, role);
    }

    // ==================== 边缘场景：Token过期 ====================
    @Test
    void testVerifyExpiredToken() throws InterruptedException {
        // 生成一个短期过期的Token（1秒过期）
        String shortExpireToken = JWT.create()
                .withClaim("userId", testUserId)
                .withClaim("phone", testPhone)
                .withClaim("role", testRole)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000)) // 1秒后过期
                .sign(Algorithm.HMAC256(jwtUtil.getSecret()));

        // 立即校验：有效
        assertTrue(jwtUtil.verifyToken(shortExpireToken));

        // 等待2秒，Token过期后校验：无效
        Thread.sleep(2000);
        assertFalse(jwtUtil.verifyToken(shortExpireToken));
    }

    // ==================== 核心测试：刷新Token ====================
    @Test
    void testRefreshAccessToken() {
        // 生成刷新Token
        String refreshToken = jwtUtil.generateRefreshToken(testUserId, testPhone, testRole);
        assertNotNull(refreshToken);

        // 用刷新Token生成新的访问Token
        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
        assertNotNull(newAccessToken);

        // 校验新Token有效性
        assertTrue(jwtUtil.verifyToken(newAccessToken));
        assertEquals(testUserId, jwtUtil.getUserIdFromToken(newAccessToken));
    }
}
