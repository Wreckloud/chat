package com.wreckloud.wolfchat.community.application.service;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumModerationLog;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumModerationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 版务日志写入服务，避免业务服务各自维护日志写入细节。
 */
@Service
@RequiredArgsConstructor
public class ForumModerationLogService {
    private final WfForumModerationLogMapper wfForumModerationLogMapper;

    public void record(Long operatorUserId, String targetType, Long targetId, String action, String reason) {
        if (operatorUserId == null || targetId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(targetType) || !StringUtils.hasText(action) || !StringUtils.hasText(reason)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        WfForumModerationLog log = new WfForumModerationLog();
        log.setOperatorUserId(operatorUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setReason(reason);

        int affectedRows = wfForumModerationLogMapper.insert(log);
        if (affectedRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }
}
