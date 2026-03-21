package com.wreckloud.wolfchat.ai.application.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wreckloud.wolfchat.ai.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI Responses API 文本客户端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiResponsesClient implements AiTextClient {
    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    private static final int DEFAULT_TIMEOUT_MS = 20_000;
    private static final String PROVIDER_OPENAI = "openai";
    private static final String PROVIDER_DEEPSEEK = "deepseek";

    private final AiConfig aiConfig;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public boolean isAvailable() {
        if (!Boolean.TRUE.equals(aiConfig.getEnabled())) {
            return false;
        }
        String provider = trimToEmpty(aiConfig.getProvider()).toLowerCase();
        if (!PROVIDER_OPENAI.equals(provider) && !PROVIDER_DEEPSEEK.equals(provider)) {
            return false;
        }
        return StringUtils.hasText(aiConfig.getApiKey())
                && StringUtils.hasText(aiConfig.getModel());
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return null;
        }
        String normalizedUserPrompt = normalizePrompt(userPrompt);
        if (!StringUtils.hasText(normalizedUserPrompt)) {
            return null;
        }
        String normalizedSystemPrompt = normalizePrompt(systemPrompt);
        String provider = trimToEmpty(aiConfig.getProvider()).toLowerCase();
        if (PROVIDER_DEEPSEEK.equals(provider)) {
            return completeByDeepSeekChat(normalizedSystemPrompt, normalizedUserPrompt);
        }
        return completeByOpenAiResponses(normalizedSystemPrompt, normalizedUserPrompt);
    }

    private String completeByOpenAiResponses(String systemPrompt, String userPrompt) {
        String endpoint = normalizeBaseUrl(aiConfig.getBaseUrl(), PROVIDER_OPENAI) + "/v1/responses";
        HttpRequest request = buildPostRequest(endpoint, buildResponsesRequestBody(systemPrompt, userPrompt));
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI 调用失败: status={}, body={}", response.statusCode(), response.body());
                return null;
            }
            return parseResponsesOutputText(response.body());
        } catch (Exception e) {
            log.warn("OpenAI 调用异常: {}", e.getMessage());
            return null;
        }
    }

    private String completeByDeepSeekChat(String systemPrompt, String userPrompt) {
        String endpoint = normalizeBaseUrl(aiConfig.getBaseUrl(), PROVIDER_DEEPSEEK) + "/chat/completions";
        HttpRequest request = buildPostRequest(endpoint, buildChatCompletionRequestBody(systemPrompt, userPrompt));
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DeepSeek 调用失败: status={}, body={}", response.statusCode(), response.body());
                return null;
            }
            return parseChatCompletionText(response.body());
        } catch (Exception e) {
            log.warn("DeepSeek 调用异常: {}", e.getMessage());
            return null;
        }
    }

    private HttpRequest buildPostRequest(String endpoint, JSONObject body) {
        Integer timeoutMs = aiConfig.getTimeoutMs() == null || aiConfig.getTimeoutMs() <= 0
                ? DEFAULT_TIMEOUT_MS
                : aiConfig.getTimeoutMs();
        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Authorization", "Bearer " + aiConfig.getApiKey().trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();
    }

    private JSONObject buildResponsesRequestBody(String systemPrompt, String userPrompt) {
        JSONObject body = new JSONObject();
        body.put("model", aiConfig.getModel().trim());
        body.put("temperature", aiConfig.getTemperature());
        body.put("max_output_tokens", aiConfig.getMaxOutputTokens());

        JSONArray input = new JSONArray();
        if (StringUtils.hasText(systemPrompt)) {
            input.add(buildInputItem("system", systemPrompt));
        }
        input.add(buildInputItem("user", userPrompt));
        body.put("input", input);
        return body;
    }

    private JSONObject buildChatCompletionRequestBody(String systemPrompt, String userPrompt) {
        JSONObject body = new JSONObject();
        body.put("model", aiConfig.getModel().trim());
        body.put("temperature", aiConfig.getTemperature());
        body.put("max_tokens", aiConfig.getMaxOutputTokens());

        JSONArray messages = new JSONArray();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(buildChatMessage("system", systemPrompt));
        }
        messages.add(buildChatMessage("user", userPrompt));
        body.put("messages", messages);
        return body;
    }

    private JSONObject buildInputItem(String role, String text) {
        JSONObject item = new JSONObject();
        item.put("role", role);
        JSONObject content = new JSONObject();
        content.put("type", "input_text");
        content.put("text", text);
        item.put("content", List.of(content));
        return item;
    }

    private JSONObject buildChatMessage(String role, String content) {
        JSONObject item = new JSONObject();
        item.put("role", role);
        item.put("content", content);
        return item;
    }

    private String parseResponsesOutputText(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(body);
            JSONArray output = root.getJSONArray("output");
            if (output == null || output.isEmpty()) {
                return null;
            }
            StringBuilder textBuilder = new StringBuilder();
            for (int i = 0; i < output.size(); i++) {
                JSONObject outputItem = output.getJSONObject(i);
                if (outputItem == null) {
                    continue;
                }
                JSONArray contentArray = outputItem.getJSONArray("content");
                if (contentArray == null || contentArray.isEmpty()) {
                    continue;
                }
                for (int j = 0; j < contentArray.size(); j++) {
                    JSONObject contentItem = contentArray.getJSONObject(j);
                    if (contentItem == null) {
                        continue;
                    }
                    if (!"output_text".equalsIgnoreCase(contentItem.getString("type"))) {
                        continue;
                    }
                    String text = contentItem.getString("text");
                    if (StringUtils.hasText(text)) {
                        if (textBuilder.length() > 0) {
                            textBuilder.append('\n');
                        }
                        textBuilder.append(text.trim());
                    }
                }
            }
            if (textBuilder.length() == 0) {
                return null;
            }
            return textBuilder.toString();
        } catch (Exception e) {
            log.warn("AI 响应解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String parseChatCompletionText(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(body);
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject firstChoice = choices.getJSONObject(0);
            if (firstChoice == null) {
                return null;
            }
            JSONObject message = firstChoice.getJSONObject("message");
            if (message == null) {
                return null;
            }
            String content = message.getString("content");
            if (!StringUtils.hasText(content)) {
                return null;
            }
            return content.trim();
        } catch (Exception e) {
            log.warn("DeepSeek 响应解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeBaseUrl(String configuredBaseUrl, String provider) {
        String baseUrl = StringUtils.hasText(configuredBaseUrl)
                ? configuredBaseUrl.trim()
                : defaultBaseUrl(provider);
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String defaultBaseUrl(String provider) {
        if (PROVIDER_DEEPSEEK.equalsIgnoreCase(provider)) {
            return DEFAULT_DEEPSEEK_BASE_URL;
        }
        return DEFAULT_OPENAI_BASE_URL;
    }

    private String normalizePrompt(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
