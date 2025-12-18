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
 * @Description 群公告实体类
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_group_notice")
public class WfGroupNotice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long publisherId;

    private String title;

    private String content;

    private Boolean isPinned;

    private LocalDateTime publishTime;

    @TableLogic(value = "0", delval = "1")
    private Boolean isDeleted;
}

