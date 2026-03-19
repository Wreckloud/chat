package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.application.support.AccountMaskingSupport;
import com.wreckloud.wolfchat.account.domain.entity.WfLoginRecord;
import com.wreckloud.wolfchat.account.domain.enums.LoginMethod;
import com.wreckloud.wolfchat.account.domain.enums.LoginResult;
import com.wreckloud.wolfchat.account.infra.mapper.WfLoginRecordMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * @Description 登录记录服务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Service
@RequiredArgsConstructor
public class LoginRecordService {
    private static final int ACCOUNT_MASK_MAX_LENGTH = 128;
    private static final int IP_MAX_LENGTH = 64;
    private static final int USER_AGENT_MAX_LENGTH = 255;
    private static final int CLIENT_TYPE_MAX_LENGTH = 32;
    private static final int CLIENT_VERSION_MAX_LENGTH = 32;
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final String CLIENT_VERSION_HEADER = "X-Client-Version";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String UNKNOWN_CLIENT_TYPE = "UNKNOWN";

    private final WfLoginRecordMapper wfLoginRecordMapper;

    public void record(
            Long userId,
            LoginMethod loginMethod,
            LoginResult loginResult,
            Integer failCode,
            String account,
            HttpServletRequest request
    ) {
        WfLoginRecord record = new WfLoginRecord();
        record.setUserId(userId);
        record.setLoginMethod(loginMethod);
        record.setLoginResult(loginResult);
        record.setFailCode(failCode);
        record.setAccountMask(limitLength(AccountMaskingSupport.maskAccount(account), ACCOUNT_MASK_MAX_LENGTH));
        record.setIp(limitLength(resolveClientIp(request), IP_MAX_LENGTH));
        record.setUserAgent(limitLength(resolveHeader(request, USER_AGENT_HEADER), USER_AGENT_MAX_LENGTH));
        record.setClientType(limitLength(resolveClientType(request), CLIENT_TYPE_MAX_LENGTH));
        record.setClientVersion(limitLength(resolveHeader(request, CLIENT_VERSION_HEADER), CLIENT_VERSION_MAX_LENGTH));
        record.setLoginTime(LocalDateTime.now());

        int insertRows = wfLoginRecordMapper.insert(record);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = resolveHeader(request, FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            int separatorIndex = forwardedFor.indexOf(',');
            if (separatorIndex > 0) {
                return forwardedFor.substring(0, separatorIndex).trim();
            }
            return forwardedFor;
        }
        return request.getRemoteAddr();
    }

    private String resolveClientType(HttpServletRequest request) {
        String clientType = resolveHeader(request, CLIENT_TYPE_HEADER);
        if (StringUtils.hasText(clientType)) {
            return clientType;
        }
        return UNKNOWN_CLIENT_TYPE;
    }

    private String resolveHeader(HttpServletRequest request, String headerName) {
        if (request == null || !StringUtils.hasText(headerName)) {
            return null;
        }
        String headerValue = request.getHeader(headerName);
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        return headerValue.trim();
    }

    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

}
