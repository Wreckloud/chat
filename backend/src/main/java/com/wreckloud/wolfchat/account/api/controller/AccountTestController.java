package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 账户测试控制器
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Tag(name = "账户测试", description = "账户相关测试接口")
@RestController
@RequestMapping("/account/test")
public class AccountTestController {

    @Autowired
    private WfUserMapper wfUserMapper;

    @Operation(summary = "根据ID查询用户", description = "通过用户ID查询用户详细信息")
    @GetMapping("/user/{id}")
    public Result<WfUser> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id) {
        WfUser user = wfUserMapper.selectById(id);
        return Result.ok(user);
    }
}

