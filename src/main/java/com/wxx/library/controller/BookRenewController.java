package com.wxx.library.controller;


import com.wxx.library.annotation.RateLimit;
import com.wxx.library.dto.BookRenewDTO;
import com.wxx.library.util.Result;
import com.wxx.library.service.BookRenewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/book/renew")
@Tag(name = "图书续借接口", description = "图书续借相关操作")
public class BookRenewController {

    @Autowired
    private BookRenewService bookRenewService;

    @Operation(summary = "图书续借", description = "用户续借借阅中的图书，限制1次续借，延长30天归还时间")
    @PostMapping
    @RateLimit(maxCount = 10,period = 60)
    public Result<String> renewBook(
            @Valid @RequestBody @Schema(description = "续借请求参数") BookRenewDTO renewDTO
    ) {
        boolean success = bookRenewService.renewBook(renewDTO);
        if(success) {
            return Result.success("续借成功，新预计归还时间延长30天");
        }else {
            return Result.error("续借失败，请稍后重试");
        }
    }
}
