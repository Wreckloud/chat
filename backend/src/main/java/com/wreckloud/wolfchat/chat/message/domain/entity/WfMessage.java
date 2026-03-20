package com.wreckloud.wolfchat.chat.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 消息实体
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Data
@TableName("wf_message")
public class WfMessage {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话ID
     */
    private Long conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 客户端消息ID（用于幂等去重）
     */
    private String clientMsgId;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：TEXT-文本，IMAGE-图片，VIDEO-视频，FILE-文件
     */
    private MessageType msgType;

    /**
     * 媒体对象 Key
     */
    private String mediaKey;

    /**
     * 视频封面对象 Key
     */
    private String mediaPosterKey;

    /**
     * 媒体宽度
     */
    private Integer mediaWidth;

    /**
     * 媒体高度
     */
    private Integer mediaHeight;

    /**
     * 媒体大小
     */
    private Long mediaSize;

    /**
     * 媒体 MIME 类型
     */
    private String mediaMimeType;

    /**
     * 回复目标消息ID
     */
    private Long replyToMessageId;

    /**
     * 回复目标发送者ID
     */
    private Long replyToSenderId;

    /**
     * 回复预览文案
     */
    private String replyToPreview;

    /**
     * 接收者是否可见：1-可见，0-仅发送者可见
     */
    private Integer receiverVisible;

    /**
     * 是否已送达
     */
    private MessageDeliveryStatus delivered;

    /**
     * 送达时间
     */
    private LocalDateTime deliveredTime;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;
}

