package com.wreckloud.wolfchat.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * @Description 邮箱认证链接配置
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.email")
public class EmailVerifyLinkConfig {
    /**
     * 邮箱认证链接地址（不包含 token 参数）
     * 示例：http://localhost:8080/api/auth/email/verify
     */
    private String verifyUrl;

    /**
     * 重置密码链接地址（不包含 token 参数）
     * 示例：http://localhost:8080/api/auth/password/reset
     */
    private String resetPasswordUrl;

    @PostConstruct
    public void validate() {
        validateUrl("wolfchat.email.verify-url", verifyUrl);
        validateUrl("wolfchat.email.reset-password-url", resetPasswordUrl);
    }

    private void validateUrl(String key, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("配置缺失: " + key);
        }
        if (value.contains("?")) {
            throw new IllegalArgumentException("配置非法: " + key + " 不允许包含查询参数");
        }
    }
}
