package com.example.gamerecord.vo;

import lombok.Data;

/**
 * 用户信息（脱敏，不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    /** 绑定邮箱（可为 null） */
    private String email;
    /** 邮箱是否已验证 */
    private Boolean emailVerified;
    private String role;
}
