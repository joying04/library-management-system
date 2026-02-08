package com.wxx.library.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxx.library.dto.BookQueryDTO;
import com.wxx.library.entity.Book;
import com.wxx.library.util.Result;
import com.wxx.library.vo.BookVO;

import java.util.List;

/**
 * 图书服务接口
 */
public interface BookService extends IService<Book> {

    /**
     * 图书分页查询（支持多条件）
     */
    Result<Page<BookVO>> getBookPage(BookQueryDTO queryDTO);

    /**
     * 根据ID查询图书详情
     */
    Result<BookVO> getBookById(Long id);

    /**
     * 获取热门图书（借阅次数前10，Redis缓存）
     */
    Result<List<BookVO>> getHotBooks();

    /**
     * 新增图书（仅管理员）
     */
    Result<Book> addBook(Book book);

    /**
     * 更新图书信息（仅管理员）
     */
    Result<Boolean> updateBook(Book book);

    /**
     * 删除图书（逻辑删除，仅管理员）
     */
    Result<Boolean> deleteBook(Long id);

    /**
     * 更新图书库存（内部调用，事务中使用）
     */
    boolean updateStock(Long bookId, Integer change);

    /**
     * 乐观锁扣减库存
     * @param bookId
     * @param version
     * @return
     */
    boolean decreaseStockWithVersion(Long bookId, Integer version);

}
