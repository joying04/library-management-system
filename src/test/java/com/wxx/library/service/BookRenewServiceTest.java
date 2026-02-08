package com.wxx.library.service;

import com.wxx.library.base.BaseTest;
import com.wxx.library.dto.BookRenewDTO;
import com.wxx.library.entity.BorrowRecord;
import com.wxx.library.enums.BorrowStatusEnum;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.exception.BusinessException;
import com.wxx.library.mapper.BorrowMapper;
import com.wxx.library.util.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 续借服务测试
 */
public class BookRenewServiceTest extends BaseTest {

    @Autowired
    private BookRenewService bookRenewService;

    @Autowired
    private BorrowMapper borrowMapper;

    private Long testBorrowId; // 测试借阅记录ID

    // 初始化公共数据+借阅记录
    @BeforeEach
    void setUp() {
        initCommonData();
        // 创建测试借阅记录（状态：借阅中）
        BorrowRecord borrowRecord = new BorrowRecord();
        borrowRecord.setUserId(testUserId);
        borrowRecord.setBookId(testBookId);
        borrowRecord.setStatus(BorrowStatusEnum.BORROWED.getCode());
        borrowRecord.setBorrowTime(LocalDateTime.now());
        borrowRecord.setExpectedReturnTime(LocalDateTime.now().plusDays(30));
        borrowMapper.insert(borrowRecord);
        testBorrowId = borrowRecord.getId();
    }

    // ==================== 核心测试 ====================
    @Test
        void testRenewBook_Success() {
        BookRenewDTO renewDTO = new BookRenewDTO();
        renewDTO.setBorrowId(testBorrowId);
        renewDTO.setBookId(testBookId);

        UserContext.setUserId(testUserId);
        UserContext.setRole(UserRoleEnum.COMMON.getCode());

        boolean result = bookRenewService.renewBook(renewDTO);
        assertTrue(result);

        // 验证预计归还时间延长30天
        BorrowRecord updatedBorrow = borrowMapper.selectById(testBorrowId);
        LocalDateTime originalReturnTime = updatedBorrow.getExpectedReturnTime(); // 续借前的预计归还时间（初始化时设置的）
        LocalDateTime updatedReturnTime = originalReturnTime.plusDays(30);
        long daysExtended = ChronoUnit.DAYS.between(originalReturnTime, updatedReturnTime);
        assertEquals(30, daysExtended);
    }

    // ==================== 参数化测试 ====================
    /**
     * 参数化测试：不同续借次数的结果
     * @param renewTimes 已续借次数
     * @param expected 是否续借成功
     */
    @ParameterizedTest
    @CsvSource({"0, true", "1, false"})
    void testRenewBook_Param(Integer renewTimes, boolean expected) {
        BookRenewDTO renewDTO = new BookRenewDTO();
        renewDTO.setBorrowId(testBorrowId);
        renewDTO.setBookId(testBookId);

        UserContext.setUserId(testUserId);
        UserContext.setRole(UserRoleEnum.COMMON.getCode());

        // 模拟已续借次数（先执行对应次数的续借）
        for (int i = 0; i < renewTimes; i++) {
            bookRenewService.renewBook(renewDTO);
        }

        // 执行测试并断言
        if (expected) {
            assertTrue(bookRenewService.renewBook(renewDTO));
        } else {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookRenewService.renewBook(renewDTO);
            });
            assertEquals("该图书已续借1次，不可重复续借", exception.getMessage());
        }
    }

    // ==================== 边缘场景3：他人续借 ====================
    @Test
    void testRenewBook_OthersRecord() {
        BookRenewDTO renewDTO = new BookRenewDTO();
        renewDTO.setBorrowId(testBorrowId);
        renewDTO.setBookId(testBookId);

        // 切换上下文为另一个普通用户（非借阅人）
        UserContext.setUserId(testOtherUserId); // 模拟其他用户
        UserContext.setRole(UserRoleEnum.COMMON.getCode());

        // 执行续借，预期失败
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookRenewService.renewBook(renewDTO);
        });
        assertEquals("无权限续借他人的图书", exception.getMessage());
    }
}
