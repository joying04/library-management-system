package com.wxx.library.common.feign;

import com.wxx.library.common.entity.Book;
import com.wxx.library.common.result.Result;
import com.wxx.library.common.vo.BookVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 图书服务 Feign 客户端
 * 供借阅服务远程调用获取/更新图书信息
 */
@FeignClient(name = "library-book", path = "/api/book")
public interface BookFeignClient {

    /**
     * 根据ID查询图书信息
     */
    @GetMapping("/feign/detail/{id}")
    Result<Book> getBookByIdForFeign(@PathVariable("id") Long id);

    /**
     * 查询图书库存
     */
    @GetMapping("/feign/stock/{id}")
    Result<Integer> getBookStock(@PathVariable("id") Long id);

    /**
     * 扣减图书库存（乐观锁）
     */
    @GetMapping("/feign/stock/decrease")
    Result<Boolean> decreaseStock(@RequestParam("bookId") Long bookId,
                                  @RequestParam("version") Integer version);

    /**
     * 增加图书借阅次数
     */
    @GetMapping("/feign/borrow-count/increase")
    Result<Boolean> increaseBorrowCount(@RequestParam("bookId") Long bookId);

    /**
     * 增加图书库存
     */
    @GetMapping("/feign/stock/increase")
    Result<Boolean> increaseStock(@RequestParam("bookId") Long bookId);
}
