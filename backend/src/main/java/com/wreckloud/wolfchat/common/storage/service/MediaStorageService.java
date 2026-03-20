package com.wreckloud.wolfchat.common.storage.service;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.config.MediaStorageConfig;
import com.wreckloud.wolfchat.common.storage.model.MediaUploadPolicy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 媒体存储服务（本地文件）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaStorageService {
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String UPLOAD_TOKEN_PREFIX = "media:upload:token:";
    private static final String LOCAL_POLICY_PLACEHOLDER = "local";
    private static final int SUCCESS_ACTION_STATUS = 200;

    private final MediaStorageConfig mediaStorageConfig;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成前端上传策略（本地上传令牌）。
     */
    public MediaUploadPolicy buildUploadPolicy(Long userId, String objectKey, long maxFileSizeBytes) {
        if (userId == null || userId <= 0L) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "用户信息无效");
        }
        if (!StringUtils.hasText(objectKey) || maxFileSizeBytes <= 0L) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象无效");
        }

        long expireAt = Instant.now().getEpochSecond() + mediaStorageConfig.getUploadPolicyExpireSeconds();
        String uploadToken = UUID.randomUUID().toString().replace("-", "");
        UploadGrant grant = new UploadGrant(userId, objectKey.trim(), maxFileSizeBytes, expireAt);
        stringRedisTemplate.opsForValue().set(
                buildUploadTokenRedisKey(uploadToken),
                JSON.toJSONString(grant),
                mediaStorageConfig.getUploadPolicyExpireSeconds(),
                TimeUnit.SECONDS
        );

        MediaUploadPolicy policy = new MediaUploadPolicy();
        policy.setHost(mediaStorageConfig.getPublicBaseUrl() + mediaStorageConfig.getUploadPath());
        policy.setAccessKeyId(LOCAL_POLICY_PLACEHOLDER);
        policy.setPolicy(LOCAL_POLICY_PLACEHOLDER);
        policy.setSignature(uploadToken);
        policy.setObjectKey(grant.getObjectKey());
        policy.setExpireAt(expireAt);
        policy.setSuccessActionStatus(SUCCESS_ACTION_STATUS);
        return policy;
    }

    /**
     * 写入上传文件。
     */
    public void storeUploadedObject(String objectKey, String uploadToken, MultipartFile file) {
        if (!StringUtils.hasText(objectKey) || !StringUtils.hasText(uploadToken)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "上传参数不完整");
        }
        if (file == null || file.isEmpty() || file.getSize() <= 0L) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "上传文件不能为空");
        }

        UploadGrant grant = loadUploadGrant(uploadToken.trim());
        if (grant == null) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "上传令牌无效或已过期");
        }

        String normalizedObjectKey = objectKey.trim();
        if (!normalizedObjectKey.equals(grant.getObjectKey())) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象不匹配");
        }
        if (file.getSize() > grant.getMaxFileSizeBytes()) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "文件大小超过上传限制");
        }

        Path targetFilePath = resolveStoragePath(normalizedObjectKey);
        try {
            Files.createDirectories(targetFilePath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存媒体文件失败: key={}", normalizedObjectKey, e);
            throw new BaseException(ErrorCode.SYSTEM_ERROR, "媒体文件保存失败");
        }
    }

    /**
     * 生成媒体签名读取地址。
     */
    public String buildSignedReadUrl(String objectKey) {
        return buildSignedReadUrl(objectKey, null);
    }

    /**
     * 生成媒体签名读取地址。
     */
    public String buildSignedReadUrl(String objectKey, String process) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象不存在");
        }
        String normalizedObjectKey = objectKey.trim();
        String normalizedProcess = StringUtils.hasText(process) ? process.trim() : "";
        long expireAt = Instant.now().getEpochSecond() + mediaStorageConfig.getDownloadUrlExpireSeconds();
        String signature = sign(buildReadSignPayload(normalizedObjectKey, expireAt, normalizedProcess));

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(mediaStorageConfig.getPublicBaseUrl())
                .append(mediaStorageConfig.getReadPath())
                .append("?key=").append(encodeQuery(normalizedObjectKey))
                .append("&expires=").append(expireAt)
                .append("&signature=").append(encodeQuery(signature));
        if (StringUtils.hasText(normalizedProcess)) {
            urlBuilder.append("&process=").append(encodeQuery(normalizedProcess));
        }
        return urlBuilder.toString();
    }

    /**
     * 校验媒体读签名并解析本地文件。
     */
    public StoredObject resolveSignedReadableObject(String objectKey, Long expires, String signature, String process) {
        if (!StringUtils.hasText(objectKey) || expires == null || !StringUtils.hasText(signature)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "媒体读取参数不完整");
        }
        long now = Instant.now().getEpochSecond();
        if (expires < now) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体链接已过期");
        }

        String normalizedObjectKey = objectKey.trim();
        String normalizedProcess = StringUtils.hasText(process) ? process.trim() : "";
        String expectedSignature = sign(buildReadSignPayload(normalizedObjectKey, expires, normalizedProcess));
        if (!secureEquals(expectedSignature, signature.trim())) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体链接无效");
        }

        Path filePath = resolveStoragePath(normalizedObjectKey);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象不存在");
        }

        try {
            String contentType = resolveContentType(filePath);
            long fileSize = Files.size(filePath);
            return new StoredObject(filePath, contentType, fileSize);
        } catch (IOException e) {
            log.error("读取媒体文件失败: key={}", normalizedObjectKey, e);
            throw new BaseException(ErrorCode.SYSTEM_ERROR, "媒体文件读取失败");
        }
    }

    private UploadGrant loadUploadGrant(String uploadToken) {
        String json = stringRedisTemplate.opsForValue().get(buildUploadTokenRedisKey(uploadToken));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json, UploadGrant.class);
        } catch (Exception e) {
            log.warn("解析上传令牌失败: token={}", uploadToken, e);
            return null;
        }
    }

    private String buildUploadTokenRedisKey(String uploadToken) {
        return UPLOAD_TOKEN_PREFIX + uploadToken;
    }

    private Path resolveStoragePath(String objectKey) {
        Path rootPath = Paths.get(mediaStorageConfig.getLocalRootDir()).toAbsolutePath().normalize();
        String normalizedObjectKey = objectKey.replace("\\", "/");
        while (normalizedObjectKey.startsWith("/")) {
            normalizedObjectKey = normalizedObjectKey.substring(1);
        }
        Path targetPath = rootPath.resolve(normalizedObjectKey).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象路径非法");
        }
        return targetPath;
    }

    private String buildReadSignPayload(String objectKey, long expires, String process) {
        return "GET\n"
                + objectKey + "\n"
                + expires + "\n"
                + process;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(new SecretKeySpec(
                    mediaStorageConfig.getSignSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            ));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.SYSTEM_ERROR, "媒体签名失败");
        }
    }

    private boolean secureEquals(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String encodeQuery(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String resolveContentType(Path filePath) throws IOException {
        String contentType = Files.probeContentType(filePath);
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= fileName.length() - 1) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "mov":
                return "video/quicktime";
            case "m4v":
                return "video/x-m4v";
            case "webm":
                return "video/webm";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class StoredObject {
        private final Path filePath;
        private final String contentType;
        private final long fileSize;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class UploadGrant {
        private Long userId;
        private String objectKey;
        private Long maxFileSizeBytes;
        private Long expireAt;
    }
}
