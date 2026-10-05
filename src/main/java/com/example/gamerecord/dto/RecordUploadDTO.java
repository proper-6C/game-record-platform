package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 对局上传表单（与上传文件一同提交）
 */
@Data
public class RecordUploadDTO {

    @NotBlank(message = "游戏名称不能为空")
    @Size(max = 100, message = "游戏名称长度不能超过100")
    private String gameName;

    /** 游戏模式（排位/匹配等） */
    @Size(max = 50, message = "游戏模式长度不能超过50")
    private String gameMode;

    /** 对局日期，格式 yyyy-MM-dd */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate matchDate;

    /** 对局结果：win/lose/draw */
    private String result;

    /** 段位 */
    @Size(max = 50, message = "段位长度不能超过50")
    private String rank;

    /** 对局描述 */
    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    /** 标签（逗号分隔，如 翻盘,五杀,排位） */
    @Size(max = 255, message = "标签长度不能超过255")
    private String tags;
}
