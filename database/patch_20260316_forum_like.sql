-- 论坛点赞能力补丁
-- 说明：为主题/回复增加 like_count，并新增点赞关系表

USE `wolf_chat`;

ALTER TABLE `wf_forum_thread`
    ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `reply_count`;

ALTER TABLE `wf_forum_reply`
    ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `quote_reply_id`;

CREATE TABLE IF NOT EXISTS `wf_forum_thread_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `thread_id` BIGINT NOT NULL COMMENT '主题ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_thread_user` (`thread_id`, `user_id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_thread_time` (`thread_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题点赞关系表';

CREATE TABLE IF NOT EXISTS `wf_forum_reply_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reply_id` BIGINT NOT NULL COMMENT '回复ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reply_user` (`reply_id`, `user_id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_reply_time` (`reply_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回复点赞关系表';
