package com.wreckloud.wolfchat.account.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 封禁到期收敛任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBanExpireTask {
    private static final int BATCH_SIZE = 200;

    private final UserBanService userBanService;

    @Scheduled(fixedDelay = 60_000L)
    public void expireBans() {
        try {
            int restoredUserCount = userBanService.expireDueBansAndRestoreUsers(BATCH_SIZE);
            if (restoredUserCount > 0) {
                log.info("封禁到期收敛完成: restoredUserCount={}", restoredUserCount);
            }
        } catch (Exception e) {
            log.warn("封禁到期收敛任务失败: {}", e.getMessage());
            log.debug("封禁到期收敛任务异常详情", e);
        }
    }
}
