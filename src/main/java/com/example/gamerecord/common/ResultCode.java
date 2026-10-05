package com.example.gamerecord.common;

import lombok.Getter;

/**
 * 业务状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限操作"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
