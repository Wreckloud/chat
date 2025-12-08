package com.wreckloud.wolfchat.account.application.service;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.wreckloud.wolfchat.account.api.vo.CaptchaVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description 验证码服务
 * @Author Wreckloud
 * @Date 2025-12-06
 */

public interface CaptchaService {

    /**
     * 生成验证码
     *
     * @return 验证码VO（包含key和图片Base64）
     */
    CaptchaVO generateCaptcha();

    /**
     * 验证验证码
     *
     * @param captchaKey  验证码key
     * @param captchaCode 用户输入的验证码
     * @return true-验证通过，false-验证失败
     */
    boolean verifyCaptcha(String captchaKey, String captchaCode);
}

