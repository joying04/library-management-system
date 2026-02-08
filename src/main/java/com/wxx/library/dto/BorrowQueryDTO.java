package com.wxx.library.dto;


import com.wxx.library.constant.SystemConstant;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.Min;

/**
 * 借阅记录查询入参DTO（统一接收前端查询参数）
 */
@Data
public class BorrowQueryDTO {

    /**
     * 页码（默认1，最小值1）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = SystemConstant.DEFAULT_PAGE_NUM;

    /**
     * 每页条数（默认10，最小值1，最大值50）
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Range(max = 50, message = "每页条数不能超过50")
    private Integer pageSize = SystemConstant.DEFAULT_PAGE_SIZE;

    /**
     * 用户ID（管理员查询时使用，普通用户无需传递）
     */
    private Long userId;

    /**
     * 图书ID（可选查询条件）
     */
    private Long bookId;

    /**
     * 借阅状态（1-借阅中，2-已归还，3-已逾期；）
     */
    @Range(min = 1, max = 3, message = "借阅状态只能是1（借阅中）、2（已归还）、3（已逾期）")
    private Integer status;

}
