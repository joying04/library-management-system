package com.wxx.library.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxx.library.common.entity.BorrowEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅事件Mapper
 */
@Mapper
public interface BorrowEventMapper extends BaseMapper<BorrowEvent> {
}
