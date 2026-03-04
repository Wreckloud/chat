package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 行者服务
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final WfUserMapper wfUserMapper;

    /**
     * 根据行者ID获取用户信息 VO
     */
    public UserVO getUserVOById(Long userId) {
        return UserConverter.toUserVO(getByIdOrThrow(userId));
    }

    /**
     * 根据行者ID获取行者实体，不存在抛异常
     */
    public WfUser getByIdOrThrow(Long userId) {
        if (userId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        WfUser user = wfUserMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 根据狼藉号获取行者实体，不存在抛异常
     */
    public WfUser getByWolfNoOrThrow(String wolfNo) {
        if (!StringUtils.hasText(wolfNo)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        LambdaQueryWrapper<WfUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUser::getWolfNo, wolfNo);
        WfUser user = wfUserMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BaseException(ErrorCode.WOLF_NO_NOT_FOUND);
        }
        return user;
    }

    /**
     * 根据行者ID获取可用账号（状态必须为 NORMAL）
     */
    public WfUser getEnabledByIdOrThrow(Long userId) {
        WfUser user = getByIdOrThrow(userId);
        checkEnabled(user);
        return user;
    }

    /**
     * 根据狼藉号获取可用账号（状态必须为 NORMAL）
     */
    public WfUser getEnabledByWolfNoOrThrow(String wolfNo) {
        WfUser user = getByWolfNoOrThrow(wolfNo);
        checkEnabled(user);
        return user;
    }

    /**
     * 批量查询行者并按 userId 建立映射
     */
    public Map<Long, WfUser> getUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return wfUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(WfUser::getId, user -> user));
    }

    private void checkEnabled(WfUser user) {
        if (UserStatus.DISABLED.equals(user.getStatus())) {
            throw new BaseException(ErrorCode.USER_DISABLED);
        }
    }
}
