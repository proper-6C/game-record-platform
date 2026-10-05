package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 编辑对局信息请求（文件与封面不变）
 */
@Data
public class RecordEditDTO {

    @NotBlank(message = "游戏名称不能为空")
    @Size(max = 100, message = "游戏名称长度不能超过100")
    private String gameName;

    @Size(max = 50, message = "游戏模式长度不能超过50")
    private String gameMode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate matchDate;

    private String result;

    @Size(max = 50, message = "段位长度不能超过50")
    private String rank;

    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    /** 标签（逗号分隔） */
    @Size(max = 255, message = "标签长度不能超过255")
    private String tags;
}
