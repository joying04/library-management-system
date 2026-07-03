package com.wxx.library.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxx.library.common.entity.BookRenew;
import com.wxx.library.common.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BorrowMapper extends BaseMapper<BorrowRecord> {
}
