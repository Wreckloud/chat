package com.wreckloud.wolfchat.chat.conversation.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}

