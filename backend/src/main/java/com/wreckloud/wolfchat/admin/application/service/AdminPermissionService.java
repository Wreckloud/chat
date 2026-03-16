package com.wreckloud.wolfchat.admin.application.service;

import com.wreckloud.wolfchat.admin.config.AdminConsoleConfig;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端权限校验服务。
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private final AdminConsoleConfig adminConsoleConfig;

    public void assertAdmin(Long userId) {
        if (userId == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        List<Long> allowedUserIds = adminConsoleConfig.getAllowedUserIds();
        if (allowedUserIds == null || !allowedUserIds.contains(userId)) {
            throw new BaseException(ErrorCode.ADMIN_FORBIDDEN);
        }
    }
}

