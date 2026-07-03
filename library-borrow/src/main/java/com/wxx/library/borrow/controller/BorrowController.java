package com.wxx.library.borrow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.common.constant.SystemConstant;
import com.wxx.library.common.dto.BookRenewDTO;
import com.wxx.library.common.dto.BorrowQueryDTO;
import com.wxx.library.common.entity.BookRenew;
import com.wxx.library.common.entity.BorrowRecord;
import com.wxx.library.common.result.Result;
import com.wxx.library.borrow.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/borrow")
@Tag(name = "借阅管理接口")
@Slf4j
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Operation(summary = "借阅图书")
    @PostMapping("/borrow")
    public Result<Boolean> borrowBook(@RequestParam Long bookId, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader(SystemConstant.HEADER_USER_ID));
        return borrowService.borrowBook(userId, bookId);
    }

    @Operation(summary = "归还图书")
    @PostMapping("/return")
    public Result<Boolean> returnBook(@RequestParam Long borrowId, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader(SystemConstant.HEADER_USER_ID));
        return borrowService.returnBook(userId, borrowId);
    }

    @Operation(summary = "续借图书")
    @PostMapping("/renew")
    public Result<BookRenew> renewBook(@Valid @RequestBody BookRenewDTO renewDTO, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader(SystemConstant.HEADER_USER_ID));
        return borrowService.renewBook(userId, renewDTO);
    }

    @Operation(summary = "我的借阅记录")
    @GetMapping("/my-records")
    public Result<Page<BorrowRecord>> getMyBorrowRecords(BorrowQueryDTO queryDTO, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader(SystemConstant.HEADER_USER_ID));
        return borrowService.getMyBorrowRecords(userId, queryDTO);
    }
}
