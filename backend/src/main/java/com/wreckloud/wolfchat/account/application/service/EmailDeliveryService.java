package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.config.EmailDeliveryConfig;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @Description 邮件投递服务
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryService {
    private final EmailDeliveryConfig emailDeliveryConfig;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public void sendVerifyLink(String email, String verifyLink, int expireMinutes) {
        String subject = buildSubject("邮箱认证");
        String content = "你好，\n\n"
                + "请在 " + expireMinutes + " 分钟内点击以下链接完成邮箱认证：\n"
                + verifyLink + "\n\n"
                + "如果这不是你的操作，请忽略本邮件。";
        sendMail("邮箱认证链接", email, subject, content, verifyLink);
    }

    public void sendResetPasswordLink(String email, String resetLink, int expireMinutes) {
        String subject = buildSubject("重置密码");
        String content = "你好，\n\n"
                + "请在 " + expireMinutes + " 分钟内点击以下链接重置密码：\n"
                + resetLink + "\n\n"
                + "如果这不是你的操作，请忽略本邮件。";
        sendMail("重置密码链接", email, subject, content, resetLink);
    }

    public void sendPasswordChangeCode(String email, String code, int expireMinutes) {
        String subject = buildSubject("修改密码验证码");
        String content = "你好，\n\n"
                + "你正在进行密码修改，本次验证码为："
                + code
                + "（"
                + expireMinutes
                + " 分钟内有效）。\n\n"
                + "如果这不是你的操作，请忽略本邮件。";
        sendMail("改密验证码", email, subject, content, code);
    }

    private void sendMail(String scene, String email, String subject, String content, String link) {
        if (!Boolean.TRUE.equals(emailDeliveryConfig.getEnabled())) {
            log.info("{}(开发模式): email={}, link={}", scene, email, link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailDeliveryConfig.getFrom().trim());
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);
        try {
            JavaMailSender javaMailSender = mailSenderProvider.getIfAvailable();
            if (javaMailSender == null) {
                log.error("邮件发送未配置: 缺少 JavaMailSender，请检查 spring.mail.* 配置");
                throw new BaseException(ErrorCode.EMAIL_SEND_FAILED);
            }
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("{}发送失败: email={}", scene, email, e);
            throw new BaseException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String buildSubject(String suffix) {
        return "[" + emailDeliveryConfig.getSubjectPrefix().trim() + "] " + suffix;
    }
}
