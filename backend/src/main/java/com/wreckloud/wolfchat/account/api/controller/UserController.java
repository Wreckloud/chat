package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 行者 Controller
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Tag(name = "行者", description = "行者信息相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final WfUserMapper wfUserMapper;

    /**
     * 获取当前登录行者信息
     */
    @Operation(summary = "获取当前行者信息", description = "获取当前登录行者的信息（需要登录）")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }

        WfUser user = wfUserMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        UserVO userVO = new UserVO();
        userVO.setWolfNo(user.getWolfNo());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setStatus(user.getStatus() != null ? user.getStatus().getValue() : null);

        return Result.success(userVO);
    }
}


