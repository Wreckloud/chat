package com.wreckloud.wolfchat.ai.application.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * ModelClient 适配器：复用现有 AiTextClient。
 */
@Service
@RequiredArgsConstructor
public class ModelClientAdapter implements ModelClient {
    private final AiTextClient aiTextClient;

    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return null;
        }
        return aiTextClient.complete(null, prompt.trim());
    }

    @Override
    public String chat(List<ModelMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        String systemPrompt = null;
        StringBuilder userPromptBuilder = new StringBuilder();
        for (ModelMessage message : messages) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = message.getRole() == null ? "" : message.getRole().trim().toLowerCase();
            if ("system".equals(role) && !StringUtils.hasText(systemPrompt)) {
                systemPrompt = message.getContent().trim();
                continue;
            }
            if (userPromptBuilder.length() > 0) {
                userPromptBuilder.append('\n');
            }
            userPromptBuilder
                    .append(role.isEmpty() ? "user" : role)
                    .append(": ")
                    .append(message.getContent().trim());
        }
        if (userPromptBuilder.length() == 0) {
            return null;
        }
        return aiTextClient.complete(systemPrompt, userPromptBuilder.toString());
    }
}
