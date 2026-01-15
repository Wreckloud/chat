-- WolfChat 数据库初始化脚本
-- 仅包含建表语句，不包含测试数据
-- 号码池会自动补充（当 UNUSED 数量低于 10 个时，系统会自动补充 50 个）

-- 1. 创建狼藉号池表
DROP TABLE IF EXISTS `wf_no_pool`;
CREATE TABLE `wf_no_pool` (
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

-- 2. 创建行者表
DROP TABLE IF EXISTS `wf_user`;
CREATE TABLE `wf_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wolf_no` VARCHAR(10) NOT NULL COMMENT '狼藉号（唯一标识）',
    `login_key` VARCHAR(64) NOT NULL COMMENT '登录密码（阶段1简化存储，预留哈希空间）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '行者名（行者在群落中的称呼，将被其他行者看到，注册后可修改）',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常，DISABLED-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wolf_no` (`wolf_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行者表';
