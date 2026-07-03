package com.wxx.library.book.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.common.dto.BookQueryDTO;
import com.wxx.library.common.entity.Book;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.vo.BookVO;

import java.util.List;

public interface BookService {
    
    Result<Page<BookVO>> getBookPage(BookQueryDTO queryDTO);
    
    Result<BookVO> getBookById(Long id);
    
    Result<List<BookVO>> getHotBooks();
    
    Result<Book> addBook(Book book);
    
    Result<Boolean> updateBook(Book book);
    
    Result<Boolean> deleteBook(Long id);
    
    // Feign 接口
    Result<Book> getBookByIdForFeign(Long id);
    Result<Integer> getBookStock(Long id);
    Result<Boolean> decreaseStock(Long bookId, Integer version);
    Result<Boolean> increaseBorrowCount(Long bookId);
    Result<Boolean> increaseStock(Long bookId);
}
