package com.example.gamerecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.dto.RecordEditDTO;
import com.example.gamerecord.dto.RecordQueryDTO;
import com.example.gamerecord.dto.RecordUploadDTO;
import com.example.gamerecord.entity.Comment;
import com.example.gamerecord.entity.GameRecord;
import com.example.gamerecord.entity.LikeRecord;
import com.example.gamerecord.entity.Notification;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.mapper.CommentMapper;
import com.example.gamerecord.mapper.GameRecordMapper;
import com.example.gamerecord.mapper.LikeRecordMapper;
import com.example.gamerecord.mapper.NotificationMapper;
import com.example.gamerecord.mapper.UserMapper;
import com.example.gamerecord.util.JwtUtil;
import com.example.gamerecord.util.PosterUtil;
import com.example.gamerecord.util.UserContext;
import com.example.gamerecord.util.VideoCoverUtil;import com.example.gamerecord.vo.HomeStatsVO;
import com.example.gamerecord.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 对局记录服务：上传、列表、详情、删除
 */
@Service
@RequiredArgsConstructor
public class RecordService {

    /** 允许的图片格式 */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    /** 允许的视频格式 */
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "mov");

    private final GameRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final NotificationMapper notificationMapper;
    private final VideoCoverUtil videoCoverUtil;
    private final PosterUtil posterUtil;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 上传对局（图片或视频） */
    public RecordVO upload(RecordUploadDTO dto, MultipartFile file) {
        Long userId = UserContext.require();
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }

        // 校验文件类型
        String originalName = file.getOriginalFilename();
        String ext = extractExt(originalName);
        boolean isImage = IMAGE_EXTS.contains(ext);
        boolean isVideo = VIDEO_EXTS.contains(ext);
        if (!isImage && !isVideo) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持图片(jpg/png/gif/webp)或视频(mp4/mov)");
        }

        // UUID 重命名保存
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dest = new File(uploadDir, filename);
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            throw new BizException(ResultCode.ERROR, "文件保存失败，请重试");
        }
        String url = "/uploads/" + filename;

        GameRecord record = new GameRecord();
        record.setUserId(userId);
        record.setGameName(dto.getGameName());
        record.setGameMode(dto.getGameMode());
        record.setMatchDate(dto.getMatchDate());
        record.setResult(dto.getResult());
        record.setRank(dto.getRank());
        record.setDescription(dto.getDescription());
        record.setTags(dto.getTags());
        if (isImage) {
            // 图片直接作为封面
            record.setCoverUrl(url);
        } else {
            // 视频：videoUrl 存视频地址，封面用 ffmpeg 抽第一帧（失败则封面为空，前端显示占位图）
            record.setVideoUrl(url);
            String coverName = videoCoverUtil.extractCover(dest.getAbsoluteFile(), uploadDir);
            if (coverName != null) {
                record.setCoverUrl("/uploads/" + coverName);
            }
        }
        record.setLikeCount(0);
        record.setCommentCount(0);
        // 手动填充时间，保证上传接口立即返回 createTime（数据库默认值不回填内存对象）
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);

        return toVO(record, userId);
    }

    /** 主页统计：平台总览 + 热门游戏榜 + 玩家获赞榜（无需登录） */
    public HomeStatsVO homeStats() {
        HomeStatsVO vo = new HomeStatsVO();
        vo.setTotalRecords(recordMapper.countPublic());
        vo.setTotalLikes(likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetType, LikeService.TYPE_RECORD)));
        vo.setTotalComments(commentMapper.selectCount(null));
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setTopGames(recordMapper.selectTopGames());
        vo.setTopPlayers(recordMapper.selectTopPlayers());
        return vo;
    }

    /** 对局列表（支持搜索 + 最新/最热排序） */
    public PageResult<RecordVO> page(RecordQueryDTO dto) {        Page<GameRecord> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<GameRecord> wrapper = new LambdaQueryWrapper<>();
        // 普通列表只显示正常状态（已下架的对局隐藏）
        wrapper.eq(GameRecord::getStatus, 0);
        if (StringUtils.hasText(dto.getGameName())) {
            wrapper.like(GameRecord::getGameName, dto.getGameName());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(GameRecord::getUserId, dto.getUserId());
        }
        if (StringUtils.hasText(dto.getRank())) {
            wrapper.like(GameRecord::getRank, dto.getRank());
        }
        if (StringUtils.hasText(dto.getTag())) {
            wrapper.like(GameRecord::getTags, dto.getTag());
        }
        if ("hot".equalsIgnoreCase(dto.getSortBy())) {
            wrapper.orderByDesc(GameRecord::getLikeCount).orderByDesc(GameRecord::getCreateTime);
        } else {
            wrapper.orderByDesc(GameRecord::getCreateTime);
        }
        recordMapper.selectPage(page, wrapper);

        Long currentUserId = UserContext.get();
        List<RecordVO> list = page.getRecords().stream()
                .map(r -> toVO(r, currentUserId))
                .toList();
        return PageResult.of(page, list);
    }

    /** 对局详情 */
    public RecordVO getDetail(Long id) {
        GameRecord record = recordMapper.selectById(id);
        if (record == null || record.getStatus() == 1) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }
        return toVO(record, UserContext.get());
    }

    /** 我点赞过的对局（个人中心） */
    public PageResult<RecordVO> likedPage(Long pageNum, Long size) {
        Long userId = UserContext.require();

        // 当前用户点赞过的对局 ID（按点赞时间倒序）
        List<LikeRecord> likes = likeRecordMapper.selectList(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, LikeService.TYPE_RECORD)
                .orderByDesc(LikeRecord::getCreateTime));

        Page<GameRecord> page = new Page<>(pageNum, size);
        if (likes.isEmpty()) {
            return PageResult.of(page, List.of());
        }
        List<Long> ids = likes.stream().map(LikeRecord::getTargetId).toList();
        recordMapper.selectPage(page, new LambdaQueryWrapper<GameRecord>()
                .in(GameRecord::getId, ids)
                .eq(GameRecord::getStatus, 0)
                .orderByDesc(GameRecord::getCreateTime));

        List<RecordVO> list = page.getRecords().stream()
                .map(r -> toVO(r, userId))
                .toList();
        return PageResult.of(page, list);
    }

    /** 删除自己的对局（级联删除评论与点赞） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwn(Long id) {
        Long userId = UserContext.require();
        GameRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能删除自己上传的对局");
        }

        // 删除该对局下的评论，及其点赞
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getRecordId, id));
        for (Comment comment : comments) {
            likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                    .eq(LikeRecord::getTargetId, comment.getId())
                    .eq(LikeRecord::getTargetType, "comment"));
        }
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getRecordId, id));
        // 删除对局本身的点赞
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetId, id)
                .eq(LikeRecord::getTargetType, "record"));
        // 删除关联该对局的通知
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetType, "record")
                .eq(Notification::getTargetId, id));
        recordMapper.deleteById(id);
    }

    /** 生成对局分享海报，返回海报 URL（需登录） */
    public String generatePoster(Long id) {
        GameRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }
        User uploader = userMapper.selectById(record.getUserId());
        String filename = posterUtil.generate(record,
                uploader == null ? null : uploader.getNickname(), uploadDir);
        if (filename == null) {
            throw new BizException(ResultCode.ERROR, "海报生成失败，请重试");
        }
        return "/uploads/" + filename;
    }

    /** 编辑自己的对局（仅信息字段，文件/封面不变） */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RecordEditDTO dto) {
        Long userId = UserContext.require();
        GameRecord record = recordMapper.selectById(id);
        if (record == null || record.getStatus() == 1) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能编辑自己上传的对局");
        }
        record.setGameName(dto.getGameName());
        record.setGameMode(dto.getGameMode());
        record.setMatchDate(dto.getMatchDate());
        record.setResult(dto.getResult());
        record.setRank(dto.getRank());
        record.setDescription(dto.getDescription());
        record.setTags(dto.getTags());
        recordMapper.updateById(record);
    }

    /** 实体转视图：补充上传者信息与当前用户点赞状态 */
    private RecordVO toVO(GameRecord record, Long currentUserId) {
        RecordVO vo = new RecordVO();
        vo.setId(record.getId());
        vo.setGameName(record.getGameName());
        vo.setGameMode(record.getGameMode());
        vo.setMatchDate(record.getMatchDate());
        vo.setResult(record.getResult());
        vo.setRank(record.getRank());
        vo.setDescription(record.getDescription());
        vo.setTags(splitTags(record.getTags()));
        vo.setCoverUrl(record.getCoverUrl());
        vo.setVideoUrl(record.getVideoUrl());
        vo.setLikeCount(record.getLikeCount());
        vo.setCommentCount(record.getCommentCount());
        vo.setCreateTime(record.getCreateTime());
        vo.setUploaderId(record.getUserId());

        User uploader = userMapper.selectById(record.getUserId());
        if (uploader != null) {
            vo.setUploaderName(uploader.getNickname());
            vo.setUploaderAvatar(uploader.getAvatar());
        }
        vo.setLiked(isLiked(record.getId(), "record", currentUserId));
        return vo;
    }

    /** 逗号分隔的标签字符串 -> 去空数组 */
    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /** 查询当前用户是否已点赞 */
    private boolean isLiked(Long targetId, String targetType, Long userId) {
        if (userId == null) {
            return false;
        }
        return likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, targetId)
                .eq(LikeRecord::getTargetType, targetType)) > 0;
    }

    /** 提取文件扩展名（转小写，不含点） */
    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
