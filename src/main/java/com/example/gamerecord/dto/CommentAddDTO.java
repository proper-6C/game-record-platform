package com.example.gamerecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求
 */
@Data
public class CommentAddDTO {

    /** 所属对局ID */
    @NotNull(message = "对局ID不能为空")
    private Long recordId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;

    /** 父评论ID，0 或空表示一级评论 */
    private Long parentId = 0L;

    /** 回复的目标用户ID（@ 提及） */
    private Long toUserId;

    /** 评论配图URL（可选，先经 /api/file/upload 上传） */
    private String imageUrl;
}
