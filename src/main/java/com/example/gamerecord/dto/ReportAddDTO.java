package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交举报请求
 */
@Data
public class ReportAddDTO {

    /** 被举报对局ID */
    @NotNull(message = "对局ID不能为空")
    private Long recordId;

    /** 举报原因 */
    @NotBlank(message = "请选择举报原因")
    @Size(max = 50, message = "举报原因不能超过50字")
    private String reason;

    /** 补充说明 */
    @Size(max = 500, message = "补充说明不能超过500字")
    private String detail;
}
