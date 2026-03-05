package com.wreckloud.wolfchat.account.application.task;

import com.wreckloud.wolfchat.account.application.service.EmailCodeService;
import com.wreckloud.wolfchat.account.config.EmailCodeCleanupConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Description 邮箱验证码过期清理任务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailCodeCleanupTask {
    private final EmailCodeService emailCodeService;
    private final EmailCodeCleanupConfig cleanupConfig;

    /**
     * 定时清理过期验证码
     */
    @Scheduled(cron = "${wolfchat.email-code.cleanup.cron}")
    public void cleanupExpiredCodes() {
        if (!Boolean.TRUE.equals(cleanupConfig.getEnabled())) {
            return;
        }

        int retainDays = cleanupConfig.getRetainDays();
        int batchSize = cleanupConfig.getBatchSize();
        int maxBatches = cleanupConfig.getMaxBatches();
        int deleted = emailCodeService.cleanupExpiredCodes(retainDays, batchSize, maxBatches);

        if (deleted > 0) {
            log.info(
                    "邮箱验证码清理完成: deleted={}, retainDays={}, batchSize={}, maxBatches={}",
                    deleted,
                    retainDays,
                    batchSize,
                    maxBatches
            );
        }
    }
}
