package com.wxx.library.user.controller;

import com.wxx.library.common.annotation.AdminRequired;
import com.wxx.library.common.constant.SystemConstant;
import com.wxx.library.common.dto.UserLoginDTO;
import com.wxx.library.common.dto.UserRegisterDTO;
import com.wxx.library.common.entity.User;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.util.JwtUtil;
import com.wxx.library.common.vo.LoginVO;
import com.wxx.library.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理接口")
@Validated
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody UserRegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }

    @Operation(summary = "用户登录", description = "返回JWT双令牌")
    @PostMapping("/login")
    public Result<?> login(@Validated @RequestBody UserLoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization") String authorization,
                            @RequestParam("refreshToken") String refreshToken) {
        String accessToken = jwtUtil.getTokenFromHeader(authorization);
        if (!StringUtils.hasText(accessToken)) {
            return Result.error("登出失败，Token格式错误");
        }
        // refreshToken已经是请求参数，不需要从Header中提取
        if (!StringUtils.hasText(refreshToken)) {
            return Result.error("登出失败，刷新Token格式错误");
        }
        boolean success = userService.logout(accessToken, refreshToken);
        return success ? Result.success("登出成功") : Result.error("登出失败");
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/auth/refresh-token")
    public Result<String> refreshToken(@RequestBody Map<String, String> params) {
        String refreshToken = params.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error("刷新令牌不能为空");
        }
        String blacklistKey = SystemConstant.REDIS_TOKEN_BLACKLIST_KEY + ":" + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            return Result.error("刷新令牌已失效，请重新登录");
        }
        try {
            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
            return Result.success(newAccessToken);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<?> getCurrentUser(HttpServletRequest request) {
        Long userId = extractUserIdFromHeader(request);
        return userService.getCurrentUser(userId);
    }

    @Operation(summary = "根据ID查询用户（Feign调用）")
    @GetMapping("/info/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "根据手机号查询用户（Feign调用）")
    @GetMapping("/phone/{phone}")
    public Result<User> getUserByPhone(@PathVariable String phone) {
        return userService.getUserByPhone(phone);
    }

    @Operation(summary = "更新用户状态（管理员）")
    @PutMapping("/status/{userId}/{status}")
    @AdminRequired
    public Result<?> updateUserStatus(@PathVariable Long userId, @PathVariable Integer status) {
        return userService.updateUserStatus(userId, status);
    }

    private Long extractUserIdFromHeader(HttpServletRequest request) {
        String userIdHeader = request.getHeader(SystemConstant.HEADER_USER_ID);
        if (StringUtils.hasText(userIdHeader)) {
            return Long.parseLong(userIdHeader);
        }
        return -1L;
    }
}
