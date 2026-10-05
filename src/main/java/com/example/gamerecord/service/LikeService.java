package com.example.gamerecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.dto.LikeToggleDTO;
import com.example.gamerecord.entity.Comment;
import com.example.gamerecord.entity.GameRecord;
import com.example.gamerecord.entity.LikeRecord;
import com.example.gamerecord.mapper.CommentMapper;
import com.example.gamerecord.mapper.GameRecordMapper;
import com.example.gamerecord.mapper.LikeRecordMapper;
import com.example.gamerecord.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞服务：点赞/取消、状态查询
 *
 * <p>防刷：数据库 like_record 表对 (user_id, target_id, target_type) 建了唯一索引，
 * 同一用户对同一对象只会有一条记录。</p>
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    public static final String TYPE_RECORD = "record";
    public static final String TYPE_COMMENT = "comment";

    private final LikeRecordMapper likeRecordMapper;
    private final GameRecordMapper recordMapper;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    /** 点赞/取消点赞（toggle），返回 {liked, likeCount} */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggle(LikeToggleDTO dto) {
        Long userId = UserContext.require();
        String targetType = dto.getTargetType();
        if (!TYPE_RECORD.equals(targetType) && !TYPE_COMMENT.equals(targetType)) {
            throw new BizException(ResultCode.BAD_REQUEST, "targetType 只能是 record 或 comment");
        }
        // 校验目标存在
        checkTargetExists(dto.getTargetId(), targetType);

        LikeRecord existing = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, dto.getTargetId())
                .eq(LikeRecord::getTargetType, targetType));

        boolean liked;
        if (existing != null) {
            // 已点赞 -> 取消
            likeRecordMapper.deleteById(existing.getId());
            adjustCount(dto.getTargetId(), targetType, -1);
            liked = false;
        } else {
            // 未点赞 -> 点赞
            LikeRecord like = new LikeRecord();
            like.setUserId(userId);
            like.setTargetId(dto.getTargetId());
            like.setTargetType(targetType);
            likeRecordMapper.insert(like);
            adjustCount(dto.getTargetId(), targetType, 1);
            liked = true;

            // 发送通知（赞对局 -> 通知上传者；赞评论 -> 通知评论作者）
            if (TYPE_RECORD.equals(targetType)) {
                GameRecord record = recordMapper.selectById(dto.getTargetId());
                notificationService.notify(record.getUserId(), userId, NotificationService.TYPE_LIKE, record.getId());
            } else {
                Comment comment = commentMapper.selectById(dto.getTargetId());
                notificationService.notify(comment.getUserId(), userId, NotificationService.TYPE_LIKE, comment.getRecordId());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", getLikeCount(dto.getTargetId(), targetType));
        return result;
    }

    /** 查询当前用户对某对象的点赞状态（未登录返回 false） */
    public Map<String, Object> status(Long targetId, String targetType) {
        Long userId = UserContext.get();
        Map<String, Object> result = new HashMap<>();
        result.put("liked", userId != null && isLiked(targetId, targetType, userId));
        return result;
    }

    /** 是否已点赞 */
    public boolean isLiked(Long targetId, String targetType, Long userId) {
        return likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, targetId)
                .eq(LikeRecord::getTargetType, targetType)) > 0;
    }

    /** 目标存在性校验 */
    private void checkTargetExists(Long targetId, String targetType) {
        if (TYPE_RECORD.equals(targetType)) {
            if (recordMapper.selectById(targetId) == null) {
                throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
            }
        } else {
            Comment comment = commentMapper.selectById(targetId);
            if (comment == null || comment.getDeleted() == 1) {
                throw new BizException(ResultCode.NOT_FOUND, "评论不存在");
            }
        }
    }

    /** 更新点赞计数 */
    private void adjustCount(Long targetId, String targetType, int delta) {
        String sql = delta > 0
                ? "like_count = like_count + 1"
                : "like_count = GREATEST(like_count - 1, 0)";
        if (TYPE_RECORD.equals(targetType)) {
            recordMapper.update(null, new LambdaUpdateWrapper<GameRecord>()
                    .eq(GameRecord::getId, targetId)
                    .setSql(sql));
        } else {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, targetId)
                    .setSql(sql));
        }
    }

    /** 查询当前点赞数 */
    private Integer getLikeCount(Long targetId, String targetType) {
        if (TYPE_RECORD.equals(targetType)) {
            GameRecord record = recordMapper.selectById(targetId);
            return record == null ? 0 : record.getLikeCount();
        }
        Comment comment = commentMapper.selectById(targetId);
        return comment == null ? 0 : comment.getLikeCount();
    }
}
