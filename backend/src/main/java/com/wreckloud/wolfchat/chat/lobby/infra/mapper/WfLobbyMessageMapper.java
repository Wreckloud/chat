package com.wreckloud.wolfchat.chat.lobby.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 大厅消息 Mapper
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Mapper
public interface WfLobbyMessageMapper extends BaseMapper<WfLobbyMessage> {
    @Select("SELECT * FROM wf_lobby_message WHERE sender_id = #{senderId} AND client_msg_id = #{clientMsgId} LIMIT 1")
    WfLobbyMessage selectBySenderAndClientMsgId(@Param("senderId") Long senderId, @Param("clientMsgId") String clientMsgId);
}
