package com.wreckloud.wolfchat.account.application.service;

/**
 * @Description 短信验证码服务接口
 * @Author Wreckloud
 * @Date 2025-12-07
 */
public interface SmsService {

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

