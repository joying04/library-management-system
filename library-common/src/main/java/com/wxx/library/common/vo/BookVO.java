package com.wxx.library.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

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
    private Integer stockCount;

    @Schema(description = "分类")
    private Long categoryId;

    @Schema(description = "是否可借阅")
    private Boolean borrowable;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "借阅次数")
    private Integer borrowCount;

    @Schema(description = "封面URL")
    private String coverUrl;
}
