package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 点赞/取消点赞请求
 */
@Data
public class LikeToggleDTO {

    /** 被点赞对象ID（对局ID或评论ID） */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    /** 对象类型：record / comment */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;
}
