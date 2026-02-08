package com.wxx.library.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxx.library.constant.SystemConstant;
import com.wxx.library.enums.BorrowStatusEnum;
import com.wxx.library.enums.ResultCode;
import com.wxx.library.dto.BorrowQueryDTO;
import com.wxx.library.entity.Book;
import com.wxx.library.entity.BorrowRecord;
import com.wxx.library.entity.User;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.exception.BusinessException;
import com.wxx.library.mapper.BorrowMapper;
import com.wxx.library.service.BookService;
import com.wxx.library.service.BorrowService;
import com.wxx.library.service.UserService;
import com.wxx.library.util.RedisUtil;
import com.wxx.library.util.Result;
import com.wxx.library.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 借阅服务实现类
 */
@Service
@Slf4j
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, BorrowRecord> implements BorrowService {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${library.borrow.max-count}")
    private Integer maxBorrowCount; // 最大借阅数量

    @Value("${library.borrow.max-days}")
    private Integer maxBorrowDays; // 最长借阅天数

    /**
     * 借阅图书（事务保证原子性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BorrowRecord> borrowBook(Long bookId) {
        Long userId = UserContext.getUserId();
        // Redis分布式锁（锁key=图书ID，控制并发）
        String lockKey = "lock:book:borrow:" + bookId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked;
        try {
            // 尝试获取锁：最多等待3秒，持有30秒自动释放（避免死锁/锁泄漏）
            isLocked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("借阅人数过多，请稍后重试");
            }

            // 1. 校验用户是否存在
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST);
            }

            // 2. 校验用户借阅数量
            LambdaQueryWrapper<BorrowRecord> borrowCountWrapper = new LambdaQueryWrapper<>();
            borrowCountWrapper.eq(BorrowRecord::getUserId, userId)
                    .eq(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED.getCode()); // 1-借阅中
            long borrowingCount = count(borrowCountWrapper);
            if (borrowingCount >= maxBorrowCount) {
                throw new BusinessException(ResultCode.BORROW_COUNT_LIMIT);
            }

            // 3. 校验图书是否存在、库存是否充足
            Book book = bookService.getById(bookId);
            if (book == null) {
                throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
            }
            if (book.getStock() <= 0) {
                throw new BusinessException(ResultCode.BOOK_STOCK_EMPTY);
            }

            // 4. 创建借阅记录
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setUserId(userId);
            borrowRecord.setBookId(bookId);
            borrowRecord.setStatus(BorrowStatusEnum.BORROWED.getCode()); // 1-借阅中
            borrowRecord.setBorrowTime(LocalDateTime.now());
            borrowRecord.setExpectedReturnTime(LocalDateTime.now().plusDays(maxBorrowDays)); // 预计归还时间

            boolean saveSuccess = save(borrowRecord);
            if (!saveSuccess) {
                throw new BusinessException("创建借阅记录失败");
            }

            // 调用乐观锁扣减库存方法
            boolean stockSuccess = bookService.decreaseStockWithVersion(bookId, book.getVersion());
            if (!stockSuccess) {
                throw new BusinessException("图书已被他人借走，请重试");
            }

            // 5. 增加图书借阅次数
            boolean borrowCountSuccess = bookService.lambdaUpdate()
                    .setSql("borrow_count = borrow_count + 1")
                    .eq(Book::getId, bookId)
                    .update();
            if (!borrowCountSuccess) {
                throw new BusinessException("更新图书借阅次数失败");
            }

            // 6. 清除热门图书缓存
            redisUtil.delete(SystemConstant.hotBookKey);

            // 7. 关联查询用户和图书名称
            borrowRecord.setUserName(user.getName());
            borrowRecord.setBookName(book.getName());

            log.info("用户借阅图书成功，用户ID：{}，图书ID：{}，图书名称：{}",
                    userId, bookId, book.getName());
            return Result.success(borrowRecord);
        } catch (InterruptedException e) {
            log.error("获取借阅锁失败", e);
            throw new BusinessException("系统繁忙，请稍后重试");
        } finally {
            // 释放锁（必须在finally中执行，避免锁泄漏）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 归还图书（事务保证）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> returnBook(Long recordId) {
        Long userId = UserContext.getUserId();
        Integer userRole = UserContext.getRole();

        // 1. 校验借阅记录是否存在
        BorrowRecord borrowRecord = getById(recordId);
        if (borrowRecord == null) {
            throw new BusinessException(ResultCode.RECORD_NOT_EXIST);
        }

        // 2. 权限校验：普通用户只能归还自己的记录
        if (userRole.equals(UserRoleEnum.COMMON.getCode()) && !borrowRecord.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 状态校验：不能归还已归还/已逾期的记录
        if (!borrowRecord.getStatus().equals(BorrowStatusEnum.BORROWED.getCode())) {
            throw new BusinessException(ResultCode.RECORD_STATUS_ERROR);
        }

        // 4. 更新借阅记录状态
        borrowRecord.setStatus(BorrowStatusEnum.RETURNED.getCode()); // 已归还
        borrowRecord.setReturnTime(LocalDateTime.now());
        // 计算逾期天数
        long overdueDays = ChronoUnit.DAYS.between(borrowRecord.getExpectedReturnTime(), LocalDateTime.now());
        borrowRecord.setOverdueDays(overdueDays > 0 ? (int) overdueDays : 0);

        boolean updateSuccess = updateById(borrowRecord);
        if (!updateSuccess) {
            throw new BusinessException("更新借阅记录失败");
        }

        // 5. 恢复图书库存
        Book book = bookService.getById(borrowRecord.getBookId());
        if(book.getStock()>=book.getTotalStock()) {
            throw new BusinessException("图书库存已达上限，无需重复归还");
        }
        boolean stockSuccess = bookService.updateStock(borrowRecord.getBookId(), 1);
        if (!stockSuccess) {
            throw new BusinessException("恢复图书库存失败");
        }

        log.info("用户归还图书成功，记录ID：{}，用户ID：{}，图书ID：{}",
                recordId, userId, borrowRecord.getBookId());
        return Result.success(true);
    }

    /**
     * 借阅记录分页查询（关联用户和图书表）
     */
    @Override
    public Result<Page<BorrowRecord>> getBorrowPage(BorrowQueryDTO queryDTO) {
        Long userId = UserContext.getUserId();
        Integer userRole = UserContext.getRole();

        // 1. 构建分页对象
        Page<BorrowRecord> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<BorrowRecord> queryWrapper = new LambdaQueryWrapper<>();
        // 普通用户仅查自己的记录
        if (userRole.equals(UserRoleEnum.COMMON.getCode())) {
            queryWrapper.eq(BorrowRecord::getUserId, userId);
        } else {
            // 管理员可按用户ID查询
            if (queryDTO.getUserId() != null) {
                queryWrapper.eq(BorrowRecord::getUserId, queryDTO.getUserId());
            }
        }
        // 按图书ID查询
        if (queryDTO.getBookId() != null) {
            queryWrapper.eq(BorrowRecord::getBookId, queryDTO.getBookId());
        }
        // 按状态查询
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(BorrowRecord::getStatus, queryDTO.getStatus());
        }
        // 排序：按借阅时间降序
        queryWrapper.orderByDesc(BorrowRecord::getBorrowTime);

        // 3. 自定义SQL关联查询（调用Mapper的自定义方法）
        Page<BorrowRecord> borrowPage = baseMapper.selectBorrowRecordPage(page, queryWrapper);

        log.info("借阅记录分页查询成功，页码：{}，每页条数：{}，总条数：{}",
                queryDTO.getPageNum(), queryDTO.getPageSize(), borrowPage.getTotal());
        return Result.success(borrowPage);
    }

    /**
     * 逾期图书提醒（定时任务，每天凌晨1点执行）
     */
    @Scheduled(cron = "0 0 1 * * ?") // cron表达式：每天凌晨1点
    @Override
    public void overdueReminder() {
        log.info("开始执行逾期图书提醒任务");

        // 1. 查询即将逾期的图书（预计归还时间-当前时间 <= 3天，且未归还）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusDays(3);
        LambdaQueryWrapper<BorrowRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED.getCode()) // 借阅中
                .between(BorrowRecord::getExpectedReturnTime, now, reminderTime);

        List<BorrowRecord> overdueRecords = list(queryWrapper);
        if (overdueRecords.isEmpty()) {
            log.info("无即将逾期的图书，任务结束");
            return;
        }

        // 2. 模拟发送提醒（实际项目中可集成短信/邮件服务）
        for (BorrowRecord record : overdueRecords) {
            User user = userService.getById(record.getUserId());
            Book book = bookService.getById(record.getBookId());
            log.info("逾期提醒：用户【{}】（手机号：{}）借阅的图书【{}】将于{}逾期，请及时归还",
                    user.getName(), user.getPhone(), book.getName(), record.getExpectedReturnTime());
        }

        log.info("逾期图书提醒任务执行完成，共提醒{}条记录", overdueRecords.size());
    }
}
