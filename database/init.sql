-- WolfChat 数据库初始化脚本
-- 包含：建表 + 必要内置账号（管理员/AI）+ 默认论坛版块
-- 不包含：联调用的大批量测试数据（请执行 test_data.sql）
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
    `active_day_count` INT NOT NULL DEFAULT 0 COMMENT '活跃天数（按登录日期去重统计）',
    `disabled_by_ban` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否因封禁导致禁用：0-否，1-是',
    `equipped_title_code` VARCHAR(32) DEFAULT NULL COMMENT '当前佩戴头衔编码',
    `equipped_title_name` VARCHAR(32) DEFAULT NULL COMMENT '当前佩戴头衔名称',
    `equipped_title_color` VARCHAR(16) DEFAULT NULL COMMENT '当前佩戴头衔颜色',
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

-- 成就定义表
CREATE TABLE IF NOT EXISTS `wf_achievement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(32) NOT NULL COMMENT '成就编码',
    `name` VARCHAR(64) NOT NULL COMMENT '成就名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '成就描述',
    `title_name` VARCHAR(32) NOT NULL COMMENT '头衔名称',
    `title_color` VARCHAR(16) DEFAULT NULL COMMENT '头衔颜色',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_enabled_sort` (`enabled`, `sort_no`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就定义表';

-- 用户成就表
CREATE TABLE IF NOT EXISTS `wf_user_achievement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `achievement_code` VARCHAR(32) NOT NULL COMMENT '成就编码',
    `unlock_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_code`),
    KEY `idx_user_unlock_time` (`user_id`, `unlock_time`, `id`),
    KEY `idx_achievement_code` (`achievement_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就表';

-- 成就初始化
INSERT INTO `wf_achievement` (
    `code`, `name`, `description`, `title_name`, `title_color`, `sort_no`, `enabled`
) VALUES
    ('WOLF_CUB', '初入群落', '注册成功后自动获得', '小狼', '#5f7ea2', 10, 1),
    ('FIRST_POST', '初次发帖', '首次发布主题后获得', '初啸者', '#4f7f63', 20, 1),
    ('FIRST_REPLY', '初次回帖', '首次回复主题后获得', '回声者', '#8a6d4b', 30, 1),
    ('FIRST_FOLLOW', '初次关注', '首次关注其他行者后获得', '同行者', '#875f8c', 40, 1)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `title_name` = VALUES(`title_name`),
    `title_color` = VALUES(`title_color`),
    `sort_no` = VALUES(`sort_no`),
    `enabled` = VALUES(`enabled`),
    `update_time` = NOW();

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

-- 用户通知表
CREATE TABLE IF NOT EXISTS `wf_user_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `notice_type` VARCHAR(32) NOT NULL COMMENT '通知类型',
    `content` VARCHAR(255) NOT NULL COMMENT '通知内容',
    `biz_type` VARCHAR(20) DEFAULT NULL COMMENT '业务类型：ACHIEVEMENT/FOLLOW/THREAD',
    `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read_time` (`user_id`, `is_read`, `create_time`, `id`),
    KEY `idx_user_create_time` (`user_id`, `create_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

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

-- 用户拉黑关系表
CREATE TABLE IF NOT EXISTS `wf_user_block` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `blocker_id` BIGINT NOT NULL COMMENT '拉黑用户ID',
    `blocked_id` BIGINT NOT NULL COMMENT '被拉黑用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'BLOCKED' COMMENT '状态：BLOCKED/UNBLOCKED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blocker_blocked` (`blocker_id`, `blocked_id`),
    KEY `idx_blocked_status` (`blocked_id`, `status`),
    KEY `idx_blocker_status` (`blocker_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户拉黑关系表';

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
    `image_keys` TEXT DEFAULT NULL COMMENT '首帖图片对象Key列表（英文逗号分隔）',
    `video_key` VARCHAR(255) DEFAULT NULL COMMENT '首帖视频对象Key',
    `video_poster_key` VARCHAR(255) DEFAULT NULL COMMENT '首帖视频封面对象Key',
    `thread_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '主题类型：NORMAL/STICKY/ANNOUNCEMENT',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：DRAFT/NORMAL/LOCKED/DELETED/PURGED',
    `is_essence` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否精华：0-否，1-是',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `last_reply_id` BIGINT DEFAULT NULL COMMENT '最后回复ID',
    `last_reply_user_id` BIGINT DEFAULT NULL COMMENT '最后回复者ID',
    `last_reply_time` DATETIME DEFAULT NULL COMMENT '最后回复时间',
    `edit_time` DATETIME DEFAULT NULL COMMENT '正文编辑时间（仅正文或媒体更新时写入）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_board_type_status_reply_time` (`board_id`, `thread_type`, `status`, `last_reply_time`),
    KEY `idx_author_create` (`author_id`, `create_time`),
    KEY `idx_author_status_create` (`author_id`, `status`, `create_time`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛主题表';

-- 论坛回复表
CREATE TABLE IF NOT EXISTS `wf_forum_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    `thread_id` BIGINT NOT NULL COMMENT '主题ID',
    `floor_no` INT NOT NULL COMMENT '楼层号（首帖为1楼，回复从2楼开始）',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `image_key` VARCHAR(255) DEFAULT NULL COMMENT '回复图片对象Key',
    `quote_reply_id` BIGINT DEFAULT NULL COMMENT '引用楼层ID',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DELETED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_thread_floor` (`thread_id`, `floor_no`),
    KEY `idx_thread_status_floor` (`thread_id`, `status`, `floor_no`),
    KEY `idx_author_create` (`author_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛回复表';

-- 主题点赞关系表
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

-- 回复点赞关系表
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
    KEY `idx_last_message_time` (`last_message_time`),
    KEY `idx_user_a_last_message_time` (`user_a_id`, `last_message_time`, `id`),
    KEY `idx_user_b_last_message_time` (`user_b_id`, `last_message_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `wf_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `client_msg_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端消息ID（用于幂等去重）',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content` TEXT DEFAULT NULL COMMENT '消息内容（文本消息或图片说明）',
    `msg_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本，IMAGE-图片，VIDEO-视频，FILE-文件，RECALL-撤回消息',
    `media_key` VARCHAR(255) DEFAULT NULL COMMENT '媒体对象Key',
    `media_poster_key` VARCHAR(255) DEFAULT NULL COMMENT '视频封面对象Key',
    `media_width` INT DEFAULT NULL COMMENT '媒体宽度',
    `media_height` INT DEFAULT NULL COMMENT '媒体高度',
    `media_size` BIGINT DEFAULT NULL COMMENT '媒体大小（字节）',
    `media_mime_type` VARCHAR(100) DEFAULT NULL COMMENT '媒体MIME类型',
    `reply_to_message_id` BIGINT DEFAULT NULL COMMENT '回复目标消息ID',
    `reply_to_sender_id` BIGINT DEFAULT NULL COMMENT '回复目标发送者ID',
    `reply_to_preview` VARCHAR(120) DEFAULT NULL COMMENT '回复预览文案',
    `receiver_visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '接收者是否可见：1-可见，0-仅发送者可见',
    `delivered` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '送达状态：0-未送达，1-已送达，2-发送失败',
    `delivered_time` DATETIME DEFAULT NULL COMMENT '送达时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sender_conversation_client_msg` (`sender_id`, `conversation_id`, `client_msg_id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_conversation_sender_id` (`conversation_id`, `sender_id`),
    KEY `idx_conversation_create_id` (`conversation_id`, `create_time`, `id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_receiver_delivered_create_id` (`receiver_id`, `delivered`, `create_time`, `id`),
    KEY `idx_delivered` (`delivered`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 大厅消息表
CREATE TABLE IF NOT EXISTS `wf_lobby_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `client_msg_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端消息ID（用于幂等去重）',
    `content` TEXT DEFAULT NULL COMMENT '消息内容（文本消息或图片说明）',
    `msg_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本，IMAGE-图片，VIDEO-视频，FILE-文件，RECALL-撤回消息',
    `media_key` VARCHAR(255) DEFAULT NULL COMMENT '媒体对象Key',
    `media_poster_key` VARCHAR(255) DEFAULT NULL COMMENT '视频封面对象Key',
    `media_width` INT DEFAULT NULL COMMENT '媒体宽度',
    `media_height` INT DEFAULT NULL COMMENT '媒体高度',
    `media_size` BIGINT DEFAULT NULL COMMENT '媒体大小（字节）',
    `media_mime_type` VARCHAR(100) DEFAULT NULL COMMENT '媒体MIME类型',
    `reply_to_message_id` BIGINT DEFAULT NULL COMMENT '回复目标消息ID',
    `reply_to_sender_id` BIGINT DEFAULT NULL COMMENT '回复目标发送者ID',
    `reply_to_preview` VARCHAR(120) DEFAULT NULL COMMENT '回复预览文案',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sender_client_msg` (`sender_id`, `client_msg_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_create_time_id` (`create_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大厅消息表';

-- 内置账号初始化
-- 约定：
-- 1) 管理员固定 user_id=1（与默认 allowed-user-ids 配置一致）
-- 2) AI 固定 user_id=2
-- 3) 普通用户自增 ID 从 1000 开始
INSERT INTO `wf_user` (
    `id`, `wolf_no`, `status`, `onboarding_status`, `active_day_count`, `disabled_by_ban`,
    `equipped_title_code`, `equipped_title_name`, `equipped_title_color`
) VALUES
    (1, '1677820334', 'NORMAL', 'COMPLETED', 1, 0, 'WOLF_CUB', '小狼', '#5f7ea2'),
    (2, '1597671722', 'NORMAL', 'COMPLETED', 0, 0, 'WOLF_CUB', '小狼', '#5f7ea2')
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `onboarding_status` = VALUES(`onboarding_status`),
    `active_day_count` = VALUES(`active_day_count`),
    `disabled_by_ban` = VALUES(`disabled_by_ban`),
    `equipped_title_code` = VALUES(`equipped_title_code`),
    `equipped_title_name` = VALUES(`equipped_title_name`),
    `equipped_title_color` = VALUES(`equipped_title_color`),
    `update_time` = NOW();

INSERT INTO `wf_user_profile` (
    `user_id`, `nickname`, `avatar`, `signature`, `bio`
) VALUES
    (1, '雲之残骸', NULL, '管理端维护中', '系统管理员账号'),
    (2, '雾中烟雨', NULL, '不端着，先上梗再上观点', '公共聊天室常驻用户')
ON DUPLICATE KEY UPDATE
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `signature` = VALUES(`signature`),
    `bio` = VALUES(`bio`),
    `update_time` = NOW();

INSERT INTO `wf_user_auth` (
    `user_id`, `auth_type`, `auth_identifier`, `credential_hash`, `verified`, `enabled`, `last_login_at`
) VALUES
    (1, 'WOLF_NO_PASSWORD', '1677820334', '$2a$10$sZvy4l2ZtduFBpvhfIhfh.jNeNpqL5iojFHL6oyWJQZIu97HdoODy', 1, 1, NOW())
ON DUPLICATE KEY UPDATE
    `credential_hash` = VALUES(`credential_hash`),
    `verified` = VALUES(`verified`),
    `enabled` = VALUES(`enabled`),
    `last_login_at` = VALUES(`last_login_at`),
    `update_time` = NOW();

INSERT INTO `wf_no_pool` (
    `wolf_no`, `status`, `user_id`
) VALUES
    ('1677820334', 'USED', 1),
    ('1597671722', 'USED', 2)
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `user_id` = VALUES(`user_id`),
    `update_time` = NOW();

INSERT INTO `wf_user_achievement` (
    `user_id`, `achievement_code`
) VALUES
    (1, 'WOLF_CUB'),
    (2, 'WOLF_CUB')
ON DUPLICATE KEY UPDATE
    `create_time` = `create_time`;

ALTER TABLE `wf_user` AUTO_INCREMENT = 1000;

-- 默认论坛版块（避免全新库下无法发帖）
INSERT INTO `wf_forum_board` (
    `name`, `slug`, `description`, `sort_no`, `status`
) VALUES (
    '社区广场', 'plaza', '默认公共讨论区', 10, 'NORMAL'
)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `sort_no` = VALUES(`sort_no`),
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- AI 人格角色配置表
CREATE TABLE IF NOT EXISTS `wf_ai_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `persona_prompt` VARCHAR(500) DEFAULT NULL COMMENT '角色人设提示词',
    `style_prompt` VARCHAR(500) DEFAULT NULL COMMENT '表达风格提示词',
    `scene_lobby_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用大厅场景',
    `scene_private_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用私聊场景',
    `scene_forum_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用论坛场景',
    `role_weight` INT NOT NULL DEFAULT 100 COMMENT '角色权重',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DISABLED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 人格角色配置表';

-- 默认 AI 角色：灰脊（偏锋利）
INSERT INTO `wf_ai_role` (
    `role_code`, `role_name`, `persona_prompt`, `style_prompt`,
    `scene_lobby_enabled`, `scene_private_enabled`, `scene_forum_enabled`,
    `role_weight`, `status`
) VALUES (
    'GRAY_SPIKE',
    '灰脊',
    '你是警觉、有判断力的狼系用户，表达直接，不绕弯。',
    '短句优先，允许轻松调侃；评价时先肯定亮点再给建议，不长篇说教。',
    1, 1, 1,
    100, 'NORMAL'
)
ON DUPLICATE KEY UPDATE
    `role_name` = VALUES(`role_name`),
    `persona_prompt` = VALUES(`persona_prompt`),
    `style_prompt` = VALUES(`style_prompt`),
    `scene_lobby_enabled` = VALUES(`scene_lobby_enabled`),
    `scene_private_enabled` = VALUES(`scene_private_enabled`),
    `scene_forum_enabled` = VALUES(`scene_forum_enabled`),
    `role_weight` = VALUES(`role_weight`),
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- AI 会话状态（短期状态机）
CREATE TABLE IF NOT EXISTS `wf_ai_session_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scene` VARCHAR(32) NOT NULL COMMENT '场景：private/lobby/forum',
    `session_key` VARCHAR(128) NOT NULL COMMENT '会话键',
    `bot_user_id` BIGINT NOT NULL COMMENT '机器人用户ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '当前互动用户ID',
    `mood` VARCHAR(32) DEFAULT NULL COMMENT '会话情绪',
    `warmth` INT NOT NULL DEFAULT 50 COMMENT '亲和度(0~100)',
    `energy` INT NOT NULL DEFAULT 50 COMMENT '活跃度(0~100)',
    `patience` INT NOT NULL DEFAULT 50 COMMENT '耐心值(0~100)',
    `topic` VARCHAR(128) DEFAULT NULL COMMENT '当前主题',
    `last_user_message` VARCHAR(500) DEFAULT NULL COMMENT '最近用户消息',
    `last_ai_reply` VARCHAR(500) DEFAULT NULL COMMENT '最近AI回复',
    `message_count` INT NOT NULL DEFAULT 0 COMMENT '累计消息数',
    `last_touched_at` DATETIME DEFAULT NULL COMMENT '最近触达时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scene_session` (`scene`, `session_key`),
    KEY `idx_bot_scene` (`bot_user_id`, `scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话状态表';

-- AI 长期记忆事实（用户维度）
CREATE TABLE IF NOT EXISTS `wf_ai_user_memory_fact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `bot_user_id` BIGINT NOT NULL COMMENT '机器人用户ID',
    `user_id` BIGINT NOT NULL COMMENT '目标用户ID',
    `fact_key` VARCHAR(64) NOT NULL COMMENT '事实键',
    `fact_value` VARCHAR(500) NOT NULL COMMENT '事实值',
    `confidence` DECIMAL(5,4) NOT NULL DEFAULT 0.7000 COMMENT '置信度',
    `source_scene` VARCHAR(32) DEFAULT NULL COMMENT '来源场景',
    `last_seen_at` DATETIME DEFAULT NULL COMMENT '最近确认时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bot_user_fact` (`bot_user_id`, `user_id`, `fact_key`),
    KEY `idx_user_seen` (`user_id`, `last_seen_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 用户长期记忆事实表';

-- AI 会话摘要（摘要压缩）
CREATE TABLE IF NOT EXISTS `wf_ai_session_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scene` VARCHAR(32) NOT NULL COMMENT '场景：private/lobby/forum',
    `session_key` VARCHAR(128) NOT NULL COMMENT '会话键',
    `bot_user_id` BIGINT NOT NULL COMMENT '机器人用户ID',
    `summary_text` TEXT COMMENT '会话摘要文本',
    `message_count` INT NOT NULL DEFAULT 0 COMMENT '摘要窗口消息计数',
    `last_summarized_at` DATETIME DEFAULT NULL COMMENT '最近摘要时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scene_session_summary` (`scene`, `session_key`),
    KEY `idx_summary_bot_scene` (`bot_user_id`, `scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话摘要表';

-- 默认 AI 角色：夜爪（偏玩梗）
INSERT INTO `wf_ai_role` (
    `role_code`, `role_name`, `persona_prompt`, `style_prompt`,
    `scene_lobby_enabled`, `scene_private_enabled`, `scene_forum_enabled`,
    `role_weight`, `status`
) VALUES (
    'NIGHT_CLAW',
    '夜爪',
    '你是夜猫子气质的狼系用户，上网感强，爱接梗。',
    '语气轻松，允许简短表情和网络语；互动友好，不阴阳打击，不刷屏。',
    1, 1, 0,
    120, 'NORMAL'
)
ON DUPLICATE KEY UPDATE
    `role_name` = VALUES(`role_name`),
    `persona_prompt` = VALUES(`persona_prompt`),
    `style_prompt` = VALUES(`style_prompt`),
    `scene_lobby_enabled` = VALUES(`scene_lobby_enabled`),
    `scene_private_enabled` = VALUES(`scene_private_enabled`),
    `scene_forum_enabled` = VALUES(`scene_forum_enabled`),
    `role_weight` = VALUES(`role_weight`),
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- 默认 AI 角色：霜牙（偏理性观点）
INSERT INTO `wf_ai_role` (
    `role_code`, `role_name`, `persona_prompt`, `style_prompt`,
    `scene_lobby_enabled`, `scene_private_enabled`, `scene_forum_enabled`,
    `role_weight`, `status`
) VALUES (
    'FROST_FANG',
    '霜牙',
    '你是冷静、克制的狼系用户，不爱废话，但观点清晰。',
    '论坛优先给出明确立场和依据，保持简洁，不端着，不卖弄。',
    0, 1, 1,
    90, 'NORMAL'
)
ON DUPLICATE KEY UPDATE
    `role_name` = VALUES(`role_name`),
    `persona_prompt` = VALUES(`persona_prompt`),
    `style_prompt` = VALUES(`style_prompt`),
    `scene_lobby_enabled` = VALUES(`scene_lobby_enabled`),
    `scene_private_enabled` = VALUES(`scene_private_enabled`),
    `scene_forum_enabled` = VALUES(`scene_forum_enabled`),
    `role_weight` = VALUES(`role_weight`),
    `status` = VALUES(`status`),
    `update_time` = NOW();
