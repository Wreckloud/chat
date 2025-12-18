package com.wreckloud.wolfchat.common.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 管理员配置属性
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.admin")
public class AdminProperties {
    
    /**
     * 超级管理员WF号列表（逗号分隔）
     * 示例：1000001,1000002
     */
    private String superAdminWfNumbers;
    
    /**
     * 获取超级管理员WF号列表
     * @return WF号列表
     */
    public List<Long> getSuperAdminWfNumberList() {
        if (StringUtils.isBlank(superAdminWfNumbers)) {
            return Collections.emptyList();
        }
        
        try {
            return Arrays.stream(superAdminWfNumbers.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("超级管理员WF号配置格式错误: " + superAdminWfNumbers, e);
        }
    }
    
    /**
     * 检查是否是超级管理员
     * @param wfNo WF号
     * @return true-是超级管理员
     */
    public boolean isSuperAdmin(Long wfNo) {
        if (wfNo == null) {
            return false;
        }
        return getSuperAdminWfNumberList().contains(wfNo);
    }
}

