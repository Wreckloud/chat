package com.wreckloud.wolfchat.common.storage.model;

import lombok.Data;

/**
 * @Description 媒体上传策略
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Data
public class MediaUploadPolicy {
    /**
     * 上传地址
     */
    private String host;

    /**
     * 占位字段（保留给前端上传表单）
     */
    private String accessKeyId;

    /**
     * 占位字段（保留给前端上传表单）
     */
    private String policy;

    /**
     * 上传签名（本地模式下作为一次性上传令牌）
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
