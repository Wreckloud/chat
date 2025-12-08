package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Description 用户实体类
 *
 * @author Wreckloud
 * @date 2025-12-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_user")
public class WfUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wfNo;

    private String username;

    private String passwordHash;

    private String salt;

    private String mobile;

    private String email;

    private String wxOpenid;

    private String wxUnionid;

    private String avatar;

    private Integer gender;

    private String signature;

    private Integer status;

    private String lastLoginIp;

    private LocalDateTime lastLoginTime;

    private LocalDateTime lastLogoutTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Boolean isDeleted;
}

