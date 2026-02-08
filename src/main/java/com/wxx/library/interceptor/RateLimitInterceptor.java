package com.wxx.library.interceptor;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxx.library.annotation.RateLimit;
import com.wxx.library.util.RedisUtil;
import com.wxx.library.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(com.wxx.library.annotation.RateLimit.class);
        if (rateLimit == null) {
            return true;
        }
        // 限流key：IP+接口路径
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        String key = "rate_limit:" + ip + ":" + path;
        if (redisUtil.isRateLimited(key, rateLimit.maxCount(), rateLimit.period())) {
            response.setContentType("application/json;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(Result.error("请求过于频繁，请稍后重试")));
            writer.flush();
            writer.close();
            return false;
        }
        return true;
    }
}
