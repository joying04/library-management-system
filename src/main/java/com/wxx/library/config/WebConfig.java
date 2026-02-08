package com.wxx.library.config;

import com.wxx.library.interceptor.JwtInterceptor;
import com.wxx.library.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置（拦截器注册、跨域配置）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    /**
     * 注册限流/JWT拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //限流拦截器
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/register",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        //JWT拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 拦截所有接口
                .excludePathPatterns( // 白名单（无需登录）
                        "/user/login",
                        "/user/register",
                        "/user/auth/refresh-token",
                        "/book/list",
                        "/book/detail/**",
                        "/book/hot",
                        "/swagger-ui/**",
                        "/v3/api-docs/**" // Swagger文档接口
                );
    }

    /**
     * 跨域配置（解决前端跨域问题）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 允许所有来源（生产环境需指定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 预检请求缓存时间
    }
}
