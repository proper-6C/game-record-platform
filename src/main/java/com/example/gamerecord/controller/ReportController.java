package com.example.gamerecord.controller;

import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.Result;
import com.example.gamerecord.dto.ReportAddDTO;
import com.example.gamerecord.dto.ReportHandleDTO;
import com.example.gamerecord.service.ReportService;
import com.example.gamerecord.vo.ReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报接口：用户提交举报，管理员查询与处理
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 提交举报（需登录） */
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody ReportAddDTO dto) {
        reportService.add(dto);
        return Result.success();
    }

    /** 举报分页列表（仅管理员，可按状态过滤） */
    @GetMapping("/list")
    public Result<PageResult<ReportVO>> list(@RequestParam(defaultValue = "1") Long page,
                                             @RequestParam(defaultValue = "10") Long size,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(reportService.page(page, size, status));
    }

    /** 处理举报（仅管理员）：驳回 或 下架对局 */
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody ReportHandleDTO dto) {
        reportService.handle(id, dto);
        return Result.success();
    }

    /** 恢复已下架对局（仅管理员） */
    @PostMapping("/restore-record")
    public Result<Void> restore(@RequestParam Long recordId) {
        reportService.restoreRecord(recordId);
        return Result.success();
    }
}
