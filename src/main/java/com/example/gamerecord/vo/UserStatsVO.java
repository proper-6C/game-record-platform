package com.example.gamerecord.vo;

import lombok.Data;

import java.util.List;

/**
 * 个人数据看板统计（个人中心）
 */
@Data
public class UserStatsVO {

    /** 上传对局总数 */
    private Long totalUploads;

    /** 收到的点赞总数（自己上传的对局被赞次数） */
    private Long totalLikesReceived;

    /** 发出的点赞总数（赞过别人的对局次数） */
    private Long totalLikesGiven;

    /** 收到的评论总数 */
    private Long totalCommentsReceived;

    /** 胜率（0-100，两位小数；没有对局时为 0） */
    private Double winRate;

    /** 常用游戏 Top 3（按上传局数降序） */
    private List<GameStatVO> topGames;

    /** 单个游戏统计 */
    @Data
    public static class GameStatVO {
        private String gameName;
        private Long count;
    }
}
