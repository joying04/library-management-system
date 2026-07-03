package com.wxx.library.common.dto;

import com.wxx.library.common.constant.SystemConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.Min;

@Data
public class BorrowQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Integer currentPage = SystemConstant.DEFAULT_PAGE_NUM;

    @Min(value = 1, message = "每页条数不能小于1")
    @Range(max = 50, message = "每页条数不能超过50")
    private Integer pageSize = SystemConstant.DEFAULT_PAGE_SIZE;

    private Long userId;

    private Long bookId;

    @Range(min = 1, max = 3, message = "借阅状态只能是1（借阅中）、2（已归还）、3（已逾期）")
    private Integer status;
}
