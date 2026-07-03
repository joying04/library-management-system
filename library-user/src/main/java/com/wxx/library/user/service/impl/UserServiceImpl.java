package com.wxx.library.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxx.library.common.constant.SystemConstant;
import com.wxx.library.common.dto.UserLoginDTO;
import com.wxx.library.common.dto.UserRegisterDTO;
import com.wxx.library.common.entity.User;
import com.wxx.library.common.enums.UserRoleEnum;
import com.wxx.library.common.enums.UserStatusEnum;
import com.wxx.library.common.exception.BusinessException;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.result.ResultCode;
import com.wxx.library.common.util.JwtUtil;
import com.wxx.library.common.util.PasswordUtil;
import com.wxx.library.common.vo.LoginVO;
import com.wxx.library.user.mapper.UserMapper;
import com.wxx.library.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.expire}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-expire}")
    private Long refreshTokenExpire;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<User> register(UserRegisterDTO registerDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, registerDTO.getPhone());
        User existUser = getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }

        String encryptedPassword = PasswordUtil.encryptPassword(registerDTO.getPassword());

        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(encryptedPassword);
        user.setRole(UserRoleEnum.COMMON.getCode());
        user.setStatus(UserStatusEnum.ENABLE.getCode());

        boolean saveSuccess = save(user);
        if (!saveSuccess) {
            throw new BusinessException("用户注册失败");
        }

        user.setPassword(null);
        log.info("用户注册成功，手机号：{}", registerDTO.getPhone());
        return Result.success(user);
    }

    @Override
    public Result<LoginVO> login(UserLoginDTO loginDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, loginDTO.getPhone());
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        boolean passwordMatch = PasswordUtil.checkPassword(loginDTO.getPassword(), user.getPassword());
        if (!passwordMatch) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        if (user.getStatus().equals(UserStatusEnum.DISABLE.getCode())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getPhone(), user.getRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setExpireTime(accessTokenExpire / 1000);
        loginVO.setRefreshExpireTime(refreshTokenExpire / 1000);
        user.setPassword(null);
        loginVO.setUser(user);

        log.info("用户登录成功，用户ID：{}，手机号：{}", user.getId(), loginDTO.getPhone());
        return Result.success(loginVO);
    }

    @Override
    public boolean logout(String accessToken, String refreshToken) {
        try {
            handleTokenBlacklist(accessToken);
            if (StringUtils.hasText(refreshToken)) {
                handleTokenBlacklist(refreshToken);
            }
            return true;
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return false;
        }
    }

    private void handleTokenBlacklist(String token) {
        if (!jwtUtil.verifyToken(token)) {
            return;
        }
        Date expirationDate = jwtUtil.getExpirationDate(token);
        long remainMilliseconds = expirationDate.getTime() - System.currentTimeMillis();
        if (remainMilliseconds <= 0) {
            return;
        }
        String blacklistKey = SystemConstant.REDIS_TOKEN_BLACKLIST_KEY + ":" + token;
        redisTemplate.opsForValue().set(blacklistKey, "invalid", remainMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public Result<User> getCurrentUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<User> getUserById(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<User> getUserByPhone(String phone) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateUserStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        boolean updateSuccess = lambdaUpdate()
                .set(User::getStatus, status)
                .eq(User::getId, userId)
                .update();

        if (!updateSuccess) {
            throw new BusinessException("更新用户状态失败");
        }

        log.info("用户状态更新成功，用户ID：{}，新状态：{}", userId, status);
        return Result.success(true);
    }
}
