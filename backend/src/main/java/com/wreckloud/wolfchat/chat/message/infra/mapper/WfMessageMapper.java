package com.wreckloud.wolfchat.chat.message.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * @Description 消息 Mapper
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Mapper
public interface WfMessageMapper extends BaseMapper<WfMessage> {
    @Select("SELECT id FROM wf_message WHERE conversation_id = #{conversationId} AND sender_id = #{senderId} ORDER BY id DESC LIMIT 1")
    Long selectLastMessageIdByConversationAndSender(@Param("conversationId") Long conversationId, @Param("senderId") Long senderId);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM wf_message",
            "WHERE conversation_id = #{conversationId}",
            "AND sender_id = #{senderId}",
            "AND receiver_visible = 1",
            "<if test='afterMessageId != null and afterMessageId &gt; 0'>",
            "AND id &gt; #{afterMessageId}",
            "</if>",
            "</script>"
    })
    Long countByConversationAndSenderAfterMessageId(@Param("conversationId") Long conversationId,
                                                    @Param("senderId") Long senderId,
                                                    @Param("afterMessageId") Long afterMessageId);

    @Select("SELECT * FROM wf_message " +
            "WHERE sender_id = #{senderId} " +
            "AND conversation_id = #{conversationId} " +
            "AND client_msg_id = #{clientMsgId} " +
            "LIMIT 1")
    WfMessage selectBySenderAndConversationAndClientMsgId(@Param("senderId") Long senderId,
                                                          @Param("conversationId") Long conversationId,
                                                          @Param("clientMsgId") String clientMsgId);

    @Select("SELECT COUNT(1) FROM wf_message " +
            "WHERE conversation_id = #{conversationId} " +
            "AND receiver_id = #{receiverId} " +
            "AND msg_type = 'SYSTEM' " +
            "AND content = #{content} " +
            "AND create_time &gt;= #{startTime}")
    Long countSystemNoticeSince(@Param("conversationId") Long conversationId,
                                @Param("receiverId") Long receiverId,
                                @Param("content") String content,
                                @Param("startTime") LocalDateTime startTime);
}

