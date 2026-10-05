package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词（管理员后台维护，评论时自动过滤）
 */
@Data
@TableName("sensitive_word")
public class SensitiveWord {

    /** 敏感词ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 敏感词 */
    private String word;

    /** 添加时间（数据库默认值） */
    private LocalDateTime createTime;
}
