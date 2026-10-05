package com.example.gamerecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.entity.GameRecord;
import com.example.gamerecord.entity.Notification;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.mapper.GameRecordMapper;
import com.example.gamerecord.mapper.NotificationMapper;
import com.example.gamerecord.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 站内通知服务
 *
 * <p>在点赞、评论、回复时由对应 Service 调用 {@link #notify} 发送通知；
 * 通知统一关联到对局（targetType=record），前端点击跳转对局详情。</p>
 *
 * <p>同时维护 SSE 长连接（{@link #subscribe}），有新通知时向在线用户实时推送未读数，
 * 前端铃铛即时亮红点，无需刷新页面。</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_LIKE = "like";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_REPLY = "reply";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final GameRecordMapper recordMapper;

    /** 在线用户的 SSE 连接（同一用户可能多标签页，用列表管理） */
    private final Map<Long, List<SseEmitter>> sseClients = new ConcurrentHashMap<>();

    /**
     * 为指定用户建立 SSE 长连接（前端用 EventSource 连接，query 携带 token）
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = 不超时，靠心跳与断线回调清理
        // 客户端断开 / 超时 / 出错时从注册表移除该连接
        Runnable cleanup = () -> {
            List<SseEmitter> list = sseClients.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    sseClients.remove(userId);
                }
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError((e) -> cleanup.run());
        sseClients.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        // 首次连接立即推送一次当前未读数，保证前端拿到初始状态
        pushToUser(userId, unreadCount(userId));
        return emitter;
    }

    /** 向指定用户的全部在线连接推送未读数 */
    public void pushToUser(Long userId, long unread) {
        List<SseEmitter> list = sseClients.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("unread")
                        .data(Map.of("unread", unread)));
            } catch (IOException | IllegalStateException e) {
                // 连接已断开，交给 onError/onCompletion 清理
            }
        }
    }

    /** 当前在线连接数（监控用） */
    public int onlineCount() {
        return sseClients.size();
    }

    /**
     * 发送通知
     *
     * @param receiverId 接收者用户ID
     * @param actorId    触发者用户ID
     * @param type       like / comment / reply
     * @param recordId   关联对局ID（内容里的游戏名由此查出）
     */
    public void notify(Long receiverId, Long actorId, String type, Long recordId) {
        // 接收者为空或就是自己时不发通知
        if (receiverId == null || receiverId.equals(actorId)) {
            return;
        }
        GameRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        User actor = userMapper.selectById(actorId);
        String actorName = actor != null && actor.getNickname() != null ? actor.getNickname() : "用户";

        String content;
        if (TYPE_LIKE.equals(type)) {
            content = actorName + " 赞了你的对局《" + record.getGameName() + "》";
        } else if (TYPE_COMMENT.equals(type)) {
            content = actorName + " 评论了你的对局《" + record.getGameName() + "》";
        } else {
            content = actorName + " 回复了你的评论";
        }

        Notification notification = new Notification();
        notification.setUserId(receiverId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setContent(content);
        notification.setTargetType("record");
        notification.setTargetId(recordId);
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        // 实时推送：接收者在线时立即告知未读数变化
        pushToUser(receiverId, unreadCount(receiverId));
    }

    /** 当前用户通知分页（最新在前） */
    public PageResult<Notification> page(Long userId, Long pageNum, Long size) {
        Page<Notification> page = new Page<>(pageNum, size);
        notificationMapper.selectPage(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime));
        return PageResult.of(page, page.getRecords());
    }

    /** 未读通知数 */
    public Long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    /** 标记单条已读（只能操作自己的通知） */
    public void markRead(Long userId, Long id) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    /** 全部标记已读 */
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    /** 未读数量统计（给 Map 接口用） */
    public Map<String, Long> unreadCountMap(Long userId) {
        return Map.of("unread", unreadCount(userId));
    }
}
