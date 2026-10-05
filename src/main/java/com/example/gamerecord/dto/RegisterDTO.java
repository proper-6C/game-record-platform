package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度需在6-50之间")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    /** 绑定邮箱（可选；填了则必须同时提供 code 验证码） */
    @Pattern(regexp = "^$|^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    /** 邮箱验证码（配合 email 使用） */
    private String code;
}
