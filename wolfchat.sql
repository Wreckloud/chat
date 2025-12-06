-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `wolf_chat`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `wolf_chat`;

-- 2. 聊天号号码池表：wf_no_pool
--   - 管理所有号码（普通号 + 靓号）
--   - is_pretty = 1 表示靓号
--   - status: 0未使用 1已使用 2冻结
--   - user_id 记录被谁占用（未使用为 NULL）

CREATE TABLE `wf_no_pool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wf_no` BIGINT NOT NULL COMMENT '唯一的 wf 号',

  `is_pretty` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否靓号：0否 1是',

  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1已使用 2冻结',
  `user_id` BIGINT NULL COMMENT '使用该号码的用户ID（未使用为NULL）',

  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_no` (`wf_no`),
  KEY `idx_status` (`status`),
  KEY `idx_is_pretty` (`is_pretty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天号号码池（包含靓号）';

-- 3. 用户表：wf_user
--   - 帐号本体：wf_no / mobile / email / 微信
--   - 基本资料：昵称、头像、性别、签名
--   - 状态 & 登录信息：status / last_login_time / last_logout_time
--   - create_time / update_time 交给数据库填

CREATE TABLE `wf_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `wf_no` BIGINT NOT NULL COMMENT '聊天号（对应 wf_no_pool.wf_no）',

  `username` VARCHAR(32) NOT NULL COMMENT '昵称（展示名）',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '登录密码密文',
  `salt` VARCHAR(64) DEFAULT NULL COMMENT '加盐（可选）',

  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(64) DEFAULT NULL COMMENT '邮箱',

  `wx_openid` VARCHAR(64) DEFAULT NULL COMMENT '微信 openid（单应用唯一）',
  `wx_unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid（跨应用唯一）',

  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT DEFAULT NULL COMMENT '性别：0未知 1男 2女',
  `signature` VARCHAR(255) DEFAULT NULL COMMENT '个性签名',

  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 2禁用 3注销',

  `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_logout_time` DATETIME DEFAULT NULL COMMENT '最后下线时间',

  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',

  PRIMARY KEY (`id`),

  UNIQUE KEY `uk_wf_no` (`wf_no`),
  UNIQUE KEY `uk_mobile` (`mobile`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_wx_unionid` (`wx_unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
