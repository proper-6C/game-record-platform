package com.example.gamerecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.PageResult;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.dto.ReportAddDTO;
import com.example.gamerecord.dto.ReportHandleDTO;
import com.example.gamerecord.entity.GameRecord;
import com.example.gamerecord.entity.Report;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.mapper.GameRecordMapper;
import com.example.gamerecord.mapper.ReportMapper;
import com.example.gamerecord.mapper.UserMapper;
import com.example.gamerecord.util.UserContext;
import com.example.gamerecord.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报服务：提交举报、管理员分页查询、处理举报（下架/驳回）、恢复上架
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final GameRecordMapper recordMapper;
    private final UserMapper userMapper;

    /** 提交举报（同一用户对同一对局只能举报一次） */
    public void add(ReportAddDTO dto) {
        Long userId = UserContext.require();
        GameRecord record = recordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }
        if (record.getUserId().equals(userId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能举报自己的对局");
        }
        if (reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getRecordId, dto.getRecordId())
                .eq(Report::getReporterId, userId)) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "你已举报过该对局");
        }

        Report report = new Report();
        report.setRecordId(dto.getRecordId());
        report.setReporterId(userId);
        report.setReason(dto.getReason());
        report.setDetail(dto.getDetail());
        report.setStatus(0);
        reportMapper.insert(report);
    }

    /** 管理员分页查询举报（按状态过滤） */
    public PageResult<ReportVO> page(Long pageNum, Long size, Integer status) {
        requireAdmin();
        Page<Report> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreateTime);
        reportMapper.selectPage(page, wrapper);

        List<ReportVO> list = page.getRecords().stream().map(r -> {
            ReportVO vo = new ReportVO();
            vo.setId(r.getId());
            vo.setRecordId(r.getRecordId());
            vo.setReason(r.getReason());
            vo.setDetail(r.getDetail());
            vo.setStatus(r.getStatus());
            vo.setHandleResult(r.getHandleResult());
            vo.setCreateTime(r.getCreateTime());
            vo.setHandleTime(r.getHandleTime());

            GameRecord record = recordMapper.selectById(r.getRecordId());
            if (record != null) {
                vo.setRecordName(record.getGameName());
                vo.setRecordStatus(record.getStatus());
                User uploader = userMapper.selectById(record.getUserId());
                vo.setUploaderName(uploader == null ? null : uploader.getNickname());
            }
            User reporter = userMapper.selectById(r.getReporterId());
            vo.setReporterName(reporter == null ? null : reporter.getNickname());
            return vo;
        }).toList();
        return PageResult.of(page, list);
    }

    /** 管理员处理举报：result 处理说明；takeDown=true 时下架该对局 */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, ReportHandleDTO dto) {
        requireAdmin();
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "举报不存在");
        }
        report.setStatus(1);
        report.setHandleResult(dto.getResult());
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);

        if (Boolean.TRUE.equals(dto.getTakeDown())) {
            recordMapper.update(null, new LambdaUpdateWrapper<GameRecord>()
                    .eq(GameRecord::getId, report.getRecordId())
                    .set(GameRecord::getStatus, 1));
        }
    }

    /** 管理员恢复已下架对局 */
    @Transactional(rollbackFor = Exception.class)
    public void restoreRecord(Long recordId) {
        requireAdmin();
        GameRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对局不存在");
        }
        recordMapper.update(null, new LambdaUpdateWrapper<GameRecord>()
                .eq(GameRecord::getId, recordId)
                .set(GameRecord::getStatus, 0));
    }

    /** 管理员校验 */
    private void requireAdmin() {
        Long userId = UserContext.require();
        User user = userMapper.selectById(userId);
        if (user == null || !"admin".equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可操作");
        }
    }
}
