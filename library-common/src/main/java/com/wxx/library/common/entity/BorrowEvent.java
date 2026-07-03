package com.wxx.library.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅事件记录实体
 */
@Data
@TableName("borrow_event")
@Schema(description = "借阅事件记录")
public class BorrowEvent {

    @Schema(description = "事件ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "图书ID")
    private Long bookId;

    @Schema(description = "借阅记录ID")
    private Long borrowRecordId;

    @Schema(description = "事件类型：BORROW-借阅, RETURN-归还, RENEW-续借, OVERDUE-逾期")
    private String eventType;

    @Schema(description = "事件详情（JSON格式）")
    private String eventData;

    @Schema(description = "RabbitMQ消息ID")
    private String messageId;

    @Schema(description = "消费时间")
    private LocalDateTime consumeTime;

    @Schema(description = "状态：1-已处理 2-处理失败")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
