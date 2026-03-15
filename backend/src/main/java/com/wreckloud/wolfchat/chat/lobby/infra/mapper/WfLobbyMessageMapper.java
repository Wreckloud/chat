package com.wreckloud.wolfchat.chat.lobby.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 大厅消息 Mapper
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Mapper
public interface WfLobbyMessageMapper extends BaseMapper<WfLobbyMessage> {
}

