package com.example.gamerecord.controller;

import com.example.gamerecord.common.Result;
import com.example.gamerecord.dto.LikeToggleDTO;
import com.example.gamerecord.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 点赞接口：点赞/取消、状态查询
 */
@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /** 点赞/取消点赞（需登录），返回 { liked, likeCount } */
    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggle(@Valid @RequestBody LikeToggleDTO dto) {
        return Result.success(likeService.toggle(dto));
    }

    /** 当前用户点赞状态，返回 { liked } */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam Long targetId,
                                              @RequestParam String targetType) {
        return Result.success(likeService.status(targetId, targetType));
    }
}
