package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @Description 认证服务
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final WfUserMapper wfUserMapper;
    private final WolfNoService wolfNoService;
    private final JwtUtil jwtUtil;

    /**
     * 注册
     * 用户输入行者名和密码，系统分配狼藉号，创建行者，返回 token
     *
     * @param nickname 行者名（行者在群落中的称呼）
     * @param password 密码
     * @return 登录响应（包含 token、userInfo）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(String nickname, String password) {
        // 1. 分配狼藉号（先分配，userId暂时为null）
        String wolfNo = wolfNoService.allocateWolfNo(null);

        // 2. 创建行者
        WfUser user = new WfUser();
        user.setWolfNo(wolfNo);
        user.setLoginKey(password); // TODO:阶段1简化存储，后续升级为哈希存储
        user.setNickname(nickname);
        user.setStatus("NORMAL");
        wfUserMapper.insert(user);

        // 3. 更新号码池中的 userId（分配时userId为null，现在更新为实际userId）
        wolfNoService.updateUserIdByWolfNo(wolfNo, user.getId());

        // 4. 构建并返回登录响应
        log.info("行者注册成功: wolfNo={}, nickname={}, userId={}", wolfNo, nickname, user.getId());
        return buildLoginVO(user);
    }

    /**
     * 登录
     * 验证狼藉号+密码，返回 token
     *
     * @param wolfNo  狼藉号
     * @param loginKey 登录密码
     * @return 登录响应（包含 token、userInfo，不包含 loginKey）
     */
    public LoginVO login(String wolfNo, String loginKey) {
        // 1. 查询行者
        LambdaQueryWrapper<WfUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUser::getWolfNo, wolfNo);
        WfUser user = wfUserMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BaseException(ErrorCode.WOLF_NO_NOT_FOUND);
        }

        // 2. 检查状态
        if ("DISABLED".equals(user.getStatus())) {
            throw new BaseException(ErrorCode.USER_DISABLED);
        }

        // 3. 验证密码（阶段1简化，直接比较明文）
        if (!loginKey.equals(user.getLoginKey())) {
            throw new BaseException(ErrorCode.LOGIN_KEY_ERROR);
        }

        // 4. 构建并返回登录响应
        log.info("行者登录成功: wolfNo={}, userId={}", wolfNo, user.getId());
        return buildLoginVO(user);
    }

    /**
     * 构建登录响应 VO
     * 生成 JWT token 并封装用户信息
     *
     * @param user 行者实体
     * @return 登录响应
     */
    private LoginVO buildLoginVO(WfUser user) {
        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getWolfNo());

        // 构建响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);

        UserVO userVO = new UserVO();
        userVO.setWolfNo(user.getWolfNo());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setStatus(user.getStatus());
        loginVO.setUserInfo(userVO);

        return loginVO;
    }

}

