package com.wreckloud.wolfchat.ai.application.client;

/**
 * 文本推理客户端（底层实现接口）。
 * <p>
 * 对论文/架构口径中的 {@link ModelClient} 提供基础能力支撑。
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

