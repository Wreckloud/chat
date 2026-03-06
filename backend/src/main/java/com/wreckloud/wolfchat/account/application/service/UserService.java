package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.UserPublicVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.entity.WfUserAuth;
import com.wreckloud.wolfchat.account.domain.entity.WfUserProfile;
import com.wreckloud.wolfchat.account.domain.enums.OnboardingStatus;
import com.wreckloud.wolfchat.account.domain.enums.UserAuthType;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserAuthMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserProfileMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private static final String DELETED_NICKNAME_PREFIX = "已注销用户";

    private final WfUserMapper wfUserMapper;
    private final WfUserProfileMapper wfUserProfileMapper;
    private final WfUserAuthMapper wfUserAuthMapper;
    private final UserAuthService userAuthService;
    private final SessionUserService sessionUserService;

    /**
     * 根据行者ID获取用户信息 VO
     */
    public UserVO getCurrentUserVOById(Long userId) {
        return UserConverter.toUserVO(getByIdOrThrow(userId));
    }

    /**
     * 根据行者ID获取公开用户信息 VO（不包含隐私字段）
     */
    public UserPublicVO getPublicUserVOById(Long userId) {
        return UserConverter.toUserPublicVO(getByIdOrThrow(userId));
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
        return attachUserExtOrThrow(user);
    }

    /**
     * 根据狼藉号获取行者实体，不存在抛异常
     */
    public WfUser getByWolfNoOrThrow(String wolfNo) {
        if (!StringUtils.hasText(wolfNo)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        LambdaQueryWrapper<WfUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUser::getWolfNo, wolfNo.trim());
        WfUser user = wfUserMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BaseException(ErrorCode.WOLF_NO_NOT_FOUND);
        }
        return attachUserExtOrThrow(user);
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
     * 更新当前用户新用户引导状态
     */
    public void updateOnboardingStatus(Long userId, OnboardingStatus onboardingStatus) {
        if (onboardingStatus == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        WfUser user = getEnabledByIdOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<WfUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUser::getId, user.getId())
                .set(WfUser::getOnboardingStatus, onboardingStatus);
        if (OnboardingStatus.PENDING.equals(onboardingStatus)) {
            updateWrapper.set(WfUser::getOnboardingCompletedAt, null);
        } else {
            updateWrapper.set(WfUser::getOnboardingCompletedAt, now);
        }
        int updateRows = wfUserMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    /**
     * 创建用户资料
     */
    public void createProfile(Long userId, String nickname) {
        if (userId == null || !StringUtils.hasText(nickname)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        WfUserProfile profile = new WfUserProfile();
        profile.setUserId(userId);
        profile.setNickname(nickname.trim());
        int insertRows = wfUserProfileMapper.insert(profile);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    /**
     * 注销当前用户（测试期：立即生效）
     * 逻辑删除用户，清空敏感资料，禁用并归档认证标识
     */
    @Transactional(rollbackFor = Exception.class)
    public void deactivateCurrentUser(Long userId) {
        WfUser user = getEnabledByIdOrThrow(userId);

        LambdaUpdateWrapper<WfUser> userUpdateWrapper = new LambdaUpdateWrapper<>();
        userUpdateWrapper.eq(WfUser::getId, userId)
                .set(WfUser::getStatus, UserStatus.DISABLED);
        int userUpdateRows = wfUserMapper.update(null, userUpdateWrapper);
        if (userUpdateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        LambdaUpdateWrapper<WfUserProfile> profileUpdateWrapper = new LambdaUpdateWrapper<>();
        profileUpdateWrapper.eq(WfUserProfile::getUserId, userId)
                .set(WfUserProfile::getNickname, buildDeletedNickname(user.getId()))
                .set(WfUserProfile::getAvatar, null)
                .set(WfUserProfile::getSignature, null)
                .set(WfUserProfile::getBio, null);
        int profileUpdateRows = wfUserProfileMapper.update(null, profileUpdateWrapper);
        if (profileUpdateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        userAuthService.disableAndArchiveAllAuthByUserId(userId);
        sessionUserService.invalidateUserCache(userId);
    }

    /**
     * 批量查询行者并按 userId 建立映射
     */
    public Map<Long, WfUser> getUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<WfUser> users = wfUserMapper.selectBatchIds(userIds);
        Map<Long, WfUser> userMap = users.stream()
                .collect(Collectors.toMap(WfUser::getId, user -> user, (left, right) -> left));

        List<WfUser> userList = new ArrayList<>(userMap.values());
        attachProfilesOrThrow(userList);
        attachEmailAuthSummary(userList);
        return userMap;
    }

    private void checkEnabled(WfUser user) {
        if (UserStatus.DISABLED.equals(user.getStatus())) {
            throw new BaseException(ErrorCode.USER_DISABLED);
        }
    }

    private WfUser attachUserExtOrThrow(WfUser user) {
        attachProfileOrThrow(user);
        attachEmailAuthSummary(user);
        return user;
    }

    private void attachProfileOrThrow(WfUser user) {
        LambdaQueryWrapper<WfUserProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserProfile::getUserId, user.getId());
        WfUserProfile profile = wfUserProfileMapper.selectOne(queryWrapper);
        if (profile == null) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        user.setNickname(profile.getNickname());
        user.setAvatar(profile.getAvatar());
    }

    private void attachProfilesOrThrow(List<WfUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> ids = users.stream()
                .map(WfUser::getId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<WfUserProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WfUserProfile::getUserId, ids);
        List<WfUserProfile> profiles = wfUserProfileMapper.selectList(queryWrapper);
        if (profiles.size() != ids.size()) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        Map<Long, WfUserProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(WfUserProfile::getUserId, profile -> profile));

        for (WfUser user : users) {
            WfUserProfile profile = profileMap.get(user.getId());
            if (profile == null) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
            user.setNickname(profile.getNickname());
            user.setAvatar(profile.getAvatar());
        }
    }

    private void attachEmailAuthSummary(WfUser user) {
        WfUserAuth emailAuth = findEnabledEmailAuthByUserId(user.getId());
        applyEmailAuthSummary(user, emailAuth);
    }

    private void attachEmailAuthSummary(List<WfUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> ids = users.stream()
                .map(WfUser::getId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WfUserAuth::getUserId, ids)
                .eq(WfUserAuth::getAuthType, UserAuthType.EMAIL_PASSWORD)
                .eq(WfUserAuth::getEnabled, true);
        List<WfUserAuth> emailAuthList = wfUserAuthMapper.selectList(queryWrapper);

        Map<Long, WfUserAuth> emailAuthMap = new HashMap<>();
        for (WfUserAuth auth : emailAuthList) {
            WfUserAuth old = emailAuthMap.put(auth.getUserId(), auth);
            if (old != null) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        }

        for (WfUser user : users) {
            WfUserAuth auth = emailAuthMap.get(user.getId());
            applyEmailAuthSummary(user, auth);
        }
    }

    private void applyEmailAuthSummary(WfUser user, WfUserAuth emailAuth) {
        if (emailAuth == null) {
            user.setEmail(null);
            user.setEmailVerified(false);
            return;
        }
        user.setEmail(emailAuth.getAuthIdentifier());
        user.setEmailVerified(Boolean.TRUE.equals(emailAuth.getVerified()));
    }

    private WfUserAuth findEnabledEmailAuthByUserId(Long userId) {
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAuth::getUserId, userId)
                .eq(WfUserAuth::getAuthType, UserAuthType.EMAIL_PASSWORD)
                .eq(WfUserAuth::getEnabled, true)
                .last("LIMIT 1");
        return wfUserAuthMapper.selectOne(queryWrapper);
    }

    private String buildDeletedNickname(Long userId) {
        return DELETED_NICKNAME_PREFIX + "#" + userId;
    }

}
