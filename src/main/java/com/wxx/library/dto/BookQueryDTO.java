package com.wxx.library.dto;


import com.wxx.library.constant.SystemConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图书查询请求DTO（分页+条件查询）
 */
@Data
@Schema(name = "BookQueryDTO对象", description = "图书查询请求参数")
public class BookQueryDTO {

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = SystemConstant.DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数（默认10）", example = "10")
    private Integer pageSize = SystemConstant.DEFAULT_PAGE_SIZE;

    @Schema(description = "图书名称（模糊查询）")
    private String name;

    @Schema(description = "作者（模糊查询）")
    private String author;

    @Schema(description = "分类（精确查询）")
    private String category;

    @Schema(description = "ISBN（精确查询）")
    private String isbn;
}
