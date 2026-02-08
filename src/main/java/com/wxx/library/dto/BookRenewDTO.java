package com.wxx.library.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "BookRenewDTO对象", description = "图书续借请求参数")
public class BookRenewDTO {

    @Schema(description = "借阅记录ID（必传，用于定位续借的记录）", required = true)
    @NotNull(message = "借阅记录ID不能为空")
    private Long borrowId;

    @Schema(description = "图书ID（必传，用于二次校验）", required = true)
    @NotNull(message = "图书ID不能为空")
    private Long bookId;
}
