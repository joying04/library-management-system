package com.wxx.library.entity;


import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 图书实体类
 */
@Data
@TableName("book")
@Schema(name = "Book对象", description = "图书表")
public class Book {

    @Schema(description = "图书ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "图书名称", required = true)
    @NotBlank(message = "图书名称不能为空")
    private String name;

    @Schema(description = "作者", required = true)
    @NotBlank(message = "作者不能为空")
    private String author;

    @Schema(description = "ISBN编号（唯一）", required = true)
    @NotBlank(message = "ISBN不能为空")
    private String isbn;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "出版日期")
    private LocalDateTime publishDate;

    @Schema(description = "当前库存", example = "10")
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    @Schema(description = "总库存", example = "100")
    @NotNull(message = "总库存不能为空")
    @Min(value = 0, message = "总库存不能小于0")
    private Integer totalStock;

    @Schema(description = "图书分类", required = true)
    @NotBlank(message = "分类不能为空")
    private String category;

    @Schema(description = "图书描述")
    private String description;

    @Schema(description = "借阅次数", example = "0")
    private Integer borrowCount;

    @Schema(description = "图书封面URL")
    private String coverUrl;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除：0-未删除，1-已删除", example = "0")
    @TableLogic
    private Integer deleted;

    @Schema(description = "乐观锁版本号", example = "1")
    @Version
    private Integer version;
}

