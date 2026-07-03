package com.wxx.library.common.feign;

import com.wxx.library.common.entity.User;
import com.wxx.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端
 * 供借阅服务远程调用获取用户信息
 */
@FeignClient(name = "library-user", path = "/api/user")
public interface UserFeignClient {

    /**
     * 根据ID查询用户信息
     */
    @GetMapping("/info/{userId}")
    Result<User> getUserById(@PathVariable("userId") Long userId);

    /**
     * 根据手机号查询用户信息（内部调用）
     */
    @GetMapping("/phone/{phone}")
    Result<User> getUserByPhone(@PathVariable("phone") String phone);
}
