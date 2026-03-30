package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.ChangePasswordDTO;
import com.wreckloud.wolfchat.account.api.dto.SendBindEmailLinkDTO;
import com.wreckloud.wolfchat.account.api.dto.UpdateEquippedTitleDTO;
import com.wreckloud.wolfchat.account.api.dto.UpdateProfileDTO;
import com.wreckloud.wolfchat.account.api.dto.UpdateOnboardingStatusDTO;
import com.wreckloud.wolfchat.account.api.vo.UserAchievementVO;
import com.wreckloud.wolfchat.account.api.vo.UserHomeThreadPageVO;
import com.wreckloud.wolfchat.account.api.vo.UserHomeVO;
import com.wreckloud.wolfchat.account.api.vo.UserPublicVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.account.application.service.UserAchievementService;
import com.wreckloud.wolfchat.account.application.service.UserHomeService;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final UserService userService;
    private final UserHomeService userHomeService;
    private final UserAchievementService userAchievementService;
    private final AuthService authService;

    /**
     * 获取当前登录行者信息
     */
    @Operation(summary = "获取当前行者信息", description = "获取当前登录行者的信息（需要登录）")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(userService.getCurrentUserVOById(userId));
    }

    /**
     * 获取指定行者信息
     */
    @Operation(summary = "获取指定行者信息", description = "根据行者ID获取基础信息（需要登录）")
    @GetMapping("/{userId}")
    public Result<UserPublicVO> getUserById(@PathVariable Long userId) {
        return Result.success(userService.getPublicUserVOById(userId));
    }

    /**
     * 获取指定行者主页
     */
    @Operation(summary = "获取行者主页", description = "获取指定行者主页信息、统计与最近主题（需要登录）")
    @GetMapping("/{userId}/home")
    public Result<UserHomeVO> getUserHome(@PathVariable Long userId) {
        Long currentUserId = UserContext.getRequiredUserId();
        return Result.success(userHomeService.getUserHome(currentUserId, userId));
    }

    /**
     * 获取指定行者发布的主题
     */
    @Operation(summary = "获取行者主题列表", description = "分页获取指定行者发布的主题（需要登录）")
    @GetMapping("/{userId}/threads")
    public Result<UserHomeThreadPageVO> listUserThreads(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long size) {
        return Result.success(userHomeService.listUserThreads(userId, page, size));
    }

    /**
     * 获取当前行者主题（我的/草稿/垃圾站）
     */
    @Operation(summary = "获取我的主题列表", description = "分页获取当前登录行者主题，支持 mine/draft/trash 与关键词搜索（需要登录）")
    @GetMapping("/me/threads")
    public Result<UserHomeThreadPageVO> listMyThreads(@RequestParam(defaultValue = "mine") String tab,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(userHomeService.listMyThreads(userId, tab, keyword, page, size));
    }

    /**
     * 修改当前登录行者的资料
     */
    @Operation(summary = "更新个人资料", description = "更新当前登录行者的资料（需要登录）")
    @PutMapping("/profile")
    public Result<UserVO> updateCurrentUserProfile(@RequestBody @Validated UpdateProfileDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        userService.updateCurrentUserProfile(userId, dto);
        return Result.success("资料更新成功", userService.getCurrentUserVOById(userId));
    }

    /**
     * 修改当前登录行者的密码
     */
    @Operation(summary = "修改密码", description = "校验原密码后修改当前登录行者密码（需要登录）")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        authService.changePassword(
                userId,
                dto.getOldLoginKey(),
                dto.getNewLoginKey(),
                dto.getConfirmLoginKey(),
                dto.getEmailCode()
        );
        return Result.success("密码修改成功", null);
    }

    /**
     * 发送改密验证码
     */
    @Operation(summary = "发送改密验证码", description = "发送修改密码所需邮箱验证码（需要登录）")
    @PostMapping("/password/change-code/send")
    public Result<Void> sendPasswordChangeCode() {
        Long userId = UserContext.getRequiredUserId();
        authService.sendPasswordChangeCode(userId);
        return Result.success("验证码已发送", null);
    }

    /**
     * 发送邮箱认证链接
     */
    @Operation(summary = "发送邮箱认证链接", description = "给当前待认证邮箱发送认证链接（需要登录）")
    @PostMapping("/email-link/send")
    public Result<Void> sendBindEmailVerifyLink(@RequestBody @Validated SendBindEmailLinkDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        try {
            authService.sendBindEmailVerifyLink(userId, dto.getEmail());
            return Result.success("认证链接已发送", null);
        } catch (BaseException e) {
            // 幂等兜底：60秒内重复触发直接返回成功，避免“已收到邮件但前端提示失败”。
            if (ErrorCode.EMAIL_SEND_TOO_FREQUENT.getCode().equals(e.getCode())) {
                return Result.success("认证链接已发送，请稍候查收", null);
            }
            throw e;
        }
    }

    /**
     * 更新新用户引导状态
     */
    @Operation(summary = "更新引导状态", description = "更新当前登录行者的新用户引导状态（需要登录）")
    @PutMapping("/onboarding/status")
    public Result<Void> updateOnboardingStatus(@RequestBody @Validated UpdateOnboardingStatusDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        userService.updateOnboardingStatus(userId, dto.getOnboardingStatus());
        return Result.success("引导状态更新成功", null);
    }

    /**
     * 获取当前用户成就列表
     */
    @Operation(summary = "获取我的成就", description = "获取当前登录行者的成就与头衔信息（需要登录）")
    @GetMapping("/me/achievements")
    public Result<List<UserAchievementVO>> listMyAchievements() {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(userAchievementService.listMyAchievements(userId));
    }

    /**
     * 佩戴头衔
     */
    @Operation(summary = "佩戴头衔", description = "佩戴已解锁成就对应头衔（需要登录）")
    @PutMapping("/me/title/equip")
    public Result<Void> equipTitle(@RequestBody @Validated UpdateEquippedTitleDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        userAchievementService.equipTitle(userId, dto.getAchievementCode());
        return Result.success("佩戴成功", null);
    }

    /**
     * 取消佩戴头衔
     */
    @Operation(summary = "取消佩戴头衔", description = "取消当前佩戴头衔（需要登录）")
    @PutMapping("/me/title/unequip")
    public Result<Void> unequipTitle() {
        Long userId = UserContext.getRequiredUserId();
        userAchievementService.unequipTitle(userId);
        return Result.success("已取消佩戴", null);
    }

    /**
     * 注销当前账号
     */
    @Operation(summary = "注销当前账号", description = "测试期立即注销当前账号（需要登录）")
    @DeleteMapping("/me")
    public Result<Void> deactivateCurrentUser() {
        Long userId = UserContext.getRequiredUserId();
        userService.deactivateCurrentUser(userId);
        return Result.success("账号已注销", null);
    }
}


