package com.wxx.library.vo;


import com.wxx.library.entity.User;
import lombok.Data;

/**
 * 登录响应VO（返回给前端的登录结果）
 */

@Data
public class LoginVO {
    private String token; // 访问令牌
    private String refreshToken; // 刷新令牌
    private Long expireTime; // 访问令牌过期时间（秒）
    private Long refreshExpireTime; // 刷新令牌过期时间（秒）
    private User user; // 用户信息
}
