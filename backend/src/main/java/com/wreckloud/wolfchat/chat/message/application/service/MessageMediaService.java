package com.wreckloud.wolfchat.chat.message.application.service;

import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.common.storage.service.OssStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @Description 消息媒体组装服务
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Service
@RequiredArgsConstructor
public class MessageMediaService {
    private static final String IMAGE_PREVIEW_TEXT = "[图片]";
    private static final String VIDEO_PREVIEW_TEXT = "[视频]";
    private static final String FILE_PREVIEW_TEXT = "[文件]";
    private static final String VIDEO_POSTER_PROCESS = "video/snapshot,t_1000,f_jpg,w_480,m_fast";
    private static final int REPLY_PREVIEW_MAX_LENGTH = 80;

    private final OssStorageService ossStorageService;

    /**
     * 生成会话列表里的最近消息预览
     */
    public String buildConversationPreview(MessageType msgType, String content) {
        if (MessageType.IMAGE.equals(msgType)) {
            return IMAGE_PREVIEW_TEXT;
        }
        if (MessageType.VIDEO.equals(msgType)) {
            return VIDEO_PREVIEW_TEXT;
        }
        if (MessageType.FILE.equals(msgType)) {
            return FILE_PREVIEW_TEXT;
        }
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) : content;
    }

    /**
     * 生成引用回复的预览文案
     */
    public String buildReplyPreview(MessageType msgType, String content) {
        String preview = buildConversationPreview(msgType, content);
        if (!StringUtils.hasText(preview)) {
            return "";
        }
        return preview.length() > REPLY_PREVIEW_MAX_LENGTH
                ? preview.substring(0, REPLY_PREVIEW_MAX_LENGTH)
                : preview;
    }

    /**
     * 为图片消息补齐签名访问地址
     */
    public MessageVO fillMedia(MessageVO vo) {
        if (vo == null || !StringUtils.hasText(vo.getMediaKey())) {
            return vo;
        }
        MessageType msgType = vo.getMsgType();
        if (!MessageType.IMAGE.equals(msgType)
                && !MessageType.VIDEO.equals(msgType)
                && !MessageType.FILE.equals(msgType)) {
            return vo;
        }
        vo.setMediaUrl(ossStorageService.buildSignedReadUrl(vo.getMediaKey()));
        if (MessageType.VIDEO.equals(msgType)) {
            vo.setMediaPosterUrl(ossStorageService.buildSignedReadUrl(vo.getMediaKey(), VIDEO_POSTER_PROCESS));
        }
        return vo;
    }

    public List<MessageVO> fillMedia(List<MessageVO> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        for (MessageVO item : list) {
            fillMedia(item);
        }
        return list;
    }
}
