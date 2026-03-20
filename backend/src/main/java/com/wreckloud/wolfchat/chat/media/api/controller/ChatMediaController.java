package com.wreckloud.wolfchat.chat.media.api.controller;

import com.wreckloud.wolfchat.chat.media.api.dto.ApplyChatUploadPolicyDTO;
import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.storage.model.OssPostPolicy;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 聊天媒体控制器
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Tag(name = "聊天-媒体", description = "聊天媒体上传相关接口")
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
public class ChatMediaController {
    private final ChatMediaService chatMediaService;

    @Operation(summary = "申请图片上传策略", description = "申请聊天图片直传 OSS 的表单策略")
    @PostMapping("/chat/image/upload-policy")
    public Result<OssPostPolicy> applyImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请视频上传策略", description = "申请聊天视频直传 OSS 的表单策略")
    @PostMapping("/chat/video/upload-policy")
    public Result<OssPostPolicy> applyVideoUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createVideoUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请文件上传策略", description = "申请聊天文件直传 OSS 的表单策略")
    @PostMapping("/chat/file/upload-policy")
    public Result<OssPostPolicy> applyFileUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createFileUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛主题图片上传策略", description = "申请论坛主题图片直传 OSS 的表单策略")
    @PostMapping("/forum/thread/image/upload-policy")
    public Result<OssPostPolicy> applyForumThreadImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createForumThreadImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛主题视频上传策略", description = "申请论坛主题视频直传 OSS 的表单策略")
    @PostMapping("/forum/thread/video/upload-policy")
    public Result<OssPostPolicy> applyForumThreadVideoUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createForumThreadVideoUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛回复图片上传策略", description = "申请论坛回复图片直传 OSS 的表单策略")
    @PostMapping("/forum/reply/image/upload-policy")
    public Result<OssPostPolicy> applyForumReplyImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        log.info("申请论坛回帖图片上传策略: userId={}, extension={}, size={}", userId, dto.getExtension(), dto.getSize());
        return Result.success(chatMediaService.createForumReplyImageUploadPolicy(userId, dto));
    }
}
