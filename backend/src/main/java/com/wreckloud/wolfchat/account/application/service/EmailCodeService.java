package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfEmailCode;
import com.wreckloud.wolfchat.account.domain.enums.EmailCodeScene;
import com.wreckloud.wolfchat.account.infra.mapper.WfEmailCodeMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description 邮箱验证码服务
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {
    /**
     * 验证码有效时长（分钟）
     */
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 10;

    /**
     * 最小发送间隔（秒）
     */
    private static final int SEND_INTERVAL_SECONDS = 60;

    /**
     * 单日发送上限（同邮箱 + 同场景）
     */
    private static final int DAILY_SEND_LIMIT = 20;

    private final WfEmailCodeMapper wfEmailCodeMapper;

    /**
     * 发送验证码
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendCode(String email, EmailCodeScene scene) {
        if (!StringUtils.hasText(email) || scene == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        checkSendInterval(email, scene, now);
        checkDailyLimit(email, scene, now.toLocalDate());

        String verifyCode = generateVerifyCode();
        WfEmailCode record = new WfEmailCode();
        record.setEmail(email);
        record.setScene(scene.getCode());
        record.setVerifyCode(verifyCode);
        record.setUsed(false);
        record.setExpireTime(now.plusMinutes(VERIFY_CODE_EXPIRE_MINUTES));

        int insertRows = wfEmailCodeMapper.insert(record);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        // 当前阶段不接入真实邮箱服务，验证码输出到日志用于联调
        logVerifyCode(email, scene, verifyCode);
        log.info("邮箱验证码发送成功: email={}, scene={}", email, scene.getCode());
    }

    /**
     * 校验并消费验证码
     */
    @Transactional(rollbackFor = Exception.class)
    public void verifyAndConsume(String email, EmailCodeScene scene, String verifyCode) {
        if (!StringUtils.hasText(email) || scene == null || !StringUtils.hasText(verifyCode)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        WfEmailCode latestCode = getLatestUnusedCode(email, scene);
        if (latestCode == null) {
            throw new BaseException(ErrorCode.EMAIL_CODE_ERROR);
        }
        if (latestCode.getExpireTime().isBefore(now)) {
            throw new BaseException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (!verifyCode.equals(latestCode.getVerifyCode())) {
            throw new BaseException(ErrorCode.EMAIL_CODE_ERROR);
        }

        LambdaUpdateWrapper<WfEmailCode> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfEmailCode::getId, latestCode.getId())
                .eq(WfEmailCode::getUsed, false)
                .set(WfEmailCode::getUsed, true)
                .set(WfEmailCode::getUsedTime, now);
        int updateRows = wfEmailCodeMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.EMAIL_CODE_ERROR);
        }
    }

    private void checkSendInterval(String email, EmailCodeScene scene, LocalDateTime now) {
        WfEmailCode latestCode = getLatestCode(email, scene);
        if (latestCode == null) {
            return;
        }
        long seconds = Duration.between(latestCode.getCreateTime(), now).getSeconds();
        if (seconds < SEND_INTERVAL_SECONDS) {
            throw new BaseException(ErrorCode.EMAIL_CODE_SEND_TOO_FREQUENT);
        }
    }

    private void checkDailyLimit(String email, EmailCodeScene scene, LocalDate day) {
        LocalDateTime dayStart = day.atStartOfDay();
        LambdaQueryWrapper<WfEmailCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfEmailCode::getEmail, email)
                .eq(WfEmailCode::getScene, scene.getCode())
                .ge(WfEmailCode::getCreateTime, dayStart);
        long count = wfEmailCodeMapper.selectCount(queryWrapper);
        if (count >= DAILY_SEND_LIMIT) {
            throw new BaseException(ErrorCode.EMAIL_CODE_DAILY_LIMIT);
        }
    }

    private WfEmailCode getLatestCode(String email, EmailCodeScene scene) {
        LambdaQueryWrapper<WfEmailCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfEmailCode::getEmail, email)
                .eq(WfEmailCode::getScene, scene.getCode())
                .orderByDesc(WfEmailCode::getCreateTime)
                .last("LIMIT 1");
        return wfEmailCodeMapper.selectOne(queryWrapper);
    }

    private WfEmailCode getLatestUnusedCode(String email, EmailCodeScene scene) {
        LambdaQueryWrapper<WfEmailCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfEmailCode::getEmail, email)
                .eq(WfEmailCode::getScene, scene.getCode())
                .eq(WfEmailCode::getUsed, false)
                .orderByDesc(WfEmailCode::getCreateTime)
                .last("LIMIT 1");
        return wfEmailCodeMapper.selectOne(queryWrapper);
    }

    private String generateVerifyCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private void logVerifyCode(String email, EmailCodeScene scene, String verifyCode) {
        log.info(
                "验证码(开发模式): email={}, scene={}, code={}, expireInMinutes={}",
                email,
                scene.getCode(),
                verifyCode,
                VERIFY_CODE_EXPIRE_MINUTES
        );
    }
}
