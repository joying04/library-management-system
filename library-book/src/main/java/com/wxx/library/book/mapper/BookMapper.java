package com.wxx.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxx.library.common.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 查询热门图书
     */
    List<Book> selectHotBooks(@Param("limit") Integer limit);

    /**
     * 扣减库存（乐观锁）
     */
    int decreaseStock(@Param("bookId") Long bookId, @Param("version") Integer version);

    /**
     * 增加借阅次数
     */
    int increaseBorrowCount(@Param("bookId") Long bookId);

    /**
     * 增加库存
     */
    int increaseStock(@Param("bookId") Long bookId);
}
