package com.wxx.library.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.annotation.RateLimit;
import com.wxx.library.dto.BookQueryDTO;
import com.wxx.library.entity.Book;
import com.wxx.library.annotation.AdminRequired;
import com.wxx.library.service.BookService;
import com.wxx.library.util.Result;
import com.wxx.library.vo.BookVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书控制器
 */
@RestController
@RequestMapping("/book")
@Tag(name = "图书管理接口")
@Validated
public class BookController {

    @Autowired
    private BookService bookService;

    @Operation(summary = "图书分页查询", description = "支持名称、作者、分类、ISBN多条件查询，无需登录;默认页码1，每页10条")
    @GetMapping("/list")
    @RateLimit(maxCount = 100,period = 60)  //1分钟最多100次请求
    public Result<Page<BookVO>> getBookPage(BookQueryDTO queryDTO) {
        return bookService.getBookPage(queryDTO);
    }

    @Operation(summary = "图书详情查询", description = "根据ID查询图书详情，无需登录")
    @GetMapping("/detail/{id}")
    @RateLimit(maxCount = 50,period = 60)
    public Result<BookVO> getBookById(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long id
    ) {
        return bookService.getBookById(id);
    }

    @Operation(summary = "热门图书查询", description = "查询借阅次数前10的图书，Redis缓存，无需登录")
    @GetMapping("/hot")
    @RateLimit(maxCount = 80,period = 60)
    public Result<List<BookVO>> getHotBooks() {
        return bookService.getHotBooks();
    }

    @Operation(summary = "新增图书", description = "仅管理员可操作，ISBN需唯一")
    @PostMapping("/add")
    @RateLimit(maxCount = 5,period = 60)
    @AdminRequired
    public Result<Book> addBook(
            @Parameter(description = "图书信息", required = true)
            @Validated @RequestBody Book book
    ) {
        return bookService.addBook(book);
    }

    @Operation(summary = "更新图书", description = "仅管理员可操作，支持乐观锁")
    @PutMapping("/update")
    @RateLimit(maxCount = 5,period = 60)
    @AdminRequired
    public Result<Boolean> updateBook(
            @Parameter(description = "图书信息（含ID）", required = true)
            @Validated @RequestBody Book book
    ) {
        return bookService.updateBook(book);
    }

    @Operation(summary = "删除图书", description = "仅管理员可操作，逻辑删除")
    @DeleteMapping("/delete/{id}")
    @RateLimit(maxCount = 5,period = 60)
    @AdminRequired
    public Result<Boolean> deleteBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable Long id
    ) {
        return bookService.deleteBook(id);
    }
}

