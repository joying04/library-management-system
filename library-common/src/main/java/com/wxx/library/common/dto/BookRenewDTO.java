package com.wxx.library.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "BookRenewDTO对象", description = "图书续借请求参数")
public class BookRenewDTO {

    @Schema(description = "借阅记录ID", required = true)
    @NotNull(message = "借阅记录ID不能为空")
    private Long borrowRecordId;

    @Schema(description = "续借原因")
    private String renewReason;
}
