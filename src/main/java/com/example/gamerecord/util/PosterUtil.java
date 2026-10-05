package com.example.gamerecord.util;

import com.example.gamerecord.entity.GameRecord;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 分享海报生成工具
 *
 * <p>用 java.awt 原生绘制一张 600x850 的对局分享海报（无第三方依赖），
 * 输出 PNG 到 uploadDir 并返回文件名。封面图缺失时用游戏名占位。</p>
 */
@Component
public class PosterUtil {

    private static final int W = 600;
    private static final int H = 850;

    /** 生成海报，返回文件名（如 poster_xxx.png）；失败返回 null */
    public String generate(GameRecord record, String uploaderName, String uploadDir) {
        try {
            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 深色渐变背景
            g.setPaint(new GradientPaint(0, 0, new Color(15, 23, 42), 0, H, new Color(30, 41, 59)));
            g.fillRect(0, 0, W, H);

            // 平台标题
            g.setFont(font(Font.BOLD, 30));
            g.setColor(Color.WHITE);
            g.drawString("游戏对局记录", 40, 62);
            g.setFont(font(Font.PLAIN, 14));
            g.setColor(new Color(148, 163, 184));
            g.drawString("SHARE MATCH RECORD", 42, 90);

            // 封面区（500x300）
            int coverX = 50, coverY = 116, coverW = 500, coverH = 300;
            BufferedImage cover = loadCover(record.getCoverUrl(), uploadDir);
            if (cover != null) {
                g.setColor(new Color(255, 255, 255, 60));
                g.fillRoundRect(coverX - 3, coverY - 3, coverW + 6, coverH + 6, 14, 14);
                g.drawImage(cover, coverX, coverY, coverW, coverH, null);
                g.setColor(new Color(255, 255, 255, 26));
                g.fillRect(coverX, coverY, coverW, coverH);
            } else {
                g.setColor(new Color(51, 65, 85));
                g.fillRoundRect(coverX, coverY, coverW, coverH, 14, 14);
                g.setFont(font(Font.BOLD, 30));
                g.setColor(new Color(148, 163, 184));
                String title = record.getGameName() == null ? "" : record.getGameName();
                g.drawString(title, coverX + 30, coverY + 160);
            }

            // 游戏名 + 结果徽章
            g.setFont(font(Font.BOLD, 34));
            g.setColor(Color.WHITE);
            String gameName = record.getGameName() == null ? "" : record.getGameName();
            g.drawString(ellipsis(gameName, 9), 50, 470);

            String result = record.getResult() == null ? "" : record.getResult();
            if (!result.isEmpty()) {
                Color rc = switch (result) {
                    case "win" -> new Color(52, 199, 89);
                    case "lose" -> new Color(255, 69, 58);
                    default -> new Color(148, 163, 184);
                };
                String resultText = switch (result) {
                    case "win" -> "胜利";
                    case "lose" -> "失败";
                    default -> "平局";
                };
                g.setColor(rc);
                g.fillRoundRect(50, 488, 92, 38, 19, 19);
                g.setFont(font(Font.BOLD, 18));
                g.setColor(Color.WHITE);
                g.drawString(resultText, 76, 515);
            }

            // 信息行：模式 / 段位 / 日期
            g.setFont(font(Font.PLAIN, 20));
            g.setColor(new Color(203, 213, 225));
            String mode = record.getGameMode() == null ? "" : record.getGameMode();
            String rank = record.getRank() == null ? "" : record.getRank();
            String date = record.getMatchDate() == null ? "" : String.valueOf(record.getMatchDate());
            StringBuilder info = new StringBuilder();
            if (!mode.isEmpty()) info.append(mode);
            if (!rank.isEmpty()) info.append(info.length() > 0 ? "  ·  " : "").append(rank);
            if (!date.isEmpty()) info.append(info.length() > 0 ? "  ·  " : "").append(date);
            g.drawString(info.toString(), 50, 580);

            // 描述（最多两行）
            String desc = record.getDescription() == null ? "" : record.getDescription().trim();
            if (!desc.isEmpty()) {
                g.setFont(font(Font.PLAIN, 17));
                g.setColor(new Color(148, 163, 184));
                drawWrapped(g, desc, 50, 615, 500, 2);
            }

            // 分隔线 + 底部上传者
            g.setColor(new Color(148, 163, 184, 120));
            g.fillRect(50, 760, 500, 2);
            g.setFont(font(Font.BOLD, 20));
            g.setColor(Color.WHITE);
            String by = uploaderName == null || uploaderName.isEmpty() ? "神秘玩家" : uploaderName;
            g.drawString("@" + by + " 分享", 50, 800);
            g.setFont(font(Font.PLAIN, 15));
            g.setColor(new Color(148, 163, 184));
            g.drawString("游戏对局记录分享平台", W - 40 - 160, 800);

            g.dispose();

            String filename = "poster_" + UUID.randomUUID().toString().replace("-", "") + ".png";
            ImageIO.write(image, "png", new File(uploadDir, filename).getAbsoluteFile());
            return filename;
        } catch (IOException e) {
            return null;
        }
    }

    /** 从 /uploads/xxx.png 加载封面图（文件位于 uploadDir） */
    private BufferedImage loadCover(String coverUrl, String uploadDir) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        String name = coverUrl.substring(coverUrl.lastIndexOf('/') + 1);
        try {
            return ImageIO.read(new File(uploadDir, name));
        } catch (IOException e) {
            return null;
        }
    }

    private Font font(int style, int size) {
        return new Font("Microsoft YaHei", style, size);
    }

    private String ellipsis(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "…";
    }

    /** 按宽度换行绘制，最多 maxLines 行，超出加省略号 */
    private void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int maxLines) {
        java.awt.FontMetrics metrics = g.getFontMetrics();
        String[] parts = text.split("\n");
        StringBuilder line = new StringBuilder();
        int lineY = y;
        int lines = 0;
        String flat = String.join(" ", parts);
        for (int i = 0; i < flat.length(); i++) {
            line.append(flat.charAt(i));
            if (metrics.stringWidth(line.toString()) > maxWidth) {
                line.deleteCharAt(line.length() - 1);
                g.drawString(line.toString(), x, lineY);
                lines++;
                lineY += 28;
                if (lines >= maxLines) {
                    g.drawString("…", x + metrics.stringWidth(line.toString()) + 2, lineY);
                    return;
                }
                line.setLength(0);
                line.append(flat.charAt(i));
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, lineY);
        }
    }
}
