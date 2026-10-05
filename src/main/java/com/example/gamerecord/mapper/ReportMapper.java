package com.example.gamerecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gamerecord.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报 Mapper
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
