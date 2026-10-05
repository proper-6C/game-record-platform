package com.example.gamerecord.controller;

import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.Result;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.entity.Notification;
import com.example.gamerecord.service.NotificationService;
import com.example.gamerecord.util.JwtUtil;
import com.example.gamerecord.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 站内通知接口（需登录）
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    /** 我的通知分页 */
    @GetMapping("/list")
    public Result<PageResult<Notification>> list(@RequestParam(defaultValue = "1") Long page,
                                                 @RequestParam(defaultValue = "10") Long size) {
        Long userId = UserContext.require();
        return Result.success(notificationService.page(userId, page, size));
    }

    /**
     * 实时通知流（SSE，EventSource 无法带请求头，故用 query 携带 token）
     * <p>前端 new EventSource('/api/notification/stream?token=xxx')，监听 unread 事件。</p>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(value = "token", required = false) String token) {
        Long userId = token == null ? null : jwtUtil.parseUserId(token);
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return notificationService.subscribe(userId);
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = UserContext.require();
        return Result.success(notificationService.unreadCountMap(userId));
    }

    /** 标记单条已读 */
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        Long userId = UserContext.require();
        notificationService.markRead(userId, id);
        return Result.success(null);
    }

    /** 全部已读 */
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = UserContext.require();
        notificationService.markAllRead(userId);
        return Result.success(null);
    }
}
