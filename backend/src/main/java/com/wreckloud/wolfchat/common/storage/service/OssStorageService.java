package com.wreckloud.wolfchat.common.storage.service;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.config.OssStorageConfig;
import com.wreckloud.wolfchat.common.storage.model.OssPostPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description OSS 签名服务
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Service
@RequiredArgsConstructor
public class OssStorageService {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final int SUCCESS_ACTION_STATUS = 200;

    private final OssStorageConfig ossStorageConfig;

    /**
     * 生成表单直传策略
     */
    public OssPostPolicy buildPostPolicy(String objectKey, long maxFileSizeBytes) {
        ensureConfigured();
        if (maxFileSizeBytes <= 0) {
            throw new BaseException(ErrorCode.OSS_CONFIG_INCOMPLETE, "OSS 上传大小限制未配置");
        }

        long expireAt = Instant.now().getEpochSecond() + ossStorageConfig.getUploadPolicyExpireSeconds();
        Map<String, Object> policyDocument = new LinkedHashMap<>();
        policyDocument.put("expiration", Instant.ofEpochSecond(expireAt).toString());
        policyDocument.put("conditions", buildPolicyConditions(objectKey, maxFileSizeBytes));

        String policyJson = JSON.toJSONString(policyDocument);
        String policy = Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(policy);

        OssPostPolicy postPolicy = new OssPostPolicy();
        postPolicy.setHost(buildBucketHost());
        postPolicy.setAccessKeyId(ossStorageConfig.getAccessKeyId());
        postPolicy.setPolicy(policy);
        postPolicy.setSignature(signature);
        postPolicy.setObjectKey(objectKey);
        postPolicy.setExpireAt(expireAt);
        postPolicy.setSuccessActionStatus(SUCCESS_ACTION_STATUS);
        return postPolicy;
    }

    /**
     * 生成私有对象的签名访问地址
     */
    public String buildSignedReadUrl(String objectKey) {
        return buildSignedReadUrl(objectKey, null);
    }

    /**
     * 生成私有对象的签名访问地址（支持 x-oss-process）
     */
    public String buildSignedReadUrl(String objectKey, String process) {
        ensureConfigured();
        if (!StringUtils.hasText(objectKey)) {
            throw new BaseException(ErrorCode.MEDIA_FILE_INVALID, "媒体对象不存在");
        }

        long expireAt = Instant.now().getEpochSecond() + ossStorageConfig.getDownloadUrlExpireSeconds();
        String canonicalResource = "/" + ossStorageConfig.getBucket() + "/" + objectKey;
        if (StringUtils.hasText(process)) {
            canonicalResource = canonicalResource + "?x-oss-process=" + process;
        }
        String stringToSign = "GET\n\n\n" + expireAt + "\n" + canonicalResource;
        String signature = sign(stringToSign);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(buildBucketHost())
                .append("/")
                .append(objectKey)
                .append("?OSSAccessKeyId=")
                .append(encodeQuery(ossStorageConfig.getAccessKeyId()))
                .append("&Expires=")
                .append(expireAt)
                .append("&Signature=")
                .append(encodeQuery(signature));
        if (StringUtils.hasText(process)) {
            urlBuilder.append("&x-oss-process=").append(encodeQuery(process));
        }
        return urlBuilder.toString();
    }

    private List<Object> buildPolicyConditions(String objectKey, long maxFileSizeBytes) {
        List<Object> conditions = new ArrayList<>();
        conditions.add(List.of("eq", "$key", objectKey));
        conditions.add(List.of("content-length-range", 1, maxFileSizeBytes));
        conditions.add(List.of("eq", "$success_action_status", String.valueOf(SUCCESS_ACTION_STATUS)));
        return conditions;
    }

    private String buildBucketHost() {
        return "https://" + ossStorageConfig.getBucket() + "." + ossStorageConfig.getEndpoint();
    }

    private void ensureConfigured() {
        if (!hasConfiguredText(ossStorageConfig.getEndpoint())
                || !hasConfiguredText(ossStorageConfig.getBucket())
                || !hasConfiguredText(ossStorageConfig.getAccessKeyId())
                || !hasConfiguredText(ossStorageConfig.getAccessKeySecret())
                || ossStorageConfig.getUploadPolicyExpireSeconds() == null
                || ossStorageConfig.getUploadPolicyExpireSeconds() <= 0
                || ossStorageConfig.getDownloadUrlExpireSeconds() == null
                || ossStorageConfig.getDownloadUrlExpireSeconds() <= 0) {
            throw new BaseException(ErrorCode.OSS_CONFIG_INCOMPLETE, "OSS 配置未完成");
        }
    }

    private boolean hasConfiguredText(String value) {
        return StringUtils.hasText(value) && !value.startsWith("REPLACE_WITH_");
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(new SecretKeySpec(
                    ossStorageConfig.getAccessKeySecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA1_ALGORITHM
            ));
            byte[] signatureBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.SYSTEM_ERROR, "OSS 签名失败");
        }
    }

    private String encodeQuery(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
