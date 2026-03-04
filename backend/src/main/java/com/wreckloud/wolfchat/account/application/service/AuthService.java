package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.EmailCodeScene;
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

import java.util.Locale;

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

    private final WfUserMapper wfUserMapper;
    private final WolfNoService wolfNoService;
    private final EmailCodeService emailCodeService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册
     * 用户输入行者名、密码、邮箱，系统分配狼藉号，创建行者，返回 token
     *
     * @param nickname 行者名（行者在群落中的称呼）
     * @param password 密码
     * @param email 邮箱
     * @return 登录响应（包含 token、userInfo）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(String nickname, String password, String email) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedLoginKey = normalizeLoginKey(password);
        if (userService.findByEmail(normalizedEmail) != null) {
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
        int insertRows = wfUserMapper.insert(user);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        // 3. 更新号码池中的 userId（分配时userId为null，现在更新为实际userId）
        wolfNoService.updateUserIdByWolfNo(wolfNo, user.getId());

        // 4. 自动发送邮箱认证验证码（开发阶段仅打印日志）
        emailCodeService.sendCode(normalizedEmail, EmailCodeScene.BIND_EMAIL);

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
     * @return 登录响应（包含 token、userInfo，不包含 loginKey）
     */
    public LoginVO login(String account, String loginKey) {
        WfUser user;
        String accountType;
        String normalizedLoginKey = normalizeLoginKey(loginKey);
        String normalizedAccount = account == null ? "" : account.trim();
        if (isEmailAccount(normalizedAccount)) {
            String normalizedEmail = normalizeEmail(normalizedAccount);
            user = userService.getEnabledByEmailOrThrow(normalizedEmail);
            requireEmailVerified(user);
            accountType = "email";
        } else {
            user = userService.getEnabledByWolfNoOrThrow(normalizedAccount);
            accountType = "wolfNo";
        }

        // 2. 验证密码
        verifyLoginKey(normalizedLoginKey, user.getLoginKey(), ErrorCode.LOGIN_KEY_ERROR);

        // 3. 构建并返回登录响应
        log.info("行者登录成功: accountType={}, account={}, userId={}", accountType, normalizedAccount, user.getId());
        return buildLoginVO(user);
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

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        return email.trim().toLowerCase(Locale.ROOT);
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

