package com.wxx.library.user.config;

import com.wxx.library.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    @Value("${jwt.refresh-expire}")
    private Long refreshExpire;

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.prefix:Bearer}")
    private String prefix;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expire, refreshExpire, header, prefix);
    }
}
