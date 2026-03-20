package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUserAuth;
import com.wreckloud.wolfchat.account.domain.enums.UserAuthType;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserAuthMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * @Description 用户认证服务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Service
@RequiredArgsConstructor
public class UserAuthService {
    private static final String DELETED_IDENTIFIER_PREFIX = "DELETED";

    private final WfUserAuthMapper wfUserAuthMapper;

    public void createWolfNoPasswordAuth(Long userId, String wolfNo, String credentialHash) {
        createPasswordAuth(userId, UserAuthType.WOLF_NO_PASSWORD, wolfNo, credentialHash, true);
    }

    public void createEmailPasswordAuth(Long userId, String email, String credentialHash, boolean verified) {
        createPasswordAuth(userId, UserAuthType.EMAIL_PASSWORD, normalizeEmailIdentifier(email), credentialHash, verified);
    }

    public WfUserAuth findByTypeAndIdentifier(UserAuthType authType, String authIdentifier) {
        if (authType == null || !StringUtils.hasText(authIdentifier)) {
            return null;
        }
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAuth::getAuthType, authType)
                .eq(WfUserAuth::getAuthIdentifier, authIdentifier.trim())
                .eq(WfUserAuth::getEnabled, true)
                .last("LIMIT 1");
        return wfUserAuthMapper.selectOne(queryWrapper);
    }

    public WfUserAuth findAnyByTypeAndIdentifier(UserAuthType authType, String authIdentifier) {
        if (authType == null || !StringUtils.hasText(authIdentifier)) {
            return null;
        }
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAuth::getAuthType, authType)
                .eq(WfUserAuth::getAuthIdentifier, authIdentifier.trim())
                .last("LIMIT 1");
        return wfUserAuthMapper.selectOne(queryWrapper);
    }

    public WfUserAuth getWolfNoPasswordAuthByUserIdOrThrow(Long userId) {
        WfUserAuth auth = findByUserIdAndType(userId, UserAuthType.WOLF_NO_PASSWORD);
        if (auth == null) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        return auth;
    }

    public WfUserAuth findAnyEmailAuthByUserId(Long userId) {
        return findAnyByUserIdAndType(userId, UserAuthType.EMAIL_PASSWORD);
    }

    public void touchLoginAt(Long authId) {
        if (authId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        LambdaUpdateWrapper<WfUserAuth> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserAuth::getId, authId)
                .set(WfUserAuth::getLastLoginAt, LocalDateTime.now());
        int updateRows = wfUserAuthMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    public void updateAllPasswordCredentialByUserId(Long userId, String credentialHash) {
        if (userId == null || !StringUtils.hasText(credentialHash)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        LambdaUpdateWrapper<WfUserAuth> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserAuth::getUserId, userId)
                .eq(WfUserAuth::getEnabled, true)
                .in(WfUserAuth::getAuthType, UserAuthType.WOLF_NO_PASSWORD, UserAuthType.EMAIL_PASSWORD)
                .set(WfUserAuth::getCredentialHash, credentialHash);
        int updateRows = wfUserAuthMapper.update(null, updateWrapper);
        if (updateRows < 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    public void bindEmailPasswordAuth(Long userId, String email, String credentialHash) {
        if (userId == null || !StringUtils.hasText(email) || !StringUtils.hasText(credentialHash)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        String normalizedEmail = normalizeEmailIdentifier(email);
        WfUserAuth existing = findAnyByTypeAndIdentifier(UserAuthType.EMAIL_PASSWORD, normalizedEmail);
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_USED);
        }

        WfUserAuth current = findAnyByUserIdAndType(userId, UserAuthType.EMAIL_PASSWORD);
        if (current == null) {
            createPasswordAuth(userId, UserAuthType.EMAIL_PASSWORD, normalizedEmail, credentialHash, true);
            return;
        }

        LambdaUpdateWrapper<WfUserAuth> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserAuth::getId, current.getId())
                .set(WfUserAuth::getAuthIdentifier, normalizedEmail)
                .set(WfUserAuth::getCredentialHash, credentialHash)
                .set(WfUserAuth::getVerified, true)
                .set(WfUserAuth::getEnabled, true);
        try {
            int updateRows = wfUserAuthMapper.update(null, updateWrapper);
            if (updateRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (DuplicateKeyException e) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_USED);
        }
    }

    public void disableAndArchiveAllAuthByUserId(Long userId) {
        if (userId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAuth::getUserId, userId);
        List<WfUserAuth> authList = wfUserAuthMapper.selectList(queryWrapper);
        if (authList.isEmpty()) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        for (WfUserAuth auth : authList) {
            LambdaUpdateWrapper<WfUserAuth> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WfUserAuth::getId, auth.getId())
                    .set(WfUserAuth::getAuthIdentifier, buildDeletedIdentifier(auth))
                    .set(WfUserAuth::getCredentialHash, "")
                    .set(WfUserAuth::getVerified, false)
                    .set(WfUserAuth::getEnabled, false)
                    .set(WfUserAuth::getLastLoginAt, null);
            int updateRows = wfUserAuthMapper.update(null, updateWrapper);
            if (updateRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        }
    }

    public long resolvePasswordVersion(Long userId) {
        if (userId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(WfUserAuth::getUpdateTime)
                .eq(WfUserAuth::getUserId, userId)
                .eq(WfUserAuth::getEnabled, true)
                .in(WfUserAuth::getAuthType, UserAuthType.WOLF_NO_PASSWORD, UserAuthType.EMAIL_PASSWORD)
                .orderByDesc(WfUserAuth::getUpdateTime)
                .orderByDesc(WfUserAuth::getId)
                .last("LIMIT 1");
        WfUserAuth latestAuth = wfUserAuthMapper.selectOne(queryWrapper);
        if (latestAuth == null || latestAuth.getUpdateTime() == null) {
            return 0L;
        }
        return latestAuth.getUpdateTime().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private WfUserAuth findByUserIdAndType(Long userId, UserAuthType authType) {
        if (userId == null || authType == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAuth::getUserId, userId)
                .eq(WfUserAuth::getAuthType, authType)
                .eq(WfUserAuth::getEnabled, true)
                .last("LIMIT 1");
        return wfUserAuthMapper.selectOne(queryWrapper);
    }

    private WfUserAuth findAnyByUserIdAndType(Long userId, UserAuthType authType) {
        if (userId == null || authType == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        LambdaQueryWrapper<WfUserAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserAuth::getUserId, userId)
                .eq(WfUserAuth::getAuthType, authType)
                .last("LIMIT 1");
        return wfUserAuthMapper.selectOne(queryWrapper);
    }

    private void createPasswordAuth(Long userId, UserAuthType authType, String identifier, String credentialHash, boolean verified) {
        if (userId == null || authType == null || !StringUtils.hasText(identifier) || !StringUtils.hasText(credentialHash)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        WfUserAuth auth = new WfUserAuth();
        auth.setUserId(userId);
        auth.setAuthType(authType);
        auth.setAuthIdentifier(identifier.trim());
        auth.setCredentialHash(credentialHash);
        auth.setVerified(verified);
        auth.setEnabled(true);
        try {
            int insertRows = wfUserAuthMapper.insert(auth);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (DuplicateKeyException e) {
            if (UserAuthType.EMAIL_PASSWORD.equals(authType)) {
                throw new BaseException(ErrorCode.EMAIL_ALREADY_USED);
            }
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private String normalizeEmailIdentifier(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildDeletedIdentifier(WfUserAuth auth) {
        return DELETED_IDENTIFIER_PREFIX + "_U" + auth.getUserId() + "_A" + auth.getId();
    }
}
