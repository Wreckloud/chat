package com.wreckloud.wolfchat.common.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description OSS 存储配置
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.oss")
public class OssStorageConfig {
    /**
     * OSS Endpoint，例如：oss-cn-chengdu.aliyuncs.com
     */
    private String endpoint;

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * RAM AccessKey ID
     */
    private String accessKeyId;

    /**
     * RAM AccessKey Secret
     */
    private String accessKeySecret;

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
}
