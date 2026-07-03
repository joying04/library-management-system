package com.wxx.library.borrow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.common.dto.BookRenewDTO;
import com.wxx.library.common.dto.BorrowQueryDTO;
import com.wxx.library.common.entity.BookRenew;
import com.wxx.library.common.entity.BorrowRecord;
import com.wxx.library.common.result.Result;

public interface BorrowService {

    Result<Boolean> borrowBook(Long userId, Long bookId);

    Result<Boolean> returnBook(Long userId, Long borrowId);

    Result<BookRenew> renewBook(Long userId, BookRenewDTO renewDTO);

    Result<Page<BorrowRecord>> getMyBorrowRecords(Long userId, BorrowQueryDTO queryDTO);
}
