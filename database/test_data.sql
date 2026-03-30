-- WolfChat 本地演示数据脚本（可选）
-- 适用：已执行 init.sql 后的本地运行环境

USE `wolf_chat`;

-- 演示账号说明（明文密码一致）
-- 密码：Wolf123456
-- 账号1~12：狼藉号 1234567890 ~ 1234567901
-- 邮箱策略：部分已认证、部分未认证、部分未绑定（覆盖登录/资料/通知场景）
SET @seed_password_hash := '$2a$10$ENlIGWJCpjy8Dz63BNO4M.cJatv7N3l1LyBUJ6kVmFRLcztHelhXS';

-- 1) 用户主表
INSERT INTO `wf_user` (
    `wolf_no`, `status`, `onboarding_status`, `onboarding_completed_at`,
    `first_login_at`, `last_login_at`, `active_day_count`,
    `disabled_by_ban`,
    `equipped_title_code`, `equipped_title_name`, `equipped_title_color`,
    `create_time`, `update_time`
) VALUES
    ('1234567890', 'NORMAL', 'COMPLETED', '2026-03-09 10:00:00', '2026-03-09 10:00:00', '2026-03-10 10:00:00', 5, 0, 'WOLF_CUB', '小狼', '#5f7ea2', NOW(), NOW()),
    ('1234567891', 'NORMAL', 'PENDING', NULL, '2026-03-09 11:00:00', '2026-03-10 09:00:00', 3, 0, 'FIRST_POST', '初啸者', '#4f7f63', NOW(), NOW()),
    ('1234567892', 'NORMAL', 'PENDING', NULL, '2026-03-09 12:00:00', '2026-03-10 08:30:00', 2, 0, NULL, NULL, NULL, NOW(), NOW()),
    ('1234567893', 'NORMAL', 'COMPLETED', '2026-03-10 09:10:00', '2026-03-09 20:00:00', '2026-03-11 12:15:00', 6, 0, 'FIRST_REPLY', '回声者', '#8a6d4b', NOW(), NOW()),
    ('1234567894', 'NORMAL', 'COMPLETED', '2026-03-10 09:20:00', '2026-03-09 20:10:00', '2026-03-11 08:40:00', 4, 0, 'FIRST_FOLLOW', '同行者', '#875f8c', NOW(), NOW()),
    ('1234567895', 'NORMAL', 'PENDING', NULL, '2026-03-09 20:20:00', '2026-03-11 10:10:00', 3, 0, NULL, NULL, NULL, NOW(), NOW()),
    ('1234567896', 'NORMAL', 'COMPLETED', '2026-03-10 09:35:00', '2026-03-09 20:30:00', '2026-03-11 11:45:00', 5, 0, 'FIRST_POST', '初啸者', '#4f7f63', NOW(), NOW()),
    ('1234567897', 'NORMAL', 'PENDING', NULL, '2026-03-09 20:40:00', '2026-03-11 09:05:00', 2, 0, NULL, NULL, NULL, NOW(), NOW()),
    ('1234567898', 'NORMAL', 'COMPLETED', '2026-03-11 09:10:00', '2026-03-10 08:00:00', '2026-03-12 21:30:00', 7, 0, 'FIRST_FOLLOW', '同行者', '#875f8c', NOW(), NOW()),
    ('1234567899', 'NORMAL', 'SKIPPED', NULL, '2026-03-10 09:00:00', '2026-03-13 08:20:00', 1, 0, NULL, NULL, NULL, NOW(), NOW()),
    ('1234567900', 'NORMAL', 'PENDING', NULL, '2026-03-10 09:05:00', '2026-03-14 18:05:00', 2, 0, NULL, NULL, NULL, NOW(), NOW()),
    ('1234567901', 'DISABLED', 'COMPLETED', '2026-03-10 10:30:00', '2026-03-10 10:00:00', '2026-03-10 10:30:00', 1, 1, NULL, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `onboarding_status` = VALUES(`onboarding_status`),
    `onboarding_completed_at` = VALUES(`onboarding_completed_at`),
    `first_login_at` = VALUES(`first_login_at`),
    `last_login_at` = VALUES(`last_login_at`),
    `active_day_count` = VALUES(`active_day_count`),
    `disabled_by_ban` = VALUES(`disabled_by_ban`),
    `equipped_title_code` = VALUES(`equipped_title_code`),
    `equipped_title_name` = VALUES(`equipped_title_name`),
    `equipped_title_color` = VALUES(`equipped_title_color`),
    `update_time` = NOW();

SET @uid_1 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567890' LIMIT 1);
SET @uid_2 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567891' LIMIT 1);
SET @uid_3 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567892' LIMIT 1);
SET @uid_4 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567893' LIMIT 1);
SET @uid_5 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567894' LIMIT 1);
SET @uid_6 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567895' LIMIT 1);
SET @uid_7 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567896' LIMIT 1);
SET @uid_8 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567897' LIMIT 1);
SET @uid_9 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567898' LIMIT 1);
SET @uid_10 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567899' LIMIT 1);
SET @uid_11 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567900' LIMIT 1);
SET @uid_12 := (SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1234567901' LIMIT 1);

-- 2) 号码池（标记为已分配）
INSERT INTO `wf_no_pool` (`wolf_no`, `status`, `user_id`, `create_time`, `update_time`) VALUES
    ('1234567890', 'USED', @uid_1, NOW(), NOW()),
    ('1234567891', 'USED', @uid_2, NOW(), NOW()),
    ('1234567892', 'USED', @uid_3, NOW(), NOW()),
    ('1234567893', 'USED', @uid_4, NOW(), NOW()),
    ('1234567894', 'USED', @uid_5, NOW(), NOW()),
    ('1234567895', 'USED', @uid_6, NOW(), NOW()),
    ('1234567896', 'USED', @uid_7, NOW(), NOW()),
    ('1234567897', 'USED', @uid_8, NOW(), NOW()),
    ('1234567898', 'USED', @uid_9, NOW(), NOW()),
    ('1234567899', 'USED', @uid_10, NOW(), NOW()),
    ('1234567900', 'USED', @uid_11, NOW(), NOW()),
    ('1234567901', 'USED', @uid_12, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `user_id` = VALUES(`user_id`),
    `update_time` = NOW();

-- 3) 成就定义与用户成就
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

INSERT INTO `wf_user_achievement` (
    `user_id`, `achievement_code`, `unlock_time`, `create_time`
) VALUES
    (@uid_1, 'WOLF_CUB', '2026-03-09 10:00:00', NOW()),
    (@uid_1, 'FIRST_REPLY', '2026-03-10 10:15:00', NOW()),
    (@uid_1, 'FIRST_FOLLOW', '2026-03-10 10:16:00', NOW()),
    (@uid_2, 'WOLF_CUB', '2026-03-09 11:00:00', NOW()),
    (@uid_2, 'FIRST_POST', '2026-03-10 09:45:00', NOW()),
    (@uid_3, 'WOLF_CUB', '2026-03-09 12:00:00', NOW()),
    (@uid_4, 'WOLF_CUB', '2026-03-09 20:00:00', NOW()),
    (@uid_4, 'FIRST_REPLY', '2026-03-10 11:22:00', NOW()),
    (@uid_5, 'WOLF_CUB', '2026-03-09 20:10:00', NOW()),
    (@uid_5, 'FIRST_FOLLOW', '2026-03-10 12:10:00', NOW()),
    (@uid_6, 'WOLF_CUB', '2026-03-09 20:20:00', NOW()),
    (@uid_7, 'WOLF_CUB', '2026-03-09 20:30:00', NOW()),
    (@uid_7, 'FIRST_POST', '2026-03-11 08:50:00', NOW()),
    (@uid_8, 'WOLF_CUB', '2026-03-09 20:40:00', NOW())
ON DUPLICATE KEY UPDATE
    `unlock_time` = VALUES(`unlock_time`);

-- 4) 用户资料
INSERT INTO `wf_user_profile` (
    `user_id`, `nickname`, `avatar`, `signature`, `bio`, `create_time`, `update_time`
) VALUES
    (@uid_1, 'Wreckloud', NULL, '欢迎来到 WolfChat', '常驻账号，日常在社区和大厅发言', NOW(), NOW()),
    (@uid_2, 'USVING', NULL, '今天也要写代码', '活跃用户，常在夜间回复私聊', NOW(), NOW()),
    (@uid_3, '旅行青蛙', NULL, '消息已读但未回', '主要用于浏览与轻互动', NOW(), NOW()),
    (@uid_4, '夜航灯塔', NULL, '慢慢来，先把功能做稳', '偏好讨论产品体验和交互细节', NOW(), NOW()),
    (@uid_5, '纸鸢', NULL, '今天也在调接口', '偏好写实现记录和踩坑总结', NOW(), NOW()),
    (@uid_6, '北岸回声', NULL, '晚上再上线', '偏好在大厅里聊天打卡', NOW(), NOW()),
    (@uid_7, '星尘旅人', NULL, '每周都想优化一点点', '偏好发帖讨论社区规则与产品方向', NOW(), NOW()),
    (@uid_8, '橘子汽水', NULL, '正在潜水观察', '低频用户，主要浏览帖子和聊天记录', NOW(), NOW()),
    (@uid_9, '南风', NULL, '今天想把未读做完', '偏好整理清单与活动安排', NOW(), NOW()),
    (@uid_10, '白噪音', NULL, '上线前再看看日志', '偏好刷大厅与看通知', NOW(), NOW()),
    (@uid_11, '墨迹', NULL, '写完再说', '偏好在论坛里讨论需求边界', NOW(), NOW()),
    (@uid_12, '已封禁示例', NULL, '（账号被封禁）', '用于呈现封禁状态下的页面表现', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `signature` = VALUES(`signature`),
    `bio` = VALUES(`bio`),
    `update_time` = NOW();

-- 5) 认证信息（狼藉号密码 + 邮箱密码）
INSERT INTO `wf_user_auth` (
    `user_id`, `auth_type`, `auth_identifier`, `credential_hash`, `verified`, `enabled`, `last_login_at`, `create_time`, `update_time`
) VALUES
    (@uid_1, 'WOLF_NO_PASSWORD', '1234567890', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_2, 'WOLF_NO_PASSWORD', '1234567891', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_3, 'WOLF_NO_PASSWORD', '1234567892', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_4, 'WOLF_NO_PASSWORD', '1234567893', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_5, 'WOLF_NO_PASSWORD', '1234567894', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_6, 'WOLF_NO_PASSWORD', '1234567895', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_7, 'WOLF_NO_PASSWORD', '1234567896', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_8, 'WOLF_NO_PASSWORD', '1234567897', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_9, 'WOLF_NO_PASSWORD', '1234567898', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_10, 'WOLF_NO_PASSWORD', '1234567899', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_11, 'WOLF_NO_PASSWORD', '1234567900', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_12, 'WOLF_NO_PASSWORD', '1234567901', @seed_password_hash, 1, 0, NOW(), NOW(), NOW()),
    (@uid_1, 'EMAIL_PASSWORD', 'wolf1@example.com', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_2, 'EMAIL_PASSWORD', 'wolf2@example.com', @seed_password_hash, 0, 1, NOW(), NOW(), NOW()),
    (@uid_4, 'EMAIL_PASSWORD', 'wolf4@example.com', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_5, 'EMAIL_PASSWORD', 'wolf5@example.com', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_6, 'EMAIL_PASSWORD', 'wolf6@example.com', @seed_password_hash, 0, 1, NOW(), NOW(), NOW()),
    (@uid_7, 'EMAIL_PASSWORD', 'wolf7@example.com', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_9, 'EMAIL_PASSWORD', 'wolf9@example.com', @seed_password_hash, 1, 1, NOW(), NOW(), NOW()),
    (@uid_10, 'EMAIL_PASSWORD', 'wolf10@example.com', @seed_password_hash, 0, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `credential_hash` = VALUES(`credential_hash`),
    `verified` = VALUES(`verified`),
    `enabled` = VALUES(`enabled`),
    `last_login_at` = VALUES(`last_login_at`),
    `update_time` = NOW();

-- 6) 关注关系（构造互关/单向关注混合场景）
INSERT INTO `wf_follow` (
    `follower_id`, `followee_id`, `status`, `create_time`, `update_time`
) VALUES
    (@uid_1, @uid_2, 'FOLLOWING', NOW(), NOW()),
    (@uid_2, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_1, @uid_3, 'FOLLOWING', NOW(), NOW()),
    (@uid_1, @uid_4, 'FOLLOWING', NOW(), NOW()),
    (@uid_4, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_2, @uid_4, 'FOLLOWING', NOW(), NOW()),
    (@uid_5, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_6, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_7, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_2, @uid_6, 'FOLLOWING', NOW(), NOW()),
    (@uid_6, @uid_2, 'FOLLOWING', NOW(), NOW()),
    (@uid_3, @uid_7, 'FOLLOWING', NOW(), NOW()),
    (@uid_7, @uid_3, 'FOLLOWING', NOW(), NOW()),
    (@uid_8, @uid_2, 'FOLLOWING', NOW(), NOW()),
    (@uid_9, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_1, @uid_9, 'FOLLOWING', NOW(), NOW()),
    (@uid_10, @uid_1, 'FOLLOWING', NOW(), NOW()),
    (@uid_11, @uid_2, 'FOLLOWING', NOW(), NOW()),
    (@uid_2, @uid_11, 'FOLLOWING', NOW(), NOW()),
    (@uid_10, @uid_3, 'UNFOLLOWED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- 7) 会话与消息（账号1 与 账号2）：日常聊天基线
SET @conv_user_a := LEAST(@uid_1, @uid_2);
SET @conv_user_b := GREATEST(@uid_1, @uid_2);

INSERT INTO `wf_conversation` (
    `user_a_id`, `user_b_id`, `last_message_id`, `last_message`, `last_message_time`,
    `user_a_unread_count`, `user_b_unread_count`, `create_time`, `update_time`
) VALUES
    (@conv_user_a, @conv_user_b, 990002, '收到，晚上图书馆见', '2026-03-10 09:31:00', 0, 0, NOW(), NOW())
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
    (990001, @conv_12, @uid_1, @uid_2, '你好，晚点一起去图书馆吗', 'TEXT', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-10 09:30:00', '2026-03-10 09:30:00'),
    (990002, @conv_12, @uid_2, @uid_1, '收到，晚上图书馆见', 'TEXT', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-10 09:31:00', '2026-03-10 09:31:00')
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
    `last_message` = '收到，晚上图书馆见',
    `last_message_time` = '2026-03-10 09:31:00',
    `update_time` = NOW()
WHERE `id` = @conv_12;

-- 7.2) 会话与消息（账号1 与 账号4）：构造未读 + 未送达消息（覆盖离线补发与未读）
SET @conv_user_a_14 := LEAST(@uid_1, @uid_4);
SET @conv_user_b_14 := GREATEST(@uid_1, @uid_4);

INSERT INTO `wf_conversation` (
    `user_a_id`, `user_b_id`, `last_message_id`, `last_message`, `last_message_time`,
    `user_a_unread_count`, `user_b_unread_count`, `create_time`, `update_time`
) VALUES
    (@conv_user_a_14, @conv_user_b_14, 990012, '我这边先离线一会儿', '2026-03-12 22:31:00', 0, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `last_message_id` = VALUES(`last_message_id`),
    `last_message` = VALUES(`last_message`),
    `last_message_time` = VALUES(`last_message_time`),
    `user_a_unread_count` = VALUES(`user_a_unread_count`),
    `user_b_unread_count` = VALUES(`user_b_unread_count`),
    `update_time` = NOW();

SET @conv_14 := (
    SELECT `id`
    FROM `wf_conversation`
    WHERE `user_a_id` = @conv_user_a_14 AND `user_b_id` = @conv_user_b_14
    LIMIT 1
);

INSERT INTO `wf_message` (
    `id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`,
    `delivered`, `delivered_time`, `create_time`
) VALUES
    (990011, @conv_14, @uid_4, @uid_1, '我在看你论坛那篇公告，写得不错。', 'TEXT', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-12 22:30:00', '2026-03-12 22:30:00'),
    (990012, @conv_14, @uid_1, @uid_4, '谢谢！我准备把社区从帖子流升级成 BBS 楼层。', 'TEXT', NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-03-12 22:31:00'),
    (990013, @conv_14, @uid_1, @uid_4, '你离线期间我先把接口和表都对齐了，等你上线再一起验。', 'TEXT', NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-03-12 22:31:20')
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
SET `last_message_id` = 990013,
    `last_message` = '你离线期间我先把接口和表都对齐了，等你上线再一起验。',
    `last_message_time` = '2026-03-12 22:31:20',
    `update_time` = NOW()
WHERE `id` = @conv_14;

-- 7.3) 会话与消息（账号2 与 账号6）：构造富媒体消息（IMAGE/FILE）覆盖渲染与下载
SET @conv_user_a_26 := LEAST(@uid_2, @uid_6);
SET @conv_user_b_26 := GREATEST(@uid_2, @uid_6);

INSERT INTO `wf_conversation` (
    `user_a_id`, `user_b_id`, `last_message_id`, `last_message`, `last_message_time`,
    `user_a_unread_count`, `user_b_unread_count`, `create_time`, `update_time`
) VALUES
    (@conv_user_a_26, @conv_user_b_26, 990023, '[文件] 课程协作清单.pdf', '2026-03-13 09:12:00', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `last_message_id` = VALUES(`last_message_id`),
    `last_message` = VALUES(`last_message`),
    `last_message_time` = VALUES(`last_message_time`),
    `user_a_unread_count` = VALUES(`user_a_unread_count`),
    `user_b_unread_count` = VALUES(`user_b_unread_count`),
    `update_time` = NOW();

SET @conv_26 := (
    SELECT `id`
    FROM `wf_conversation`
    WHERE `user_a_id` = @conv_user_a_26 AND `user_b_id` = @conv_user_b_26
    LIMIT 1
);

INSERT INTO `wf_message` (
    `id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`,
    `delivered`, `delivered_time`, `create_time`
) VALUES
    (990021, @conv_26, @uid_2, @uid_6, '我把课堂安排和活动截图发你，帮我看看排版是否清楚。', 'TEXT', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-13 09:10:00', '2026-03-13 09:10:00'),
    (990022, @conv_26, @uid_2, @uid_6, '社团活动流程草图（随手拍）', 'IMAGE', 'chat/image/1234567891/2026/03/13/board_sketch_001.jpg', 1080, 1440, 246812, 'image/jpeg', 1, '2026-03-13 09:11:00', '2026-03-13 09:11:00'),
    (990023, @conv_26, @uid_6, @uid_2, '课程协作清单.pdf', 'FILE', 'chat/file/1234567895/2026/03/13/课程协作清单.pdf', NULL, NULL, 483220, 'application/pdf', 1, '2026-03-13 09:12:00', '2026-03-13 09:12:00')
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
SET `last_message_id` = 990023,
    `last_message` = '[文件] 课程协作清单.pdf',
    `last_message_time` = '2026-03-13 09:12:00',
    `update_time` = NOW()
WHERE `id` = @conv_26;

-- 8) 大厅消息（补充更多发言人/更真实的时间线）
INSERT INTO `wf_lobby_message` (
    `id`, `sender_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`, `create_time`
) VALUES
    (980001, @uid_1, '欢迎来到公共聊天室，在线的人都可以发言。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-10 10:20:00'),
    (980002, @uid_2, '这里后续可以一起聊社区话题和项目进展。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-10 10:21:00'),
    (980003, @uid_6, '今天上线了一版通知页，大家帮我看看未读角标有没有漏。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-11 21:05:00'),
    (980004, @uid_7, '论坛那边我想加“置顶/精华/锁帖”的标签展示，你们觉得放哪里更顺手？', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-11 21:08:00'),
    (980005, @uid_9, '我明天把“会话未读闭环”补一下，顺便测 WS 离线补发。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, '2026-03-12 09:10:00')
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

-- 9) 论坛基础数据（版块/主题/回复）
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
    (980102, @board_dev, @uid_2, '课堂随拍分享与上传记录',
     '今天整理了课堂随拍和笔记上传流程，欢迎补充建议。', 'STICKY', 'NORMAL', 0,
     86, 1, 2, 970103, @uid_1, '2026-03-10 10:15:00', '2026-03-10 09:45:00', NOW()),
    (980103, @board_life, @uid_3, '今日打卡：你在听什么歌？',
     '欢迎在这里分享今日循环播放歌单。', 'NORMAL', 'NORMAL', 0,
     15, 0, 1, NULL, NULL, '2026-03-10 08:40:00', '2026-03-10 08:40:00', NOW()),
    (980104, @board_dev, @uid_9, '会话未读模型：我现在这样设计对吗？',
     '我计划只在 `wf_conversation` 维护未读数，消息表不引入 read 状态，客户端进入会话后调用 /read 清零。这样是否足够覆盖论文主线？', 'NORMAL', 'NORMAL', 0,
     33, 3, 1, 970106, @uid_2, '2026-03-12 10:40:00', '2026-03-12 10:10:00', NOW()),
    (980105, @board_general, @uid_10, '【求助】登录后偶尔提示状态失效？',
     '刚刚用邮箱登录后刷新会话列表提示未授权，过一会又好了。有人遇到过吗？（可能是我本地缓存问题）', 'NORMAL', 'NORMAL', 0,
     42, 1, 0, 970107, @uid_1, '2026-03-12 11:05:00', '2026-03-12 11:00:00', NOW()),
    (980106, @board_life, @uid_7, '周末大家准备怎么放松？',
     '我打算先把论文结构列完，再去跑步。你们呢？', 'NORMAL', 'LOCKED', 0,
     12, 2, 0, 970109, @uid_6, '2026-03-13 20:11:00', '2026-03-13 19:50:00', NOW())
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
    (970103, 980102, 2, @uid_1, '流程很顺，晚点我把现场照片也补上。', NULL, 1, 'NORMAL', '2026-03-10 10:15:00', NOW()),
    (970104, 980104, 2, @uid_2, '如果只做论文主线，我觉得够了：未读数放会话表，进入会话就清零。后续要扩“逐条已读”再另起表。', NULL, 2, 'NORMAL', '2026-03-12 10:25:00', NOW()),
    (970105, 980104, 3, @uid_6, '赞同。另外未读总数接口也要测一下：/conversations/unread-count。', 970104, 1, 'NORMAL', '2026-03-12 10:33:00', NOW()),
    (970106, 980104, 4, @uid_9, '收到，我先按最小可交付闭环做。', 970105, 0, 'NORMAL', '2026-03-12 10:40:00', NOW()),
    (970107, 980105, 2, @uid_1, '我之前也遇到过，后来发现是设备时间偏差导致登录状态校验失败。你先校准系统时间试试。', NULL, 1, 'NORMAL', '2026-03-12 11:05:00', NOW()),
    (970108, 980106, 2, @uid_6, '我周末想把 Lobby 的消息分页再压一下，避免首屏抖动。', NULL, 0, 'NORMAL', '2026-03-13 20:05:00', NOW()),
    (970109, 980106, 3, @uid_7, '锁帖前留个记录：本帖到此为止，周末愉快。', 970108, 0, 'NORMAL', '2026-03-13 20:11:00', NOW())
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
    (980103, @uid_1, '2026-03-10 10:24:00'),
    (980104, @uid_1, '2026-03-12 10:45:00'),
    (980104, @uid_2, '2026-03-12 10:46:00'),
    (980105, @uid_4, '2026-03-12 11:10:00')
ON DUPLICATE KEY UPDATE
    `create_time` = VALUES(`create_time`);

INSERT INTO `wf_forum_reply_like` (
    `reply_id`, `user_id`, `create_time`
) VALUES
    (970101, @uid_1, '2026-03-10 10:25:00'),
    (970101, @uid_3, '2026-03-10 10:26:00'),
    (970102, @uid_2, '2026-03-10 10:27:00'),
    (970103, @uid_2, '2026-03-10 10:28:00'),
    (970104, @uid_1, '2026-03-12 10:47:00'),
    (970107, @uid_2, '2026-03-12 11:12:00')
ON DUPLICATE KEY UPDATE
    `create_time` = VALUES(`create_time`);

-- 论坛统计字段：用聚合回写，避免后续加主题/回复时漏改
UPDATE `wf_forum_board` b
SET
    b.`thread_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_thread` t
        WHERE t.`board_id` = b.`id` AND t.`status` IN ('NORMAL', 'LOCKED')
    ),
    b.`reply_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_reply` r
        JOIN `wf_forum_thread` t ON t.`id` = r.`thread_id`
        WHERE t.`board_id` = b.`id` AND t.`status` IN ('NORMAL', 'LOCKED') AND r.`status` <> 'DELETED'
    ),
    b.`last_thread_id` = (
        SELECT t2.`id`
        FROM `wf_forum_thread` t2
        WHERE t2.`board_id` = b.`id` AND t2.`status` IN ('NORMAL', 'LOCKED')
        ORDER BY COALESCE(t2.`last_reply_time`, t2.`create_time`) DESC, t2.`id` DESC
        LIMIT 1
    ),
    b.`last_reply_time` = (
        SELECT COALESCE(MAX(t3.`last_reply_time`), MAX(t3.`create_time`))
        FROM `wf_forum_thread` t3
        WHERE t3.`board_id` = b.`id` AND t3.`status` IN ('NORMAL', 'LOCKED')
    ),
    b.`update_time` = NOW()
WHERE b.`id` IN (@board_general, @board_dev, @board_life);

UPDATE `wf_forum_thread` t
SET
    t.`reply_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_reply` r
        WHERE r.`thread_id` = t.`id` AND r.`status` <> 'DELETED'
    ),
    t.`like_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_thread_like` l
        WHERE l.`thread_id` = t.`id`
    ),
    t.`last_reply_id` = (
        SELECT r2.`id`
        FROM `wf_forum_reply` r2
        WHERE r2.`thread_id` = t.`id` AND r2.`status` <> 'DELETED'
        ORDER BY r2.`floor_no` DESC, r2.`id` DESC
        LIMIT 1
    ),
    t.`last_reply_user_id` = (
        SELECT r3.`author_id`
        FROM `wf_forum_reply` r3
        WHERE r3.`thread_id` = t.`id` AND r3.`status` <> 'DELETED'
        ORDER BY r3.`floor_no` DESC, r3.`id` DESC
        LIMIT 1
    ),
    t.`last_reply_time` = (
        SELECT MAX(r4.`create_time`)
        FROM `wf_forum_reply` r4
        WHERE r4.`thread_id` = t.`id` AND r4.`status` <> 'DELETED'
    ),
    t.`update_time` = NOW()
WHERE t.`board_id` IN (@board_general, @board_dev, @board_life);

-- 10) 版务日志记录
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

-- 11) 登录记录
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

-- 12) 系统通知记录
INSERT INTO `wf_user_notice` (
    `id`, `user_id`, `notice_type`, `content`, `biz_type`, `biz_id`, `is_read`, `read_time`, `create_time`, `update_time`
) VALUES
    (940001, @uid_1, 'ACHIEVEMENT_UNLOCK', '你解锁了成就「初入群落」，获得头衔「小狼」', 'ACHIEVEMENT', NULL, 0, NULL, '2026-03-10 10:40:00', NOW()),
    (940002, @uid_1, 'THREAD_LIKED', '你的主题收到新的点赞', 'THREAD', 980101, 0, NULL, '2026-03-10 10:42:00', NOW()),
    (940003, @uid_1, 'THREAD_REPLIED', '你的主题收到新的回复', 'THREAD', 980101, 1, '2026-03-10 10:50:00', '2026-03-10 10:45:00', NOW()),
    (940004, @uid_2, 'FOLLOW_RECEIVED', '你收到新的关注', 'FOLLOW', NULL, 0, NULL, '2026-03-10 10:47:00', NOW()),
    (940005, @uid_4, 'CHAT_MESSAGE_REPLIED', '你的消息收到新的回复', 'CHAT', @conv_14, 0, NULL, '2026-03-12 22:31:30', NOW()),
    (940006, @uid_9, 'THREAD_REPLIED', '你的主题收到新的回复', 'THREAD', 980104, 0, NULL, '2026-03-12 10:25:30', NOW())
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `notice_type` = VALUES(`notice_type`),
    `content` = VALUES(`content`),
    `biz_type` = VALUES(`biz_type`),
    `biz_id` = VALUES(`biz_id`),
    `is_read` = VALUES(`is_read`),
    `read_time` = VALUES(`read_time`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

-- 13) 封禁记录（历史记录，不影响当前登录）
INSERT INTO `wf_user_ban_record` (
    `id`, `user_id`, `operator_user_id`, `reason`, `start_time`, `end_time`, `status`, `lifted_at`, `create_time`, `update_time`
) VALUES
    (930001, @uid_3, @uid_1, '历史封禁记录：课堂群冲突言论', '2026-03-09 08:00:00', '2026-03-09 12:00:00', 'LIFTED', '2026-03-09 10:30:00', '2026-03-09 08:00:00', NOW())
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `operator_user_id` = VALUES(`operator_user_id`),
    `reason` = VALUES(`reason`),
    `start_time` = VALUES(`start_time`),
    `end_time` = VALUES(`end_time`),
    `status` = VALUES(`status`),
    `lifted_at` = VALUES(`lifted_at`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

-- ===========================
-- 扩展数据（更接近真实校园日常）
-- 覆盖：更多会话未读、离线补发、论坛状态变化、登录记录与通知类型
-- ===========================

-- A) 会话与消息（账号7 与 账号8）：构造“对方多条离线未送达”覆盖补发顺序
SET @conv_user_a_78 := LEAST(@uid_7, @uid_8);
SET @conv_user_b_78 := GREATEST(@uid_7, @uid_8);

INSERT INTO `wf_conversation` (
    `user_a_id`, `user_b_id`, `last_message_id`, `last_message`, `last_message_time`,
    `user_a_unread_count`, `user_b_unread_count`, `create_time`, `update_time`
) VALUES
    (@conv_user_a_78, @conv_user_b_78, 990034, '我先发几条，看看离线补发顺不顺', '2026-03-14 22:12:00', 0, 4, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `last_message_id` = VALUES(`last_message_id`),
    `last_message` = VALUES(`last_message`),
    `last_message_time` = VALUES(`last_message_time`),
    `user_a_unread_count` = VALUES(`user_a_unread_count`),
    `user_b_unread_count` = VALUES(`user_b_unread_count`),
    `update_time` = NOW();

SET @conv_78 := (
    SELECT `id`
    FROM `wf_conversation`
    WHERE `user_a_id` = @conv_user_a_78 AND `user_b_id` = @conv_user_b_78
    LIMIT 1
);

INSERT INTO `wf_message` (
    `id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`,
    `delivered`, `delivered_time`, `create_time`
) VALUES
    (990031, @conv_78, @uid_7, @uid_8, '1/4：你现在在线吗？', 'TEXT', NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-03-14 22:10:00'),
    (990032, @conv_78, @uid_7, @uid_8, '2/4：我在测 WS 补发和 ACK。', 'TEXT', NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-03-14 22:10:20'),
    (990033, @conv_78, @uid_7, @uid_8, '3/4：如果你刚好离线，重连后应该会一次性推送。', 'TEXT', NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-03-14 22:11:00'),
    (990034, @conv_78, @uid_7, @uid_8, '4/4：我先发几条，看看离线补发顺不顺', 'TEXT', NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-03-14 22:12:00')
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
SET `last_message_id` = 990034,
    `last_message` = '我先发几条，看看离线补发顺不顺',
    `last_message_time` = '2026-03-14 22:12:00',
    `update_time` = NOW()
WHERE `id` = @conv_78;

-- B) 大厅消息：补 1 条图片 + 1 条文件（覆盖富媒体渲染/下载）
INSERT INTO `wf_lobby_message` (
    `id`, `sender_id`, `content`, `msg_type`,
    `media_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`, `create_time`
) VALUES
    (980006, @uid_2, '傍晚校园光影（主路口）', 'IMAGE',
     'chat/image/1234567891/2026/03/14/theme_preview.png', 1170, 2532, 182744, 'image/png', '2026-03-14 09:20:00'),
    (980007, @uid_6, '大厅资料：校园活动清单（草稿）.txt', 'FILE',
     'chat/file/1234567895/2026/03/14/campus_regression_checklist.txt', NULL, NULL, 8921, 'text/plain', '2026-03-14 09:22:00')
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

-- C) 论坛：补充已删除主题与回复（用于回收站与统计边界）
INSERT INTO `wf_forum_thread` (
    `id`, `board_id`, `author_id`, `title`, `content`, `thread_type`, `status`, `is_essence`,
    `view_count`, `reply_count`, `like_count`, `last_reply_id`, `last_reply_user_id`, `last_reply_time`,
    `create_time`, `update_time`
) VALUES
    (980110, @board_general, @uid_11, '【归档】这条主题已转入回收站',
     '该帖已归档，用于保留历史讨论，不再出现在广场列表。', 'NORMAL', 'DELETED', 0,
     9, 2, 0, 970121, @uid_11, '2026-03-14 10:05:00', '2026-03-14 10:00:00', NOW())
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
    (970120, 980110, 2, @uid_11, '先回一条，等下删主题。', NULL, 0, 'NORMAL', '2026-03-14 10:03:00', NOW()),
    (970121, 980110, 3, @uid_11, '这条回复已归档，不参与楼层展示。', 970120, 0, 'DELETED', '2026-03-14 10:05:00', NOW())
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

INSERT INTO `wf_forum_moderation_log` (
    `id`, `operator_user_id`, `target_type`, `target_id`, `action`, `reason`, `create_time`
) VALUES
    (950005, @uid_11, 'THREAD', 980110, 'DELETE_THREAD', 'AUTHOR_OPERATION', '2026-03-14 10:06:00'),
    (950006, @uid_11, 'REPLY', 970121, 'DELETE_REPLY', 'AUTHOR_OPERATION', '2026-03-14 10:06:10')
ON DUPLICATE KEY UPDATE
    `operator_user_id` = VALUES(`operator_user_id`),
    `target_type` = VALUES(`target_type`),
    `target_id` = VALUES(`target_id`),
    `action` = VALUES(`action`),
    `reason` = VALUES(`reason`),
    `create_time` = VALUES(`create_time`);

-- D) 登录记录：补充多终端与失败原因（便于管理端筛选）
INSERT INTO `wf_login_record` (
    `id`, `user_id`, `login_method`, `login_result`, `fail_code`, `account_mask`, `ip`, `user_agent`, `client_type`, `client_version`, `login_time`
) VALUES
    (960003, @uid_4, 'WOLF_NO', 'SUCCESS', NULL, '1234****93', '192.168.3.20', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome', 'WEB', 'admin-1.0.0', '2026-03-12 21:10:00'),
    (960004, @uid_6, 'EMAIL', 'SUCCESS', NULL, 'w***6@example.com', '192.168.3.33', 'MiniProgram iOS', 'WECHAT_MINIPROGRAM', '1.0.0', '2026-03-13 09:00:00'),
    (960005, NULL, 'UNKNOWN', 'FAIL', 2001, '12**90', '192.168.3.88', 'MiniProgram Devtools', 'WECHAT_MINIPROGRAM', '1.0.0', '2026-03-13 09:01:00'),
    (960006, @uid_12, 'WOLF_NO', 'FAIL', 2001, '1234****01', '192.168.3.55', 'MiniProgram Android', 'WECHAT_MINIPROGRAM', '1.0.0', '2026-03-14 08:40:00')
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

-- E) 系统通知：补充“未读/已读混合 + 不同用户”，并覆盖更多业务提示文本
INSERT INTO `wf_user_notice` (
    `id`, `user_id`, `notice_type`, `content`, `biz_type`, `biz_id`, `is_read`, `read_time`, `create_time`, `update_time`
) VALUES
    (940007, @uid_8, 'LOBBY_MESSAGE_REPLIED', '你的大厅消息收到新的回复', 'LOBBY', NULL, 0, NULL, '2026-03-14 22:12:10', NOW()),
    (940008, @uid_2, 'THREAD_LIKED', '你的主题「课堂随拍分享与上传记录」收到新的点赞', 'THREAD', 980102, 1, '2026-03-12 12:00:00', '2026-03-12 11:58:00', NOW()),
    (940009, @uid_6, 'FOLLOW_RECEIVED', '你收到新的关注（来自 南风）', 'FOLLOW', NULL, 0, NULL, '2026-03-12 09:30:00', NOW())
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `notice_type` = VALUES(`notice_type`),
    `content` = VALUES(`content`),
    `biz_type` = VALUES(`biz_type`),
    `biz_id` = VALUES(`biz_id`),
    `is_read` = VALUES(`is_read`),
    `read_time` = VALUES(`read_time`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

-- F) 封禁记录：补 1 条 ACTIVE（与 uid_12 的 disabled_by_ban=1 配套）
INSERT INTO `wf_user_ban_record` (
    `id`, `user_id`, `operator_user_id`, `reason`, `start_time`, `end_time`, `status`, `lifted_at`, `create_time`, `update_time`
) VALUES
    (930002, @uid_12, @uid_1, '当前封禁：课堂群恶意刷屏', '2026-03-14 08:30:00', '2026-04-01 08:30:00', 'ACTIVE', NULL, '2026-03-14 08:30:00', NOW())
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `operator_user_id` = VALUES(`operator_user_id`),
    `reason` = VALUES(`reason`),
    `start_time` = VALUES(`start_time`),
    `end_time` = VALUES(`end_time`),
    `status` = VALUES(`status`),
    `lifted_at` = VALUES(`lifted_at`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

-- G) 重新回写论坛统计（包含扩展数据）
UPDATE `wf_forum_board` b
SET
    b.`thread_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_thread` t
        WHERE t.`board_id` = b.`id` AND t.`status` IN ('NORMAL', 'LOCKED')
    ),
    b.`reply_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_reply` r
        JOIN `wf_forum_thread` t ON t.`id` = r.`thread_id`
        WHERE t.`board_id` = b.`id` AND t.`status` IN ('NORMAL', 'LOCKED') AND r.`status` <> 'DELETED'
    ),
    b.`last_thread_id` = (
        SELECT t2.`id`
        FROM `wf_forum_thread` t2
        WHERE t2.`board_id` = b.`id` AND t2.`status` IN ('NORMAL', 'LOCKED')
        ORDER BY COALESCE(t2.`last_reply_time`, t2.`create_time`) DESC, t2.`id` DESC
        LIMIT 1
    ),
    b.`last_reply_time` = (
        SELECT COALESCE(MAX(t3.`last_reply_time`), MAX(t3.`create_time`))
        FROM `wf_forum_thread` t3
        WHERE t3.`board_id` = b.`id` AND t3.`status` IN ('NORMAL', 'LOCKED')
    ),
    b.`update_time` = NOW()
WHERE b.`id` IN (@board_general, @board_dev, @board_life);

UPDATE `wf_forum_thread` t
SET
    t.`reply_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_reply` r
        WHERE r.`thread_id` = t.`id` AND r.`status` <> 'DELETED'
    ),
    t.`like_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_thread_like` l
        WHERE l.`thread_id` = t.`id`
    ),
    t.`last_reply_id` = (
        SELECT r2.`id`
        FROM `wf_forum_reply` r2
        WHERE r2.`thread_id` = t.`id` AND r2.`status` <> 'DELETED'
        ORDER BY r2.`floor_no` DESC, r2.`id` DESC
        LIMIT 1
    ),
    t.`last_reply_user_id` = (
        SELECT r3.`author_id`
        FROM `wf_forum_reply` r3
        WHERE r3.`thread_id` = t.`id` AND r3.`status` <> 'DELETED'
        ORDER BY r3.`floor_no` DESC, r3.`id` DESC
        LIMIT 1
    ),
    t.`last_reply_time` = (
        SELECT MAX(r4.`create_time`)
        FROM `wf_forum_reply` r4
        WHERE r4.`thread_id` = t.`id` AND r4.`status` <> 'DELETED'
    ),
    t.`update_time` = NOW()
WHERE t.`board_id` IN (@board_general, @board_dev, @board_life);

-- H) 校园生活增强数据（覆盖拉黑、草稿/回收站、楼中楼、AI状态与媒体）
SET @ai_uid := COALESCE((SELECT `id` FROM `wf_user` WHERE `wolf_no` = '1597671722' LIMIT 1), 2);

-- H.1) 拉黑关系：覆盖 BLOCKED / UNBLOCKED 两种状态
INSERT INTO `wf_user_block` (
    `blocker_id`, `blocked_id`, `status`, `create_time`, `update_time`
) VALUES
    (@uid_11, @uid_1, 'BLOCKED', '2026-03-16 21:10:00', NOW()),
    (@uid_3, @uid_10, 'UNBLOCKED', '2026-03-15 19:20:00', NOW()),
    (@uid_8, @uid_12, 'BLOCKED', '2026-03-15 08:05:00', NOW())
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- H.2) 私聊：补充“回复链 + 拉黑后发送失败（仅发送者可见）”
SET @conv_user_a_111 := LEAST(@uid_1, @uid_11);
SET @conv_user_b_111 := GREATEST(@uid_1, @uid_11);

INSERT INTO `wf_conversation` (
    `user_a_id`, `user_b_id`, `last_message_id`, `last_message`, `last_message_time`,
    `user_a_unread_count`, `user_b_unread_count`, `create_time`, `update_time`
) VALUES
    (@conv_user_a_111, @conv_user_b_111, 990202, '消息发送失败', '2026-03-16 21:13:00', 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `last_message_id` = VALUES(`last_message_id`),
    `last_message` = VALUES(`last_message`),
    `last_message_time` = VALUES(`last_message_time`),
    `user_a_unread_count` = VALUES(`user_a_unread_count`),
    `user_b_unread_count` = VALUES(`user_b_unread_count`),
    `update_time` = NOW();

SET @conv_111 := (
    SELECT `id`
    FROM `wf_conversation`
    WHERE `user_a_id` = @conv_user_a_111 AND `user_b_id` = @conv_user_b_111
    LIMIT 1
);

INSERT INTO `wf_message` (
    `id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `msg_type`,
    `media_key`, `media_poster_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`,
    `reply_to_message_id`, `reply_to_sender_id`, `reply_to_preview`,
    `receiver_visible`, `delivered`, `delivered_time`, `create_time`
) VALUES
    (990201, @conv_111, @uid_11, @uid_1, '晚课下课了没？', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, 1, 1, '2026-03-16 21:11:00', '2026-03-16 21:11:00'),
    (990202, @conv_111, @uid_1, @uid_11, '刚下课，操场跑两圈？', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, NULL,
     990201, @uid_11, '晚课下课了没？', 0, 2, NULL, '2026-03-16 21:13:00')
ON DUPLICATE KEY UPDATE
    `conversation_id` = VALUES(`conversation_id`),
    `sender_id` = VALUES(`sender_id`),
    `receiver_id` = VALUES(`receiver_id`),
    `content` = VALUES(`content`),
    `msg_type` = VALUES(`msg_type`),
    `media_key` = VALUES(`media_key`),
    `media_poster_key` = VALUES(`media_poster_key`),
    `media_width` = VALUES(`media_width`),
    `media_height` = VALUES(`media_height`),
    `media_size` = VALUES(`media_size`),
    `media_mime_type` = VALUES(`media_mime_type`),
    `reply_to_message_id` = VALUES(`reply_to_message_id`),
    `reply_to_sender_id` = VALUES(`reply_to_sender_id`),
    `reply_to_preview` = VALUES(`reply_to_preview`),
    `receiver_visible` = VALUES(`receiver_visible`),
    `delivered` = VALUES(`delivered`),
    `delivered_time` = VALUES(`delivered_time`),
    `create_time` = VALUES(`create_time`);

-- H.3) 大厅：补一组校园日常时间线（文本/图片/视频）
INSERT INTO `wf_lobby_message` (
    `id`, `sender_id`, `content`, `msg_type`,
    `media_key`, `media_poster_key`, `media_width`, `media_height`, `media_size`, `media_mime_type`,
    `reply_to_message_id`, `reply_to_sender_id`, `reply_to_preview`, `create_time`
) VALUES
    (980201, @uid_4, '图书馆二楼靠窗还有 6 个座位，晚点可能就满了。', 'TEXT',
     NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, '2026-03-16 18:35:00'),
    (980202, @uid_7, '今晚操场风有点大，夜跑记得带外套。', 'IMAGE',
     'chat/image/1234567896/2026/03/16/campus-track-night.jpg', NULL, 1280, 720, 214002, 'image/jpeg',
     NULL, NULL, NULL, '2026-03-16 20:05:00'),
    (980203, @uid_9, '三食堂新开的窗口我录了 12 秒，味道还行。', 'VIDEO',
     'chat/video/1234567898/2026/03/16/canteen-window.mp4', 'chat/image/1234567898/2026/03/16/canteen-window.poster.jpg', 720, 1280, 13602488, 'video/mp4',
     980201, @uid_4, '图书馆二楼靠窗还有 6 个座位，晚点可能就满了。', '2026-03-16 20:10:00')
ON DUPLICATE KEY UPDATE
    `sender_id` = VALUES(`sender_id`),
    `content` = VALUES(`content`),
    `msg_type` = VALUES(`msg_type`),
    `media_key` = VALUES(`media_key`),
    `media_poster_key` = VALUES(`media_poster_key`),
    `media_width` = VALUES(`media_width`),
    `media_height` = VALUES(`media_height`),
    `media_size` = VALUES(`media_size`),
    `media_mime_type` = VALUES(`media_mime_type`),
    `reply_to_message_id` = VALUES(`reply_to_message_id`),
    `reply_to_sender_id` = VALUES(`reply_to_sender_id`),
    `reply_to_preview` = VALUES(`reply_to_preview`),
    `create_time` = VALUES(`create_time`);

-- H.4) 论坛：校园主题（普通帖 + 媒体帖 + 草稿 + 回收站）
INSERT INTO `wf_forum_thread` (
    `id`, `board_id`, `author_id`, `title`, `content`,
    `image_keys`, `video_key`, `video_poster_key`,
    `thread_type`, `status`, `is_essence`,
    `view_count`, `reply_count`, `like_count`,
    `last_reply_id`, `last_reply_user_id`, `last_reply_time`, `edit_time`,
    `create_time`, `update_time`
) VALUES
    (980201, @board_general, @uid_2, '图书馆二楼自习位实时交换帖',
     '晚上七点以后人会突然变多，大家可以在这里同步空位信息，免得白跑。靠窗位置适合背书，插座位在中段。',
     'chat/forum/thread/image/1234567891/2026/03/16/library-seat-1.jpg,chat/forum/thread/image/1234567891/2026/03/16/library-seat-2.jpg', NULL, NULL,
     'NORMAL', 'NORMAL', 0, 57, 3, 4, 970203, @uid_2, '2026-03-16 19:25:00', '2026-03-16 19:00:00',
     '2026-03-16 18:50:00', NOW()),
    (980202, @board_life, @uid_6, '操场夜跑 10 分钟剪影（附视频）',
     '今天配速一般，但风景很好。后面想把大家的夜跑路线整理成一张地图。', NULL,
     'chat/forum/thread/video/1234567895/2026/03/16/night-run.mp4', 'chat/forum/thread/image/1234567895/2026/03/16/night-run.poster.jpg',
     'NORMAL', 'NORMAL', 0, 29, 1, 2, 970204, @uid_1, '2026-03-16 21:08:00', NULL,
     '2026-03-16 21:00:00', NOW()),
    (980203, @board_dev, @uid_9, '校园网晚高峰丢包排查记录',
     '机房到宿舍区在 21:00~22:30 丢包明显，先记录链路：网关、DNS、出口，后续补抓包截图。', NULL, NULL, NULL,
     'NORMAL', 'NORMAL', 0, 41, 2, 3, 970206, @uid_9, '2026-03-16 22:15:00', NULL,
     '2026-03-16 21:35:00', NOW()),
    (980204, @board_general, @uid_1, '期末周自习室预约攻略（草稿）',
     '先写个草稿：不同教学楼的开放时间和预约入口，整理完再发布。', NULL, NULL, NULL,
     'NORMAL', 'DRAFT', 0, 0, 0, 0, NULL, NULL, NULL, '2026-03-16 22:40:00',
     '2026-03-16 22:30:00', NOW()),
    (980205, @board_general, @uid_1, '旧版选课吐槽汇总（回收站）',
     '历史归档：保留给个人回看，不再对外展示。', NULL, NULL, NULL,
     'NORMAL', 'PURGED', 0, 18, 0, 1, NULL, NULL, NULL, '2026-03-16 17:40:00',
     '2026-03-15 20:00:00', NOW())
ON DUPLICATE KEY UPDATE
    `board_id` = VALUES(`board_id`),
    `author_id` = VALUES(`author_id`),
    `title` = VALUES(`title`),
    `content` = VALUES(`content`),
    `image_keys` = VALUES(`image_keys`),
    `video_key` = VALUES(`video_key`),
    `video_poster_key` = VALUES(`video_poster_key`),
    `thread_type` = VALUES(`thread_type`),
    `status` = VALUES(`status`),
    `is_essence` = VALUES(`is_essence`),
    `view_count` = VALUES(`view_count`),
    `reply_count` = VALUES(`reply_count`),
    `like_count` = VALUES(`like_count`),
    `last_reply_id` = VALUES(`last_reply_id`),
    `last_reply_user_id` = VALUES(`last_reply_user_id`),
    `last_reply_time` = VALUES(`last_reply_time`),
    `edit_time` = VALUES(`edit_time`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

INSERT INTO `wf_forum_reply` (
    `id`, `thread_id`, `floor_no`, `author_id`, `content`, `image_key`, `quote_reply_id`, `like_count`, `status`, `create_time`, `update_time`
) VALUES
    (970201, 980201, 2, @uid_4, '刚从图书馆出来，靠窗基本满了，中间还有两排空位。', NULL, NULL, 2, 'NORMAL', '2026-03-16 19:06:00', NOW()),
    (970202, 980201, 3, @uid_7, '回复 @夜航灯塔：收到，我晚自习改去中间区域。', NULL, 970201, 1, 'NORMAL', '2026-03-16 19:18:00', NOW()),
    (970203, 980201, 4, @uid_2, '我补一下：22:00 后插座位会空出来一些。', 'chat/forum/reply/image/1234567891/2026/03/16/library-plug-area.jpg', 970202, 1, 'NORMAL', '2026-03-16 19:25:00', NOW()),
    (970204, 980202, 2, @uid_1, '这个夜景不错，配速控制也挺稳。', NULL, NULL, 1, 'NORMAL', '2026-03-16 21:08:00', NOW()),
    (970205, 980203, 2, @uid_5, '我这边宿舍区 21:40 会突然掉到 30% 丢包，可能是高峰拥塞。', NULL, NULL, 1, 'NORMAL', '2026-03-16 22:05:00', NOW()),
    (970206, 980203, 3, @uid_9, '收到，我明天补一版 traceroute 结果。', NULL, 970205, 0, 'NORMAL', '2026-03-16 22:15:00', NOW())
ON DUPLICATE KEY UPDATE
    `thread_id` = VALUES(`thread_id`),
    `floor_no` = VALUES(`floor_no`),
    `author_id` = VALUES(`author_id`),
    `content` = VALUES(`content`),
    `image_key` = VALUES(`image_key`),
    `quote_reply_id` = VALUES(`quote_reply_id`),
    `like_count` = VALUES(`like_count`),
    `status` = VALUES(`status`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

INSERT INTO `wf_forum_thread_like` (
    `thread_id`, `user_id`, `create_time`
) VALUES
    (980201, @uid_1, '2026-03-16 19:10:00'),
    (980201, @uid_4, '2026-03-16 19:12:00'),
    (980201, @uid_7, '2026-03-16 19:14:00'),
    (980201, @uid_9, '2026-03-16 19:16:00'),
    (980202, @uid_2, '2026-03-16 21:09:00'),
    (980202, @uid_7, '2026-03-16 21:10:00'),
    (980203, @uid_1, '2026-03-16 22:08:00'),
    (980203, @uid_5, '2026-03-16 22:09:00'),
    (980203, @uid_6, '2026-03-16 22:10:00')
ON DUPLICATE KEY UPDATE
    `create_time` = VALUES(`create_time`);

INSERT INTO `wf_forum_reply_like` (
    `reply_id`, `user_id`, `create_time`
) VALUES
    (970201, @uid_1, '2026-03-16 19:20:00'),
    (970201, @uid_2, '2026-03-16 19:21:00'),
    (970202, @uid_4, '2026-03-16 19:22:00'),
    (970203, @uid_7, '2026-03-16 19:26:00'),
    (970204, @uid_6, '2026-03-16 21:12:00'),
    (970205, @uid_9, '2026-03-16 22:11:00')
ON DUPLICATE KEY UPDATE
    `create_time` = VALUES(`create_time`);

INSERT INTO `wf_forum_moderation_log` (
    `id`, `operator_user_id`, `target_type`, `target_id`, `action`, `reason`, `create_time`
) VALUES
    (950010, @uid_1, 'THREAD', 980205, 'DELETE_THREAD', 'AUTHOR_OPERATION', '2026-03-16 17:45:00')
ON DUPLICATE KEY UPDATE
    `operator_user_id` = VALUES(`operator_user_id`),
    `target_type` = VALUES(`target_type`),
    `target_id` = VALUES(`target_id`),
    `action` = VALUES(`action`),
    `reason` = VALUES(`reason`),
    `create_time` = VALUES(`create_time`);

-- H.5) AI：补充会话状态 + 摘要 + 用户长期记忆
INSERT INTO `wf_ai_session_state` (
    `scene`, `session_key`, `bot_user_id`, `user_id`, `mood`,
    `warmth`, `energy`, `patience`, `topic`,
    `last_user_message`, `last_ai_reply`, `message_count`, `last_touched_at`,
    `create_time`, `update_time`
) VALUES
    ('lobby', 'lobby:global', @ai_uid, @uid_1, 'CURIOUS',
     64, 58, 73, '期末周作息',
     '今晚图书馆还有位吗？', '二楼中段还有几排，靠窗基本满了。', 18, '2026-03-16 22:20:00',
     NOW(), NOW()),
    ('private', CONCAT('private:', LEAST(@ai_uid, @uid_1), ':', GREATEST(@ai_uid, @uid_1)), @ai_uid, @uid_1, 'PLAYFUL',
     72, 61, 67, '复习进度',
     '明天概率论要小测，我有点慌。', '先把题型过一轮，别上来就死磕难题。', 9, '2026-03-16 22:28:00',
     NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `bot_user_id` = VALUES(`bot_user_id`),
    `user_id` = VALUES(`user_id`),
    `mood` = VALUES(`mood`),
    `warmth` = VALUES(`warmth`),
    `energy` = VALUES(`energy`),
    `patience` = VALUES(`patience`),
    `topic` = VALUES(`topic`),
    `last_user_message` = VALUES(`last_user_message`),
    `last_ai_reply` = VALUES(`last_ai_reply`),
    `message_count` = VALUES(`message_count`),
    `last_touched_at` = VALUES(`last_touched_at`),
    `update_time` = NOW();

INSERT INTO `wf_ai_session_summary` (
    `scene`, `session_key`, `bot_user_id`, `summary_text`, `message_count`, `last_summarized_at`,
    `create_time`, `update_time`
) VALUES
    ('lobby', 'lobby:global', @ai_uid,
     '最近讨论集中在图书馆空位、夜跑和食堂窗口；用户偏好短句互动，AI 以轻调侃风格跟进。',
     36, '2026-03-16 22:21:00', NOW(), NOW()),
    ('private', CONCAT('private:', LEAST(@ai_uid, @uid_1), ':', GREATEST(@ai_uid, @uid_1)), @ai_uid,
     '与用户围绕期末复习与作息聊了两轮，用户更偏好直接建议而非长解释。',
     12, '2026-03-16 22:29:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `bot_user_id` = VALUES(`bot_user_id`),
    `summary_text` = VALUES(`summary_text`),
    `message_count` = VALUES(`message_count`),
    `last_summarized_at` = VALUES(`last_summarized_at`),
    `update_time` = NOW();

INSERT INTO `wf_ai_user_memory_fact` (
    `bot_user_id`, `user_id`, `fact_key`, `fact_value`, `confidence`, `source_scene`, `last_seen_at`,
    `create_time`, `update_time`
) VALUES
    (@ai_uid, @uid_1, 'favorite_place', '图书馆二楼靠窗位', 0.8200, 'lobby', '2026-03-16 22:20:00', NOW(), NOW()),
    (@ai_uid, @uid_1, 'recent_topic', '期末周复习节奏', 0.7600, 'private', '2026-03-16 22:28:00', NOW(), NOW()),
    (@ai_uid, @uid_2, 'hobby', '夜跑和校园夜景拍摄', 0.7100, 'forum', '2026-03-16 21:10:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `fact_value` = VALUES(`fact_value`),
    `confidence` = VALUES(`confidence`),
    `source_scene` = VALUES(`source_scene`),
    `last_seen_at` = VALUES(`last_seen_at`),
    `update_time` = NOW();

-- H.6) 通知：补更多校园语境通知文案（未读/已读混合）
INSERT INTO `wf_user_notice` (
    `id`, `user_id`, `notice_type`, `content`, `biz_type`, `biz_id`, `is_read`, `read_time`, `create_time`, `update_time`
) VALUES
    (940020, @uid_2, 'THREAD_REPLIED', '你的主题「图书馆二楼自习位实时交换帖」有了新的回复', 'THREAD', 980201, 0, NULL, '2026-03-16 19:26:00', NOW()),
    (940021, @uid_6, 'THREAD_LIKED', '你的主题「操场夜跑 10 分钟剪影（附视频）」收到了新的点赞', 'THREAD', 980202, 0, NULL, '2026-03-16 21:12:00', NOW()),
    (940022, @uid_1, 'LOBBY_MESSAGE_REPLIED', '你在大厅的消息收到新回应', 'LOBBY', NULL, 1, '2026-03-16 20:20:00', '2026-03-16 20:15:00', NOW())
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `notice_type` = VALUES(`notice_type`),
    `content` = VALUES(`content`),
    `biz_type` = VALUES(`biz_type`),
    `biz_id` = VALUES(`biz_id`),
    `is_read` = VALUES(`is_read`),
    `read_time` = VALUES(`read_time`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW();

-- I) 最终回写论坛统计（确保草稿/回收站不进入版块统计）
UPDATE `wf_forum_board` b
SET
    b.`thread_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_thread` t
        WHERE t.`board_id` = b.`id` AND t.`status` IN ('NORMAL', 'LOCKED')
    ),
    b.`reply_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_reply` r
        JOIN `wf_forum_thread` t ON t.`id` = r.`thread_id`
        WHERE t.`board_id` = b.`id` AND t.`status` IN ('NORMAL', 'LOCKED') AND r.`status` <> 'DELETED'
    ),
    b.`last_thread_id` = (
        SELECT t2.`id`
        FROM `wf_forum_thread` t2
        WHERE t2.`board_id` = b.`id` AND t2.`status` IN ('NORMAL', 'LOCKED')
        ORDER BY COALESCE(t2.`last_reply_time`, t2.`create_time`) DESC, t2.`id` DESC
        LIMIT 1
    ),
    b.`last_reply_time` = (
        SELECT COALESCE(MAX(t3.`last_reply_time`), MAX(t3.`create_time`))
        FROM `wf_forum_thread` t3
        WHERE t3.`board_id` = b.`id` AND t3.`status` IN ('NORMAL', 'LOCKED')
    ),
    b.`update_time` = NOW()
WHERE b.`id` IN (@board_general, @board_dev, @board_life);

UPDATE `wf_forum_thread` t
SET
    t.`reply_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_reply` r
        WHERE r.`thread_id` = t.`id` AND r.`status` <> 'DELETED'
    ),
    t.`like_count` = (
        SELECT COUNT(1)
        FROM `wf_forum_thread_like` l
        WHERE l.`thread_id` = t.`id`
    ),
    t.`last_reply_id` = (
        SELECT r2.`id`
        FROM `wf_forum_reply` r2
        WHERE r2.`thread_id` = t.`id` AND r2.`status` <> 'DELETED'
        ORDER BY r2.`floor_no` DESC, r2.`id` DESC
        LIMIT 1
    ),
    t.`last_reply_user_id` = (
        SELECT r3.`author_id`
        FROM `wf_forum_reply` r3
        WHERE r3.`thread_id` = t.`id` AND r3.`status` <> 'DELETED'
        ORDER BY r3.`floor_no` DESC, r3.`id` DESC
        LIMIT 1
    ),
    t.`last_reply_time` = (
        SELECT MAX(r4.`create_time`)
        FROM `wf_forum_reply` r4
        WHERE r4.`thread_id` = t.`id` AND r4.`status` <> 'DELETED'
    ),
    t.`update_time` = NOW()
WHERE t.`board_id` IN (@board_general, @board_dev, @board_life);

