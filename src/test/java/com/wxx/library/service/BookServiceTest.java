package com.wxx.library.service;


import com.wxx.library.base.BaseTest;
import com.wxx.library.entity.Book;
import com.wxx.library.enums.ResultCode;
import com.wxx.library.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 图书服务测试
 */
public class BookServiceTest extends BaseTest {

    @Autowired
    private BookService bookService;

    // 初始化公共测试数据
    @BeforeEach
    void setUp() {
        initCommonData();
    }

    // ==================== 核心测试 ====================
    // 测试正常库存扣减（乐观锁生效）
    @Test
    void testDecreaseStockWithVersion_Success() {
        Book book = bookMapper.selectById(testBookId);
        Integer originalVersion = book.getVersion();
        Integer originalStock = book.getStock();

        boolean result = bookService.decreaseStockWithVersion(testBookId, originalVersion);

        assertTrue(result);
        Book updatedBook = bookMapper.selectById(testBookId);
        assertEquals(originalStock - 1, updatedBook.getStock());
        assertEquals(originalVersion + 1, updatedBook.getVersion());
    }

    // ==================== 参数化测试 ====================
    /**
     * 参数化测试：不同库存场景的扣减结果
     * @param stock 初始库存
     * @param expected 是否扣减成功
     */
    @ParameterizedTest
    @CsvSource({"5, true", "0, false", "1, true", "-1, false"})
    void testDecreaseStockWithVersion_Param(Integer stock, boolean expected) {
        // 准备测试数据
        Book book = bookMapper.selectById(testBookId);
        book.setStock(stock);
        bookMapper.updateById(book);

        // 执行测试并断言
        if (expected) {
            assertTrue(bookService.decreaseStockWithVersion(testBookId, book.getVersion()));
        } else {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookService.decreaseStockWithVersion(testBookId, book.getVersion());
            });
            assertEquals(ResultCode.BOOK_STOCK_EMPTY.getMessage(), exception.getMessage());
        }
    }

    // ==================== 边缘场景1：库存为0时并发借阅 ====================
    @Test
    void testDecreaseStock_Concurrent_StockEmpty() throws InterruptedException {
        // 1. 初始化库存为0
        Book book = bookMapper.selectById(testBookId);
        book.setStock(0);
        bookMapper.updateById(book);
        Integer originalVersion = book.getVersion();

        // 2. 模拟2个线程同时借阅
        Thread thread1 = new Thread(() -> {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookService.decreaseStockWithVersion(testBookId, originalVersion);
            });
            assertEquals(ResultCode.BOOK_STOCK_EMPTY.getMessage(), exception.getMessage());
        });

        Thread thread2 = new Thread(() -> {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookService.decreaseStockWithVersion(testBookId, originalVersion);
            });
            assertEquals(ResultCode.BOOK_STOCK_EMPTY.getMessage(), exception.getMessage());
        });

        // 3. 启动线程并等待执行完成
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // 4. 断言最终库存仍为0（无超卖）
        Book updatedBook = bookMapper.selectById(testBookId);
        assertEquals(0, updatedBook.getStock());
        assertEquals(originalVersion, updatedBook.getVersion()); // 版本号未变（未更新成功）
    }
}
