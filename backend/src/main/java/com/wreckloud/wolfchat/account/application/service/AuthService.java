package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.entity.WfUserAuth;
import com.wreckloud.wolfchat.account.domain.enums.LoginMethod;
import com.wreckloud.wolfchat.account.domain.enums.LoginResult;
import com.wreckloud.wolfchat.account.domain.enums.OnboardingStatus;
import com.wreckloud.wolfchat.account.domain.enums.UserAuthType;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @Description 认证服务
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int LOGIN_KEY_MIN_LENGTH = 6;
    private static final int LOGIN_KEY_MAX_LENGTH = 64;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final WfUserMapper wfUserMapper;
    private final WolfNoService wolfNoService;
    private final EmailVerifyLinkService emailVerifyLinkService;
    private final PasswordResetLinkService passwordResetLinkService;
    private final LoginRecordService loginRecordService;
    private final UserService userService;
    private final UserAchievementService userAchievementService;
    private final UserAuthService userAuthService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册
     * 用户输入行者名、密码、可选邮箱，系统分配狼藉号，创建行者，返回 token
     *
     * @param nickname 行者名（行者在群落中的称呼）
     * @param password 密码
     * @param email 邮箱（选填）
     * @return 登录响应（包含 token、userInfo）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(String nickname, String password, String email) {
        String normalizedEmail = normalizeOptionalEmail(email);
        String normalizedLoginKey = normalizeLoginKey(password);
        if (normalizedEmail != null
                && userAuthService.findAnyByTypeAndIdentifier(UserAuthType.EMAIL_PASSWORD, normalizedEmail) != null) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_USED);
        }

        String wolfNo = wolfNoService.allocateWolfNo(null);

        WfUser user = new WfUser();
        user.setWolfNo(wolfNo);
        user.setStatus(UserStatus.NORMAL);
        user.setOnboardingStatus(OnboardingStatus.PENDING);
        user.setActiveDayCount(0);
        int insertRows = wfUserMapper.insert(user);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        userService.createProfile(user.getId(), nickname);

        String credentialHash = encodeLoginKey(normalizedLoginKey);
        userAuthService.createWolfNoPasswordAuth(user.getId(), wolfNo, credentialHash);
        if (normalizedEmail != null) {
            userAuthService.createEmailPasswordAuth(user.getId(), normalizedEmail, credentialHash, false);
            emailVerifyLinkService.sendBindVerifyLink(user.getId(), normalizedEmail);
        }

        wolfNoService.updateUserIdByWolfNo(wolfNo, user.getId());
        userAchievementService.grantRegisterAchievement(user.getId());

        log.info("行者注册成功: userId={}", user.getId());
        return buildLoginVO(userService.getByIdOrThrow(user.getId()));
    }

    /**
     * 登录
     * 验证账号+密码，返回 token
     *
     * @param account 登录账号（支持狼藉号或邮箱）
     * @param loginKey 登录密码（明文）
     * @param request HTTP 请求
     * @return 登录响应（包含 token、userInfo，不包含 loginKey）
     */
    public LoginVO login(String account, String loginKey, HttpServletRequest request) {
        WfUser user = null;
        WfUserAuth auth = null;
        String normalizedAccount = normalizeAccount(account);
        LoginMethod loginMethod = resolveLoginMethod(normalizedAccount);
        try {
            String normalizedLoginKey = normalizeLoginKey(loginKey);
            if (!StringUtils.hasText(normalizedAccount)) {
                throw new BaseException(ErrorCode.PARAM_ERROR);
            }

            if (LoginMethod.EMAIL.equals(loginMethod)) {
                String normalizedEmail = normalizeEmail(normalizedAccount);
                auth = getEnabledEmailAuthOrThrow(normalizedEmail);
                requireEmailVerified(auth);
                user = userService.getEnabledByIdOrThrow(auth.getUserId());
            } else {
                user = userService.getEnabledByWolfNoOrThrow(normalizedAccount);
                auth = userAuthService.getWolfNoPasswordAuthByUserIdOrThrow(user.getId());
            }

            verifyLoginKey(normalizedLoginKey, auth.getCredentialHash(), ErrorCode.LOGIN_KEY_ERROR);
            refreshLoginStats(user);
            userAuthService.touchLoginAt(auth.getId());
            recordLoginSafely(user.getId(), loginMethod, LoginResult.SUCCESS, null, normalizedAccount, request);

            log.info("行者登录成功: method={}, userId={}", loginMethod.getValue(), user.getId());
            return buildLoginVO(user);
        } catch (BaseException e) {
            recordLoginSafely(user == null ? null : user.getId(), loginMethod, LoginResult.FAIL, e.getCode(), normalizedAccount, request);
            throw e;
        } catch (Exception e) {
            recordLoginSafely(
                    user == null ? null : user.getId(),
                    loginMethod,
                    LoginResult.FAIL,
                    ErrorCode.SYSTEM_ERROR.getCode(),
                    normalizedAccount,
                    request
            );
            throw e;
        }
    }

    /**
     * 发送重置密码链接
     */
    public void sendResetPasswordLink(String email) {
        String normalizedEmail = normalizeEmail(email);
        WfUserAuth emailAuth = getEnabledEmailAuthOrThrow(normalizedEmail);
        requireEmailVerified(emailAuth);
        userService.getEnabledByIdOrThrow(emailAuth.getUserId());
        passwordResetLinkService.sendResetLink(emailAuth.getUserId(), normalizedEmail);
    }

    /**
     * 判断重置密码链接 token 是否可用
     */
    public boolean isResetPasswordTokenAvailable(String token) {
        return passwordResetLinkService.isTokenAvailable(token);
    }

    /**
     * 通过邮箱重置链接重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByToken(String token, String newLoginKey, String confirmLoginKey) {
        String normalizedNewLoginKey = normalizeLoginKey(newLoginKey);
        String normalizedConfirmLoginKey = normalizeLoginKey(confirmLoginKey);

        if (!normalizedNewLoginKey.equals(normalizedConfirmLoginKey)) {
            throw new BaseException(ErrorCode.NEW_LOGIN_KEY_NOT_MATCH);
        }

        PasswordResetLinkService.ResetTarget resetTarget = passwordResetLinkService.verifyAndConsumeToken(token);
        WfUserAuth emailAuth = getEnabledEmailAuthOrThrow(resetTarget.getEmail());
        requireEmailVerified(emailAuth);
        if (!emailAuth.getUserId().equals(resetTarget.getUserId())) {
            throw new BaseException(ErrorCode.PASSWORD_RESET_LINK_INVALID);
        }
        userService.getEnabledByIdOrThrow(emailAuth.getUserId());

        userAuthService.updateAllPasswordCredentialByUserId(emailAuth.getUserId(), encodeLoginKey(normalizedNewLoginKey));

        log.info("链接重置密码成功: userId={}", emailAuth.getUserId());
    }

    /**
     * 修改登录密码
     *
     * @param userId 当前登录行者ID
     * @param oldLoginKey 原密码（明文）
     * @param newLoginKey 新密码（明文）
     * @param confirmLoginKey 确认密码（明文）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldLoginKey, String newLoginKey, String confirmLoginKey) {
        String normalizedOldLoginKey = normalizeLoginKey(oldLoginKey);
        String normalizedNewLoginKey = normalizeLoginKey(newLoginKey);
        String normalizedConfirmLoginKey = normalizeLoginKey(confirmLoginKey);

        userService.getEnabledByIdOrThrow(userId);
        WfUserAuth wolfAuth = userAuthService.getWolfNoPasswordAuthByUserIdOrThrow(userId);
        verifyLoginKey(normalizedOldLoginKey, wolfAuth.getCredentialHash(), ErrorCode.OLD_LOGIN_KEY_ERROR);

        if (!normalizedNewLoginKey.equals(normalizedConfirmLoginKey)) {
            throw new BaseException(ErrorCode.NEW_LOGIN_KEY_NOT_MATCH);
        }

        userAuthService.updateAllPasswordCredentialByUserId(userId, encodeLoginKey(normalizedNewLoginKey));

        log.info("行者修改密码成功: userId={}", userId);
    }

    /**
     * 发送绑定邮箱认证链接
     */
    public void sendBindEmailVerifyLink(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        userService.getEnabledByIdOrThrow(userId);
        checkEmailCanBind(userId, normalizedEmail);
        emailVerifyLinkService.sendBindVerifyLink(userId, normalizedEmail);
    }

    /**
     * 校验认证链接并完成邮箱认证
     */
    @Transactional(rollbackFor = Exception.class)
    public void verifyBindEmailByToken(String token) {
        EmailVerifyLinkService.VerifyTarget verifyTarget = emailVerifyLinkService.verifyAndConsumeToken(token);

        Long userId = verifyTarget.getUserId();
        String normalizedEmail = normalizeEmail(verifyTarget.getEmail());
        userService.getEnabledByIdOrThrow(userId);
        checkEmailCanBind(userId, normalizedEmail);

        WfUserAuth wolfAuth = userAuthService.getWolfNoPasswordAuthByUserIdOrThrow(userId);
        userAuthService.bindEmailPasswordAuth(userId, normalizedEmail, wolfAuth.getCredentialHash());

        log.info("邮箱认证成功: userId={}", userId);
    }

    /**
     * 构建登录响应 VO
     * 生成 JWT token 并封装用户信息
     *
     * @param user 行者实体
     * @return 登录响应
     */
    private LoginVO buildLoginVO(WfUser user) {
        String token = jwtUtil.generateToken(user.getId());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);

        UserVO userVO = UserConverter.toUserVO(user);
        loginVO.setUserInfo(userVO);

        return loginVO;
    }

    private String encodeLoginKey(String rawLoginKey) {
        return passwordEncoder.encode(rawLoginKey);
    }

    private void verifyLoginKey(String rawLoginKey, String encodedLoginKey, ErrorCode errorCode) {
        if (!passwordEncoder.matches(rawLoginKey, encodedLoginKey)) {
            throw new BaseException(errorCode);
        }
    }

    private void requireEmailVerified(WfUserAuth emailAuth) {
        if (!Boolean.TRUE.equals(emailAuth.getVerified())) {
            throw new BaseException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void checkEmailCanBind(Long currentUserId, String email) {
        WfUserAuth currentEmailAuth = userAuthService.findAnyEmailAuthByUserId(currentUserId);
        if (currentEmailAuth != null && !email.equals(currentEmailAuth.getAuthIdentifier())) {
            throw new BaseException(ErrorCode.EMAIL_REBIND_NOT_ALLOWED);
        }

        WfUserAuth existing = userAuthService.findAnyByTypeAndIdentifier(UserAuthType.EMAIL_PASSWORD, email);
        if (existing != null && !existing.getUserId().equals(currentUserId)) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_USED);
        }
    }

    private boolean isEmailAccount(String account) {
        return account != null && account.contains("@");
    }

    private LoginMethod resolveLoginMethod(String account) {
        if (!StringUtils.hasText(account)) {
            return LoginMethod.UNKNOWN;
        }
        return isEmailAccount(account) ? LoginMethod.EMAIL : LoginMethod.WOLF_NO;
    }

    private String normalizeAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        return account.trim();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        return normalizedEmail;
    }

    private String normalizeOptionalEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return normalizeEmail(email);
    }

    private void refreshLoginStats(WfUser user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        if (user.getFirstLoginAt() == null) {
            user.setFirstLoginAt(now);
        }

        LocalDateTime lastLoginAt = user.getLastLoginAt();
        LocalDate lastActiveDate = lastLoginAt == null ? null : lastLoginAt.toLocalDate();
        if (lastActiveDate == null || !lastActiveDate.equals(today)) {
            Integer activeDayCount = user.getActiveDayCount();
            user.setActiveDayCount(activeDayCount == null ? 1 : activeDayCount + 1);
        }

        user.setLastLoginAt(now);
        int updateRows = wfUserMapper.updateById(user);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private WfUserAuth getEnabledEmailAuthOrThrow(String normalizedEmail) {
        WfUserAuth emailAuth = userAuthService.findByTypeAndIdentifier(UserAuthType.EMAIL_PASSWORD, normalizedEmail);
        if (emailAuth == null) {
            throw new BaseException(ErrorCode.EMAIL_NOT_BOUND);
        }
        return emailAuth;
    }

    private void recordLoginSafely(
            Long userId,
            LoginMethod loginMethod,
            LoginResult loginResult,
            Integer failCode,
            String account,
            HttpServletRequest request
    ) {
        try {
            loginRecordService.record(userId, loginMethod, loginResult, failCode, account, request);
        } catch (Exception e) {
            log.error(
                    "登录记录写入失败: userId={}, method={}, result={}",
                    userId,
                    loginMethod == null ? null : loginMethod.getValue(),
                    loginResult == null ? null : loginResult.getValue(),
                    e
            );
        }
    }

    private String normalizeLoginKey(String loginKey) {
        if (!StringUtils.hasText(loginKey)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        String normalizedLoginKey = loginKey.trim();
        if (normalizedLoginKey.length() < LOGIN_KEY_MIN_LENGTH || normalizedLoginKey.length() > LOGIN_KEY_MAX_LENGTH) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        return normalizedLoginKey;
    }
}
