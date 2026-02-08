package com.wxx.library.interceptor;


import com.wxx.library.annotation.AdminRequired;
import com.wxx.library.constant.SystemConstant;
import com.wxx.library.enums.ResultCode;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.util.JwtUtil;
import com.wxx.library.util.RedisUtil;
import com.wxx.library.util.Result;
import com.wxx.library.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * JWT拦截器
 */
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 前置拦截（请求处理前执行）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 1. 放行OPTIONS请求（跨域预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 非Controller方法直接放行（如Swagger文档接口）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 3. 获取请求头中的Token
        String headerValue = request.getHeader(jwtUtil.getHeader());
        String token = jwtUtil.getTokenFromHeader(headerValue);

        // 4. Token校验
        if (!StringUtils.hasText(token)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            UserContext.clear();
            return false;
        }

        // 5. 验证Token有效性（签名+过期时间）
        if (!jwtUtil.verifyToken(token)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            UserContext.clear();
            return false;
        }

        // 6.校验Token是否在黑名单中
        String blacklistKey1 = SystemConstant.REDIS_TOKEN_BLACKLIST_KEY + ":" + token;
        Boolean isBlacklisted1 = redisUtil.hasKey(blacklistKey1);
        if (Boolean.TRUE.equals(isBlacklisted1)) {
            log.warn("拦截到黑名单Token请求，接口：{}", request.getRequestURI());
            writeErrorResponse(response, ResultCode.UNAUTHORIZED, "Token已失效，请重新登录");
            UserContext.clear();
            return false;
        }

        // 7. 解析Token中的用户信息，存入线程上下文(供后续业务使用)
        Long userId = jwtUtil.getUserIdFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);
        UserContext.setUserId(userId);
        UserContext.setRole(role);

        // 8. 角色权限校验（如果方法有@AdminRequired注解，必须是管理员）
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        AdminRequired adminRequired = handlerMethod.getMethodAnnotation(AdminRequired.class);
        if (adminRequired != null && !role.equals(UserRoleEnum.ADMIN.getCode())) { // 1-管理员
            writeErrorResponse(response, ResultCode.FORBIDDEN);
            UserContext.clear();
            return false;
        }

        log.info("用户ID：{}，角色：{}，请求接口：{}", userId, role.equals(UserRoleEnum.ADMIN.getCode()) ? "管理员" : "普通用户", request.getRequestURI());
        return true;
    }

    /**
     * 后置处理（请求处理后执行，异常时不执行）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理ThreadLocal中的用户信息（避免内存泄漏）
        UserContext.clear();
    }

    // writeErrorResponse方法：支持默认信息和自定义错误信息
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) {
        writeErrorResponse(response, resultCode, resultCode.getMessage());
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode, String message) {
        try {
            //设置响应格式为JSON, 编码 UTF-8
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(resultCode.getCode());  //设置响应状态码
            Result<?> result = Result.error(resultCode.getCode(), message);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(objectMapper.writeValueAsString(result));
                writer.flush();
            }
        } catch (IOException e) {
            log.error("JWT拦截器写入错误响应失败", e);
        }
    }
}
