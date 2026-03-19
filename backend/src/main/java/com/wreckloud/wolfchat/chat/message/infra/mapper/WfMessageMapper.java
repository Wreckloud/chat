package com.wreckloud.wolfchat.chat.message.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Description 消息 Mapper
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Mapper
public interface WfMessageMapper extends BaseMapper<WfMessage> {
    @Select("SELECT COUNT(1) FROM wf_message WHERE conversation_id = #{conversationId} AND sender_id = #{senderId}")
    Long countByConversationAndSender(@Param("conversationId") Long conversationId, @Param("senderId") Long senderId);

    @Select("SELECT EXISTS(SELECT 1 FROM wf_message WHERE conversation_id = #{conversationId} AND sender_id = #{senderId} LIMIT 1)")
    Integer existsByConversationAndSender(@Param("conversationId") Long conversationId, @Param("senderId") Long senderId);
}

