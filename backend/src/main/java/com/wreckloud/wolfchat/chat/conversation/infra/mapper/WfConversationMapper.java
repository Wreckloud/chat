package com.wreckloud.wolfchat.chat.conversation.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 会话 Mapper
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Mapper
public interface WfConversationMapper extends BaseMapper<WfConversation> {
}

