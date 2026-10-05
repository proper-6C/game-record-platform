package com.example.gamerecord.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对局记录视图（含上传者信息和当前用户点赞状态）
 */
@Data
public class RecordVO {

    private Long id;
    private String gameName;
    private String gameMode;
    private LocalDate matchDate;
    private String result;
    private String rank;
    private String description;
    /** 标签数组（由逗号分隔的 tags 拆出） */
    private java.util.List<String> tags;
    private String coverUrl;
    private String videoUrl;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;

    /** 上传者信息 */
    private Long uploaderId;
    private String uploaderName;
    private String uploaderAvatar;

    /** 当前登录用户是否已点赞 */
    private Boolean liked;
}
