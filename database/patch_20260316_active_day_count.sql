-- 活跃天数字段迁移脚本
-- 说明：将 wf_user.login_count 替换为 wf_user.active_day_count

USE `wolf_chat`;

ALTER TABLE `wf_user`
    ADD COLUMN `active_day_count` INT NOT NULL DEFAULT 0 COMMENT '活跃天数（按登录日期去重统计）' AFTER `last_login_at`;

-- 历史值无法精确回推，按“有登录记录记 1 天，无登录记录记 0 天”初始化
UPDATE `wf_user`
SET `active_day_count` = CASE
    WHEN `last_login_at` IS NULL THEN 0
    ELSE 1
END;

ALTER TABLE `wf_user`
    DROP COLUMN `login_count`;
