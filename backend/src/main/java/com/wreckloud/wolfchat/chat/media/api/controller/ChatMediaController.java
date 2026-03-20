package com.wreckloud.wolfchat.chat.media.api.controller;

import com.wreckloud.wolfchat.chat.media.api.dto.ApplyChatUploadPolicyDTO;
import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.storage.model.MediaUploadPolicy;
import com.wreckloud.wolfchat.common.storage.service.MediaStorageService;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final MediaStorageService mediaStorageService;

    @Operation(summary = "申请图片上传策略", description = "申请聊天图片本地直传的表单策略")
    @PostMapping("/chat/image/upload-policy")
    public Result<MediaUploadPolicy> applyImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请视频上传策略", description = "申请聊天视频本地直传的表单策略")
    @PostMapping("/chat/video/upload-policy")
    public Result<MediaUploadPolicy> applyVideoUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createVideoUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请文件上传策略", description = "申请聊天文件本地直传的表单策略")
    @PostMapping("/chat/file/upload-policy")
    public Result<MediaUploadPolicy> applyFileUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createFileUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛主题图片上传策略", description = "申请论坛主题图片本地直传的表单策略")
    @PostMapping("/forum/thread/image/upload-policy")
    public Result<MediaUploadPolicy> applyForumThreadImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createForumThreadImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛主题视频上传策略", description = "申请论坛主题视频本地直传的表单策略")
    @PostMapping("/forum/thread/video/upload-policy")
    public Result<MediaUploadPolicy> applyForumThreadVideoUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createForumThreadVideoUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛回复图片上传策略", description = "申请论坛回复图片本地直传的表单策略")
    @PostMapping("/forum/reply/image/upload-policy")
    public Result<MediaUploadPolicy> applyForumReplyImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        log.info("申请论坛回帖图片上传策略: userId={}, extension={}, size={}", userId, dto.getExtension(), dto.getSize());
        return Result.success(chatMediaService.createForumReplyImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "上传媒体文件", description = "使用 upload-policy 返回的参数上传媒体到本地存储")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadMedia(@RequestPart("file") MultipartFile file,
                                      @RequestParam("key") String objectKey,
                                      @RequestParam("signature") String signature) {
        mediaStorageService.storeUploadedObject(objectKey, signature, file);
        return Result.success(objectKey);
    }

    @Operation(summary = "读取媒体文件", description = "使用签名链接读取本地媒体文件")
    @GetMapping("/object")
    public ResponseEntity<FileSystemResource> readMedia(@RequestParam("key") String objectKey,
                                                        @RequestParam("expires") Long expires,
                                                        @RequestParam("signature") String signature,
                                                        @RequestParam(value = "process", required = false) String process) {
        MediaStorageService.StoredObject storedObject = mediaStorageService.resolveSignedReadableObject(
                objectKey,
                expires,
                signature,
                process
        );
        FileSystemResource resource = new FileSystemResource(storedObject.getFilePath());
        MediaType contentType = resolveContentType(storedObject.getContentType());
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(storedObject.getFileSize())
                .body(resource);
    }

    private MediaType resolveContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
