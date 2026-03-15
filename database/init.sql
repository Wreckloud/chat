-- WolfChat 数据库初始化脚本
-- 仅包含建表语句，不包含测试数据
-- 号码池会自动补充（当 UNUSED 数量低于 10 个时，系统会自动补充 50 个）

CREATE DATABASE IF NOT EXISTS `wolf_chat`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
USE `wolf_chat`;

-- 狼藉号池表
CREATE TABLE IF NOT EXISTS `wf_no_pool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wolf_no` VARCHAR(10) NOT NULL COMMENT '狼藉号（10位，首位1-9，避免前导0）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用，USED-已使用，RESERVED-预留',
    `user_id` BIGINT DEFAULT NULL COMMENT '已使用时绑定的行者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wolf_no` (`wolf_no`),
    KEY `idx_status_id` (`status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='狼藉号池表';

-- 行者用户表
CREATE TABLE IF NOT EXISTS `wf_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wolf_no` VARCHAR(10) NOT NULL COMMENT '狼藉号（唯一标识）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常，DISABLED-禁用或已注销',
    `onboarding_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '新用户引导状态：PENDING/COMPLETED/SKIPPED',
    `onboarding_completed_at` DATETIME DEFAULT NULL COMMENT '引导完成时间',
    `first_login_at` DATETIME DEFAULT NULL COMMENT '首次登录时间',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `login_count` INT NOT NULL DEFAULT 0 COMMENT '登录次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wolf_no` (`wolf_no`),
    KEY `idx_status` (`status`),
    KEY `idx_onboarding_status` (`onboarding_status`),
    KEY `idx_last_login_at` (`last_login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行者表';

-- 行者认证表
CREATE TABLE IF NOT EXISTS `wf_user_auth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `auth_type` VARCHAR(32) NOT NULL COMMENT '认证类型：WOLF_NO_PASSWORD/EMAIL_PASSWORD',
    `auth_identifier` VARCHAR(128) NOT NULL COMMENT '认证标识（狼藉号/邮箱）',
    `credential_hash` VARCHAR(100) NOT NULL COMMENT '认证凭据哈希（密码）',
    `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已认证：0-否，1-是',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_identifier` (`auth_type`, `auth_identifier`),
    UNIQUE KEY `uk_user_type` (`user_id`, `auth_type`),
    KEY `idx_user_enabled` (`user_id`, `enabled`),
    KEY `idx_type_enabled` (`auth_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行者认证表';

-- 行者资料表
CREATE TABLE IF NOT EXISTS `wf_user_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（唯一）',
    `nickname` VARCHAR(64) NOT NULL COMMENT '行者名（公开显示）',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `signature` VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
    `bio` VARCHAR(1000) DEFAULT NULL COMMENT '个人简介',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行者资料表';

-- 登录记录表
CREATE TABLE IF NOT EXISTS `wf_login_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未匹配到用户时为空）',
    `login_method` VARCHAR(20) NOT NULL COMMENT '登录方式：WOLF_NO/EMAIL/UNKNOWN',
    `login_result` VARCHAR(20) NOT NULL COMMENT '登录结果：SUCCESS/FAIL',
    `fail_code` INT DEFAULT NULL COMMENT '失败错误码（成功为空）',
    `account_mask` VARCHAR(128) DEFAULT NULL COMMENT '脱敏登录账号',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'User-Agent',
    `client_type` VARCHAR(32) DEFAULT NULL COMMENT '客户端类型',
    `client_version` VARCHAR(32) DEFAULT NULL COMMENT '客户端版本',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_login_time` (`user_id`, `login_time`),
    KEY `idx_result_login_time` (`login_result`, `login_time`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录记录表';

-- 邮箱验证码使用 Redis 存储（不落库）

-- 行者封禁记录表
CREATE TABLE IF NOT EXISTS `wf_user_ban_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '被封禁用户ID',
    `operator_user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `reason` VARCHAR(500) NOT NULL COMMENT '封禁原因',
    `start_time` DATETIME NOT NULL COMMENT '封禁开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '封禁结束时间（为空表示永久封禁）',
    `status` VARCHAR(20) NOT NULL COMMENT '记录状态：ACTIVE/LIFTED/EXPIRED',
    `lifted_at` DATETIME DEFAULT NULL COMMENT '解除时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `start_time`),
    KEY `idx_status_time` (`status`, `start_time`),
    KEY `idx_operator_time` (`operator_user_id`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行者封禁记录表';

-- 关注关系表
CREATE TABLE IF NOT EXISTS `wf_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
    `followee_id` BIGINT NOT NULL COMMENT '被关注者ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'FOLLOWING' COMMENT '状态：FOLLOWING/UNFOLLOWED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_followee_id` (`followee_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';

-- 论坛版块表
CREATE TABLE IF NOT EXISTS `wf_forum_board` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版块ID',
    `name` VARCHAR(64) NOT NULL COMMENT '版块名称',
    `slug` VARCHAR(64) NOT NULL COMMENT '版块短标识',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '版块描述',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/CLOSED',
    `thread_count` INT NOT NULL DEFAULT 0 COMMENT '主题数',
    `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
    `last_thread_id` BIGINT DEFAULT NULL COMMENT '最近活跃主题ID',
    `last_reply_time` DATETIME DEFAULT NULL COMMENT '最近活跃时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛版块表';

-- 论坛主题表
CREATE TABLE IF NOT EXISTS `wf_forum_thread` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主题ID',
    `board_id` BIGINT NOT NULL COMMENT '版块ID',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `title` VARCHAR(120) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '首帖内容',
    `thread_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '主题类型：NORMAL/STICKY/ANNOUNCEMENT',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/LOCKED/DELETED',
    `is_essence` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否精华：0-否，1-是',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
    `last_reply_id` BIGINT DEFAULT NULL COMMENT '最后回复ID',
    `last_reply_user_id` BIGINT DEFAULT NULL COMMENT '最后回复者ID',
    `last_reply_time` DATETIME DEFAULT NULL COMMENT '最后回复时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_board_type_status_reply_time` (`board_id`, `thread_type`, `status`, `last_reply_time`),
    KEY `idx_author_create` (`author_id`, `create_time`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛主题表';

-- 论坛回复表
CREATE TABLE IF NOT EXISTS `wf_forum_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    `thread_id` BIGINT NOT NULL COMMENT '主题ID',
    `floor_no` INT NOT NULL COMMENT '楼层号（首帖为1楼，回复从2楼开始）',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `quote_reply_id` BIGINT DEFAULT NULL COMMENT '引用楼层ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DELETED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_thread_floor` (`thread_id`, `floor_no`),
    KEY `idx_thread_status_floor` (`thread_id`, `status`, `floor_no`),
    KEY `idx_author_create` (`author_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛回复表';

-- 论坛版务日志表
CREATE TABLE IF NOT EXISTS `wf_forum_moderation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator_user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型：THREAD/REPLY',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `action` VARCHAR(40) NOT NULL COMMENT '操作类型：LOCK_THREAD/UNLOCK_THREAD/STICKY_THREAD/UNSTICKY_THREAD/ESSENCE_THREAD/UNESSENCE_THREAD/DELETE_THREAD/DELETE_REPLY',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '操作原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_operator_time` (`operator_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛版务日志表';

-- 会话表
CREATE TABLE IF NOT EXISTS `wf_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_a_id` BIGINT NOT NULL COMMENT '会话参与者A（较小的用户ID）',
    `user_b_id` BIGINT NOT NULL COMMENT '会话参与者B（较大的用户ID）',
    `last_message_id` BIGINT DEFAULT NULL COMMENT '最近一条消息ID（预留）',
    `last_message` VARCHAR(500) DEFAULT NULL COMMENT '最近一条消息内容（冗余）',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最近消息时间',
    `user_a_unread_count` INT NOT NULL DEFAULT 0 COMMENT 'A侧未读数',
    `user_b_unread_count` INT NOT NULL DEFAULT 0 COMMENT 'B侧未读数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_a_user_b` (`user_a_id`, `user_b_id`),
    KEY `idx_user_a_id` (`user_a_id`),
    KEY `idx_user_b_id` (`user_b_id`),
    KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `wf_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content` TEXT DEFAULT NULL COMMENT '消息内容（文本消息或图片说明）',
    `msg_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本，IMAGE-图片，VIDEO-视频，FILE-文件',
    `media_key` VARCHAR(255) DEFAULT NULL COMMENT '媒体对象Key',
    `media_width` INT DEFAULT NULL COMMENT '媒体宽度',
    `media_height` INT DEFAULT NULL COMMENT '媒体高度',
    `media_size` BIGINT DEFAULT NULL COMMENT '媒体大小（字节）',
    `media_mime_type` VARCHAR(100) DEFAULT NULL COMMENT '媒体MIME类型',
    `delivered` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已送达：0-未送达，1-已送达',
    `delivered_time` DATETIME DEFAULT NULL COMMENT '送达时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_delivered` (`delivered`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 大厅消息表
CREATE TABLE IF NOT EXISTS `wf_lobby_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `content` TEXT DEFAULT NULL COMMENT '消息内容（文本消息或图片说明）',
    `msg_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本，IMAGE-图片，VIDEO-视频，FILE-文件',
    `media_key` VARCHAR(255) DEFAULT NULL COMMENT '媒体对象Key',
    `media_width` INT DEFAULT NULL COMMENT '媒体宽度',
    `media_height` INT DEFAULT NULL COMMENT '媒体高度',
    `media_size` BIGINT DEFAULT NULL COMMENT '媒体大小（字节）',
    `media_mime_type` VARCHAR(100) DEFAULT NULL COMMENT '媒体MIME类型',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大厅消息表';
