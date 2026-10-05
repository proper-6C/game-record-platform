package com.example.gamerecord.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论视图（含回复列表）
 */
@Data
public class CommentVO {

    private Long id;
    private Long recordId;
    private Long userId;
    private String nickname;
    private String avatar;

    /** 父评论ID，0 表示一级评论 */
    private Long parentId;

    /** 回复的目标用户ID */
    private Long toUserId;

    /** 回复的目标用户昵称 */
    private String toUserName;

    private String content;

    /** 评论配图URL */
    private String imageUrl;

    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createTime;

    /** 二级回复列表（仅一级评论有） */
    private List<CommentVO> replyList = new ArrayList<>();
}
