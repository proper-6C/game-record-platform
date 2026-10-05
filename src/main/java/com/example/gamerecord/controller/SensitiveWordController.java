package com.example.gamerecord.controller;

import com.example.gamerecord.common.Result;
import com.example.gamerecord.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 敏感词管理接口（仅管理员）
 */
@RestController
@RequestMapping("/api/sensitive-word")
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    /** 敏感词列表（id + word） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(sensitiveWordService.list());
    }

    /** 添加敏感词 */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Map<String, String> body) {
        sensitiveWordService.add(body.get("word"));
        return Result.success();
    }

    /** 删除敏感词 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.success();
    }
}
