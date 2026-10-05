package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 绑定 / 解绑邮箱请求
 */
@Data
public class EmailBindDTO {

    /** 目标邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    /** 验证码（6 位） */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为 6 位数字")
    private String code;
}
