package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.application.service.UserBanService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.entity.WfUserBanRecord;
import com.wreckloud.wolfchat.account.domain.entity.WfUserProfile;
import com.wreckloud.wolfchat.account.domain.enums.BanRecordStatus;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserBanRecordMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserProfileMapper;
import com.wreckloud.wolfchat.admin.api.vo.AdminPageVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminUserRowVO;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端用户管理服务。
 */
@Service
@RequiredArgsConstructor
public class AdminUserManageService {
    private static final long MIN_PAGE = 1L;
    private static final long MIN_PAGE_SIZE = 1L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final WfUserMapper wfUserMapper;
    private final WfUserProfileMapper wfUserProfileMapper;
    private final WfUserBanRecordMapper wfUserBanRecordMapper;
    private final UserService userService;
    private final UserBanService userBanService;
    private final SessionUserService sessionUserService;

    public AdminPageVO<AdminUserRowVO> listUsers(long page, long size, String keyword, UserStatus status) {
        validatePage(page, size);
        String normalizedKeyword = normalizeOptionalKeyword(keyword);

        LambdaQueryWrapper<WfUser> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(WfUser::getStatus, status);
        }
        applyKeywordFilter(queryWrapper, normalizedKeyword);
        queryWrapper.orderByDesc(WfUser::getId);

        Page<WfUser> result = wfUserMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<WfUser> users = result.getRecords();
        if (users.isEmpty()) {
            return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), Collections.emptyList());
        }

        List<Long> userIds = users.stream().map(WfUser::getId).collect(Collectors.toList());
        Map<Long, WfUser> userMap = userService.getUserMap(userIds);

        List<AdminUserRowVO> list = new ArrayList<>(users.size());
        for (WfUser user : users) {
            WfUser mergedUser = userMap.get(user.getId());
            if (mergedUser == null) {
                continue;
            }
            AdminUserRowVO rowVO = new AdminUserRowVO();
            rowVO.setUserId(mergedUser.getId());
            rowVO.setWolfNo(mergedUser.getWolfNo());
            rowVO.setNickname(mergedUser.getNickname());
            rowVO.setStatus(mergedUser.getStatus());
            rowVO.setActiveDayCount(mergedUser.getActiveDayCount());
            rowVO.setLastLoginAt(mergedUser.getLastLoginAt());
            list.add(rowVO);
        }
        return AdminPageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public void updateUserStatus(Long userId, UserStatus targetStatus) {
        if (userId == null || targetStatus == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        userService.getByIdOrThrow(userId);

        LambdaUpdateWrapper<WfUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUser::getId, userId)
                .set(WfUser::getStatus, targetStatus)
                .set(WfUser::getDisabledByBan, false);
        int updateRows = wfUserMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        sessionUserService.invalidateUserCache(userId);
    }

    public void createUserBan(Long operatorUserId, Long userId, String reason, Integer durationHours) {
        if (operatorUserId == null || userId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        if (durationHours != null && durationHours <= 0) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        WfUser targetUser = userService.getByIdOrThrow(userId);
        String normalizedReason = normalizeRequiredText(reason);
        LocalDateTime now = LocalDateTime.now();

        WfUserBanRecord record = new WfUserBanRecord();
        record.setUserId(userId);
        record.setOperatorUserId(operatorUserId);
        record.setReason(normalizedReason);
        record.setStartTime(now);
        record.setEndTime(durationHours == null ? null : now.plusHours(durationHours));
        record.setStatus(BanRecordStatus.ACTIVE);

        int insertRows = wfUserBanRecordMapper.insert(record);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        if (!UserStatus.DISABLED.equals(targetUser.getStatus())) {
            LambdaUpdateWrapper<WfUser> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WfUser::getId, userId)
                    .set(WfUser::getStatus, UserStatus.DISABLED)
                    .set(WfUser::getDisabledByBan, true);
            int updateRows = wfUserMapper.update(null, updateWrapper);
            if (updateRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
            sessionUserService.invalidateUserCache(userId);
        }
    }

    public void liftUserBan(Long banId) {
        if (banId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        WfUserBanRecord record = wfUserBanRecordMapper.selectById(banId);
        if (record == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        if (!BanRecordStatus.ACTIVE.equals(record.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<WfUserBanRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserBanRecord::getId, banId)
                .eq(WfUserBanRecord::getStatus, BanRecordStatus.ACTIVE)
                .set(WfUserBanRecord::getStatus, BanRecordStatus.LIFTED)
                .set(WfUserBanRecord::getLiftedAt, now);
        int updateRows = wfUserBanRecordMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        if (!hasOtherActiveBans(record.getUserId(), banId, now)) {
            userBanService.tryRestoreUserIfBanExpired(record.getUserId());
        }
    }

    private boolean hasOtherActiveBans(Long userId, Long currentBanId, LocalDateTime now) {
        LambdaQueryWrapper<WfUserBanRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserBanRecord::getUserId, userId)
                .eq(WfUserBanRecord::getStatus, BanRecordStatus.ACTIVE)
                .ne(WfUserBanRecord::getId, currentBanId)
                .and(wrapper -> wrapper.isNull(WfUserBanRecord::getEndTime)
                        .or()
                        .gt(WfUserBanRecord::getEndTime, now));
        return wfUserBanRecordMapper.selectCount(queryWrapper) > 0;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<WfUser> queryWrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        List<Long> nicknameMatchedUserIds = listUserIdsByNickname(keyword);
        if (nicknameMatchedUserIds.isEmpty()) {
            queryWrapper.like(WfUser::getWolfNo, keyword);
            return;
        }

        queryWrapper.and(wrapper -> wrapper.like(WfUser::getWolfNo, keyword)
                .or()
                .in(WfUser::getId, nicknameMatchedUserIds));
    }

    private List<Long> listUserIdsByNickname(String keyword) {
        LambdaQueryWrapper<WfUserProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(WfUserProfile::getNickname, keyword)
                .select(WfUserProfile::getUserId);
        List<WfUserProfile> matchedProfiles = wfUserProfileMapper.selectList(queryWrapper);
        if (matchedProfiles.isEmpty()) {
            return Collections.emptyList();
        }
        return matchedProfiles.stream()
                .map(WfUserProfile::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validatePage(long page, long size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
    }

    private String normalizeOptionalKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeRequiredText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        return value.trim();
    }
}
