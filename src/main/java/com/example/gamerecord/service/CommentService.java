package com.example.gamerecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.dto.CommentAddDTO;
import com.example.gamerecord.entity.Comment;
import com.example.gamerecord.entity.GameRecord;
import com.example.gamerecord.entity.LikeRecord;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.mapper.CommentMapper;
import com.example.gamerecord.mapper.GameRecordMapper;
import com.example.gamerecord.mapper.LikeRecordMapper;
import com.example.gamerecord.mapper.UserMapper;
import com.example.gamerecord.util.UserContext;
import com.example.gamerecord.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论服务：发表、分页查询、删除
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final GameRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final NotificationService notificationService;
    private final SensitiveWordService sensitiveWordService;

    /** 发表评论/回复 */
    @Transactional(rollbackFor = Exception.class)
    public CommentVO add(CommentAddDTO dto) {
        Long userId = UserContext.require();

        GameRecord record = recordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }

        // 校验父评论存在
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0L) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || parent.getDeleted() == 1) {
                throw new BizException(ResultCode.NOT_FOUND, "被回复的评论不存在");
            }
        }

        Comment comment = new Comment();
        comment.setRecordId(dto.getRecordId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setToUserId(dto.getToUserId());
        comment.setContent(filterSensitive(dto.getContent()));
        comment.setImageUrl(dto.getImageUrl());
        comment.setLikeCount(0);
        comment.setDeleted(0);
        // 手动填充时间，保证发表接口立即返回 createTime
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        // 对局评论数 +1
        recordMapper.update(null, new LambdaUpdateWrapper<GameRecord>()
                .eq(GameRecord::getId, dto.getRecordId())
                .setSql("comment_count = comment_count + 1"));

        // 发送通知：一级评论通知对局上传者；回复通知被回复者
        if (parentId == 0L) {
            notificationService.notify(record.getUserId(), userId, NotificationService.TYPE_COMMENT, record.getId());
        } else if (dto.getToUserId() != null) {
            notificationService.notify(dto.getToUserId(), userId, NotificationService.TYPE_REPLY, record.getId());
        }

        return toVO(comment, userId);
    }

    /** 对局评论分页（一级评论 + 各自回复） */
    public PageResult<CommentVO> pageByRecord(Long recordId, Long pageNum, Long size) {
        Page<Comment> page = new Page<>(pageNum, size);
        commentMapper.selectPage(page, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRecordId, recordId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getDeleted, 0)
                .orderByDesc(Comment::getCreateTime));

        Long currentUserId = UserContext.get();
        List<CommentVO> list = page.getRecords().stream().map(c -> {
            CommentVO vo = toVO(c, currentUserId);
            // 加载二级回复（按时间正序）
            List<Comment> replies = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, c.getId())
                    .eq(Comment::getDeleted, 0)
                    .orderByAsc(Comment::getCreateTime));
            replies.forEach(r -> vo.getReplyList().add(toVO(r, currentUserId)));
            return vo;
        }).toList();

        return PageResult.of(page, list);
    }

    /** 删除自己的评论（软删除） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwn(Long id) {
        Long userId = UserContext.require();
        Comment comment = commentMapper.selectById(id);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BizException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能删除自己的评论");
        }
        comment.setDeleted(1);
        commentMapper.updateById(comment);

        // 对局评论数 -1（不低于 0）
        recordMapper.update(null, new LambdaUpdateWrapper<GameRecord>()
                .eq(GameRecord::getId, comment.getRecordId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
    }

    /** 实体转视图 */
    private CommentVO toVO(Comment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setRecordId(comment.getRecordId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setToUserId(comment.getToUserId());
        vo.setContent(comment.getContent());
        vo.setImageUrl(comment.getImageUrl());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreateTime(comment.getCreateTime());

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        // 回复目标昵称
        if (comment.getToUserId() != null) {
            User toUser = userMapper.selectById(comment.getToUserId());
            vo.setToUserName(toUser == null ? null : toUser.getNickname());
        }
        vo.setLiked(currentUserId != null && likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, currentUserId)
                        .eq(LikeRecord::getTargetId, comment.getId())
                        .eq(LikeRecord::getTargetType, "comment")) > 0);
        return vo;
    }

    /** 敏感词过滤（词库来自数据库，管理员后台可维护） */
    private String filterSensitive(String content) {
        String result = content;
        for (String word : sensitiveWordService.getWords()) {
            result = result.replace(word, "*".repeat(word.length()));
        }
        return result;
    }
}
