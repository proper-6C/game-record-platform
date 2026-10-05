package com.example.gamerecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gamerecord.entity.GameRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 对局记录 Mapper
 */
@Mapper
public interface GameRecordMapper extends BaseMapper<GameRecord> {

    /** 正常状态（未下架）对局总数，主页统计用 */
    @Select("SELECT COUNT(*) FROM game_record WHERE status = 0")
    Long countPublic();

    /** 热门游戏榜：按对局数倒序取前 5 名（主页统计用） */
    @Select("SELECT game_name AS gameName, COUNT(*) AS cnt FROM game_record WHERE status = 0 " +
            "GROUP BY game_name ORDER BY cnt DESC LIMIT 5")
    List<Map<String, Object>> selectTopGames();

    /** 玩家获赞榜：按对局获赞数倒序取前 5 名玩家（主页统计用） */
    @Select("SELECT g.user_id AS userId, u.nickname AS username, COUNT(l.id) AS cnt " +
            "FROM game_record g JOIN like_record l ON l.target_id = g.id AND l.target_type = 'record' " +
            "JOIN user u ON g.user_id = u.id WHERE g.status = 0 " +
            "GROUP BY g.user_id, u.nickname ORDER BY cnt DESC LIMIT 5")
    List<Map<String, Object>> selectTopPlayers();
}
