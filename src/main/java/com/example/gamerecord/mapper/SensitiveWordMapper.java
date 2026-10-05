package com.example.gamerecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gamerecord.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词 Mapper
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
