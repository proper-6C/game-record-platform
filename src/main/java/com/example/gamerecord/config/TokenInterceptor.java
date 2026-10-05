package com.example.gamerecord.config;

import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.util.JwtUtil;
import com.example.gamerecord.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 *
 * <p>规则：</p>
 * <ul>
 *   <li>写请求（POST/PUT/DELETE/PATCH）：必须登录，未登录返回 401</li>
 *   <li>读请求（GET）：允许匿名，携带有效 token 时注入用户上下文（用于回显点赞状态）</li>
 * </ul>
 * <p>令牌采用 JWT 无状态方案（{@link JwtUtil}），重启服务后依然有效。</p>
 */
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        Long userId = null;
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            userId = jwtUtil.parseUserId(auth.substring(BEARER_PREFIX.length()));
        }

        String method = request.getMethod();
        boolean isWriteMethod = HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method)
                || HttpMethod.PATCH.matches(method);

        if (isWriteMethod && userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录后再操作");
        }

        if (userId != null) {
            UserContext.set(userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，防止线程复用导致用户串号
        UserContext.clear();
    }
}
