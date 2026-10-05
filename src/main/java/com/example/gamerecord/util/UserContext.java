package com.example.gamerecord.util;

/**
 * 当前登录用户上下文：拦截器在请求入口写入，请求结束清理
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    /** 当前登录用户ID；未登录时为 null（匿名浏览场景） */
    public static Long get() {
        return USER_ID.get();
    }

    /** 获取当前登录用户ID，未登录时抛 401 */
    public static Long require() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new com.example.gamerecord.common.BizException(
                    com.example.gamerecord.common.ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
