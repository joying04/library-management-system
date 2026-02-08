package com.wxx.library.util;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果
 * @param <T> 响应数据类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable { //实现接口支持序列化

    // 状态码（200-成功，4xx-客户端错误，5xx-服务器错误，6xx-业务错误）
    private Integer code;

    // 响应信息（成功/失败描述）
    private String message;

    // 响应数据（成功时返回）
    private T data;

    // 时间戳（毫秒）
    private Long timestamp;

    // ============================ 成功响应 ============================
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, System.currentTimeMillis());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, System.currentTimeMillis());
    }

    // ============================ 失败响应 ============================
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, System.currentTimeMillis());
    }

}
