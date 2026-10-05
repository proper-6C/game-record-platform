-- ============================================================
-- 游戏对局记录分享平台 - 数据库初始化脚本
-- 使用方式：mysql -u root -p < init.sql  （或在 Navicat/IDEA 中执行）
-- ============================================================

CREATE DATABASE IF NOT EXISTS game_record DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE game_record;

-- ---------- 1. 用户表 ----------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名（登录用，唯一）',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：user普通用户 / admin管理员',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ---------- 2. 对局记录表 ----------
DROP TABLE IF EXISTS `game_record`;
CREATE TABLE `game_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '对局ID',
    `user_id`       BIGINT       NOT NULL COMMENT '上传用户ID',
    `game_name`     VARCHAR(100) NOT NULL COMMENT '游戏名称',
    `game_mode`     VARCHAR(50)  DEFAULT NULL COMMENT '游戏模式（如排位/匹配）',
    `match_date`    DATE         DEFAULT NULL COMMENT '对局日期',
    `result`        VARCHAR(10)  DEFAULT NULL COMMENT '对局结果：win/lose/draw',
    `rank`          VARCHAR(50)  DEFAULT NULL COMMENT '段位',
    `description`   TEXT         COMMENT '对局描述',
    `tags`          VARCHAR(255) DEFAULT NULL COMMENT '标签（逗号分隔，如 翻盘,五杀,排位）',
    `cover_url`     VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    `video_url`     VARCHAR(255) DEFAULT NULL COMMENT '对局视频URL',
    `like_count`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1已下架（违规举报处理）',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_game_name` (`game_name`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_like_count` (`like_count`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='游戏对局记录表';

-- ---------- 3. 评论表（支持楼中楼：parent_id=0 为一级评论） ----------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `record_id`   BIGINT       NOT NULL COMMENT '对局ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评论人ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父评论ID，0=一级评论',
    `to_user_id`  BIGINT       DEFAULT NULL COMMENT '回复的目标用户ID',
    `content`     VARCHAR(500) NOT NULL COMMENT '评论内容',
    `image_url`   VARCHAR(255) DEFAULT NULL COMMENT '评论配图URL',
    `like_count`  INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0正常 1已删除',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='评论表';

-- ---------- 4. 点赞记录表（user+target 唯一约束防刷） ----------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `user_id`     BIGINT      NOT NULL COMMENT '点赞用户ID',
    `target_id`   BIGINT      NOT NULL COMMENT '被点赞对象ID（对局ID或评论ID）',
    `target_type` VARCHAR(10) NOT NULL COMMENT '对象类型：record/comment',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='点赞记录表';

-- ---------- 5. 站内通知表（被点赞/被评论/被回复） ----------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
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

-- ---------- 6. 举报表（用户举报违规对局，管理员处理） ----------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
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

-- ---------- 7. 敏感词表（管理员后台维护，评论时自动过滤） ----------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '敏感词ID',
    `word`        VARCHAR(50) NOT NULL COMMENT '敏感词',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='敏感词表';

-- 初始敏感词（可后台增删）
INSERT INTO `sensitive_word` (`word`) VALUES
('代练'), ('加微信'), ('赌博'), ('诈骗'), ('广告')
ON DUPLICATE KEY UPDATE `word` = `word`;

-- ---------- 种子数据 ----------
-- 说明：不预置用户数据。启动项目后调用 POST /api/user/register 注册账号即可使用。
-- 如需初始管理员，可在注册接口调用后手动 UPDATE 用户角色字段（当前版本未引入角色）。

