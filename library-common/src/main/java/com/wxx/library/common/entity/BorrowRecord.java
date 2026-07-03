package com.wxx.library.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@TableName("borrow_record")
@Schema(name = "BorrowRecord对象", description = "借阅记录表")
public class BorrowRecord {

    @Schema(description = "记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID", required = true)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "图书ID", required = true)
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @Schema(description = "借阅时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime borrowTime;

    @Schema(description = "应归还时间")
    private LocalDateTime expectedReturnTime;

    @Schema(description = "实际归还时间")
    private LocalDateTime actualReturnTime;

    @Schema(description = "状态：1-借阅中，2-已归还，3-逾期未还")
    private Integer status;

    @Schema(description = "逾期天数", example = "0")
    private Integer overdueDays;

    @Schema(description = "续借次数", example = "0")
    private Integer renewCount;

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

    // 关联查询字段（数据库表中无该字段）
    @TableField(exist = false)
    @Schema(description = "用户名")
    private String userName;

    @TableField(exist = false)
    @Schema(description = "图书名称")
    private String bookName;
}
