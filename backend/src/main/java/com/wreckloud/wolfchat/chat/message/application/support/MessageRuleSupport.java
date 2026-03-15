package com.wreckloud.wolfchat.chat.message.application.support;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import org.springframework.util.StringUtils;

/**
 * 聊天消息规则工具
 */
public final class MessageRuleSupport {
    private static final int FILE_NAME_MAX_LENGTH = 120;
    private static final int MEDIA_CAPTION_MAX_LENGTH = 500;

    private MessageRuleSupport() {
    }

    public static MessageType normalizeMessageType(MessageType msgType) {
        return msgType == null ? MessageType.TEXT : msgType;
    }

    public static String normalizeContent(String content, MessageType msgType) {
        if (MessageType.TEXT.equals(msgType)) {
            if (!StringUtils.hasText(content)) {
                throw new BaseException(ErrorCode.MESSAGE_CONTENT_EMPTY);
            }
            return content.trim();
        }

        if (MessageType.FILE.equals(msgType)) {
            if (!StringUtils.hasText(content)) {
                throw new BaseException(ErrorCode.PARAM_ERROR, "文件名称不能为空");
            }
            String normalizedFileName = content.trim();
            if (normalizedFileName.length() > FILE_NAME_MAX_LENGTH) {
                throw new BaseException(ErrorCode.PARAM_ERROR, "文件名称不能超过 " + FILE_NAME_MAX_LENGTH + " 个字符");
            }
            return normalizedFileName;
        }

        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalized = content.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > MEDIA_CAPTION_MAX_LENGTH) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "图片说明不能超过 " + MEDIA_CAPTION_MAX_LENGTH + " 个字符");
        }
        return normalized;
    }

    public static void validatePageParams(Integer pageNum, Integer pageSize, int maxPageSize) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > maxPageSize) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
    }

    public static void validateNoMediaFields(
            String mediaKey,
            Integer mediaWidth,
            Integer mediaHeight,
            Long mediaSize,
            String mediaMimeType
    ) {
        if (StringUtils.hasText(mediaKey)
                || mediaWidth != null
                || mediaHeight != null
                || mediaSize != null
                || StringUtils.hasText(mediaMimeType)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "文本消息不支持媒体字段");
        }
    }
}
