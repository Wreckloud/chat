package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.api.dto.MobileLoginDTO;
import com.wreckloud.wolfchat.account.api.dto.MobileRegisterDTO;
import com.wreckloud.wolfchat.account.api.dto.WechatLoginDTO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;

/**
 * @Description 认证服务接口（专门处理登录/注册逻辑）
 * @Author Wreckloud
 * @Date 2025-12-07
 */
public interface AuthService {

    /**
     * 手机号注册
     *
     * @param request 注册请求
     */
    void registerByMobile(MobileRegisterDTO request);

    /**
     * 微信一键登录/注册
     * 如果用户已存在则登录，不存在则自动注册
     *
     * @param request 微信登录请求
     * @return 用户信息
     */
    UserVO loginByWechat(WechatLoginDTO request);

    /**
     * 手机号验证码登录/注册
     * 如果用户已存在则登录，不存在则自动注册
     *
     * @param request 手机号登录请求
     * @return 用户信息
     */
    UserVO loginByMobile(MobileLoginDTO request);
}

