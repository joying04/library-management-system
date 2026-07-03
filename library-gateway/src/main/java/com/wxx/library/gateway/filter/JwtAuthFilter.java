package com.wxx.library.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxx.library.common.constant.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT鉴权全局过滤器
 * 功能：验证Token、校验黑名单、提取用户信息并传递到下游服务
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.prefix:Bearer}")
    private String jwtPrefix;

    @Value("${gateway.auth.white-list:/api/user/login,/api/user/register,/api/user/auth/refresh-token,/api/book/list,/api/book/detail/**,/api/book/hot,/swagger-ui/**,/v3/api-docs/**}")
    private String whiteList;

    public JwtAuthFilter(ReactiveStringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单接口直接放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("[Gateway] 请求路径: {}, Authorization Header: {}", path, authHeader);
        
        String token = extractToken(authHeader);
        if (!StringUtils.hasText(token)) {
            log.warn("[Gateway] Token提取失败, path={}", path);
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "未登录或Token已过期");
        }

        // 3. 验证Token签名+过期
        if (!verifyToken(token)) {
            log.warn("[Gateway] Token验证失败, token前缀={}, path={}", token.substring(0, Math.min(20, token.length())), path);
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token无效或已过期");
        }

        // 4. 检查Token是否在黑名单中
        String blacklistKey = SystemConstant.REDIS_TOKEN_BLACKLIST_KEY + ":" + token;
        return stringRedisTemplate.hasKey(blacklistKey)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token已失效，请重新登录");
                    }

                    // 5. 解析用户信息，通过请求头传递到下游服务
                    DecodedJWT jwt = JWT.decode(token);
                    Long userId = jwt.getClaim("userId").asLong();
                    Integer role = jwt.getClaim("role").asInt();
                    String phone = jwt.getClaim("phone").asString();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> r.header(SystemConstant.HEADER_USER_ID, String.valueOf(userId))
                                    .header(SystemConstant.HEADER_USER_ROLE, String.valueOf(role))
                                    .header(SystemConstant.HEADER_USER_PHONE, phone))
                            .build();

                    log.debug("JWT鉴权通过：userId={}, role={}, path={}", userId, role, path);
                    return chain.filter(mutatedExchange);
                });
    }

    @Override
    public int getOrder() {
        return -1; // 最高优先级
    }

    /**
     * 判断是否为白名单接口
     */
    private boolean isWhiteList(String path) {
        String[] paths = whiteList.split(",");
        for (String pattern : paths) {
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求头提取Token
     */
    private String extractToken(String authHeader) {
        log.debug("[Gateway] extractToken - authHeader: {}", authHeader);
        if (!StringUtils.hasText(authHeader)) {
            log.warn("[Gateway] authHeader 为空");
            return null;
        }
        // 支持 "Bearer " 或 "Bearer" 两种格式
        if (authHeader.startsWith(jwtPrefix + " ")) {
            String token = authHeader.substring(jwtPrefix.length() + 1).trim();
            log.debug("[Gateway] 提取 Token (带空格), 长度: {}", token.length());
            return token;
        } else if (authHeader.startsWith(jwtPrefix)) {
            String token = authHeader.substring(jwtPrefix.length()).trim();
            log.debug("[Gateway] 提取 Token (不带空格), 长度: {}", token.length());
            return token;
        }
        log.warn("[Gateway] authHeader 格式错误, prefix: {}", jwtPrefix);
        return null;
    }

    /**
     * 验证Token
     */
    private boolean verifyToken(String token) {
        try {
            log.debug("[Gateway] 开始验证 Token, secret 前缀: {}", jwtSecret.substring(0, Math.min(10, jwtSecret.length())));
            JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
            log.debug("[Gateway] Token 验证成功");
            return true;
        } catch (Exception e) {
            log.error("[Gateway] Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 写入错误响应
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body;
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("code", status.value());
            result.put("message", message);
            result.put("data", null);
            result.put("timestamp", System.currentTimeMillis());
            body = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            body = "{\"code\":500,\"message\":\"系统异常\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
