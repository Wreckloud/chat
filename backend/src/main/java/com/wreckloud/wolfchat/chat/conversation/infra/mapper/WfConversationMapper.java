package com.wreckloud.wolfchat.chat.conversation.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * @Description 会话 Mapper
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Mapper
public interface WfConversationMapper extends BaseMapper<WfConversation> {
    @Select("SELECT COALESCE(SUM(" +
            "CASE " +
            "WHEN user_a_id = #{userId} THEN user_a_unread_count " +
            "WHEN user_b_id = #{userId} THEN user_b_unread_count " +
            "ELSE 0 END), 0) " +
            "FROM wf_conversation " +
            "WHERE user_a_id = #{userId} OR user_b_id = #{userId}")
    Long selectUnreadTotalByUserId(@Param("userId") Long userId);

    @Update("UPDATE wf_conversation " +
            "SET last_message_id = #{messageId}, " +
            "    last_message = #{lastMessage}, " +
            "    last_message_time = #{lastMessageTime} " +
            "WHERE id = #{conversationId} " +
            "  AND (last_message_time IS NULL " +
            "       OR last_message_time < #{lastMessageTime} " +
            "       OR (last_message_time = #{lastMessageTime} " +
            "           AND (last_message_id IS NULL OR last_message_id <= #{messageId})))")
    int updateLastMessageIfNewer(@Param("conversationId") Long conversationId,
                                 @Param("messageId") Long messageId,
                                 @Param("lastMessage") String lastMessage,
                                 @Param("lastMessageTime") LocalDateTime lastMessageTime);
}

