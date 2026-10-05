package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（被点赞 / 被评论 / 被回复）
 */
@Data
@TableName("notification")
public class Notification {

    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户ID */
    private Long userId;

    /** 触发通知的用户ID */
    private Long actorId;

    /** 通知类型：like / comment / reply */
    private String type;

    /** 通知内容（已组装好的展示文本，如「玩家一 赞了你的对局《英雄联盟》」） */
    private String content;

    /** 关联对象类型（统一为 record，点击跳转对局详情） */
    private String targetType;

    /** 关联对局ID */
    private Long targetId;

    /** 是否已读：0 未读 / 1 已读 */
    private Integer isRead;

    /** 通知时间（数据库默认值） */
    private LocalDateTime createTime;
}
