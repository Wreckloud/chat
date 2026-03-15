package com.wreckloud.wolfchat.chat.lobby.api.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 大厅消息转换器
 * @Author Wreckloud
 * @Date 2026-03-12
 */
public final class LobbyMessageConverter {
    private LobbyMessageConverter() {
    }

    public static LobbyMessageVO toLobbyMessageVO(WfLobbyMessage message, WfUser sender) {
        if (message == null) {
            return null;
        }
        LobbyMessageVO vo = new LobbyMessageVO();
        vo.setMessageId(message.getId());
        vo.setSenderId(message.getSenderId());
        if (sender != null) {
            vo.setSenderWolfNo(sender.getWolfNo());
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setMediaKey(message.getMediaKey());
        vo.setMediaWidth(message.getMediaWidth());
        vo.setMediaHeight(message.getMediaHeight());
        vo.setMediaSize(message.getMediaSize());
        vo.setMediaMimeType(message.getMediaMimeType());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    public static List<LobbyMessageVO> toLobbyMessageVOList(List<WfLobbyMessage> source, Map<Long, WfUser> senderMap) {
        return source.stream()
                .map(item -> toLobbyMessageVO(item, senderMap.get(item.getSenderId())))
                .collect(Collectors.toList());
    }

    public static Page<LobbyMessageVO> toLobbyMessageVOPage(Page<WfLobbyMessage> source, Map<Long, WfUser> senderMap) {
        Page<LobbyMessageVO> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(toLobbyMessageVOList(source.getRecords(), senderMap));
        return target;
    }
}

