package com.wxx.library.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxx.library.entity.BookRenew;
import org.apache.ibatis.annotations.Param;

/**
 * 图书续借Mapper
 */

public interface BookRenewMapper extends BaseMapper<BookRenew> {

    /**
     * 统计用户某本图书的续借次数（限制最多续借1次）
     */
    Integer countRenewTimes(@Param("userId") Long userId, @Param("bookId") Long bookId);

}
