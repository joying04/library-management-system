package com.wxx.library.service;

import com.wxx.library.base.BaseTest;
import com.wxx.library.entity.Book;
import com.wxx.library.entity.BorrowRecord;
import com.wxx.library.enums.BorrowStatusEnum;
import com.wxx.library.enums.ResultCode;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.exception.BusinessException;
import com.wxx.library.mapper.BorrowMapper;
import com.wxx.library.util.Result;
import com.wxx.library.util.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 借阅服务测试
 */
public class BorrowServiceTest extends BaseTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BorrowMapper borrowMapper;

    // 初始化公共数据
    @BeforeEach
    void setUp() {
        initCommonData();
        UserContext.setUserId(testUserId);
        UserContext.setRole(UserRoleEnum.COMMON.getCode());
    }

    // ==================== 核心测试 ====================
    @Test
    void testBorrowBook_Success() {
        Result<BorrowRecord> result = borrowService.borrowBook(testBookId);
        assertEquals(200, result.getCode());
        assertEquals(testBookId, result.getData().getBookId());
        assertEquals(BorrowStatusEnum.BORROWED.getCode(), result.getData().getStatus());
    }

    // ==================== 参数化测试 ====================
    /**
     * 参数化测试：不同借阅数量的结果
     * @param borrowedCount 已借阅数量
     * @param expected 是否借阅成功
     */
    @ParameterizedTest
    @CsvSource({"-1, false", "0, true", "4, true", "5, false"}) // maxBorrowCount=5
    void testBorrowBook_Param(Integer borrowedCount, boolean expected) {
        // 模拟已借阅数量（插入对应数量的借阅记录）
        for (int i = 0; i < borrowedCount; i++) {
            BorrowRecord record = new BorrowRecord();
            record.setUserId(testUserId);
            record.setBookId(testBookId + i + 1); // 用不同图书ID避免冲突
            record.setStatus(BorrowStatusEnum.BORROWED.getCode());
            record.setBorrowTime(LocalDateTime.now());
            record.setExpectedReturnTime(LocalDateTime.now().plusDays(30));
            borrowMapper.insert(record);
        }

        // 执行测试并断言
        if (expected) {
            Result<BorrowRecord> result = borrowService.borrowBook(testBookId);
            assertEquals(200, result.getCode());
        } else {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                borrowService.borrowBook(testBookId);
            });
            assertEquals(ResultCode.BORROW_COUNT_LIMIT.getMessage(), exception.getMessage());
        }
    }

    // ==================== 核心场景：并发借阅 ====================
    @Test
    void testBorrowBook_Concurrent() throws InterruptedException {
        // 1. 初始化库存为1
        Book book = bookMapper.selectById(testBookId);
        book.setStock(1);
        bookMapper.updateById(book);

        // 2. 模拟2个线程同时借阅
        Thread thread1 = new Thread(() -> {
            Result<BorrowRecord> result = borrowService.borrowBook(testBookId);
            assertEquals(200, result.getCode());
        });

        Thread thread2 = new Thread(() -> {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                borrowService.borrowBook(testBookId);
            });
            assertEquals(ResultCode.BOOK_STOCK_EMPTY.getMessage(), exception.getMessage());
        });

        // 3. 启动线程并等待执行完成
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // 4. 断言最终库存为0，无超卖
        Book updatedBook = bookMapper.selectById(testBookId);
        assertEquals(0, updatedBook.getStock());
    }
}
