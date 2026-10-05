package com.example.gamerecord.service;

import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 邮箱验证码服务：发送验证码、校验（绑定邮箱 / 邮箱验证码登录）
 *
 * <p>验证码存内存（ConcurrentHashMap），5 分钟有效、60 秒限频、连续错 5 次作废；
 * 服务重启后验证码清空（可接受，属演示级实现）。</p>
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /** 验证码缓存：email -> 验证码条目 */
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    /** 发送最小间隔（毫秒） */
    private static final long SEND_INTERVAL_MS = 60_000L;
    /** 验证码有效期（毫秒） */
    private static final long CODE_TTL_MS = 5 * 60_000L;
    /** 最大错误次数 */
    private static final int MAX_FAIL = 5;
    /** 邮箱格式 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    /** 发送验证码到指定邮箱 */
    public void sendCode(String email, String type) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BizException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }
        long now = System.currentTimeMillis();
        CodeEntry entry = codeStore.get(email);
        if (entry != null && now - entry.lastSendAt < SEND_INTERVAL_MS) {
            throw new BizException(ResultCode.BAD_REQUEST, "发送太频繁，请 60 秒后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        codeStore.put(email, new CodeEntry(code, now + CODE_TTL_MS, now, 0));
        try {
            sendMail(email, type, code);
        } catch (Exception e) {
            // 发送失败：移除验证码，避免留下"能校验但收不到"的死码
            codeStore.remove(email);
            throw new BizException(ResultCode.ERROR, "验证码发送失败（请确认 SMTP 授权码已配置）");
        }
    }

    /** 校验验证码，通过后立即消费（删除） */
    public void verify(String email, String code) {
        CodeEntry entry = codeStore.get(email);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            codeStore.remove(email);
            throw new BizException(ResultCode.BAD_REQUEST, "验证码无效或已过期，请重新获取");
        }
        if (!entry.code.equals(code)) {
            entry.failCount++;
            if (entry.failCount >= MAX_FAIL) {
                codeStore.remove(email);
            }
            throw new BizException(ResultCode.BAD_REQUEST, "验证码错误");
        }
        codeStore.remove(email);
    }

    private void sendMail(String to, String type, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        String subject = switch (type) {
            case "register" -> "【游戏对局记录平台】注册验证码";
            case "bind" -> "【游戏对局记录平台】绑定邮箱验证码";
            case "login" -> "【游戏对局记录平台】登录验证码";
            default -> "【游戏对局记录平台】验证码";
        };
        msg.setSubject(subject);
        msg.setText("您的验证码是：" + code + "，5 分钟内有效。如非本人操作，请忽略本邮件。");
        mailSender.send(msg);
    }

    /** 验证码内存条目 */
    @Data
    @AllArgsConstructor
    private static class CodeEntry {
        private String code;
        private long expireAt;
        private long lastSendAt;
        private int failCount;
    }
}
