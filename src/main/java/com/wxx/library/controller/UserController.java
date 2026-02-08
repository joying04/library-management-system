package com.wxx.library.controller;


import com.wxx.library.annotation.RateLimit;
import com.wxx.library.constant.SystemConstant;
import com.wxx.library.dto.UserLoginDTO;
import com.wxx.library.dto.UserRegisterDTO;
import com.wxx.library.annotation.AdminRequired;
import com.wxx.library.service.UserService;
import com.wxx.library.util.JwtUtil;
import com.wxx.library.util.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器（含Swagger接口文档）
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理接口")
@Validated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "用户注册", description = "用户注册，手机号唯一")
    @PostMapping("/register")
    public Result<?> register(
            @Parameter(description = "注册参数", required = true)
            @Validated @RequestBody UserRegisterDTO registerDTO
    ) {
        return userService.register(registerDTO);
    }

    @Operation(summary = "用户登录", description = "手机号+密码登录，返回JWT 双令牌（accessToken+refreshToken）")
    @PostMapping("/login")
    @RateLimit(maxCount = 10,period = 60)  //1分钟最多10次请求
    public Result<?> login(
            @Parameter(description = "登录参数", required = true)
            @Validated @RequestBody UserLoginDTO loginDTO
    ) {
        return userService.login(loginDTO);
    }

    /**
     * 登出接口：前端携带访问Token请求，加入黑名单后失效
     * @param authorization 请求头Authorization，格式：Bearer + 空格 + 访问Token
     * @return 登出响应
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "登出后当前访问Token和刷新Token均失效")
    @RateLimit(maxCount = 10,period = 60)
    public Result<?> logout(
            @RequestHeader(value = "${jwt.header}") String authorization,
            @RequestParam(value = "refreshToken") String refreshToken
    ) {
        // 提取访问Token
        String accessToken = jwtUtil.getTokenFromHeader(authorization);
        if (!StringUtils.hasText(accessToken)) {
            log.info("登出失败：请求头Token格式错误");
            return Result.error("登出失败，Token格式错误");
        }
        // 提取刷新Token（去除Bearer前缀）
        String realRefreshToken = jwtUtil.getRefreshTokenFromHeader(refreshToken);
        if (!StringUtils.hasText(realRefreshToken)) {
            log.info("登出失败：刷新Token格式错误");
            return Result.error("登出失败，刷新Token格式错误");
        }
        // 调用Service执行登出
        boolean success = userService.logout(accessToken, realRefreshToken);
        if (success) {
            return Result.success("登出成功，所有令牌已失效");
        } else {
            return Result.error("登出失败，请稍后重试");
        }
    }

    // 刷新访问令牌接口
    @Operation(summary = "刷新访问令牌", description = "accessToken过期时，用refreshToken获取新令牌")
    @PostMapping("/auth/refresh-token")
    @RateLimit(maxCount = 10,period = 3600)
    public Result<String> refreshToken(
            @Parameter(description = "刷新令牌", required = true)
            @RequestBody Map<String, String> params
    ) {
        //1.非空校验
        String refreshToken = params.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error("刷新令牌不能为空");
        }

        //2.黑名单校验
        String blacklistKey = SystemConstant.REDIS_TOKEN_BLACKLIST_KEY + ":" + refreshToken;
        if(redisTemplate.hasKey(blacklistKey)) {
            return Result.error("刷新令牌已失效，请重新登入");
        }
        //3.签名/过期校验
        try {
            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
            return Result.success(newAccessToken);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取当前登录用户信息", description = "需要登录，返回不含密码的用户信息")
    @GetMapping("/current")
    @RateLimit(maxCount = 30,period = 60)
    public Result<?> getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Operation(summary = "更新用户状态", description = "仅管理员可操作，0-禁用，1-启用")
    @PutMapping("/status/{userId}/{status}")
    @RateLimit(maxCount = 5,period = 60)
    @AdminRequired // 管理员权限注解
    public Result<?> updateUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "状态：0-禁用，1-启用", required = true)
            @PathVariable Integer status
    ) {
        return userService.updateUserStatus(userId, status);
    }
}
