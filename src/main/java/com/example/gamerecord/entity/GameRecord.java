package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游戏对局记录
 */
@Data
@TableName("game_record")
public class GameRecord {

    /** 对局ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上传用户ID */
    private Long userId;

    /** 游戏名称 */
    private String gameName;

    /** 游戏模式（排位/匹配等） */
    private String gameMode;

    /** 对局日期 */
    private LocalDate matchDate;

    /** 对局结果：win/lose/draw */
    private String result;

    /** 段位（rank 为 MySQL 保留字，需反引号转义） */
    @TableField("`rank`")
    private String rank;

    /** 对局描述 */
    private String description;

    /** 标签（逗号分隔，如 翻盘,五杀,排位） */
    private String tags;

    /** 封面图URL */
    private String coverUrl;

    /** 对局视频URL */
    private String videoUrl;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 状态：0 正常 / 1 已下架（违规举报处理） */
    private Integer status;

    /** 上传时间（数据库默认值） */
    private LocalDateTime createTime;
}
