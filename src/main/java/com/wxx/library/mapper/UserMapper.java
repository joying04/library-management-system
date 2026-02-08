package com.wxx.library.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxx.library.entity.User;

/**
 * 用户Mapper（MP BaseMapper，无需写基础CRUD）
 */

public interface UserMapper extends BaseMapper<User> {
    // 基础CRUD由MP自动生成，复杂查询可添加自定义方法
}
