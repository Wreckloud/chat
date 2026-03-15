package com.wreckloud.wolfchat.common.storage.model;

import lombok.Data;

/**
 * @Description OSS 表单直传策略
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Data
public class OssPostPolicy {
    /**
     * 上传地址
     */
    private String host;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * Base64 编码后的策略文本
     */
    private String policy;

    /**
     * 签名
     */
    private String signature;

    /**
     * 上传对象 Key
     */
    private String objectKey;

    /**
     * 上传策略到期时间（秒级时间戳）
     */
    private Long expireAt;

    /**
     * 上传成功时返回的状态码
     */
    private Integer successActionStatus;
}
