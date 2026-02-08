package com.wxx.library.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxx.library.constant.SystemConstant;
import com.wxx.library.enums.ResultCode;
import com.wxx.library.dto.UserLoginDTO;
import com.wxx.library.dto.UserRegisterDTO;
import com.wxx.library.entity.User;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.enums.UserStatusEnum;
import com.wxx.library.exception.BusinessException;
import com.wxx.library.mapper.UserMapper;
import com.wxx.library.service.UserService;
import com.wxx.library.util.JwtUtil;
import com.wxx.library.util.PasswordUtil;
import com.wxx.library.util.Result;
import com.wxx.library.util.UserContext;
import com.wxx.library.vo.LoginVO;
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

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${jwt.expire}")
    private Long accessTokenExpire; // Token过期时间（毫秒）

    @Value("${jwt.refresh-expire}")
    private Long refreshTokenExpire;

    /**
     * 用户注册（事务保证原子性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<User> register(UserRegisterDTO registerDTO) {
        // 1. 校验手机号是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, registerDTO.getPhone());
        User existUser = getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }

        // 2. 密码加密（BCrypt）
        String encryptedPassword = PasswordUtil.encryptPassword(registerDTO.getPassword());

        // 3. 构建User实体
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(encryptedPassword);
        user.setRole(UserRoleEnum.COMMON.getCode()); // 默认普通用户
        user.setStatus(UserStatusEnum.ENABLE.getCode()); // 默认正常状态

        // 4. 保存用户（MP的save方法）
        boolean saveSuccess = save(user);
        if (!saveSuccess) {
            throw new BusinessException("用户注册失败");
        }

        // 5. 隐藏密码，返回结果
        user.setPassword(null);
        log.info("用户注册成功，手机号：{}", registerDTO.getPhone());
        return Result.success(user);
    }

    /**
     * 用户登录（生成JWT Token）
     */
    @Override
    public Result<LoginVO> login(UserLoginDTO loginDTO) {
        // 1. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, loginDTO.getPhone());
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);  //用户不存在
        }

        // 2. 校验密码
        boolean passwordMatch = PasswordUtil.checkPassword(loginDTO.getPassword(), user.getPassword());
        if (!passwordMatch) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);  //密码错误
        }

        // 3. 校验用户状态
        if (user.getStatus().equals(UserStatusEnum.DISABLE.getCode())) {
            throw new BusinessException(ResultCode.USER_DISABLED);  //此用户已被禁用
        }

        // 4. 生成双令牌（访问令牌 + 刷新令牌）
        String accessToken = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getPhone(), user.getRole());

        // 5. 构建登录响应VO
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(accessToken);  //访问令牌
        loginVO.setRefreshToken(refreshToken); // 刷新令牌
        loginVO.setExpireTime(accessTokenExpire / 1000); // 访问令牌过期时间（转成秒，前端更易处理）
        loginVO.setRefreshExpireTime(refreshTokenExpire / 1000); // 刷新令牌过期时间
        user.setPassword(null); // 隐藏密码
        loginVO.setUser(user);

        // 6. 日志打印
        log.info("用户登录成功，用户ID：{}，手机号：{}，角色：{}",
                user.getId(), loginDTO.getPhone(),
                user.getRole().equals(UserRoleEnum.ADMIN.getCode()) ? "管理员" : "普通用户");

        return Result.success(loginVO);
    }

    /**
     * 登出功能：将访问/刷新Token加入Redis黑名单（与Token过期时间一致）
     * @param accessToken 前端传入的访问/刷新Token（去除Bearer前缀后）
     * @return 登出结果
     */
    // 改造logout方法
    @Override
    public boolean logout(String accessToken, String refreshToken) {
        try {
            // 1. 拉黑访问令牌
            handleTokenBlacklist(accessToken);
            // 2. 拉黑刷新令牌（若存在）
            if (StringUtils.hasText(refreshToken)) {
                handleTokenBlacklist(refreshToken);
            }
            return true;
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return false;
        }
    }

    /**
     * 通用方法：将 Token 加入黑名单
     */
    private void handleTokenBlacklist(String token) {
        if (!jwtUtil.verifyToken(token)) {
            log.info("Token已无效，无需加入黑名单：{}", token);
            return;
        }
        Date expirationDate = jwtUtil.getExpirationDate(token);
        long remainMilliseconds = expirationDate.getTime() - System.currentTimeMillis();
        if (remainMilliseconds <= 0) {
            log.info("Token已过期，无需加入黑名单：{}", token);
            return;
        }
        String blacklistKey = SystemConstant.REDIS_TOKEN_BLACKLIST_KEY + ":" + token;
        redisTemplate.opsForValue().set(
                blacklistKey,
                "invalid",
                remainMilliseconds,
                TimeUnit.MILLISECONDS
        );
        log.info("Token加入黑名单成功，剩余有效期：{}秒", remainMilliseconds / 1000);
    }


    /**
     * 获取当前登录用户信息（从线程上下文获取用户ID）
     */
    @Override
    public Result<User> getCurrentUser() {
        Long userId = UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setPassword(null); // 隐藏密码
        return Result.success(user);
    }

    /**
     * 禁用/启用用户（仅管理员操作，MP更新）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateUserStatus(Long userId, Integer status) {
        // 1. 校验用户是否存在
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 2. 更新状态（MP的lambdaUpdate方法，条件更新）
        boolean updateSuccess = lambdaUpdate()
                .set(User::getStatus, status)
                .eq(User::getId, userId)
                .update();

        if (!updateSuccess) {
            throw new BusinessException("更新用户状态失败");
        }

        log.info("用户状态更新成功，用户ID：{}，新状态：{}", userId, status.equals(UserStatusEnum.ENABLE.getCode()) ? "正常" : "禁用");
        return Result.success(true);
    }
}

