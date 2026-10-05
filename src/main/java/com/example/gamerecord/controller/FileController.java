package com.example.gamerecord.controller;

import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.Result;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 通用文件上传接口（评论配图等小文件）
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    /** 允许的图片格式 */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    /** 单张图片大小上限 5MB */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 上传图片，返回可直接访问的 URL */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.require();
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !IMAGE_EXTS.contains(ext.toLowerCase())) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持图片(jpg/png/gif/webp)");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
        File dest = new File(uploadDir, filename);
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            throw new BizException(ResultCode.ERROR, "文件保存失败，请重试");
        }
        return Result.success(Map.of("url", "/uploads/" + filename));
    }
}
