package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报记录（用户举报违规对局，管理员处理）
 */
@Data
@TableName("report")
public class Report {

    /** 举报ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被举报对局ID */
    private Long recordId;

    /** 举报人ID */
    private Long reporterId;

    /** 举报原因 */
    private String reason;

    /** 补充说明 */
    private String detail;

    /** 处理状态：0 待处理 / 1 已处理 */
    private Integer status;

    /** 处理结果说明 */
    private String handleResult;

    /** 举报时间（数据库默认值） */
    private LocalDateTime createTime;

    /** 处理时间 */
    private LocalDateTime handleTime;
}
