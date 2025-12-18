package com.wreckloud.wolfchat.account.application.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.api.dto.MobileLoginDTO;
import com.wreckloud.wolfchat.account.api.dto.MobileRegisterDTO;
import com.wreckloud.wolfchat.account.api.dto.WechatLoginDTO;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.account.application.service.NoPoolService;
import com.wreckloud.wolfchat.account.application.service.VerificationService;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfNoPoolMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import com.wreckloud.wolfchat.common.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * @Description 认证服务实现类（专门处理登录/注册逻辑）
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WfUserMapper wfUserMapper;
    private final WfNoPoolMapper wfNoPoolMapper;
    private final VerificationService verificationService;
    private final NoPoolService noPoolService;
    private final JwtUtil jwtUtil;

    /**
     * 号码池可用数量阈值
     */
    @Value("${nopool.threshold}")
    private int poolThreshold;

    /**
     * 自动补充时生成的号码数量
     */
    @Value("${nopool.auto-generate-count}")
    private int autoGenerateCount;

    /**
     * 号码池状态：0未使用 1已使用 2冻结
     */
    private static final int NO_POOL_STATUS_UNUSED = 0;
    private static final int NO_POOL_STATUS_USED = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerByMobile(MobileRegisterDTO request) {
        // 1. 验证基本信息
        if (!StringUtils.hasText(request.getCaptchaKey()) || !StringUtils.hasText(request.getCaptchaCode())) {
            throw new BaseException(ErrorCode.CAPTCHA_EMPTY);
        }
        boolean captchaValid = verificationService.verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());
        if (!captchaValid) {
            throw new BaseException(ErrorCode.CAPTCHA_ERROR);
        }
        if (StringUtils.hasText(request.getConfirmPassword())
                && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 检查手机号是否已存在
        LambdaQueryWrapper<WfUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(WfUser::getMobile, request.getMobile());
        Long existCount = wfUserMapper.selectCount(userQuery);
        if (existCount > 0) {
            throw new BaseException(ErrorCode.MOBILE_ALREADY_EXISTS);
        }

        // 3. 保底机制：检查号码池可用数量，低于阈值自动补充
        ensurePoolHasEnoughNumbers();

        // 4. 使用乐观锁分配号码（重试机制）
        // 先分配号码，再创建用户，确保满足数据库约束（wf_no NOT NULL）
        Long allocatedWfNo = allocateNumberWithRetry();
        if (allocatedWfNo == null) {
            throw new BaseException(ErrorCode.NO_AVAILABLE_NUMBER);
        }

        // 5. 加密密码
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.encrypt(request.getPassword(), salt);

        // 6. 创建用户（带已分配的号码）
        WfUser user = new WfUser();
        user.setWfNo(allocatedWfNo);
        user.setMobile(request.getMobile());
        user.setUsername("用户" + allocatedWfNo);
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        wfUserMapper.insert(user);

        // 7. 更新号码池的userId（分配号码时userId设为null，这里更新为实际用户ID）
        // 注意：如果用户创建失败，事务会回滚，号码分配也会回滚
        LambdaQueryWrapper<WfNoPool> poolQuery = new LambdaQueryWrapper<>();
        poolQuery.eq(WfNoPool::getWfNo, allocatedWfNo);
        WfNoPool noPool = wfNoPoolMapper.selectOne(poolQuery);
        if (noPool != null) {
            noPool.setUserId(user.getId());
            wfNoPoolMapper.updateById(noPool);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginByWechat(WechatLoginDTO request) {
        // 1. 保底机制：检查号码池可用数量，低于阈值自动补充
        ensurePoolHasEnoughNumbers();

        // 2. 查询用户（优先通过unionid，其次openid）
        WfUser user = null;
        if (StringUtils.hasText(request.getWxUnionid())) {
            LambdaQueryWrapper<WfUser> unionQuery = new LambdaQueryWrapper<>();
            unionQuery.eq(WfUser::getWxUnionid, request.getWxUnionid());
            user = wfUserMapper.selectOne(unionQuery);
        }

        if (user == null && StringUtils.hasText(request.getWxOpenid())) {
            LambdaQueryWrapper<WfUser> openidQuery = new LambdaQueryWrapper<>();
            openidQuery.eq(WfUser::getWxOpenid, request.getWxOpenid());
            user = wfUserMapper.selectOne(openidQuery);
        }

        // 3. 如果用户不存在，自动注册
        if (user == null) {
            user = registerByWechat(request);
        } else {
            // 4. 用户已存在，检查状态
            if (!UserStatus.NORMAL.getCode().equals(user.getStatus())) {
                throw new BaseException(ErrorCode.USER_DISABLED);
            }

            // 5. 更新登录信息和微信信息
            updateWechatInfo(user, request);
            updateLoginInfo(user);
            wfUserMapper.updateById(user);
        }

        // 6. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId());

        // 7. 转换为VO并返回
        UserVO userVO = convertToVO(user);
        return new LoginVO(token, userVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginByMobile(MobileLoginDTO request) {
        // 1. 验证短信验证码
        boolean smsCodeValid = verificationService.verifySmsCode(request.getMobile(), request.getSmsCodeKey(), request.getSmsCode());
        if (!smsCodeValid) {
            throw new BaseException(ErrorCode.SMS_CODE_ERROR);
        }

        // 2. 保底机制：检查号码池可用数量，低于阈值自动补充
        ensurePoolHasEnoughNumbers();

        // 3. 查询用户
        LambdaQueryWrapper<WfUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(WfUser::getMobile, request.getMobile());
        WfUser user = wfUserMapper.selectOne(userQuery);

        // 4. 如果用户不存在，自动注册
        if (user == null) {
            user = registerByMobileAndSms(request);
        } else {
            // 5. 用户已存在，检查状态
            if (!UserStatus.NORMAL.getCode().equals(user.getStatus())) {
                throw new BaseException(ErrorCode.USER_DISABLED);
            }
            // 6. 更新登录信息
            updateLoginInfo(user);
            wfUserMapper.updateById(user);
        }

        // 7. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId());

        // 8. 转换为VO并返回
        UserVO userVO = convertToVO(user);
        return new LoginVO(token, userVO);
    }

    /**
     * 使用乐观锁分配号码（带重试机制）
     * 如果号码被其他线程抢走，会重新选择下一个可用号码
     *
     * 使用 UPDATE ... WHERE status = 0 的乐观锁模式：
     * - updateCount == 1：分配成功
     * - updateCount == 0：号码被抢走，重试下一个
     *
     * @return 分配成功的号码，失败返回null
     */
    private Long allocateNumberWithRetry() {
        int maxRetries = 10; // 最多重试10次
        int retryCount = 0;

        while (retryCount < maxRetries) {
            // 查询一个可用号码（优先普通号）
            LambdaQueryWrapper<WfNoPool> poolQuery = new LambdaQueryWrapper<>();
            poolQuery.eq(WfNoPool::getStatus, NO_POOL_STATUS_UNUSED)
                    .eq(WfNoPool::getIsPretty, false) // 先分配普通号
                    .orderByAsc(WfNoPool::getId) // 按ID排序，确保顺序
                    .last("LIMIT 1");
            WfNoPool availableNo = wfNoPoolMapper.selectOne(poolQuery);

            if (availableNo == null) {
                // 没有可用号码
                return null;
            }

            // 使用乐观锁更新：UPDATE wf_no_pool SET status = 1, user_id = NULL WHERE wf_no = ? AND status = 0
            // userId 先设为 null，创建用户后再更新
            int updateCount = wfNoPoolMapper.allocateNumber(availableNo.getWfNo(), null);

            if (updateCount == 1) {
                // 分配成功，返回号码
                return availableNo.getWfNo();
            } else if (updateCount == 0) {
                // 号码被其他线程抢走，重试下一个
                retryCount++;
                continue;
            }
        }

        // 重试次数用完，分配失败
        return null;
    }

    /**
     * 保底机制：确保号码池有足够的可用号码
     * 如果可用号码数量低于阈值，自动生成一批号码
     *
     * 注意：生成号码失败不会影响注册流程，只记录日志
     */
    private void ensurePoolHasEnoughNumbers() {
        // 查询当前可用号码数量（普通号）
        LambdaQueryWrapper<WfNoPool> poolQuery = new LambdaQueryWrapper<>();
        poolQuery.eq(WfNoPool::getStatus, NO_POOL_STATUS_UNUSED)
                .eq(WfNoPool::getIsPretty, false);
        Long availableCount = wfNoPoolMapper.selectCount(poolQuery);

        if (availableCount < poolThreshold) {
            log.warn("号码池可用数量不足，当前：{}，阈值：{}，开始自动补充 {} 个号码",
                    availableCount, poolThreshold, autoGenerateCount);
            try {
                // 自动生成号码
                // 注意：generateNumbers 有独立事务，即使失败也不会影响当前注册事务
                int generatedCount = noPoolService.generateNumbers(autoGenerateCount);
                log.info("号码池自动补充完成，生成数量：{}，当前可用数量：{}",
                        generatedCount, availableCount + generatedCount);
            } catch (Exception e) {
                // 生成失败不影响注册流程，记录日志即可
                // 如果号码池真的没有号码，会在后续分配时抛出异常
                log.error("号码池自动补充失败，将继续尝试分配现有号码", e);
            }
        }
    }

    /**
     * 微信自动注册
     *
     * @param request 微信登录请求
     * @return 创建的用户
     */
    private WfUser registerByWechat(WechatLoginDTO request) {
        // 分配号码
        Long allocatedWfNo = allocateNumberWithRetry();
        if (allocatedWfNo == null) {
            throw new BaseException(ErrorCode.NO_AVAILABLE_NUMBER);
        }

        // 创建用户
        WfUser user = new WfUser();
        user.setWfNo(allocatedWfNo);
        user.setWxOpenid(request.getWxOpenid());
        user.setWxUnionid(request.getWxUnionid());

        // 设置昵称（优先使用微信昵称）
        if (StringUtils.hasText(request.getNickname())) {
            user.setUsername(request.getNickname());
        } else {
            user.setUsername("用户" + allocatedWfNo);
        }

        // 设置头像（优先使用微信头像）
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }

        // 设置性别
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        // 微信登录不需要密码
        user.setPasswordHash("");
        user.setSalt(null);
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        wfUserMapper.insert(user);

        // 更新号码池的userId
        LambdaQueryWrapper<WfNoPool> poolQuery = new LambdaQueryWrapper<>();
        poolQuery.eq(WfNoPool::getWfNo, allocatedWfNo);
        WfNoPool noPool = wfNoPoolMapper.selectOne(poolQuery);
        if (noPool != null) {
            noPool.setUserId(user.getId());
            wfNoPoolMapper.updateById(noPool);
        }

        return user;
    }

    /**
     * 手机号自动注册（通过短信验证码）
     *
     * @param request 手机号登录请求
     * @return 创建的用户
     */
    private WfUser registerByMobileAndSms(MobileLoginDTO request) {
        // 分配号码
        Long allocatedWfNo = allocateNumberWithRetry();
        if (allocatedWfNo == null) {
            throw new BaseException(ErrorCode.NO_AVAILABLE_NUMBER);
        }

        // 创建用户
        WfUser user = new WfUser();
        user.setWfNo(allocatedWfNo);
        user.setMobile(request.getMobile());
        user.setUsername("用户" + allocatedWfNo); // 默认昵称
        user.setPasswordHash(""); // 手机号验证码登录不需要密码
        user.setSalt(null);
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        wfUserMapper.insert(user);

        // 更新号码池的userId
        LambdaQueryWrapper<WfNoPool> poolQuery = new LambdaQueryWrapper<>();
        poolQuery.eq(WfNoPool::getWfNo, allocatedWfNo);
        WfNoPool noPool = wfNoPoolMapper.selectOne(poolQuery);
        if (noPool != null) {
            noPool.setUserId(user.getId());
            wfNoPoolMapper.updateById(noPool);
        }

        return user;
    }

    /**
     * 更新微信信息
     *
     * @param user    用户
     * @param request 微信登录请求
     */
    private void updateWechatInfo(WfUser user, WechatLoginDTO request) {
        boolean needUpdate = false;

        // 更新openid（如果变化）
        if (!StringUtils.hasText(user.getWxOpenid()) && StringUtils.hasText(request.getWxOpenid())) {
            user.setWxOpenid(request.getWxOpenid());
            needUpdate = true;
        }

        // 更新unionid（如果变化）
        if (!StringUtils.hasText(user.getWxUnionid()) && StringUtils.hasText(request.getWxUnionid())) {
            user.setWxUnionid(request.getWxUnionid());
            needUpdate = true;
        }

        // 更新昵称（如果提供了新昵称）
        if (StringUtils.hasText(request.getNickname()) && !request.getNickname().equals(user.getUsername())) {
            user.setUsername(request.getNickname());
            needUpdate = true;
        }

        // 更新头像（优先使用微信头像，如果提供了就使用）
        if (StringUtils.hasText(request.getAvatar()) && !request.getAvatar().equals(user.getAvatar())) {
            user.setAvatar(request.getAvatar());
            needUpdate = true;
        }

        // 更新性别（如果提供了）
        if (request.getGender() != null && !request.getGender().equals(user.getGender())) {
            user.setGender(request.getGender());
            needUpdate = true;
        }

        if (needUpdate) {
            user.setUpdateTime(LocalDateTime.now());
        }
    }

    /**
     * 更新登录信息
     *
     * @param user 用户
     */
    private void updateLoginInfo(WfUser user) {
        // 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 转换为VO
     *
     * @param user 用户实体
     * @return 用户VO
     */
    private UserVO convertToVO(WfUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}

