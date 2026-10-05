package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送邮箱验证码请求
 */
@Data
public class EmailSendDTO {

    /** 目标邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    /** 用途：register 注册 / bind 绑定 / login 登录 */
    @NotBlank(message = "用途不能为空")
    private String type;
}
