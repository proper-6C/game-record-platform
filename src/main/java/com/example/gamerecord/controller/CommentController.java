package com.example.gamerecord.controller;

import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.Result;
import com.example.gamerecord.dto.CommentAddDTO;
import com.example.gamerecord.service.CommentService;
import com.example.gamerecord.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口：发表、分页查询、删除
 */
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 发表评论/回复（需登录） */
    @PostMapping("/add")
    public Result<CommentVO> add(@Valid @RequestBody CommentAddDTO dto) {
        return Result.success(commentService.add(dto));
    }

    /** 对局评论分页列表（一级评论 + 回复） */
    @GetMapping("/list")
    public Result<PageResult<CommentVO>> list(@RequestParam Long recordId,
                                              @RequestParam(defaultValue = "1") Long page,
                                              @RequestParam(defaultValue = "10") Long size) {
        return Result.success(commentService.pageByRecord(recordId, page, size));
    }

    /** 删除自己的评论（需登录） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteOwn(id);
        return Result.success();
    }
}
