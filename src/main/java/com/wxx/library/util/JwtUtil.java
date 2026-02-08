package com.wxx.library.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret; // 密钥

    @Value("${jwt.expire}")
    private Long expire; // 访问令牌过期时间（毫秒，如3600000=1小时）

    @Value("${jwt.refresh-expire}")
    private Long refreshExpire; // 刷新令牌过期时间（毫秒，如86400000=24小时）

    @Value("${jwt.header}")
    private String header; // 请求头名称（如Authorization）

    @Value("${jwt.prefix}")
    private String prefix; // Token前缀（如Bearer ）

    /**
     * 生成访问令牌
     */
    public String generateToken(Long userId, String phone, Integer role) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expire);

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("phone", phone)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 生成刷新令牌（独立过期时间，载荷与访问令牌一致）
     */
    public String generateRefreshToken(Long userId, String phone, Integer role) {
        Date now = new Date();
        Date refreshExpireDate = new Date(now.getTime() + refreshExpire);

        // 刷新令牌仅过期时间不同，其他载荷与访问令牌一致，便于解析
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("phone", phone)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(refreshExpireDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 刷新访问令牌
     * 逻辑：验证刷新令牌有效性 → 解析用户信息 → 生成新的访问令牌
     */
    public String refreshAccessToken(String refreshToken) {
        // 1. 验证刷新令牌是否有效（非空+签名正确+未过期）
        if (!verifyToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        // 2. 从刷新令牌中解析用户核心信息
        Long userId = getUserIdFromToken(refreshToken);
        String phone = getPhoneFromToken(refreshToken);
        Integer role = getRoleFromToken(refreshToken);

        // 3. 生成新的访问令牌（沿用原有生成逻辑）
        return generateToken(userId, phone, role);
    }

    /**
     * 验证Token有效性（同时支持访问令牌和刷新令牌验证）
     */
    public boolean verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析Token获取过期时间（登出时计算黑名单过期时间用）
     */
    public Date getExpirationDate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new RuntimeException("Token不能为空");
        }
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt(); // 获取Token的过期时间
        } catch (JWTDecodeException e) {
            throw new RuntimeException("Token解析过期时间失败", e);
        }
    }

    /**
     * 从Token中解析用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userId").asLong();
        } catch (JWTDecodeException e) {
            throw new RuntimeException("Token解析用户ID失败");
        }
    }

    /**
     * 从Token中解析角色
     */
    public Integer getRoleFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("role").asInt();
        } catch (JWTDecodeException e) {
            throw new RuntimeException("Token解析角色失败");
        }
    }

    /**
     * 从Token中解析手机号（用于刷新令牌时复用载荷）
     */
    public String getPhoneFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("phone").asString();
        } catch (JWTDecodeException e) {
            throw new RuntimeException("Token解析手机号失败");
        }
    }

    /**
     * 从请求头中获取Token（去掉前缀）
     */
    public String getTokenFromHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue) || !headerValue.startsWith(prefix)) {
            return null;
        }
        return headerValue.substring(prefix.length()).trim();
    }

    /**
     * 从请求头获取刷新令牌
     */
    public String getRefreshTokenFromHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue) || !headerValue.startsWith(prefix)) {
            return null;
        }
        return headerValue.substring(prefix.length()).trim();
    }

    // getter方法（供拦截器使用）
    public String getHeader() {
        return header;
    }

    public String getSecret() {
        return secret;
    }
}