package com.example.gamerecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("`user`")
public class User {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录用） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 角色：user 普通用户 / admin 管理员 */
    private String role;

    /** 注册时间（数据库默认值） */
    private LocalDateTime createTime;
}
