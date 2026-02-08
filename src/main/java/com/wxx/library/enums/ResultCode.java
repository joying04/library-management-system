package com.wxx.library.enums;


import lombok.Getter;

/**
 * 统一响应状态码枚举（规范响应格式）
 */
@Getter
public enum ResultCode {

    // 成功状态
    SUCCESS(200, "操作成功"),

    // 客户端错误（4xx）
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有操作权限"),
    RESOURCE_NOT_FOUND(404, "资源不存在"),

    // 服务器错误（5xx）
    FAILED(500, "操作失败"),
    SYSTEM_ERROR(501, "系统异常，请稍后再试"),

    // 业务错误（6xx）
    USER_EXIST(601, "手机号已存在"),
    USER_NOT_EXIST(602, "用户不存在"),
    PASSWORD_ERROR(603, "密码错误"),
    USER_DISABLED(604, "用户已被禁用"),
    BOOK_NOT_EXIST(605, "图书不存在"),
    BOOK_STOCK_EMPTY(606, "图书库存不足"),
    BORROW_COUNT_LIMIT(607, "已达到最大借阅数量"),
    RECORD_NOT_EXIST(608, "借阅记录不存在"),
    RECORD_STATUS_ERROR(609, "借阅记录状态错误（已归还/已逾期）");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
