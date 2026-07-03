package com.wxx.library.common.vo;

import com.wxx.library.common.entity.User;
import lombok.Data;

@Data
public class LoginVO {
    private String token; // 访问令牌
    private String refreshToken; // 刷新令牌
    private Long expireTime; // 访问令牌过期时间（秒）
    private Long refreshExpireTime; // 刷新令牌过期时间（秒）
    private User user; // 用户信息
}
