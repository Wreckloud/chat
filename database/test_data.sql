-- WolfChat 测试数据脚本（可选）
-- 适用：已执行 init.sql 或 patch_20260310_temp.sql 后的本地联调环境

USE `wolf_chat`;

-- 测试账号说明（明文密码一致）
-- 密码：Wolf123456
-- 账号1：狼藉号 1234567890，邮箱 wolf1@example.com（已认证）
-- 账号2：狼藉号 1234567891，邮箱 wolf2@example.com（未认证）
-- 账号3：狼藉号 1234567892（未绑定邮箱）
SET @seed_password_hash := '$2a$10$ENlIGWJCpjy8Dz63BNO4M.cJatv7N3l1LyBUJ6kVmFRLcztHelhXS';

-- 1) 用户主表
INSERT INTO `wf_user` (
    `wolf_no`, `status`, `onboarding_status`, `onboarding_completed_at`,
    `first_login_at`, `last_login_at`, `active_day_count`, `create_time`, `update_time`
) VALUES
    ('1234567890', 'NORMAL', 'COMPLETED', '2026-03-09 10:00:00', '2026-03-09 10:00:00', '2026-03-10 10:00:00', 5, NOW(), NOW()),
    ('1234567891', 'NORMAL', 'PENDING', NULL, '2026-03-09 11:00:00', '2026-03-10 09:00:00', 3, NOW(), NOW()),
    ('1234567892', 'NORMAL', 'PENDING', NULL, '2026-03-09 12:00:00', '2026-03-10 08:30:00', 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `onboarding_status` = VALUES(`onboarding_status`),
    `onboarding_completed_at` = VALUES(`onboarding_completed_at`),
    `first_login_at` = VALUES(`first_login_at`),
    `last_login_at` = VALUES(`last_login_at`),
    `active_day_count` = VALUES(`active_day_count`),
    `update_time` = NOW();

SET @uid_1 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567890' LIMIT 1);
SET @uid_2 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567891' LIMIT 1);
SET @uid_3 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567892' LIMIT 1);

-- 2) 号码池（标记为已分配）
INSERT INTO `wf_no_pool` (`wolf_no`, `status`, `user_id`, `create_time`, `update_time`) VALUES
    ('1234567890', 'USED', @uid_1, NOW(), NOW()),
    ('1234567891', 'USED', @uid_2, NOW(), NOW()),
    ('1234567892', 'USED', @uid_3, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `user_id` = VALUES(`user_id`),
    `update_time` = NOW();

-- 3) 用户资料
INSERT INTO `wf_user_profile` (
    `user_id`, `nickname`, `avatar`, `signature`, `bio`, `create_time`, `update_time`
) VALUES
    (@uid_1, 'Wreckloud', NULL, '欢迎来到 WolfChat', '测试账号1，用于主账号联调', NOW(), NOW()),
    (@uid_2, 'USVING', NULL, '今天也要写代码', '测试账号2，用于双端会话联调', NOW(), NOW()),
    (@uid_3, '旅行青蛙', NULL, '消息已读但未回', '测试账号3，用于关注/列表场景', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `signature` = VALUES(`signature`),
    `bio` = VALUES(`bio`),
    `update_time` = NOW();

-- 4) 认证信息（狼藉号密码 + 邮箱密码）
INSERT INTO `wf_user_auth` (
    `user_id`, `auth_type`, `auth_identifier`, `credential_hash`, `verified`, `enabled`, `last_login_at`, `create_time`, `update_time`
) VALUES
    (@uid_1, 'WOLF_NO_PASSWORD', '1234567890', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_2, 'WOLF_NO_PASSWORD', '1234567891', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_3, 'WOLF_NO_PASSWORD', '1234567892', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_1, 'EMAIL_PASSWORD', 'wolf1@example.com', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_2, 'EMAIL_PASSWORD', 'wolf2@example.com', @seed_password_hash, 0, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `credential_hash` = VALUES(`credential_hash`),
    `verified` = VALUES(`verified`),
    `enabled` = VALUES(`enabled`),
    `last_login_at` = VALUES(`last_login_at`),
    `update_time` = NOW();

-- 5) 关注关系（1<->2 互关，1->3 单向）
INSERT INTO `wf_follow` (
    `follower_id`, `followee_id`, `status`, `create_time`, `update_time`
) VALUES
    (@uid_1, @uid_2, 'FOLLOWING', NOW(), NOW()),
    (@uid_2, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_1, @uid_3, 'FOLLOWING', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- 6) 会话与消息（账号1 与 账号2）
SET @conv_user_a := LEAST(@uid_1, @uid_2);
SET @conv_user_b := GREATEST(@uid_1, @uid_2);

INSERT INTO `wf_conversation` (
    `user_a_id`, `user_b_id`, `last_message_id`, `last_message`, `last_message_time`,
    `user_a_unread_count`, `user_b_unread_count`, `create_time`, `update_time`
) VALUES
    (@conv_user_a, @conv_user_b, 990002, '收到，欢迎测试', '2026-03-10 09:31:00', 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `last_message_id` = VALUES(`last_message_id`),
    `last_message` = VALUES(`last_message`),
    `last_message_time` = VALUES(`last_message_time`),
    `user_a_unread_count` = VALUES(`user_a_unread_count`),
    `user_b_unread_count` = VALUES(`user_b_unread_count`),
    `update_time` = NOW();

SET @conv_12 := (
    SELECT `id`
    FROM `wf_conversation`
    WHERE `user_a_id` = @conv_user_a AND `user_b_id` = @conv_user_b
    LIMIT 1
);

INSERT INTO `wf_message` (
    `id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`,
    `delivered`, `delivered_time`, `create_time`
) VALUES
    (990001, @conv_12, @uid_1, @uid_2, '你好，这是测试消息', 'TEXT', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-10 09:30:00', '2026-03-10 09:30:00'),
    (990002, @conv_12, @uid_2, @uid_1, '收到，欢迎测试', 'TEXT', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-10 09:31:00', '2026-03-10 09:31:00')
ON DUPLICATE KEY UPDATE
    `conversation_id` = VALUES(`conversation_id`),
    `sender_id` = VALUES(`sender_id`),
    `receiver_id` = VALUES(`receiver_id`),
    `content` = VALUES(`content`),
    `msg_type` = VALUES(`msg_type`),
    `media_key` = VALUES(`media_key`),
    `media_width` = VALUES(`media_width`),
    `media_height` = VALUES(`media_height`),
    `media_size` = VALUES(`media_size`),
    `media_mime_type` = VALUES(`media_mime_type`),
    `delivered` = VALUES(`delivered`),
    `delivered_time` = VALUES(`delivered_time`),
    `create_time` = VALUES(`create_time`);

UPDATE `wf_conversation`
SET `last_message_id` = 990002,
    `last_message` = '收到，欢迎测试',
    `last_message_time` = '2026-03-10 09:31:00',
    `update_time` = NOW()
WHERE `id` = @conv_12;

-- 7) 大厅消息
INSERT INTO `wf_lobby_message` (
    `id`, `sender_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`, `create_time`
) VALUES
    (980001, @uid_1, '欢迎来到公共聊天室，在线的人都可以发言。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-10 10:20:00'),
    (980002, @uid_2, '这里后续可以一起聊社区话题和项目进展。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-10 10:21:00')
ON DUPLICATE KEY UPDATE
    `sender_id` = VALUES(`sender_id`),
    `content` = VALUES(`content`),
    `msg_type` = VALUES(`msg_type`),
    `media_key` = VALUES(`media_key`),
    `media_width` = VALUES(`media_width`),
    `media_height` = VALUES(`media_height`),
    `media_size` = VALUES(`media_size`),
    `media_mime_type` = VALUES(`media_mime_type`),
    `create_time` = VALUES(`create_time`);

-- 8) 论坛基础测试数据（版块/主题/回复）
INSERT INTO `wf_forum_board` (
    `name`, `slug`, `description`, `sort_no`, `status`,
    `thread_count`, `reply_count`, `last_thread_id`, `last_reply_time`, `create_time`, `update_time`
) VALUES
    ('综合广场', 'general', '新手交流与日常讨论', 10, 'NORMAL', 0, 0, NULL, NULL, NOW(), NOW()),
    ('技术交流', 'dev', '产品开发、架构与实现交流', 20, 'NORMAL', 0, 0, NULL, NULL, NOW(), NOW()),
    ('生活茶馆', 'life', '轻松聊天与日常分享', 30, 'NORMAL', 0, 0, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `sort_no` = VALUES(`sort_no`),
    `status` = VALUES(`status`),
    `update_time` = NOW();

SET @board_general := (SELECT `id` FROM `wf_forum_board` WHERE `slug` = 'general' LIMIT 1);
SET @board_dev := (SELECT `id` FROM `wf_forum_board` WHERE `slug` = 'dev' LIMIT 1);
SET @board_life := (SELECT `id` FROM `wf_forum_board` WHERE `slug` = 'life' LIMIT 1);

INSERT INTO `wf_forum_thread` (
    `id`, `board_id`, `author_id`, `title`, `content`, `thread_type`, `status`, `is_essence`,
    `view_count`, `reply_count`, `like_count`, `last_reply_id`, `last_reply_user_id`, `last_reply_time`,
    `create_time`, `update_time`
) VALUES
    (980101, @board_general, @uid_1, '【公告】欢迎来到 WolfChat 社区',
     '这里是社区公告区，后续会持续发布版本动态与活动通知。', 'ANNOUNCEMENT', 'NORMAL', 1,
     128, 2, 3, 970102, @uid_1, '2026-03-10 10:12:00', '2026-03-10 09:20:00', NOW()),
    (980102, @board_dev, @uid_2, '聊天图片上传链路调通记录',
     '今天完成了消息图片上传、签名直传与消息回显，欢迎继续反馈。', 'STICKY', 'NORMAL', 0,
     86, 1, 2, 970103, @uid_1, '2026-03-10 10:15:00', '2026-03-10 09:45:00', NOW()),
    (980103, @board_life, @uid_3, '今日打卡：你在听什么歌？',
     '欢迎在这里分享今日循环播放歌单。', 'NORMAL', 'NORMAL', 0,
     15, 0, 1, NULL, NULL, '2026-03-10 08:40:00', '2026-03-10 08:40:00', NOW())
ON DUPLICATE KEY UPDATE
    `board_id` = VALUES(`board_id`),
    `author_id` = VALUES(`author_id`),
    `title` = VALUES(`title`),
    `content` = VALUES(`content`),
    `thread_type` = VALUES(`thread_type`),
    `status` = VALUES(`status`),
    `is_essence` = VALUES(`is_essence`),
    `view_count` = VALUES(`view_count`),
    `reply_count` = VALUES(`reply_count`),
    `like_count` = VALUES(`like_count`),
    `last_reply_id` = VALUES(`last_reply_id`),
    `last_reply_user_id` = VALUES(`last_reply_user_id`),
    `last_reply_time` = VALUES(`last_reply_time`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

INSERT INTO `wf_forum_reply` (
    `id`, `thread_id`, `floor_no`, `author_id`, `content`, `quote_reply_id`, `like_count`, `status`, `create_time`, `update_time`
) VALUES
    (970101, 980101, 2, @uid_2, '已读，感谢公告。', NULL, 2, 'NORMAL', '2026-03-10 10:05:00', NOW()),
    (970102, 980101, 3, @uid_1, '收到，后续会持续更新。', 970101, 1, 'NORMAL', '2026-03-10 10:12:00', NOW()),
    (970103, 980102, 2, @uid_1, '链路很稳，晚点补压测结果。', NULL, 1, 'NORMAL', '2026-03-10 10:15:00', NOW())
ON DUPLICATE KEY UPDATE
    `thread_id` = VALUES(`thread_id`),
    `floor_no` = VALUES(`floor_no`),
    `author_id` = VALUES(`author_id`),
    `content` = VALUES(`content`),
    `quote_reply_id` = VALUES(`quote_reply_id`),
    `like_count` = VALUES(`like_count`),
    `status` = VALUES(`status`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

INSERT INTO `wf_forum_thread_like` (
    `thread_id`, `user_id`, `create_time`
) VALUES
    (980101, @uid_1, '2026-03-10 10:18:00'),
    (980101, @uid_2, '2026-03-10 10:19:00'),
    (980101, @uid_3, '2026-03-10 10:20:00'),
    (980102, @uid_1, '2026-03-10 10:22:00'),
    (980102, @uid_2, '2026-03-10 10:23:00'),
    (980103, @uid_1, '2026-03-10 10:24:00')
ON DUPLICATE KEY UPDATE
    `create_time` = VALUES(`create_time`);

INSERT INTO `wf_forum_reply_like` (
    `reply_id`, `user_id`, `create_time`
) VALUES
    (970101, @uid_1, '2026-03-10 10:25:00'),
    (970101, @uid_3, '2026-03-10 10:26:00'),
    (970102, @uid_2, '2026-03-10 10:27:00'),
    (970103, @uid_2, '2026-03-10 10:28:00')
ON DUPLICATE KEY UPDATE
    `create_time` = VALUES(`create_time`);

UPDATE `wf_forum_board`
SET `thread_count` = 1,
    `reply_count` = 2,
    `last_thread_id` = 980101,
    `last_reply_time` = '2026-03-10 10:12:00',
    `update_time` = NOW()
WHERE `id` = @board_general;

UPDATE `wf_forum_board`
SET `thread_count` = 1,
    `reply_count` = 1,
    `last_thread_id` = 980102,
    `last_reply_time` = '2026-03-10 10:15:00',
    `update_time` = NOW()
WHERE `id` = @board_dev;

UPDATE `wf_forum_board`
SET `thread_count` = 1,
    `reply_count` = 0,
    `last_thread_id` = 980103,
    `last_reply_time` = '2026-03-10 08:40:00',
    `update_time` = NOW()
WHERE `id` = @board_life;

-- 9) 版务日志样例
INSERT INTO `wf_forum_moderation_log` (
    `id`, `operator_user_id`, `target_type`, `target_id`, `action`, `reason`, `create_time`
) VALUES
    (950001, @uid_1, 'THREAD', 980101, 'LOCK_THREAD', 'AUTHOR_OPERATION', '2026-03-10 11:00:00'),
    (950002, @uid_1, 'THREAD', 980101, 'UNLOCK_THREAD', 'AUTHOR_OPERATION', '2026-03-10 11:10:00'),
    (950003, @uid_2, 'THREAD', 980102, 'STICKY_THREAD', 'AUTHOR_OPERATION', '2026-03-10 11:20:00'),
    (950004, @uid_1, 'THREAD', 980101, 'ESSENCE_THREAD', 'AUTHOR_OPERATION', '2026-03-10 11:30:00')
ON DUPLICATE KEY UPDATE
    `operator_user_id` = VALUES(`operator_user_id`),
    `target_type` = VALUES(`target_type`),
    `target_id` = VALUES(`target_id`),
    `action` = VALUES(`action`),
    `reason` = VALUES(`reason`),
    `create_time` = VALUES(`create_time`);

-- 10) 登录记录样例
INSERT INTO `wf_login_record` (
    `id`, `user_id`, `login_method`, `login_result`, `fail_code`, `account_mask`, `ip`, `user_agent`, `client_type`, `client_version`, `login_time`
) VALUES
    (960001, @uid_1, 'WOLF_NO', 'SUCCESS', NULL, '1234****90', '127.0.0.1', 'MiniProgram Devtools', 'WECHAT_MINIPROGRAM', '1.0.0', '2026-03-10 10:00:00'),
    (960002, @uid_2, 'EMAIL', 'FAIL', 1010, 'w***2@example.com', '127.0.0.1', 'MiniProgram Devtools', 'WECHAT_MINIPROGRAM', '1.0.0', '2026-03-10 10:02:00')
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `login_method` = VALUES(`login_method`),
    `login_result` = VALUES(`login_result`),
    `fail_code` = VALUES(`fail_code`),
    `account_mask` = VALUES(`account_mask`),
    `ip` = VALUES(`ip`),
    `user_agent` = VALUES(`user_agent`),
    `client_type` = VALUES(`client_type`),
    `client_version` = VALUES(`client_version`),
    `login_time` = VALUES(`login_time`);
