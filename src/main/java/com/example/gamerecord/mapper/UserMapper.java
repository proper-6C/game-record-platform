package com.example.gamerecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gamerecord.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 某用户正常状态（未下架）的对局数 */
    @Select("SELECT COUNT(*) FROM game_record WHERE user_id = #{userId} AND status = 0")
    Long countPublicRecords(Long userId);

    /** 某用户收到的点赞总数（其正常对局被点赞的次数，like_record 为通用点赞表） */
    @Select("SELECT COUNT(*) FROM like_record l " +
            "JOIN game_record g ON g.id = l.target_id " +
            "WHERE g.user_id = #{userId} AND g.status = 0 AND l.target_type = 'record'")
    Long countReceivedLikes(Long userId);
}
