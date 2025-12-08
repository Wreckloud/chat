package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.api.vo.CaptchaVO;

/**
 * @Description 统一验证服务接口（图形验证码 + 短信验证码）
 * @Author Wreckloud
 * @Date 2025-12-07
 */
public interface VerificationService {

    /**
     * 生成图形验证码
     *
     * @return 验证码VO（包含key和图片Base64）
     */
    CaptchaVO generateCaptcha();

    /**
     * 验证图形验证码
     *
     * @param captchaKey  验证码key
     * @param captchaCode 用户输入的验证码
     * @return true-验证通过，false-验证失败
     */
    boolean verifyCaptcha(String captchaKey, String captchaCode);

    /**
     * 发送短信验证码
     *
     * @param mobile 手机号
     * @return 验证码key（用于后续验证）
     */
    String sendSmsCode(String mobile);

    /**
     * 验证短信验证码
     *
     * @param mobile      手机号
     * @param smsCodeKey  验证码key
     * @param smsCode     用户输入的验证码
     * @return true-验证通过，false-验证失败
     */
    boolean verifySmsCode(String mobile, String smsCodeKey, String smsCode);
}

