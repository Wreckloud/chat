package com.wreckloud.wolfchat.chat.lobby.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMetaVO;
import com.wreckloud.wolfchat.chat.lobby.application.service.LobbyService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 公共大厅控制器
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Tag(name = "聊天-大厅", description = "全站公共聊天室相关接口")
@RestController
@RequestMapping("/lobby")
@RequiredArgsConstructor
public class LobbyController {
    private final LobbyService lobbyService;

    @Operation(summary = "大厅消息列表", description = "分页查询公共大厅消息")
    @GetMapping("/messages")
    public Result<Page<LobbyMessageVO>> listMessages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(lobbyService.listMessages(userId, page, size));
    }

    @Operation(summary = "大厅元信息", description = "获取在线人数与最近在线信息")
    @GetMapping("/meta")
    public Result<LobbyMetaVO> getMeta() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(lobbyService.getMeta(userId));
    }
}

