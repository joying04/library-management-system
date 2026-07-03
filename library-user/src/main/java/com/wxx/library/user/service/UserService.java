package com.wxx.library.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wxx.library.common.dto.UserLoginDTO;
import com.wxx.library.common.dto.UserRegisterDTO;
import com.wxx.library.common.entity.User;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.vo.LoginVO;

public interface UserService extends IService<User> {

    Result<User> register(UserRegisterDTO registerDTO);

    Result<LoginVO> login(UserLoginDTO loginDTO);

    Result<User> getCurrentUser(Long userId);

    Result<User> getUserById(Long userId);

    Result<User> getUserByPhone(String phone);

    Result<Boolean> updateUserStatus(Long userId, Integer status);

    boolean logout(String accessToken, String refreshToken);
}
