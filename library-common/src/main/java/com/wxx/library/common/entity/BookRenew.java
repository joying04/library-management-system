package com.wxx.library.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_renew")
@Schema(name = "BookRenew对象", description = "图书续借记录表")
public class BookRenew {

    @Schema(description = "续借记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联借阅记录ID", required = true)
    @TableField("borrow_record_id")
    private Long borrowId;

    @Schema(description = "续借用户ID", required = true)
    private Long userId;

    @Schema(description = "续借图书ID", required = true)
    private Long bookId;

    @Schema(description = "原预计归还时间", required = true)
    private LocalDateTime originalExpectedReturnTime;

    @Schema(description = "续借后预计归还时间", required = true)
    private LocalDateTime newExpectedReturnTime;

    @Schema(description = "续借时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime renewTime;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除：0-未删除，1-已删除", example = "0")
    @TableLogic
    private Integer deleted;
}
