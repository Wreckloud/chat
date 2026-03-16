-- 管理端封禁恢复能力补丁
-- 作用：区分“手动禁用”和“封禁禁用”，避免解封/到期后误恢复账号状态

USE `wolf_chat`;

ALTER TABLE `wf_user`
    ADD COLUMN `disabled_by_ban` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否因封禁导致禁用：0-否，1-是'
    AFTER `active_day_count`;

