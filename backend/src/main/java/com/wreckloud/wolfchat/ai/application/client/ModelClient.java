package com.wreckloud.wolfchat.ai.application.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型调用抽象，面向“API 调用层”口径。
 */
public interface ModelClient {
    /**
     * 单段提示词调用。
     */
    String chat(String prompt);

    /**
     * 多消息上下文调用（返回原始文本结果）。
     */
    String chat(List<ModelMessage> messages);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ModelMessage {
        private String role;
        private String content;
    }
}
