package com.wxx.library.borrow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.common.constant.SystemConstant;
import com.wxx.library.common.dto.BookRenewDTO;
import com.wxx.library.common.dto.BorrowQueryDTO;
import com.wxx.library.common.entity.Book;
import com.wxx.library.common.entity.BookRenew;
import com.wxx.library.common.entity.BorrowRecord;
import com.wxx.library.common.entity.User;
import com.wxx.library.common.result.ResultCode;
import com.wxx.library.common.exception.BusinessException;
import com.wxx.library.common.feign.BookFeignClient;
import com.wxx.library.common.feign.UserFeignClient;
import com.wxx.library.common.result.Result;
import com.wxx.library.borrow.mapper.BookRenewMapper;
import com.wxx.library.borrow.mapper.BorrowMapper;
import com.wxx.library.borrow.rabbitmq.BorrowMessageProducer;
import com.wxx.library.borrow.service.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private BookRenewMapper bookRenewMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private BookFeignClient bookFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BorrowMessageProducer messageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> borrowBook(Long userId, Long bookId) {
        // 1. 获取分布式锁
        String lockKey = SystemConstant.REDIS_LOCK_BORROW_KEY + ":" + bookId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 2. 尝试加锁（最多等待3秒，锁10秒后自动释放）
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ResultCode.SYSTEM_BUSY);
            }

            // 3. 查询用户信息（Feign调用）
            Result<User> userResult = userFeignClient.getUserById(userId);
            log.info("[借阅] Feign调用User服务返回: code={}, message={}, data={}", 
                    userResult.getCode(), userResult.getMessage(), userResult.getData());
            if (!ResultCode.SUCCESS.getCode().equals(userResult.getCode())) {
                log.error("[借阅] 用户不存在, userId={}, userResult={}", userId, userResult);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }
            User user = userResult.getData();

            // 4. 检查是否有逾期未还
            LambdaQueryWrapper<BorrowRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BorrowRecord::getUserId, userId)
                    .eq(BorrowRecord::getStatus, 1) // 借阅中
                    .lt(BorrowRecord::getExpectedReturnTime, LocalDateTime.now());
            Long overdueCount = borrowMapper.selectCount(queryWrapper);
            if (overdueCount > 0) {
                throw new BusinessException(ResultCode.USER_HAS_OVERDUE);
            }

            // 5. 检查借阅数量限制
            LambdaQueryWrapper<BorrowRecord> borrowingQuery = new LambdaQueryWrapper<>();
            borrowingQuery.eq(BorrowRecord::getUserId, userId)
                    .eq(BorrowRecord::getStatus, 1);
            Long borrowingCount = borrowMapper.selectCount(borrowingQuery);
            if (borrowingCount >= user.getMaxBorrowCount()) {
                throw new BusinessException(ResultCode.BORROW_COUNT_LIMIT);
            }

            // 6. 查询图书库存（Feign调用）
            Result<Integer> stockResult = bookFeignClient.getBookStock(bookId);
            if (!ResultCode.SUCCESS.getCode().equals(stockResult.getCode()) || stockResult.getData() <= 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }

            // 7. 扣减库存（Feign调用，乐观锁）
            Book book = bookFeignClient.getBookByIdForFeign(bookId).getData();
            Result<Boolean> decreaseResult = bookFeignClient.decreaseStock(bookId, book.getVersion());
            if (!ResultCode.SUCCESS.getCode().equals(decreaseResult.getCode())) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }

            // 8. 创建借阅记录
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setUserId(userId);
            borrowRecord.setBookId(bookId);
            borrowRecord.setBorrowTime(LocalDateTime.now());
            borrowRecord.setExpectedReturnTime(LocalDateTime.now().plusDays(SystemConstant.BORROW_DAYS));
            borrowRecord.setStatus(1); // 1-借阅中
            borrowRecord.setRenewCount(0);
            borrowMapper.insert(borrowRecord);

            // 9. 增加图书借阅次数（Feign调用）
            bookFeignClient.increaseBorrowCount(bookId);

            // 10. 发送借阅事件消息（异步）
            messageProducer.sendBorrowEvent(userId, bookId, "borrow");

            log.info("借阅成功: userId={}, bookId={}", userId, bookId);
            return Result.success(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_BUSY);
        } finally {
            // 11. 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> returnBook(Long userId, Long borrowId) {
        // 1. 查询借阅记录
        BorrowRecord borrowRecord = borrowMapper.selectById(borrowId);
        if (borrowRecord == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_FOUND);
        }
        if (!borrowRecord.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }
        if (borrowRecord.getStatus() != 1) {
            throw new BusinessException(ResultCode.BORROW_STATUS_ERROR);
        }

        // 2. 更新借阅记录
        LocalDateTime now = LocalDateTime.now();
        borrowRecord.setActualReturnTime(now);
        borrowRecord.setStatus(2); // 2-已归还
        
        // 计算是否逾期
        if (now.isAfter(borrowRecord.getExpectedReturnTime())) {
            borrowRecord.setOverdueDays((int) java.time.Duration.between(borrowRecord.getExpectedReturnTime(), now).toDays());
            borrowRecord.setStatus(3); // 3-逾期未还
        }
        
        borrowMapper.updateById(borrowRecord);

        // 3. 增加图书库存（通过Feign调用）
        bookFeignClient.increaseStock(borrowRecord.getBookId());

        // 4. 发送归还事件消息（异步）
        messageProducer.sendBorrowEvent(userId, borrowRecord.getBookId(), "return");

        log.info("归还成功: userId={}, borrowId={}", userId, borrowId);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BookRenew> renewBook(Long userId, BookRenewDTO renewDTO) {
        // 1. 查询借阅记录
        BorrowRecord borrowRecord = borrowMapper.selectById(renewDTO.getBorrowRecordId());
        if (borrowRecord == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_FOUND);
        }
        if (!borrowRecord.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        // 2. 检查是否可续借
        if (borrowRecord.getRenewCount() >= SystemConstant.MAX_RENEW_COUNT) {
            throw new BusinessException(ResultCode.RENEW_COUNT_LIMIT);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedReturnTime = borrowRecord.getBorrowTime()
                .plusDays(SystemConstant.BORROW_DAYS + borrowRecord.getRenewCount() * SystemConstant.RENEW_DAYS);
        
        if (now.isAfter(expectedReturnTime)) {
            throw new BusinessException(ResultCode.RENEW_OVERDUE_NOT_ALLOWED);
        }

        // 3. 创建续借记录
        BookRenew bookRenew = new BookRenew();
        bookRenew.setUserId(userId);
        bookRenew.setBookId(borrowRecord.getBookId());
        bookRenew.setBorrowId(renewDTO.getBorrowRecordId());
        bookRenew.setOriginalExpectedReturnTime(borrowRecord.getExpectedReturnTime());
        bookRenew.setNewExpectedReturnTime(borrowRecord.getExpectedReturnTime().plusDays(SystemConstant.RENEW_DAYS));
        bookRenew.setRenewTime(now);
        bookRenewMapper.insert(bookRenew);

        // 4. 更新借阅记录续借次数和预计归还时间
        borrowRecord.setRenewCount(borrowRecord.getRenewCount() + 1);
        borrowRecord.setExpectedReturnTime(borrowRecord.getExpectedReturnTime().plusDays(SystemConstant.RENEW_DAYS));
        borrowMapper.updateById(borrowRecord);

        // 5. 发送续借事件消息
        messageProducer.sendBorrowEvent(userId, borrowRecord.getBookId(), "renew");

        log.info("续借成功: userId={}, borrowId={}", userId, renewDTO.getBorrowRecordId());
        return Result.success(bookRenew);
    }

    @Override
    public Result<Page<BorrowRecord>> getMyBorrowRecords(Long userId, BorrowQueryDTO queryDTO) {
        LambdaQueryWrapper<BorrowRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BorrowRecord::getUserId, userId)
                .eq(queryDTO.getStatus() != null, BorrowRecord::getStatus, queryDTO.getStatus())
                .orderByDesc(BorrowRecord::getCreateTime);

        Page<BorrowRecord> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        Page<BorrowRecord> resultPage = borrowMapper.selectPage(page, queryWrapper);
        
        // 填充图书名称
        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            for (BorrowRecord record : resultPage.getRecords()) {
                try {
                    Result<Book> bookResult = bookFeignClient.getBookByIdForFeign(record.getBookId());
                    if (bookResult != null && ResultCode.SUCCESS.getCode().equals(bookResult.getCode()) && bookResult.getData() != null) {
                        record.setBookName(bookResult.getData().getName());
                    }
                } catch (Exception e) {
                    log.warn("[借阅记录] 获取图书信息失败, bookId={}", record.getBookId(), e);
                }
            }
        }
        
        return Result.success(resultPage);
    }
}
