package com.example.gamerecord.dto;

import lombok.Data;

/**
 * 对局列表查询参数
 */
@Data
public class RecordQueryDTO {

    /** 页码，从 1 开始 */
    private Long page = 1L;

    /** 每页条数 */
    private Long size = 10L;

    /** 游戏名称（模糊搜索，可空） */
    private String gameName;

    /** 按上传者过滤（个人中心"我的对局"使用，可空） */
    private Long userId;

    /** 段位（模糊搜索，可空） */
    private String rank;

    /** 标签（模糊搜索，可空，如 翻盘） */
    private String tag;

    /** 排序：latest 最新 / hot 最热（按点赞数），默认 latest */
    private String sortBy = "latest";
}
