package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞记录
 */
@Data
@TableName("like_record")
public class LikeRecord {

    /** 点赞ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 点赞用户ID */
    private Long userId;

    /** 被点赞对象ID（对局ID或评论ID） */
    private Long targetId;

    /** 对象类型：record / comment */
    private String targetType;

    /** 点赞时间（数据库默认值） */
    private LocalDateTime createTime;
}
