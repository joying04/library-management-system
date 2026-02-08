package com.wxx.library.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.wxx.library.dto.UserLoginDTO;
import com.wxx.library.dto.UserRegisterDTO;
import com.wxx.library.entity.User;
import com.wxx.library.util.Result;
import com.wxx.library.vo.LoginVO;

/**
 * 用户服务接口（实习生必会Service分层）
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    Result<User> register(UserRegisterDTO registerDTO);

    /**
     * 用户登录（生成JWT Token）
     */
    Result<LoginVO> login(UserLoginDTO loginDTO);

    /**
     * 获取当前登录用户信息
     */
    Result<User> getCurrentUser();

    /**
     * 禁用/启用用户（仅管理员）
     */
    Result<Boolean> updateUserStatus(Long userId, Integer status);

    /**
     * 登出功能
     */
    boolean logout(String accessToken,String refreshToken);
}
