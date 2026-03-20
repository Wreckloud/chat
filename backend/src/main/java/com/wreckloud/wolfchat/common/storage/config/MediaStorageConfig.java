package com.wreckloud.wolfchat.common.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * @Description 媒体存储配置（本地文件）
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.storage")
public class MediaStorageConfig {
    private static final int SIGN_SECRET_MIN_BYTES = 16;

    /**
     * 对外访问基地址，例如 http://127.0.0.1:8080
     */
    private String publicBaseUrl;

    /**
     * 文件上传接口路径
     */
    private String uploadPath;

    /**
     * 文件读取接口路径
     */
    private String readPath;

    /**
     * 本地文件存储根目录
     */
    private String localRootDir;

    /**
     * 下载签名密钥
     */
    private String signSecret;

    /**
     * 上传策略有效期（秒）
     */
    private Long uploadPolicyExpireSeconds;

    /**
     * 下载签名有效期（秒）
     */
    private Long downloadUrlExpireSeconds;

    /**
     * 图片上传大小上限（字节）
     */
    private Long maxImageSizeBytes;

    /**
     * 视频上传大小上限（字节）
     */
    private Long maxVideoSizeBytes;

    /**
     * 文件上传大小上限（字节）
     */
    private Long maxFileSizeBytes;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.storage.public-base-url");
        }
        if (!StringUtils.hasText(uploadPath)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.storage.upload-path");
        }
        if (!StringUtils.hasText(readPath)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.storage.read-path");
        }
        if (!StringUtils.hasText(localRootDir)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.storage.local-root-dir");
        }
        if (!StringUtils.hasText(signSecret)) {
            throw new IllegalArgumentException("配置缺失: wolfchat.storage.sign-secret");
        }
        if (signSecret.getBytes(StandardCharsets.UTF_8).length < SIGN_SECRET_MIN_BYTES) {
            throw new IllegalArgumentException("配置非法: wolfchat.storage.sign-secret 至少需要 16 字节");
        }
        validatePositive("wolfchat.storage.upload-policy-expire-seconds", uploadPolicyExpireSeconds);
        validatePositive("wolfchat.storage.download-url-expire-seconds", downloadUrlExpireSeconds);
        validatePositive("wolfchat.storage.max-image-size-bytes", maxImageSizeBytes);
        validatePositive("wolfchat.storage.max-video-size-bytes", maxVideoSizeBytes);
        validatePositive("wolfchat.storage.max-file-size-bytes", maxFileSizeBytes);

        publicBaseUrl = trimTrailingSlash(publicBaseUrl.trim());
        uploadPath = ensureLeadingSlash(uploadPath.trim());
        readPath = ensureLeadingSlash(readPath.trim());
    }

    private void validatePositive(String key, Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("配置非法: " + key + " 必须 > 0");
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String ensureLeadingSlash(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }
}
