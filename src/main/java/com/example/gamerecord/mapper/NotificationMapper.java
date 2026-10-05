package com.example.gamerecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gamerecord.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
