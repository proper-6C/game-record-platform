package com.example.gamerecord.controller;

import com.example.gamerecord.common.Result;
import com.example.gamerecord.dto.EmailBindDTO;
import com.example.gamerecord.dto.EmailLoginDTO;
import com.example.gamerecord.dto.EmailSendDTO;
import com.example.gamerecord.service.EmailService;
import com.example.gamerecord.service.UserService;
import com.example.gamerecord.util.UserContext;
import com.example.gamerecord.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 邮箱接口：发送验证码、绑定/解绑邮箱、邮箱验证码登录
 */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final UserService userService;

    /** 发送验证码（注册/绑定/登录共用） */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody EmailSendDTO dto) {
        emailService.sendCode(dto.getEmail(), dto.getType());
        return Result.success();
    }

    /** 绑定邮箱到当前登录用户 */
    @PostMapping("/bind")
    public Result<UserVO> bind(@Valid @RequestBody EmailBindDTO dto) {
        Long userId = UserContext.require();
        return Result.success(userService.bindEmail(userId, dto.getEmail(), dto.getCode()));
    }

    /** 解绑邮箱（需当前绑定邮箱收到的验证码） */
    @PostMapping("/unbind")
    public Result<UserVO> unbind(@Valid @RequestBody EmailBindDTO dto) {
        Long userId = UserContext.require();
        return Result.success(userService.unbindEmail(userId, dto.getEmail(), dto.getCode()));
    }

    /** 邮箱验证码登录（无需密码） */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody EmailLoginDTO dto) {
        return Result.success(userService.emailLogin(dto.getEmail(), dto.getCode()));
    }
}
