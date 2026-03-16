package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfLoginRecord;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfLoginRecordMapper;
import com.wreckloud.wolfchat.admin.api.vo.AdminActionLogRowVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminLoginLogRowVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminPageVO;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumModerationLog;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumModerationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端审计服务。
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private static final long MIN_PAGE = 1L;
    private static final long MIN_PAGE_SIZE = 1L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final WfForumModerationLogMapper wfForumModerationLogMapper;
    private final WfLoginRecordMapper wfLoginRecordMapper;
    private final UserService userService;

    public AdminPageVO<AdminActionLogRowVO> listActionLogPage(long page, long size) {
        validatePage(page, size);

        LambdaQueryWrapper<WfForumModerationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(WfForumModerationLog::getCreateTime)
                .orderByDesc(WfForumModerationLog::getId);

        Page<WfForumModerationLog> result = wfForumModerationLogMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<WfForumModerationLog> records = result.getRecords();
        if (records.isEmpty()) {
            return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), Collections.emptyList());
        }

        List<Long> operatorIds = records.stream()
                .map(WfForumModerationLog::getOperatorUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, WfUser> userMap = userService.getUserMap(operatorIds);

        List<AdminActionLogRowVO> list = new ArrayList<>(records.size());
        for (WfForumModerationLog record : records) {
            AdminActionLogRowVO rowVO = new AdminActionLogRowVO();
            rowVO.setActionLogId(record.getId());
            rowVO.setOperatorName(resolveOperatorName(userMap.get(record.getOperatorUserId()), record.getOperatorUserId()));
            rowVO.setActionType(record.getAction());
            rowVO.setTargetType(record.getTargetType());
            rowVO.setTargetId(record.getTargetId());
            rowVO.setDetail(record.getReason());
            rowVO.setCreateTime(record.getCreateTime());
            list.add(rowVO);
        }
        return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public AdminPageVO<AdminLoginLogRowVO> listLoginLogPage(long page, long size) {
        validatePage(page, size);

        LambdaQueryWrapper<WfLoginRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(WfLoginRecord::getLoginTime)
                .orderByDesc(WfLoginRecord::getId);

        Page<WfLoginRecord> result = wfLoginRecordMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<WfLoginRecord> records = result.getRecords();
        if (records.isEmpty()) {
            return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), Collections.emptyList());
        }

        List<AdminLoginLogRowVO> list = new ArrayList<>(records.size());
        for (WfLoginRecord record : records) {
            AdminLoginLogRowVO rowVO = new AdminLoginLogRowVO();
            rowVO.setLogId(record.getId());
            rowVO.setAccountMask(record.getAccountMask());
            rowVO.setLoginMethod(record.getLoginMethod() == null ? null : record.getLoginMethod().getValue());
            rowVO.setLoginResult(record.getLoginResult() == null ? null : record.getLoginResult().getValue());
            rowVO.setIp(record.getIp());
            rowVO.setClientType(record.getClientType());
            rowVO.setLoginTime(record.getLoginTime());
            list.add(rowVO);
        }
        return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    private String resolveOperatorName(WfUser operator, Long operatorUserId) {
        if (operator != null && operator.getNickname() != null) {
            return operator.getNickname();
        }
        return "用户#" + operatorUserId;
    }

    private void validatePage(long page, long size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
    }
}

