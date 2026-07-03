package com.wxx.library.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * JWT工具类
 */
public class JwtUtil {

    private final String secret;
    private final Long expire;
    private final Long refreshExpire;
    private final String header;
    private final String prefix;

    public JwtUtil(String secret, Long expire, Long refreshExpire, String header, String prefix) {
        this.secret = secret;
        this.expire = expire;
        this.refreshExpire = refreshExpire;
        this.header = header;
        this.prefix = prefix;
    }

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
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String phone, Integer role) {
        Date now = new Date();
        Date refreshExpireDate = new Date(now.getTime() + refreshExpire);

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("phone", phone)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(refreshExpireDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 验证Token有效性
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
     * 解析Token获取过期时间
     */
    public Date getExpirationDate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new RuntimeException("Token不能为空");
        }
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt();
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
     * 从Token中解析手机号
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
     * 刷新访问令牌
     */
    public String refreshAccessToken(String refreshToken) {
        if (!verifyToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }
        Long userId = getUserIdFromToken(refreshToken);
        String phone = getPhoneFromToken(refreshToken);
        Integer role = getRoleFromToken(refreshToken);
        return generateToken(userId, phone, role);
    }

    /**
     * 从请求头中获取Token
     */
    public String getTokenFromHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue) || !headerValue.startsWith(prefix)) {
            return null;
        }
        return headerValue.substring(prefix.length()).trim();
    }

    public String getHeader() {
        return header;
    }

    public String getSecret() {
        return secret;
    }

    public Long getExpire() {
        return expire;
    }

    public Long getRefreshExpire() {
        return refreshExpire;
    }
}
