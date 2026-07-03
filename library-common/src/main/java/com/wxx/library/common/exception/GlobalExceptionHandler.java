package com.wxx.library.common.exception;

import com.wxx.library.common.result.Result;
import com.wxx.library.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获所有异常，返回统一的响应格式
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常（最常用）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}，状态码：{}", e.getMessage(), e.getCode());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid/@Validated注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        // 收集所有参数错误信息
        String errorMsg = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验异常：{}", errorMsg);
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), errorMsg);
    }

    /**
     * 处理参数校验异常（@PathVariable/@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        // 收集路径参数/请求参数的错误信息
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.error("参数校验异常：{}", errorMsg);
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), errorMsg);
    }

    /**
     * 处理404异常（请求接口不存在）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("404异常：{}", e.getMessage());
        return Result.error(ResultCode.RESOURCE_NOT_FOUND.getCode(), ResultCode.RESOURCE_NOT_FOUND.getMessage());
    }

    /**
     * 处理系统异常（兜底,所有未明确捕获的异常）
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleSystemException(Exception e) {
        log.error("系统异常：", e); // 打印完整堆栈信息，便于排查问题
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage());
    }
}
