package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.entity.WfUserBanRecord;
import com.wreckloud.wolfchat.account.domain.enums.BanRecordStatus;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserBanRecordMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户封禁状态服务。
 * 负责到期封禁状态收敛与禁用状态恢复。
 */
@Service
@RequiredArgsConstructor
public class UserBanService {
    private final WfUserMapper wfUserMapper;
    private final WfUserBanRecordMapper wfUserBanRecordMapper;
    private final SessionUserService sessionUserService;

    /**
     * 若用户仅因封禁导致禁用，且封禁已全部失效，则自动恢复为 NORMAL。
     */
    public boolean tryRestoreUserIfBanExpired(Long userId) {
        if (userId == null) {
            return false;
        }

        WfUser user = wfUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        if (!UserStatus.DISABLED.equals(user.getStatus()) || !Boolean.TRUE.equals(user.getDisabledByBan())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        expireDueBans(userId, now);
        if (hasActiveBan(userId, now)) {
            return false;
        }

        LambdaUpdateWrapper<WfUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUser::getId, userId)
                .eq(WfUser::getStatus, UserStatus.DISABLED)
                .eq(WfUser::getDisabledByBan, true)
                .set(WfUser::getStatus, UserStatus.NORMAL)
                .set(WfUser::getDisabledByBan, false);
        int updateRows = wfUserMapper.update(null, updateWrapper);
        if (updateRows == 1) {
            sessionUserService.invalidateUserCache(userId);
            return true;
        }
        if (updateRows != 0) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        return false;
    }

    /**
     * 批量收敛到期封禁并尝试恢复用户状态。
     */
    public int expireDueBansAndRestoreUsers(int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<WfUserBanRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserBanRecord::getStatus, BanRecordStatus.ACTIVE)
                .isNotNull(WfUserBanRecord::getEndTime)
                .le(WfUserBanRecord::getEndTime, now)
                .orderByAsc(WfUserBanRecord::getEndTime)
                .last("LIMIT " + batchSize);
        List<WfUserBanRecord> dueRecords = wfUserBanRecordMapper.selectList(queryWrapper);
        if (dueRecords == null || dueRecords.isEmpty()) {
            return 0;
        }

        Set<Long> affectedUserIds = new LinkedHashSet<>();
        for (WfUserBanRecord dueRecord : dueRecords) {
            LambdaUpdateWrapper<WfUserBanRecord> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WfUserBanRecord::getId, dueRecord.getId())
                    .eq(WfUserBanRecord::getStatus, BanRecordStatus.ACTIVE)
                    .set(WfUserBanRecord::getStatus, BanRecordStatus.EXPIRED)
                    .set(WfUserBanRecord::getLiftedAt, now);
            int updateRows = wfUserBanRecordMapper.update(null, updateWrapper);
            if (updateRows == 1 && dueRecord.getUserId() != null) {
                affectedUserIds.add(dueRecord.getUserId());
            }
        }

        for (Long userId : affectedUserIds) {
            tryRestoreUserIfBanExpired(userId);
        }
        return affectedUserIds.size();
    }

    private void expireDueBans(Long userId, LocalDateTime now) {
        LambdaUpdateWrapper<WfUserBanRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserBanRecord::getUserId, userId)
                .eq(WfUserBanRecord::getStatus, BanRecordStatus.ACTIVE)
                .isNotNull(WfUserBanRecord::getEndTime)
                .le(WfUserBanRecord::getEndTime, now)
                .set(WfUserBanRecord::getStatus, BanRecordStatus.EXPIRED)
                .set(WfUserBanRecord::getLiftedAt, now);
        wfUserBanRecordMapper.update(null, updateWrapper);
    }

    private boolean hasActiveBan(Long userId, LocalDateTime now) {
        LambdaQueryWrapper<WfUserBanRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserBanRecord::getUserId, userId)
                .eq(WfUserBanRecord::getStatus, BanRecordStatus.ACTIVE)
                .and(wrapper -> wrapper.isNull(WfUserBanRecord::getEndTime)
                        .or()
                        .gt(WfUserBanRecord::getEndTime, now));
        return wfUserBanRecordMapper.selectCount(queryWrapper) > 0;
    }
}
