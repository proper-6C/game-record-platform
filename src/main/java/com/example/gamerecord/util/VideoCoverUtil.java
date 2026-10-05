package com.example.gamerecord.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 视频封面抽取工具
 *
 * <p>调用 ffmpeg 截取视频第一帧作为封面图（jpg，宽度限制 640px）。
 * ffmpeg 路径由 application.yml 的 ffmpeg.path 配置，默认取系统 PATH 中的 ffmpeg。</p>
 */
@Slf4j
@Component
public class VideoCoverUtil {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    /**
     * 从视频抽取封面，成功返回封面文件名，失败返回 null（不阻断上传流程）
     * <p>先取第 1 秒画面，失败后自动换第 3 秒重试一次，提高大视频/黑帧视频的成功率。</p>
     *
     * @param videoFile 已保存的视频文件
     * @param uploadDir 上传目录（封面也存这里）
     */
    public String extractCover(File videoFile, String uploadDir) {
        String coverName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        File coverFile = new File(uploadDir, coverName);
        // 优先第 1 秒；失败换第 3 秒重试
        String[] ssOffsets = {"1", "3"};
        for (String ss : ssOffsets) {
            if (extractOnce(videoFile, coverFile, ss)) {
                log.info("视频封面已生成: {} (ss={}s)", coverName, ss);
                return coverName;
            }
            coverFile.delete();
        }
        log.warn("ffmpeg 抽帧失败（已重试）: {}", videoFile.getName());
        return null;
    }

    /** 单次抽帧：成功且文件有效返回 true */
    private boolean extractOnce(File videoFile, File coverFile, String ss) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",                        // 覆盖输出
                    "-i", videoFile.getAbsolutePath(),
                    "-ss", ss,                   // 取第 ss 秒画面（避免首帧黑屏）
                    "-vframes", "1",             // 只输出一帧
                    "-vf", "scale=640:-1",       // 宽度 640，等比缩放，控制体积
                    coverFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // 读掉输出流，避免缓冲区写满导致进程阻塞
            process.getInputStream().readAllBytes();

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffmpeg 抽帧超时: {} (ss={}s)", videoFile.getName(), ss);
                return false;
            }
            // 长度低于 1KB 视为无效封面（全黑/损坏）
            return process.exitValue() == 0 && coverFile.exists() && coverFile.length() > 1024;
        } catch (Exception e) {
            // 抽帧失败不影响视频上传，仅记录日志
            log.warn("ffmpeg 执行异常: {}", e.getMessage());
            return false;
        }
    }
}
