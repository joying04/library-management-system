package com.wxx.library.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxx.library.constant.SystemConstant;
import com.wxx.library.dto.BookRenewDTO;
import com.wxx.library.entity.BookRenew;
import com.wxx.library.entity.BorrowRecord;
import com.wxx.library.enums.BorrowStatusEnum;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.exception.BusinessException;
import com.wxx.library.mapper.BookRenewMapper;
import com.wxx.library.mapper.BorrowMapper;
import com.wxx.library.service.BookRenewService;
import com.wxx.library.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Service
public class BookRenewServiceImpl extends ServiceImpl<BookRenewMapper, BookRenew> implements BookRenewService {

    @Autowired
    private BookRenewMapper bookRenewMapper;

    @Autowired
    private BorrowMapper borrowMapper;

    /**
     * 图书续借核心方法
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean renewBook(BookRenewDTO renewDTO) {
        Long borrowId = renewDTO.getBorrowId();
        Long bookId = renewDTO.getBookId();
        Long userId = UserContext.getUserId(); // 从上下文获取当前登录用户ID

        // 1. 校验借阅记录是否存在且属于当前用户
        BorrowRecord borrowRecord = borrowMapper.selectByIdWithVersion(borrowId);
        if (borrowRecord == null) {
            throw new BusinessException("借阅记录不存在");
        }
        // 普通用户只能续借自己的记录
        if (!borrowRecord.getUserId().equals(userId) && UserContext.getRole().equals(UserRoleEnum.COMMON.getCode())) {
            throw new BusinessException("无权限续借他人的图书");
        }
        // 校验图书ID一致性
        if (!borrowRecord.getBookId().equals(bookId)) {
            throw new BusinessException("借阅记录与图书ID不匹配");
        }

        // 2. 校验续借条件（状态为借阅中、未逾期）
        if (!borrowRecord.getStatus().equals(BorrowStatusEnum.BORROWED.getCode())) {
            throw new BusinessException("仅借阅中的图书可续借");
        }

        if (borrowRecord.getStatus().equals(BorrowStatusEnum.OVERDUE.getCode())) {
            throw new BusinessException("图书已逾期，不可续借");
        }

        // “预计归还时间已过”的兜底判断（防止状态同步异常）
        if (borrowRecord.getExpectedReturnTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("图书已过预计归还时间，不可续借");
        }

        // 3. 校验续借次数（最多续借1次）
        Integer renewTimes = bookRenewMapper.countRenewTimes(userId, bookId);
        if (renewTimes >= SystemConstant.MAX_RENEW_TIMES) {
            throw new BusinessException("该图书已续借" + SystemConstant.MAX_RENEW_TIMES + "次，不可重复续借");
        }

        // 4. 计算新的预计归还时间（原预计归还时间 + 30天）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime originalExpectedReturnTime = borrowRecord.getExpectedReturnTime();
        LocalDateTime newExpectedReturnTime = originalExpectedReturnTime.plusDays(SystemConstant.RENEW_DAYS);

        // 5. 乐观锁更新借阅记录（防止并发续借）
        int updateCount = borrowMapper.updateExpectedReturnTime(
                borrowId, newExpectedReturnTime, borrowRecord.getVersion(), userId, bookId
        );
        if (updateCount == 0) {
            throw new BusinessException("续借失败，可能已被他人操作，请重试");
        }

        // 6. 新增续借记录
        BookRenew bookRenew = new BookRenew();
        bookRenew.setBorrowId(borrowId);
        bookRenew.setUserId(userId);
        bookRenew.setBookId(bookId);
        bookRenew.setOriginalExpectedReturnTime(originalExpectedReturnTime);
        bookRenew.setNewExpectedReturnTime(newExpectedReturnTime);
        bookRenew.setRenewTime(now);
        boolean saveSuccess = save(bookRenew);

        if(saveSuccess) {
            log.info("用户{}续借图书{}成功，借阅记录ID：{}，原预计归还时间：{}，新预计归还时间：{}",
                    userId, bookId, borrowId, originalExpectedReturnTime, newExpectedReturnTime);
        } else {
            log.warn("用户{}续借图书{}失败,借阅记录ID:{},保存续借记录失败", userId, bookId, borrowId);
        }

        return saveSuccess;
    }
}