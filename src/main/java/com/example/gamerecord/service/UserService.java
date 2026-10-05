package com.example.gamerecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gamerecord.common.BizException;
import com.example.gamerecord.dto.LoginDTO;
import com.example.gamerecord.dto.RegisterDTO;
import com.example.gamerecord.entity.Comment;
import com.example.gamerecord.entity.GameRecord;
import com.example.gamerecord.entity.LikeRecord;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.mapper.CommentMapper;
import com.example.gamerecord.mapper.GameRecordMapper;
import com.example.gamerecord.mapper.LikeRecordMapper;
import com.example.gamerecord.mapper.UserMapper;
import com.example.gamerecord.util.JwtUtil;
import com.example.gamerecord.vo.UserProfileVO;
import com.example.gamerecord.vo.UserStatsVO;
import com.example.gamerecord.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务：注册、登录、数据看板统计
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final GameRecordMapper recordMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** 注册（可选绑定邮箱：dto 携带 email + code 时自动验证并绑定） */
    public UserVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(com.example.gamerecord.common.ResultCode.BAD_REQUEST, "用户名已存在");
        }

        if (org.springframework.util.StringUtils.hasText(dto.getEmail())) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (emailCount >= 2) {
                throw new BizException(com.example.gamerecord.common.ResultCode.BAD_REQUEST, "该邮箱已绑定 2 个账号，无法继续绑定");
            }
            emailService.verify(dto.getEmail(), dto.getCode());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
            user.setEmailVerified(1);
        }
        userMapper.insert(user);
        return toVO(user);
    }

    /** 登录，返回 token + 用户信息 */
    public Map<String, Object> login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(com.example.gamerecord.common.ResultCode.BAD_REQUEST, "用户名或密码错误");
        }

        // 生成 JWT 令牌（无状态，默认 7 天有效）
        String token = jwtUtil.createToken(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", toVO(user));
        return data;
    }

    /** 邮箱验证码登录：校验验证码后按绑定邮箱找到用户并签发 JWT（同邮箱绑定多个账号时，登录最早绑定的账号） */
    public Map<String, Object> emailLogin(String email, String code) {
        emailService.verify(email, code);
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email).orderByAsc(User::getId));
        if (users.isEmpty()) {
            throw new BizException(com.example.gamerecord.common.ResultCode.NOT_FOUND, "该邮箱未绑定账号，请先注册或绑定");
        }
        User user = users.get(0);
        String token = jwtUtil.createToken(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", toVO(user));
        // 提示该邮箱绑定的账号数（便于前端展示，登录默认进入最早绑定账号）
        data.put("boundCount", users.size());
        return data;
    }

    /** 绑定邮箱到当前用户（校验验证码 + 最多 2 个账号） */
    public UserVO bindEmail(Long userId, String email, String code) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (count >= 2) {
            throw new BizException(com.example.gamerecord.common.ResultCode.BAD_REQUEST, "该邮箱已绑定 2 个账号，无法继续绑定");
        }
        emailService.verify(email, code);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(com.example.gamerecord.common.ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setEmail(email);
        user.setEmailVerified(1);
        userMapper.updateById(user);
        return toVO(user);
    }

    /** 解绑邮箱：email 必须等于当前绑定邮箱，且验证码通过 */
    public UserVO unbindEmail(Long userId, String email, String code) {
        User user = userMapper.selectById(userId);
        if (user == null || !email.equals(user.getEmail())) {
            throw new BizException(com.example.gamerecord.common.ResultCode.BAD_REQUEST, "邮箱与当前绑定不一致");
        }
        emailService.verify(email, code);
        user.setEmail(null);
        user.setEmailVerified(0);
        userMapper.updateById(user);
        return toVO(user);
    }

    /** 按 ID 查询用户 */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /** 个人数据看板统计（上传数 / 获赞 / 胜率 / 常用游戏等） */
    public UserStatsVO stats(Long userId) {
        UserStatsVO vo = new UserStatsVO();

        // 该用户上传的所有对局
        List<GameRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<GameRecord>().eq(GameRecord::getUserId, userId));
        vo.setTotalUploads((long) records.size());
        vo.setTotalLikesReceived(records.stream().mapToLong(GameRecord::getLikeCount).sum());

        // 胜率（胜利局数 / 总局数，保留两位小数；无对局时为 0）
        long wins = records.stream().filter(r -> "win".equals(r.getResult())).count();
        vo.setWinRate(records.isEmpty() ? 0.0 : Math.round(wins * 10000.0 / records.size()) / 100.0);

        // 发出的点赞数
        vo.setTotalLikesGiven(likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId)
                        .eq(LikeRecord::getTargetType, LikeService.TYPE_RECORD)));

        // 收到的评论数（自己对局下的评论）
        List<Long> recordIds = records.stream().map(GameRecord::getId).toList();
        vo.setTotalCommentsReceived(recordIds.isEmpty() ? 0L
                : commentMapper.selectCount(new LambdaQueryWrapper<Comment>().in(Comment::getRecordId, recordIds)));

        // 常用游戏 Top 3（按局数降序）
        Map<String, Long> gameCounts = new HashMap<>();
        for (GameRecord r : records) {
            gameCounts.merge(r.getGameName(), 1L, Long::sum);
        }
        vo.setTopGames(gameCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    UserStatsVO.GameStatVO g = new UserStatsVO.GameStatVO();
                    g.setGameName(e.getKey());
                    g.setCount(e.getValue());
                    return g;
                })
                .toList());
        return vo;
    }

    /** 玩家公开主页信息（无需登录；用户不存在时返回 null） */
    public UserProfileVO profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setRecordCount(userMapper.countPublicRecords(userId));
        vo.setTotalLikes(userMapper.countReceivedLikes(userId));
        return vo;
    }

    /** 脱敏转换 */
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setEmailVerified(user.getEmailVerified() != null && user.getEmailVerified() == 1);
        vo.setRole(user.getRole());
        return vo;
    }
}
