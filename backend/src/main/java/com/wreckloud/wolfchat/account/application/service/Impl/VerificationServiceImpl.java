package com.wreckloud.wolfchat.account.application.service.Impl;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.wreckloud.wolfchat.account.api.vo.CaptchaVO;
import com.wreckloud.wolfchat.account.application.service.VerificationService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description 统一验证服务实现类（图形验证码 + 短信验证码）
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 是否启用真实短信发送（默认false，使用模拟发送）
     */
    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    /**
     * 图形验证码Redis key前缀
     */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 图形验证码过期时间（分钟）
     */
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 短信验证码Redis key前缀
     */
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";

    /**
     * 短信验证码过期时间（分钟）
     */
    private static final int SMS_CODE_EXPIRE_MINUTES = 5;

    /**
     * 短信发送频率限制Redis key前缀（防止频繁发送）
     */
    private static final String SMS_RATE_LIMIT_KEY_PREFIX = "sms:rate:";

    /**
     * 短信发送频率限制时间（秒）
     */
    private static final int SMS_RATE_LIMIT_SECONDS = 60;

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

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

    @Override
    public String sendSmsCode(String mobile) {
        // 1. 频率限制检查（防止频繁发送）
        String rateLimitKey = SMS_RATE_LIMIT_KEY_PREFIX + mobile;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BaseException(ErrorCode.SMS_SEND_TOO_FREQUENT);
        }

        // 2. 生成6位数字验证码
        String smsCode = generateSmsCode();

        // 3. 生成唯一key
        String smsCodeKey = mobile + ":" + System.currentTimeMillis();

        // 4. 存储到Redis（手机号:时间戳作为key的一部分）
        String redisKey = SMS_CODE_KEY_PREFIX + mobile;
        redisTemplate.opsForValue().set(redisKey, smsCode, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 5. 设置频率限制（60秒内不能重复发送）
        redisTemplate.opsForValue().set(rateLimitKey, "1", SMS_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);

        // 6. 发送短信（如果未启用真实短信，则模拟发送）
        if (smsEnabled) {
            // TODO: 接入真实的短信服务（如阿里云、腾讯云等）
            sendRealSms(mobile, smsCode);
        } else {
            // 模拟发送（开发环境使用，打印到日志）
            log.info("【模拟短信】手机号：{}，验证码：{}，有效期：{}分钟", mobile, smsCode, SMS_CODE_EXPIRE_MINUTES);
        }

        return smsCodeKey;
    }

    @Override
    public boolean verifySmsCode(String mobile, String smsCodeKey, String smsCode) {
        if (mobile == null || smsCodeKey == null || smsCode == null) {
            return false;
        }

        String redisKey = SMS_CODE_KEY_PREFIX + mobile;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            return false; // 验证码已过期或不存在
        }

        // 验证码使用后删除
        redisTemplate.delete(redisKey);

        // 比较验证码（不区分大小写）
        return storedCode.equals(smsCode.trim());
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

    /**
     * 生成6位数字验证码
     *
     * @return 验证码
     */
    private String generateSmsCode() {
        int code = 100000 + random.nextInt(900000); // 生成100000-999999之间的随机数
        return String.valueOf(code);
    }

    /**
     * 发送真实短信（待实现）
     *
     * @param mobile  手机号
     * @param smsCode 验证码
     */
    private void sendRealSms(String mobile, String smsCode) {
        // TODO: 接入真实的短信服务
        // 示例：阿里云短信、腾讯云短信、云片短信等
        log.info("【真实短信】发送到：{}，验证码：{}", mobile, smsCode);
    }
}

