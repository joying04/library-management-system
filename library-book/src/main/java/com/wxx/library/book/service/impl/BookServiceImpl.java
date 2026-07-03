package com.wxx.library.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.common.constant.SystemConstant;
import com.wxx.library.common.dto.BookQueryDTO;
import com.wxx.library.common.entity.Book;
import com.wxx.library.common.result.ResultCode;
import com.wxx.library.common.exception.BusinessException;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.vo.BookVO;
import com.wxx.library.book.mapper.BookMapper;
import com.wxx.library.book.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookServiceImpl implements BookService {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<Page<BookVO>> getBookPage(BookQueryDTO queryDTO) {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(queryDTO.getBookName()), Book::getName, queryDTO.getBookName())
                .like(StringUtils.hasText(queryDTO.getAuthor()), Book::getAuthor, queryDTO.getAuthor())
                .eq(StringUtils.hasText(queryDTO.getIsbn()), Book::getIsbn, queryDTO.getIsbn())
                .eq(queryDTO.getCategoryId() != null, Book::getCategoryId, queryDTO.getCategoryId())
                .eq(queryDTO.getStatus() != null, Book::getStatus, queryDTO.getStatus())
                .orderByDesc(Book::getCreateTime);

        Page<Book> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        Page<Book> bookPage = bookMapper.selectPage(page, queryWrapper);

        // 转换为 VO
        Page<BookVO> voPage = new Page<>();
        BeanUtils.copyProperties(bookPage, voPage, "records");
        voPage.setRecords(bookPage.getRecords().stream()
                .map(book -> {
                    BookVO vo = new BookVO();
                    BeanUtils.copyProperties(book, vo);
                    vo.setBorrowable(book.getStockCount() > 0);
                    return vo;
                })
                .collect(Collectors.toList()));

        return Result.success(voPage);
    }

    @Override
    public Result<BookVO> getBookById(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        vo.setBorrowable(book.getStockCount() > 0);
        return Result.success(vo);
    }

    @Override
    public Result<List<BookVO>> getHotBooks() {
        // 从 Redis 缓存获取热门图书 ID 列表
        String cacheKey = SystemConstant.REDIS_HOT_BOOKS_KEY;
        Set<String> hotBookIds = stringRedisTemplate.opsForZSet().reverseRange(cacheKey, 0, 9);
        
        List<BookVO> hotBooks = new ArrayList<>();
        if (hotBookIds != null && !hotBookIds.isEmpty()) {
            for (String bookIdStr : hotBookIds) {
                Long bookId = Long.parseLong(bookIdStr);
                Book book = bookMapper.selectById(bookId);
                if (book != null) {
                    BookVO vo = new BookVO();
                    BeanUtils.copyProperties(book, vo);
                    vo.setBorrowable(book.getStockCount() > 0);
                    hotBooks.add(vo);
                }
            }
        }
        
        // 如果缓存为空，从数据库查询
        if (hotBooks.isEmpty()) {
            List<Book> books = bookMapper.selectHotBooks(10);
            hotBooks = books.stream()
                    .map(book -> {
                        BookVO vo = new BookVO();
                        BeanUtils.copyProperties(book, vo);
                        vo.setBorrowable(book.getStockCount() > 0);
                        return vo;
                    })
                    .collect(Collectors.toList());
            
            // 写入缓存
            for (int i = 0; i < hotBooks.size(); i++) {
                stringRedisTemplate.opsForZSet().add(cacheKey, String.valueOf(hotBooks.get(i).getId()), i);
            }
            stringRedisTemplate.expire(cacheKey, 24, TimeUnit.HOURS);
        }
        
        return Result.success(hotBooks);
    }

    @Override
    public Result<Book> addBook(Book book) {
        book.setBorrowCount(0);
        book.setStatus(1);
        bookMapper.insert(book);
        return Result.success(book);
    }

    @Override
    public Result<Boolean> updateBook(Book book) {
        bookMapper.updateById(book);
        // 清除缓存
        stringRedisTemplate.delete(SystemConstant.REDIS_HOT_BOOKS_KEY);
        return Result.success(true);
    }

    @Override
    public Result<Boolean> deleteBook(Long id) {
        bookMapper.deleteById(id);
        stringRedisTemplate.delete(SystemConstant.REDIS_HOT_BOOKS_KEY);
        return Result.success(true);
    }

    // ==================== Feign 接口实现 ====================

    @Override
    public Result<Book> getBookByIdForFeign(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            return Result.fail(ResultCode.BOOK_NOT_FOUND);
        }
        return Result.success(book);
    }

    @Override
    public Result<Integer> getBookStock(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            return Result.fail(ResultCode.BOOK_NOT_FOUND);
        }
        return Result.success(book.getStockCount());
    }

    @Override
    public Result<Boolean> decreaseStock(Long bookId, Integer version) {
        int rows = bookMapper.decreaseStock(bookId, version);
        if (rows > 0) {
            // 清除热门图书缓存
            stringRedisTemplate.delete(SystemConstant.REDIS_HOT_BOOKS_KEY);
            return Result.success(true);
        }
        return Result.fail(ResultCode.STOCK_NOT_ENOUGH);
    }

    @Override
    public Result<Boolean> increaseBorrowCount(Long bookId) {
        int rows = bookMapper.increaseBorrowCount(bookId);
        if (rows > 0) {
            // 更新 Redis 中的热门图书评分
            String cacheKey = SystemConstant.REDIS_HOT_BOOKS_KEY;
            stringRedisTemplate.opsForZSet().incrementScore(cacheKey, String.valueOf(bookId), 1);
            stringRedisTemplate.expire(cacheKey, 24, TimeUnit.HOURS);
            return Result.success(true);
        }
        return Result.fail(ResultCode.ERROR);
    }

    @Override
    public Result<Boolean> increaseStock(Long bookId) {
        int rows = bookMapper.increaseStock(bookId);
        if (rows > 0) {
            // 清除热门图书缓存
            stringRedisTemplate.delete(SystemConstant.REDIS_HOT_BOOKS_KEY);
            return Result.success(true);
        }
        return Result.fail(ResultCode.ERROR);
    }
}
