package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论（支持楼中楼：parentId=0 为一级评论）
 */
@Data
@TableName("comment")
public class Comment {

    /** 评论ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属对局ID */
    private Long recordId;

    /** 评论人ID */
    private Long userId;

    /** 父评论ID，0 表示一级评论 */
    private Long parentId;

    /** 回复的目标用户ID（@ 提及） */
    private Long toUserId;

    /** 评论内容 */
    private String content;

    /** 评论配图URL */
    private String imageUrl;

    /** 点赞数 */
    private Integer likeCount;

    /** 软删除：0 正常 1 已删除 */
    private Integer deleted;

    /** 评论时间（数据库默认值） */
    private LocalDateTime createTime;
}
