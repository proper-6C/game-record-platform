-- 追加：站内通知表（仅对已存在的库执行；全新安装请直接使用 sql/init.sql）
USE game_record;
CREATE TABLE IF NOT EXISTS `notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id`     BIGINT       NOT NULL COMMENT '接收通知的用户ID',
    `actor_id`    BIGINT       NOT NULL COMMENT '触发通知的用户ID',
    `type`        VARCHAR(20)  NOT NULL COMMENT '通知类型：like/comment/reply',
    `content`     VARCHAR(255) NOT NULL COMMENT '通知内容（已组装好的展示文本）',
    `target_type` VARCHAR(20)  NOT NULL COMMENT '关联对象类型：record',
    `target_id`   BIGINT       NOT NULL COMMENT '关联对局ID（点击跳转详情）',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='站内通知表';
