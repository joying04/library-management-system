package com.wxx.library.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书展示VO（前端展示专用，隐藏敏感字段）
 */
@Data
@Schema(name = "BookVO对象", description = "图书展示信息")
public class BookVO {

    @Schema(description = "图书ID")
    private Long id;

    @Schema(description = "图书名称")
    private String name;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "ISBN编号")
    private String isbn;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "出版日期")
    private LocalDateTime publishDate;

    @Schema(description = "当前库存")
    private Integer stock;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "借阅次数")
    private Integer borrowCount;

    @Schema(description = "封面URL")
    private String coverUrl;
}
