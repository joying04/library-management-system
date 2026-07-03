package com.wxx.library.common.exception;

import com.wxx.library.common.result.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    // 异常状态码
    private Integer code;

    /**
     * 构造方法（使用默认状态码：500）
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    /**
     * 构造方法（使用枚举状态码）
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
