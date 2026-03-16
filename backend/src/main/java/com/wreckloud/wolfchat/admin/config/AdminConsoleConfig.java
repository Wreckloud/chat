package com.wreckloud.wolfchat.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端访问配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.admin.console")
public class AdminConsoleConfig {
    /**
     * 允许访问管理端的用户ID列表。
     */
    private List<Long> allowedUserIds = new ArrayList<>();
}

