package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Description 用户信息VO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "聊天号", example = "1000001")
    private Long wfNo;

    @Schema(description = "昵称", example = "用户1000001")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "性别：0未知 1男 2女", example = "1")
    private Integer gender;

    @Schema(description = "个性签名", example = "这只狼很懒，什么都没有留下")
    private String signature;

    @Schema(description = "状态：1正常 2禁用 3注销", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

