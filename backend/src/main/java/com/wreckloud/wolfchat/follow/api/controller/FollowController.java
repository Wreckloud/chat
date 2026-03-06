package com.wreckloud.wolfchat.follow.api.controller;

import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.follow.api.vo.FollowUserVO;
import com.wreckloud.wolfchat.follow.application.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description 关注控制器
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Tag(name = "关注", description = "关注/粉丝/互关相关接口")
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @Operation(summary = "关注行者", description = "关注指定行者")
    @PostMapping("/{targetUserId}")
    public Result<Void> follow(@PathVariable Long targetUserId) {
        Long userId = UserContext.getRequiredUserId();
        followService.follow(userId, targetUserId);
        return Result.success("关注成功", null);
    }

    @Operation(summary = "取消关注", description = "取消关注指定行者")
    @DeleteMapping("/{targetUserId}")
    public Result<Void> unfollow(@PathVariable Long targetUserId) {
        Long userId = UserContext.getRequiredUserId();
        followService.unfollow(userId, targetUserId);
        return Result.success("已取消关注", null);
    }

    @Operation(summary = "关注列表", description = "获取当前行者的关注列表")
    @GetMapping("/following")
    public Result<List<FollowUserVO>> getFollowing() {
        Long userId = UserContext.getRequiredUserId();
        List<FollowUserVO> list = followService.getFollowing(userId);
        return Result.success(list);
    }

    @Operation(summary = "粉丝列表", description = "获取当前行者的粉丝列表")
    @GetMapping("/followers")
    public Result<List<FollowUserVO>> getFollowers() {
        Long userId = UserContext.getRequiredUserId();
        List<FollowUserVO> list = followService.getFollowers(userId);
        return Result.success(list);
    }

    @Operation(summary = "互关列表", description = "获取当前行者的互关列表")
    @GetMapping("/mutual")
    public Result<List<FollowUserVO>> getMutual() {
        Long userId = UserContext.getRequiredUserId();
        List<FollowUserVO> list = followService.getMutual(userId);
        return Result.success(list);
    }
}
