package com.wxx.library.common.result;

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
    USER_NOT_FOUND(602, "用户不存在"),
    USER_NOT_EXIST(602, "用户不存在"),
    PASSWORD_ERROR(603, "密码错误"),
    USER_DISABLED(604, "用户已被禁用"),
    USER_HAS_OVERDUE(605, "存在逾期未还图书"),
    
    BOOK_NOT_FOUND(606, "图书不存在"),
    STOCK_NOT_ENOUGH(607, "图书库存不足"),
    BORROW_COUNT_LIMIT(608, "已达到最大借阅数量"),
    
    BORROW_RECORD_NOT_FOUND(609, "借阅记录不存在"),
    BORROW_STATUS_ERROR(610, "借阅记录状态错误（已归还/已逾期）"),
    PERMISSION_DENIED(611, "没有操作权限"),
    
    RENEW_COUNT_LIMIT(612, "已达到最大续借次数"),
    RENEW_OVERDUE_NOT_ALLOWED(613, "逾期图书不允许续借"),
    
    SYSTEM_BUSY(614, "系统繁忙，请稍后再试"),
    ERROR(699, "操作失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
