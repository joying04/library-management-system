package com.wxx.library.common.dto;

import com.wxx.library.common.constant.SystemConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "BookQueryDTO对象", description = "图书查询请求参数")
public class BookQueryDTO {

    @Schema(description = "页码（默认1）", example = "1")
    private Integer currentPage = SystemConstant.DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数（默认10）", example = "10")
    private Integer pageSize = SystemConstant.DEFAULT_PAGE_SIZE;

    @Schema(description = "图书名称（模糊查询）")
    private String bookName;

    @Schema(description = "作者（模糊查询）")
    private String author;

    @Schema(description = "分类（精确查询）")
    private Long categoryId;

    @Schema(description = "ISBN（精确查询）")
    private String isbn;

    @Schema(description = "状态（精确查询）")
    private Integer status;
}
