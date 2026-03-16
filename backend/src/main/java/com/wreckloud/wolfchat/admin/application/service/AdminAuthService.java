package com.wreckloud.wolfchat.admin.application.service;

import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.admin.api.vo.AdminLoginVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理端认证服务。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final AuthService authService;
    private final UserService userService;
    private final AdminPermissionService adminPermissionService;

    public AdminLoginVO login(String account, String password, HttpServletRequest request) {
        LoginVO loginVO = authService.login(account, password, request);
        adminPermissionService.assertAdmin(loginVO.getUserInfo().getUserId());

        AdminLoginVO result = new AdminLoginVO();
        result.setToken(loginVO.getToken());
        return result;
    }

    public AdminProfileVO getCurrentAdminProfile(Long userId) {
        WfUser user = userService.getByIdOrThrow(userId);

        AdminProfileVO profileVO = new AdminProfileVO();
        profileVO.setUserId(user.getId());
        profileVO.setWolfNo(user.getWolfNo());
        profileVO.setUsername(user.getWolfNo());
        profileVO.setNickname(user.getNickname());
        profileVO.setStatus(user.getStatus());
        profileVO.setLastLoginAt(user.getLastLoginAt());
        return profileVO;
    }
}
