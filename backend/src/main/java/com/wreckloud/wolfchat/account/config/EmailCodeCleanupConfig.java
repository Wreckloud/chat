package com.wreckloud.wolfchat.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * @Description 邮箱验证码清理任务配置
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wolfchat.email-code.cleanup")
public class EmailCodeCleanupConfig {
    /**
     * 是否启用清理任务
     */
    private Boolean enabled;

    /**
     * 清理频率（Cron）
     */
    private String cron;

    /**
     * 过期后保留天数
     */
    private Integer retainDays;

    /**
     * 单批删除数量
     */
    private Integer batchSize;

    /**
     * 单次任务最大批次数
     */
    private Integer maxBatches;

    @PostConstruct
    public void validate() {
        if (enabled == null) {
            throw new IllegalArgumentException("配置缺失: wolfchat.email-code.cleanup.enabled");
        }
        if (!StringUtils.hasText(cron)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.email-code.cleanup.cron");
        }
        if (retainDays == null || retainDays < 1) {
            throw new IllegalArgumentException("配置非法: wolfchat.email-code.cleanup.retain-days 必须 >= 1");
        }
        if (batchSize == null || batchSize < 1) {
            throw new IllegalArgumentException("配置非法: wolfchat.email-code.cleanup.batch-size 必须 >= 1");
        }
        if (maxBatches == null || maxBatches < 1) {
            throw new IllegalArgumentException("配置非法: wolfchat.email-code.cleanup.max-batches 必须 >= 1");
        }
    }
}
