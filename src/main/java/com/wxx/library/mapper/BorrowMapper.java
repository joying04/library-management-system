package com.wxx.library.mapper;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.entity.BorrowRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 借阅Mapper（含自定义关联查询方法）
 */

public interface BorrowMapper extends BaseMapper<BorrowRecord> {

    /**
     * 分页查询借阅记录（关联用户表和图书表）
     * @param page 分页对象
     * @param wrapper 查询条件
     * @return 分页结果（含用户名和图书名称）
     */
    Page<BorrowRecord> selectBorrowRecordPage(
            Page<BorrowRecord> page,
            @Param(Constants.WRAPPER) Wrapper<BorrowRecord> wrapper
    );

    // 更新借阅记录的预计归还时间（续借用）
    int updateExpectedReturnTime(@Param("id") Long borrowId,
                                 @Param("newExpectedReturnTime") LocalDateTime newExpectedReturnTime,
                                 @Param("version") Integer version,
                                 @Param("userId") Long userId,
                                 @Param("bookId") Long bookId); // 乐观锁版本号

    // 根据ID查询借阅记录（含版本号，用于乐观锁更新）
    BorrowRecord selectByIdWithVersion(@Param("id") Long id);
}

