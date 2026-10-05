package com.example.gamerecord.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：所有接口异常统一转换为 Result 返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（主动抛出） */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@Valid 触发） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数错误" : fieldError.getDefaultMessage();
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    /** 未定义路径/静态资源不存在：返回 404 而非 500 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.error(ResultCode.NOT_FOUND, "接口或资源不存在");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.ERROR);
    }
}
