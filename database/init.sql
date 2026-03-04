-- WolfChat 数据库初始化脚本
-- 仅包含建表语句，不包含测试数据
-- 号码池会自动补充（当 UNUSED 数量低于 10 个时，系统会自动补充 50 个）

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
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='狼藉号池表';

-- 行者用户表
CREATE TABLE IF NOT EXISTS `wf_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wolf_no` VARCHAR(10) NOT NULL COMMENT '狼藉号（唯一标识）',
    `login_key` VARCHAR(64) NOT NULL COMMENT '登录密码哈希（BCrypt）',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱（唯一）',
    `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已认证：0-否，1-是',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '行者名（行者在群落中的称呼，将被其他行者看到，注册后可修改）',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常，DISABLED-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wolf_no` (`wolf_no`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行者表';

-- 兼容已存在的旧表结构（MySQL 8.0.33 支持 ADD COLUMN IF NOT EXISTS）
ALTER TABLE `wf_user`
    ADD COLUMN IF NOT EXISTS `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱（唯一）' AFTER `login_key`,
    ADD COLUMN IF NOT EXISTS `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已认证：0-否，1-是' AFTER `email`;

-- 若旧表缺少邮箱唯一索引，动态补齐
SET @uk_email_count := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_user'
      AND COLUMN_NAME = 'email'
      AND NON_UNIQUE = 0
);
SET @uk_email_sql := IF(@uk_email_count = 0,
    'ALTER TABLE `wf_user` ADD UNIQUE KEY `uk_email` (`email`)',
    'SELECT 1'
);
PREPARE stmt_uk_email FROM @uk_email_sql;
EXECUTE stmt_uk_email;
DEALLOCATE PREPARE stmt_uk_email;

-- 邮箱验证码表
CREATE TABLE IF NOT EXISTS `wf_email_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `email` VARCHAR(128) NOT NULL COMMENT '邮箱',
    `scene` VARCHAR(32) NOT NULL COMMENT '场景：BIND_EMAIL/RESET_PASSWORD',
    `verify_code` VARCHAR(6) NOT NULL COMMENT '验证码',
    `used` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用：0-否，1-是',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_email_scene_time` (`email`, `scene`, `create_time`),
    KEY `idx_email_scene_used` (`email`, `scene`, `used`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

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
    `content` TEXT NOT NULL COMMENT '消息内容',
    `msg_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本',
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
