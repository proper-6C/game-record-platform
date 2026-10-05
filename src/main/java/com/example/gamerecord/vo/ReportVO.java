package com.example.gamerecord.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报视图（管理员后台用，含被举报对局与举报人信息）
 */
@Data
public class ReportVO {

    private Long id;
    private Long recordId;

    /** 被举报对局游戏名 */
    private String recordName;

    /** 被举报对局当前状态：0 正常 / 1 已下架 */
    private Integer recordStatus;

    /** 对局上传者昵称 */
    private String uploaderName;

    /** 举报人昵称 */
    private String reporterName;

    private String reason;
    private String detail;
    private Integer status;
    private String handleResult;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
