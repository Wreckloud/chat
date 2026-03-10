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

-- 帖子表
CREATE TABLE IF NOT EXISTS `wf_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `room_id` BIGINT DEFAULT NULL COMMENT '关联聊天室ID（可选）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DELETED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS `wf_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '评论者ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DELETED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

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
