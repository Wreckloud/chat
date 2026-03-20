package com.wreckloud.wolfchat.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * @Description 邮件发送配置
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.email.delivery")
public class EmailDeliveryConfig {
    /**
     * 是否启用真实邮件发送
     */
    private Boolean enabled;

    /**
     * 发件人邮箱
     */
    private String from;

    /**
     * 邮件主题前缀
     */
    private String subjectPrefix;

    @PostConstruct
    public void validate() {
        if (enabled == null) {
            enabled = false;
        }
        if (!StringUtils.hasText(subjectPrefix)) {
            subjectPrefix = "WolfChat";
        }
        if (Boolean.TRUE.equals(enabled) && !StringUtils.hasText(from)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.email.delivery.from");
        }
    }
}

