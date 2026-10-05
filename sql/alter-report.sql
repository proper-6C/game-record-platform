-- 追加：角色字段 + 对局状态字段 + 举报表（仅对已存在的库执行；全新安装请直接使用 sql/init.sql）
USE game_record;

-- 用户角色字段
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user普通用户 / admin管理员' AFTER `avatar`;

-- 对局状态字段（下架后普通用户不可见）
ALTER TABLE `game_record` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1已下架' AFTER `comment_count`;

-- 举报表
CREATE TABLE IF NOT EXISTS `report` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    `record_id`     BIGINT       NOT NULL COMMENT '被举报对局ID',
    `reporter_id`   BIGINT       NOT NULL COMMENT '举报人ID',
    `reason`        VARCHAR(50)  NOT NULL COMMENT '举报原因（如 违规内容/垃圾广告）',
    `detail`        VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1已处理',
    `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果说明',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    `handle_time`   DATETIME     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='举报表';

-- 已有 admin 账号设为管理员（如存在）
UPDATE `user` SET `role` = 'admin' WHERE `username` = 'admin';
