package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报处理请求（管理员）
 */
@Data
public class ReportHandleDTO {

    /** 处理结果说明 */
    @NotBlank(message = "请填写处理结果")
    private String result;

    /** 是否下架该对局（true=确认违规并下架） */
    @NotNull(message = "请选择处理方式")
    private Boolean takeDown;
}
