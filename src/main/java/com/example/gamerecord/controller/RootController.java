package com.example.gamerecord.controller;

import com.example.gamerecord.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 根路径欢迎页：避免浏览器直接访问 http://localhost:8080/ 时无映射而报 500
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Result<Map<String, String>> index() {
        return Result.success(Map.of(
                "service", "game-record",
                "message", "后端服务运行中，API 接口见项目 README.md",
                "status", "ok"
        ));
    }
}
