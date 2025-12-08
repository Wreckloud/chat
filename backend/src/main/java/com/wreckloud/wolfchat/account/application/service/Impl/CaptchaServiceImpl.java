package com.wreckloud.wolfchat.account.application.service.Impl;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.wreckloud.wolfchat.account.api.vo.CaptchaVO;
import com.wreckloud.wolfchat.account.application.service.CaptchaService;
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
 * @Description
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 验证码Redis key前缀
     */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 验证码过期时间（分钟）
     */
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 生成验证码
     *
     * @return 验证码VO（包含key和图片Base64）
     */
    @Override
    public CaptchaVO generateCaptcha() {
        // 生成验证码文本
        String captchaText = defaultKaptcha.createText();

        // 生成验证码图片
        BufferedImage captchaImage = defaultKaptcha.createImage(captchaText);

        // 生成唯一key
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存储到Redis
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        redisTemplate.opsForValue().set(redisKey, captchaText.toLowerCase(),
                CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 将图片转换为Base64
        String base64Image = imageToBase64(captchaImage);

        return new CaptchaVO(captchaKey, base64Image);
    }

    /**
     * 验证验证码
     *
     * @param captchaKey  验证码key
     * @param captchaCode 用户输入的验证码
     * @return true-验证通过，false-验证失败
     */
    @Override
    public boolean verifyCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }

        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            return false; // 验证码已过期或不存在
        }

        // 验证码使用后删除
        redisTemplate.delete(redisKey);

        // 不区分大小写比较
        return storedCode.equalsIgnoreCase(captchaCode.trim());
    }

    /**
     * 将BufferedImage转换为Base64字符串
     *
     * @param image 图片
     * @return Base64字符串（带data:image/png;base64,前缀）
     */
    private String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64 = Base64Utils.encodeToString(imageBytes);
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            throw new RuntimeException("验证码图片转换失败", e);
        }
    }
}
