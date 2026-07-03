package com.wxx.library.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxx.library.common.annotation.AdminRequired;
import com.wxx.library.common.dto.BookQueryDTO;
import com.wxx.library.common.entity.Book;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.vo.BookVO;
import com.wxx.library.book.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book")
@Tag(name = "图书管理接口")
@Validated
@Slf4j
public class BookController {

    @Autowired
    private BookService bookService;

    @Operation(summary = "图书分页查询")
    @GetMapping("/list")
    public Result<Page<BookVO>> getBookPage(BookQueryDTO queryDTO) {
        return bookService.getBookPage(queryDTO);
    }

    @Operation(summary = "图书详情查询")
    @GetMapping("/detail/{id}")
    public Result<BookVO> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @Operation(summary = "热门图书查询（Redis缓存）")
    @GetMapping("/hot")
    public Result<List<BookVO>> getHotBooks() {
        return bookService.getHotBooks();
    }

    @Operation(summary = "新增图书（管理员）")
    @PostMapping("/add")
    @AdminRequired
    public Result<Book> addBook(@Validated @RequestBody Book book) {
        return bookService.addBook(book);
    }

    @Operation(summary = "更新图书（管理员）")
    @PutMapping("/update")
    @AdminRequired
    public Result<Boolean> updateBook(@Validated @RequestBody Book book) {
        return bookService.updateBook(book);
    }

    @Operation(summary = "删除图书（管理员）")
    @DeleteMapping("/delete/{id}")
    @AdminRequired
    public Result<Boolean> deleteBook(@PathVariable Long id) {
        return bookService.deleteBook(id);
    }

    // ==================== Feign 内部调用接口 ====================

    @Operation(summary = "根据ID查询图书（Feign调用）", hidden = true)
    @GetMapping("/feign/detail/{id}")
    public Result<Book> getBookByIdForFeign(@PathVariable Long id) {
        return bookService.getBookByIdForFeign(id);
    }

    @Operation(summary = "查询图书库存（Feign调用）", hidden = true)
    @GetMapping("/feign/stock/{id}")
    public Result<Integer> getBookStock(@PathVariable Long id) {
        return bookService.getBookStock(id);
    }

    @Operation(summary = "扣减库存（Feign调用）", hidden = true)
    @GetMapping("/feign/stock/decrease")
    public Result<Boolean> decreaseStock(@RequestParam Long bookId, @RequestParam Integer version) {
        return bookService.decreaseStock(bookId, version);
    }

    @Operation(summary = "增加借阅次数（Feign调用）", hidden = true)
    @GetMapping("/feign/borrow-count/increase")
    public Result<Boolean> increaseBorrowCount(@RequestParam Long bookId) {
        return bookService.increaseBorrowCount(bookId);
    }

    @Operation(summary = "增加库存（Feign调用）", hidden = true)
    @GetMapping("/feign/stock/increase")
    public Result<Boolean> increaseStock(@RequestParam Long bookId) {
        return bookService.increaseStock(bookId);
    }
}
