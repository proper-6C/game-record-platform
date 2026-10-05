package com.example.gamerecord.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 主页统计数据：平台总览 + 热门游戏榜 + 玩家获赞榜
 */
@Data
public class HomeStatsVO {

    /** 已上传对局总数（正常状态） */
    private Long totalRecords;
    /** 累计对局点赞数 */
    private Long totalLikes;
    /** 累计评论数 */
    private Long totalComments;
    /** 注册玩家数 */
    private Long totalUsers;
    /** 热门游戏榜：{gameName, cnt} 前 5 */
    private List<Map<String, Object>> topGames;
    /** 玩家获赞榜：{userId, username, cnt} 前 5 */
    private List<Map<String, Object>> topPlayers;
}
