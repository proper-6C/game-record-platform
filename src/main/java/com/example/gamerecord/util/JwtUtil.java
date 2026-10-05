package com.example.gamerecord.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：无状态登录令牌
 *
 * <p>token 内只携带 userId，签名由服务端密钥保证；重启服务后 token 依然有效，
 * 过期时间由 application.yml 的 jwt.expire-hours 控制（默认 7 天）。</p>
 */
@Component
public class JwtUtil {

    /** 签名密钥（HS256 要求至少 32 字节），生产环境务必修改 */
    @Value("${jwt.secret:gamerecord-jwt-secret-key-please-change-in-prod-2026}")
    private String secret;

    /** 过期时间（小时），默认 7 天 */
    @Value("${jwt.expire-hours:168}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 为指定用户生成 token */
    public String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(key())
                .compact();
    }

    /** 解析 token，有效返回 userId；无效 / 过期 / 伪造返回 null */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
