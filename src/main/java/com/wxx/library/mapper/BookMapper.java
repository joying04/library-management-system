package com.wxx.library.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxx.library.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图书Mapper
 */

public interface BookMapper extends BaseMapper<Book> {
    // 乐观锁扣减库存：where条件包含bookId和version，确保原子性
    int decreaseStockWithVersion(@Param("bookId") Long bookId, @Param("version") Integer version);

    // 乐观锁更新库存
    int updateStockWithVersion(@Param("bookId") Long bookId, @Param("change") Integer change, @Param("version") Integer version);
}
