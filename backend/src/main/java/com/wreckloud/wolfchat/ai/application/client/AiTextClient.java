package com.wreckloud.wolfchat.ai.application.client;

/**
 * 文本推理客户端。
 */
public interface AiTextClient {
    /**
     * 当前客户端是否可用（配置完整且开关开启）。
     */
    boolean isAvailable();

    /**
     * 生成文本回复，失败返回 null。
     */
    String complete(String systemPrompt, String userPrompt);
}

