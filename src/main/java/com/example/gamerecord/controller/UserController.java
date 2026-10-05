package com.example.gamerecord.controller;

import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.Result;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.dto.LoginDTO;
import com.example.gamerecord.dto.RegisterDTO;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.service.UserService;
import com.example.gamerecord.util.UserContext;
import com.example.gamerecord.vo.UserProfileVO;
import com.example.gamerecord.vo.UserStatsVO;
import com.example.gamerecord.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户接口：注册、登录、当前用户信息
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    /** 登录，返回 { token, user } */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        User user = userService.getById(userId);
        return Result.success(userService.toVO(user));
    }

    /** 个人数据看板统计（需登录，个人中心用） */
    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        Long userId = UserContext.require();
        return Result.success(userService.stats(userId));
    }

    /** 玩家公开主页信息（无需登录） */
    @GetMapping("/{id}/profile")
    public Result<UserProfileVO> profile(@PathVariable Long id) {
        UserProfileVO vo = userService.profile(id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.success(vo);
    }
}
