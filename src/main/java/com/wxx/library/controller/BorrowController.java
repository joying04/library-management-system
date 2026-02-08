package com.wxx.library.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.annotation.RateLimit;
import com.wxx.library.dto.BorrowQueryDTO;
import com.wxx.library.entity.BorrowRecord;
import com.wxx.library.annotation.AdminRequired;
import com.wxx.library.service.BorrowService;
import com.wxx.library.util.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅控制器
 */
@RestController
@RequestMapping("/borrow")
@Tag(name = "借阅管理接口")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Operation(summary = "借阅图书", description = "登录用户可操作，每人最多借阅5本")
    @PostMapping("/{bookId}")
    @RateLimit(maxCount = 20,period = 60)
    public Result<BorrowRecord> borrowBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long bookId
    ) {
        return borrowService.borrowBook(bookId);
    }

    @Operation(summary = "归还图书", description = "登录用户可归还自己的图书，管理员可代还")
    @PostMapping("/return/{recordId}")
    @RateLimit(maxCount = 20,period = 60)
    public Result<Boolean> returnBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable Long recordId
    ) {
        return borrowService.returnBook(recordId);
    }

    @Operation(summary = "借阅记录查询", description = "普通用户仅查自己的，管理员可查所有")
    @GetMapping("/records")
    @RateLimit(maxCount = 20,period = 60)
    public Result<Page<BorrowRecord>> getBorrowPage(BorrowQueryDTO queryDTO) {
        return borrowService.getBorrowPage(queryDTO);
    }

    @Operation(summary = "手动触发逾期提醒", description = "仅管理员可操作，测试用")
    @PostMapping("/overdue/reminder")
    @RateLimit(maxCount = 5,period = 60)
    @AdminRequired
    public Result<?> overdueReminder() {
        borrowService.overdueReminder();
        return Result.success("逾期提醒任务执行成功");
    }
}
