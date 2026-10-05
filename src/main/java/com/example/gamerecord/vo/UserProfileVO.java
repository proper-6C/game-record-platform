package com.example.gamerecord.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家公开主页信息（无需登录即可查看，脱敏不含密码）
 */
@Data
public class UserProfileVO {

    /** 用户ID */
    private Long id;

    /** 登录名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 角色：user 普通用户 / admin 管理员 */
    private String role;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 公开作品数（正常状态的对局数） */
    private Long recordCount;

    /** 收到的点赞总数（其公开对局被赞次数） */
    private Long totalLikes;
}
