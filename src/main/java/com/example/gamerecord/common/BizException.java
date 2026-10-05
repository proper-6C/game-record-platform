package com.example.gamerecord.common;

import lombok.Getter;

/**
 * 业务异常：Service 层抛出，由全局异常处理器统一转为 Result 返回
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
