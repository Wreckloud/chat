package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.EmailCodeScene;
import com.wreckloud.wolfchat.account.domain.enums.LoginMethod;
import com.wreckloud.wolfchat.account.domain.enums.LoginResult;
import com.wreckloud.wolfchat.account.domain.enums.OnboardingStatus;
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
    private final EmailCodeService emailCodeService;
    private final LoginRecordService loginRecordService;
    private final UserService userService;
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
        if (normalizedEmail != null && userService.findByEmail(normalizedEmail) != null) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_USED);
        }

        // 1. 分配狼藉号（先分配，userId暂时为null）
        String wolfNo = wolfNoService.allocateWolfNo(null);

        // 2. 创建行者
        WfUser user = new WfUser();
        user.setWolfNo(wolfNo);
        user.setLoginKey(encodeLoginKey(normalizedLoginKey));
        user.setEmail(normalizedEmail);
        user.setEmailVerified(false);
        user.setNickname(nickname);
        user.setStatus(UserStatus.NORMAL);
        user.setOnboardingStatus(OnboardingStatus.PENDING);
        user.setLoginCount(0);
        int insertRows = wfUserMapper.insert(user);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        // 3. 更新号码池中的 userId（分配时userId为null，现在更新为实际userId）
        wolfNoService.updateUserIdByWolfNo(wolfNo, user.getId());

        // 4. 若填写邮箱则自动发送邮箱认证验证码（开发阶段仅打印日志）
        if (normalizedEmail != null) {
            emailCodeService.sendCode(normalizedEmail, EmailCodeScene.BIND_EMAIL);
        }

        // 5. 构建并返回登录响应
        log.info("行者注册成功: wolfNo={}, nickname={}, email={}, userId={}", wolfNo, nickname, normalizedEmail, user.getId());
        return buildLoginVO(user);
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
        String normalizedAccount = normalizeAccount(account);
        LoginMethod loginMethod = resolveLoginMethod(normalizedAccount);
        try {
            String normalizedLoginKey = normalizeLoginKey(loginKey);
            if (!StringUtils.hasText(normalizedAccount)) {
                throw new BaseException(ErrorCode.PARAM_ERROR);
            }
            if (LoginMethod.EMAIL.equals(loginMethod)) {
                String normalizedEmail = normalizeEmail(normalizedAccount);
                user = userService.getEnabledByEmailOrThrow(normalizedEmail);
                requireEmailVerified(user);
            } else {
                user = userService.getEnabledByWolfNoOrThrow(normalizedAccount);
            }

            verifyLoginKey(normalizedLoginKey, user.getLoginKey(), ErrorCode.LOGIN_KEY_ERROR);
            refreshLoginStats(user);
            loginRecordService.record(user.getId(), loginMethod, LoginResult.SUCCESS, null, normalizedAccount, request);

            log.info("行者登录成功: method={}, account={}, userId={}", loginMethod.getValue(), normalizedAccount, user.getId());
            return buildLoginVO(user);
        } catch (BaseException e) {
            loginRecordService.record(user == null ? null : user.getId(), loginMethod, LoginResult.FAIL, e.getCode(), normalizedAccount, request);
            throw e;
        } catch (Exception e) {
            loginRecordService.record(user == null ? null : user.getId(), loginMethod, LoginResult.FAIL, ErrorCode.SYSTEM_ERROR.getCode(), normalizedAccount, request);
            throw e;
        }
    }

    /**
     * 发送重置密码验证码
     */
    public void sendResetPasswordCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        WfUser user = userService.getEnabledByEmailOrThrow(normalizedEmail);
        requireEmailVerified(user);
        emailCodeService.sendCode(normalizedEmail, EmailCodeScene.RESET_PASSWORD);
    }

    /**
     * 通过邮箱验证码重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByEmail(String email, String verifyCode, String newLoginKey, String confirmLoginKey) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedNewLoginKey = normalizeLoginKey(newLoginKey);
        String normalizedConfirmLoginKey = normalizeLoginKey(confirmLoginKey);
        WfUser user = userService.getEnabledByEmailOrThrow(normalizedEmail);
        requireEmailVerified(user);
        if (!normalizedNewLoginKey.equals(normalizedConfirmLoginKey)) {
            throw new BaseException(ErrorCode.NEW_LOGIN_KEY_NOT_MATCH);
        }

        emailCodeService.verifyAndConsume(normalizedEmail, EmailCodeScene.RESET_PASSWORD, verifyCode);
        user.setLoginKey(encodeLoginKey(normalizedNewLoginKey));
        int updateRows = wfUserMapper.updateById(user);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        log.info("邮箱重置密码成功: email={}, userId={}", normalizedEmail, user.getId());
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
        WfUser user = userService.getEnabledByIdOrThrow(userId);
        verifyLoginKey(normalizedOldLoginKey, user.getLoginKey(), ErrorCode.OLD_LOGIN_KEY_ERROR);

        if (!normalizedNewLoginKey.equals(normalizedConfirmLoginKey)) {
            throw new BaseException(ErrorCode.NEW_LOGIN_KEY_NOT_MATCH);
        }

        user.setLoginKey(encodeLoginKey(normalizedNewLoginKey));
        int updateRows = wfUserMapper.updateById(user);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        log.info("行者修改密码成功: userId={}", userId);
    }

    /**
     * 发送绑定邮箱验证码
     */
    public void sendBindEmailCode(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        WfUser user = userService.getEnabledByIdOrThrow(userId);
        checkEmailCanBind(user.getId(), normalizedEmail);
        emailCodeService.sendCode(normalizedEmail, EmailCodeScene.BIND_EMAIL);
    }

    /**
     * 绑定邮箱
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindEmail(Long userId, String email, String verifyCode) {
        String normalizedEmail = normalizeEmail(email);
        WfUser user = userService.getEnabledByIdOrThrow(userId);
        checkEmailCanBind(user.getId(), normalizedEmail);

        emailCodeService.verifyAndConsume(normalizedEmail, EmailCodeScene.BIND_EMAIL, verifyCode);
        user.setEmail(normalizedEmail);
        user.setEmailVerified(true);

        int updateRows = wfUserMapper.updateById(user);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        log.info("邮箱绑定成功: userId={}, email={}", userId, normalizedEmail);
    }

    /**
     * 构建登录响应 VO
     * 生成 JWT token 并封装用户信息
     *
     * @param user 行者实体
     * @return 登录响应
     */
    private LoginVO buildLoginVO(WfUser user) {
        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getWolfNo());

        // 构建响应
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

    private void requireEmailVerified(WfUser user) {
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BaseException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void checkEmailCanBind(Long currentUserId, String email) {
        WfUser existUser = userService.findByEmail(email);
        if (existUser != null && !existUser.getId().equals(currentUserId)) {
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
        if (user.getFirstLoginAt() == null) {
            user.setFirstLoginAt(now);
        }
        user.setLastLoginAt(now);
        Integer loginCount = user.getLoginCount();
        if (loginCount == null) {
            user.setLoginCount(1);
        } else {
            user.setLoginCount(loginCount + 1);
        }
        int updateRows = wfUserMapper.updateById(user);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
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

