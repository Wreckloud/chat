package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.api.converter.UserConverter;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
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


/**
 * @Description 认证服务
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final WfUserMapper wfUserMapper;
    private final WolfNoService wolfNoService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册
     * 用户输入行者名和密码，系统分配狼藉号，创建行者，返回 token
     *
     * @param nickname 行者名（行者在群落中的称呼）
     * @param password 密码
     * @return 登录响应（包含 token、userInfo）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(String nickname, String password) {
        // 1. 分配狼藉号（先分配，userId暂时为null）
        String wolfNo = wolfNoService.allocateWolfNo(null);

        // 2. 创建行者
        WfUser user = new WfUser();
        user.setWolfNo(wolfNo);
        user.setLoginKey(encodeLoginKey(password));
        user.setNickname(nickname);
        user.setStatus(UserStatus.NORMAL);
        int insertRows = wfUserMapper.insert(user);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        // 3. 更新号码池中的 userId（分配时userId为null，现在更新为实际userId）
        wolfNoService.updateUserIdByWolfNo(wolfNo, user.getId());

        // 4. 构建并返回登录响应
        log.info("行者注册成功: wolfNo={}, nickname={}, userId={}", wolfNo, nickname, user.getId());
        return buildLoginVO(user);
    }

    /**
     * 登录
     * 验证狼藉号+密码，返回 token
     *
     * @param wolfNo  狼藉号
     * @param loginKey 登录密码（明文）
     * @return 登录响应（包含 token、userInfo，不包含 loginKey）
     */
    public LoginVO login(String wolfNo, String loginKey) {
        // 1. 查询可用行者
        WfUser user = userService.getEnabledByWolfNoOrThrow(wolfNo);

        // 2. 验证密码（BCrypt）
        verifyLoginKey(loginKey, user.getLoginKey(), ErrorCode.LOGIN_KEY_ERROR);

        // 3. 构建并返回登录响应
        log.info("行者登录成功: wolfNo={}, userId={}", wolfNo, user.getId());
        return buildLoginVO(user);
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
        WfUser user = userService.getEnabledByIdOrThrow(userId);
        verifyLoginKey(oldLoginKey, user.getLoginKey(), ErrorCode.OLD_LOGIN_KEY_ERROR);

        if (!newLoginKey.equals(confirmLoginKey)) {
            throw new BaseException(ErrorCode.NEW_LOGIN_KEY_NOT_MATCH);
        }

        user.setLoginKey(encodeLoginKey(newLoginKey));
        int updateRows = wfUserMapper.updateById(user);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        log.info("行者修改密码成功: userId={}", userId);
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

}

