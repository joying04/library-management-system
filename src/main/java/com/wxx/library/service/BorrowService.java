package com.wxx.library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxx.library.dto.BorrowQueryDTO;
import com.wxx.library.entity.BorrowRecord;
import com.wxx.library.util.Result;

/**
 * 借阅服务接口
 */
public interface BorrowService extends IService<BorrowRecord> {

    /**
     * 借阅图书（事务保证：创建记录+扣减库存+增加借阅次数）
     */
    Result<BorrowRecord> borrowBook(Long bookId);

    /**
     * 归还图书（事务保证：更新记录+恢复库存）
     */
    Result<Boolean> returnBook(Long recordId);

    /**
     * 借阅记录分页查询（支持多条件，普通用户仅查自己的）
     */
    Result<Page<BorrowRecord>> getBorrowPage(BorrowQueryDTO queryDTO);

    /**
     * 逾期图书提醒
     */
    void overdueReminder();

}
