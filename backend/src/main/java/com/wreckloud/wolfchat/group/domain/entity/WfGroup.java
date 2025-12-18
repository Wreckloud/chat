package com.wreckloud.wolfchat.group.domain.entity;

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
 * @Description 群组实体类
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_group")
public class WfGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String groupAvatar;

    private String groupIntro;

    private Long ownerId;

    private Integer memberCount;

    private Integer maxMembers;

    private Boolean isAllMuted;

    private Boolean isNeedApproval;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Boolean isDeleted;
}

