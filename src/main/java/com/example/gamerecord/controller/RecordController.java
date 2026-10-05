package com.example.gamerecord.controller;

import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.Result;
import com.example.gamerecord.dto.RecordEditDTO;
import com.example.gamerecord.dto.RecordQueryDTO;
import com.example.gamerecord.dto.RecordUploadDTO;
import com.example.gamerecord.service.RecordService;
import com.example.gamerecord.vo.RecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 对局记录接口：上传、列表、详情、删除
 */
@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    /**
     * 上传对局
     * <p>请求体为 multipart/form-data：</p>
     * <ul>
     *   <li>file：对局文件（图片或视频）</li>
     *   <li>gameName、gameMode、matchDate、result、rank、description：对局信息字段</li>
     * </ul>
     */
    @PostMapping("/upload")
    public Result<RecordVO> upload(@Valid @ModelAttribute RecordUploadDTO dto,
                                   @RequestParam(value = "file", required = false) MultipartFile file) {
        return Result.success(recordService.upload(dto, file));
    }

    /** 对局列表（分页 + 搜索 + 排序） */
    @GetMapping("/list")
    public Result<PageResult<RecordVO>> list(@ModelAttribute RecordQueryDTO dto) {
        return Result.success(recordService.page(dto));
    }

    /** 主页统计：总对局/点赞/评论/用户 + 热门游戏榜 + 玩家获赞榜（无需登录） */
    @GetMapping("/stats")
    public Result<com.example.gamerecord.vo.HomeStatsVO> stats() {
        return Result.success(recordService.homeStats());
    }

    /** 对局详情 */
    @GetMapping("/{id}")
    public Result<RecordVO> detail(@PathVariable Long id) {
        return Result.success(recordService.getDetail(id));
    }

    /** 我点赞过的对局（需登录，个人中心用） */
    @GetMapping("/liked")
    public Result<PageResult<RecordVO>> liked(@RequestParam(defaultValue = "1") Long page,
                                              @RequestParam(defaultValue = "10") Long size) {
        return Result.success(recordService.likedPage(page, size));
    }

    /** 删除自己的对局 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.deleteOwn(id);
        return Result.success();
    }

    /** 生成分享海报（需登录），返回海报图片 URL */
    @PostMapping("/{id}/share-poster")
    public Result<Map<String, String>> sharePoster(@PathVariable Long id) {
        String url = recordService.generatePoster(id);
        return Result.success(Map.of("url", url));
    }

    /** 编辑自己的对局信息 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RecordEditDTO dto) {
        recordService.update(id, dto);
        return Result.success();
    }
}
