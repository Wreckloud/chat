package com.wreckloud.wolfchat.chat.media.application.service;

import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.chat.media.api.dto.ApplyChatUploadPolicyDTO;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.support.MessageRuleSupport;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.config.OssStorageConfig;
import com.wreckloud.wolfchat.common.storage.model.OssPostPolicy;
import com.wreckloud.wolfchat.common.storage.service.OssStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @Description 聊天媒体服务
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Service
@RequiredArgsConstructor
public class ChatMediaService {
    private static final Map<String, String> IMAGE_MIME_MAPPING = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif"
    );
    private static final Map<String, String> VIDEO_MIME_MAPPING = Map.of(
            "mp4", "video/mp4",
            "mov", "video/quicktime",
            "m4v", "video/x-m4v",
            "webm", "video/webm"
    );
    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("^[a-z0-9]{1,20}$");

    private final OssStorageService ossStorageService;
    private final OssStorageConfig ossStorageConfig;
    private final WfUserMapper wfUserMapper;

    @FunctionalInterface
    private interface MappedMimeValidator {
        void validate(String extension, String mimeType);
    }

    /**
     * 创建聊天图片上传策略
     */
    public OssPostPolicy createImageUploadPolicy(Long userId, ApplyChatUploadPolicyDTO dto) {
        return createMappedUploadPolicy(
                userId,
                dto,
                "image",
                IMAGE_MIME_MAPPING,
                "暂不支持该图片格式",
                ossStorageConfig.getMaxImageSizeBytes(),
                "图片上传大小上限未配置",
                "图片大小超过上传限制",
                this::validateImageMimeIfPresent
        );
    }

    /**
     * 创建聊天视频上传策略
     */
    public OssPostPolicy createVideoUploadPolicy(Long userId, ApplyChatUploadPolicyDTO dto) {
        return createMappedUploadPolicy(
                userId,
                dto,
                "video",
                VIDEO_MIME_MAPPING,
                "暂不支持该视频格式",
                ossStorageConfig.getMaxVideoSizeBytes(),
                "视频上传大小上限未配置",
                "视频大小超过上传限制",
                this::validateVideoMimeIfPresentIgnoreExtension
        );
    }

    /**
     * 创建聊天文件上传策略
     */
    public OssPostPolicy createFileUploadPolicy(Long userId, ApplyChatUploadPolicyDTO dto) {
        String extension = normalizeExtension(dto.getExtension());
        validateFileExtension(extension);
        validateFileMimeIfPresent(dto.getMimeType());
        Long maxSizeBytes = requireLimit(ossStorageConfig.getMaxFileSizeBytes(), "文件上传大小上限未配置");
        validateMediaSize(dto.getSize(), maxSizeBytes, "文件大小超过上传限制");

        String wolfNo = getWolfNoByUserId(userId);
        String objectKey = buildObjectKey("file", wolfNo, extension);
        return ossStorageService.buildPostPolicy(objectKey, maxSizeBytes);
    }

    /**
     * 统一校验聊天消息媒体字段
     */
    public void validateMessagePayload(Long userId, SendMessageCommand command) {
        MessageType msgType = command.getMsgType();
        switch (msgType) {
            case TEXT:
                MessageRuleSupport.validateNoMediaFields(
                        command.getMediaKey(),
                        command.getMediaWidth(),
                        command.getMediaHeight(),
                        command.getMediaSize(),
                        command.getMediaMimeType()
                );
                break;
            case IMAGE:
                validateImageMessage(userId, command);
                break;
            case VIDEO:
                validateVideoMessage(userId, command);
                break;
            case FILE:
                validateFileMessage(userId, command);
                break;
            default:
                throw new BaseException(ErrorCode.PARAM_ERROR, "消息类型不支持");
        }
    }

    /**
     * 校验图片消息元数据
     */
    public void validateImageMessage(Long userId, SendMessageCommand command) {
        validateMappedMessage(
                userId,
                command,
                MessageType.IMAGE,
                "image",
                IMAGE_MIME_MAPPING,
                "图片对象无效",
                "暂不支持该图片格式",
                ossStorageConfig.getMaxImageSizeBytes(),
                "图片上传大小上限未配置",
                "图片大小超过上传限制",
                "图片宽度不合法",
                "图片高度不合法",
                this::validateImageMimeIfPresent
        );
    }

    /**
     * 校验视频消息元数据
     */
    public void validateVideoMessage(Long userId, SendMessageCommand command) {
        validateMappedMessage(
                userId,
                command,
                MessageType.VIDEO,
                "video",
                VIDEO_MIME_MAPPING,
                "视频对象无效",
                "暂不支持该视频格式",
                ossStorageConfig.getMaxVideoSizeBytes(),
                "视频上传大小上限未配置",
                "视频大小超过上传限制",
                "视频宽度不合法",
                "视频高度不合法",
                this::validateVideoMimeIfPresentIgnoreExtension
        );
    }

    /**
     * 校验文件消息元数据
     */
    public void validateFileMessage(Long userId, SendMessageCommand command) {
        if (!MessageType.FILE.equals(command.getMsgType())) {
            return;
        }
        String mediaKey = command.getMediaKey();
        String wolfNo = getWolfNoByUserId(userId);
        if (!StringUtils.hasText(mediaKey) || !buildFileKeyPattern("file", wolfNo).matcher(mediaKey).matches()) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "文件对象无效");
        }

        String extension = extractExtension(mediaKey);
        validateFileExtension(extension);
        validateFileMimeIfPresent(command.getMediaMimeType());
        validateMediaSize(
                command.getMediaSize(),
                requireLimit(ossStorageConfig.getMaxFileSizeBytes(), "文件上传大小上限未配置"),
                "文件大小超过上传限制"
        );
        if (command.getMediaWidth() != null || command.getMediaHeight() != null) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "文件消息不支持宽高字段");
        }
    }

    private OssPostPolicy createMappedUploadPolicy(Long userId,
                                                   ApplyChatUploadPolicyDTO dto,
                                                   String category,
                                                   Map<String, String> mimeMapping,
                                                   String unsupportedTypeMessage,
                                                   Long configuredMaxSize,
                                                   String maxSizeConfigMessage,
                                                   String exceedSizeMessage,
                                                   MappedMimeValidator mimeValidator) {
        String extension = normalizeExtension(dto.getExtension());
        validateMappedExtension(extension, mimeMapping, unsupportedTypeMessage);
        mimeValidator.validate(extension, dto.getMimeType());
        Long maxSizeBytes = requireLimit(configuredMaxSize, maxSizeConfigMessage);
        validateMediaSize(dto.getSize(), maxSizeBytes, exceedSizeMessage);

        String wolfNo = getWolfNoByUserId(userId);
        String objectKey = buildObjectKey(category, wolfNo, extension);
        return ossStorageService.buildPostPolicy(objectKey, maxSizeBytes);
    }

    private void validateMappedMessage(Long userId,
                                       SendMessageCommand command,
                                       MessageType expectedType,
                                       String category,
                                       Map<String, String> mimeMapping,
                                       String invalidObjectMessage,
                                       String unsupportedTypeMessage,
                                       Long configuredMaxSize,
                                       String maxSizeConfigMessage,
                                       String exceedSizeMessage,
                                       String invalidWidthMessage,
                                       String invalidHeightMessage,
                                       MappedMimeValidator mimeValidator) {
        if (!expectedType.equals(command.getMsgType())) {
            return;
        }
        String mediaKey = command.getMediaKey();
        String wolfNo = getWolfNoByUserId(userId);
        if (!StringUtils.hasText(mediaKey) || !buildMappedKeyPattern(category, wolfNo, mimeMapping).matcher(mediaKey).matches()) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, invalidObjectMessage);
        }

        String extension = extractExtension(mediaKey);
        validateMappedExtension(extension, mimeMapping, unsupportedTypeMessage);
        mimeValidator.validate(extension, command.getMediaMimeType());
        validateMediaSize(
                command.getMediaSize(),
                requireLimit(configuredMaxSize, maxSizeConfigMessage),
                exceedSizeMessage
        );
        validatePositiveDimension(command.getMediaWidth(), invalidWidthMessage);
        validatePositiveDimension(command.getMediaHeight(), invalidHeightMessage);
    }

    private void validateVideoMimeIfPresentIgnoreExtension(String extension, String mimeType) {
        validateVideoMimeIfPresent(mimeType);
    }

    private void validateMappedExtension(String extension, Map<String, String> mimeMapping, String errorMessage) {
        if (!mimeMapping.containsKey(extension)) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, errorMessage);
        }
    }

    private void validateImageMimeIfPresent(String extension, String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return;
        }
        String normalizedMimeType = normalizeMimeType(mimeType);
        String expectedMimeType = IMAGE_MIME_MAPPING.get(extension);
        if (!normalizedMimeType.equals(expectedMimeType)) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, "暂不支持该图片格式");
        }
    }

    private void validateVideoMimeIfPresent(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return;
        }
        String normalizedMimeType = normalizeMimeType(mimeType);
        if (!normalizedMimeType.startsWith("video/")) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, "暂不支持该视频格式");
        }
    }

    private void validateFileMimeIfPresent(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return;
        }
        String normalizedMimeType = normalizeMimeType(mimeType);
        if (!normalizedMimeType.contains("/") || normalizedMimeType.length() > 100) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, "文件类型不合法");
        }
    }

    private void validateFileExtension(String extension) {
        if (!FILE_EXTENSION_PATTERN.matcher(extension).matches()) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, "文件后缀不合法");
        }
    }

    private void validateMediaSize(Long size, Long maxSizeBytes, String exceedMessage) {
        if (size == null || size <= 0) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体大小不合法");
        }
        if (size > maxSizeBytes) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, exceedMessage);
        }
    }

    private void validatePositiveDimension(Integer value, String errorMessage) {
        if (value == null) {
            return;
        }
        if (value <= 0) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, errorMessage);
        }
    }

    private Long requireLimit(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BaseException(ErrorCode.OSS_CONFIG_INCOMPLETE, message);
        }
        return value;
    }

    private String buildObjectKey(String category, String wolfNo, String extension) {
        LocalDate today = LocalDate.now();
        return String.format(
                Locale.ROOT,
                "chat/%s/%s/%04d/%02d/%02d/%s.%s",
                category,
                wolfNo,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );
    }

    private Pattern buildMappedKeyPattern(String category, String wolfNo, Map<String, String> mimeMapping) {
        String extensionPattern = String.join("|", mimeMapping.keySet());
        String pattern = String.format(
                Locale.ROOT,
                "^chat/%s/%s/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.(%s)$",
                category,
                Pattern.quote(wolfNo),
                extensionPattern
        );
        return Pattern.compile(pattern);
    }

    private Pattern buildFileKeyPattern(String category, String wolfNo) {
        String pattern = String.format(
                Locale.ROOT,
                "^chat/%s/%s/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.([a-z0-9]{1,20})$",
                category,
                Pattern.quote(wolfNo)
        );
        return Pattern.compile(pattern);
    }

    private String getWolfNoByUserId(Long userId) {
        if (userId == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        WfUser user = wfUserMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getWolfNo())) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        return user.getWolfNo();
    }

    private String normalizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, "文件后缀不能为空");
        }
        return extension.trim().toLowerCase(Locale.ROOT).replace(".", "");
    }

    private String normalizeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            throw new BaseException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, "文件类型不能为空");
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String extractExtension(String objectKey) {
        int index = objectKey.lastIndexOf('.');
        if (index < 0 || index == objectKey.length() - 1) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象无效");
        }
        return objectKey.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
