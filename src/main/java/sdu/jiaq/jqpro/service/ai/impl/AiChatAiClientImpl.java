package sdu.jiaq.jqpro.service.ai.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import sdu.jiaq.jqpro.common.enums.ResultCode;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.config.ai.AiChatAiProperties;
import sdu.jiaq.jqpro.service.ai.AiChatAiClient;
import sdu.jiaq.jqpro.service.ai.AiChatAiRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI-compatible client for student AI mentor chat.
 */
@Slf4j
@Service
public class AiChatAiClientImpl implements AiChatAiClient {

    private final AiChatAiProperties properties;

    public AiChatAiClientImpl(AiChatAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateReply(AiChatAiRequest request) {
        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                defaultText(properties.getModel(), "deepseek-chat"),
                buildMessages(request),
                properties.getTemperature(),
                properties.getMaxTokens()
        );

        try {
            JsonNode responseBody = buildRestClient().post()
                    .uri(buildRequestUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(defaultText(properties.getAuthHeaderName(), "Authorization"), buildAuthorizationValue())
                    .body(completionRequest)
                    .retrieve()
                    .body(JsonNode.class);
            return extractContent(responseBody);
        } catch (RestClientResponseException exception) {
            String message = extractRemoteErrorMessage(exception.getResponseBodyAsString());
            log.warn("Student AI mentor remote error: status={}, message={}", exception.getStatusCode(), message);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, message);
        } catch (ResourceAccessException exception) {
            log.error("Student AI mentor request timed out", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师请求超时，请稍后重试");
        } catch (RestClientException exception) {
            log.error("Student AI mentor API invocation failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师调用失败：" + defaultText(exception.getMessage(), "未知错误"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Student AI mentor response parsing failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师返回内容无法解析：" + defaultText(exception.getMessage(), "未知错误"));
        }
    }

    private List<ChatMessage> buildMessages(AiChatAiRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", defaultText(properties.getSystemPrompt(), "你是一位高校心理支持 AI 导师。")));
        messages.add(new ChatMessage("user", buildContextPrompt(request)));
        return messages;
    }

    private String buildContextPrompt(AiChatAiRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("会话标题：").append(safeText(request.sessionTitle())).append('\n');
        builder.append("风险等级：").append(safeText(request.riskLevel())).append('\n');
        builder.append("是否命中高风险关键词：").append(request.riskFlag() ? "是" : "否").append('\n');
        builder.append("最近对话：\n");
        if (request.historyMessages() == null || request.historyMessages().isEmpty()) {
            builder.append("暂无历史消息。\n");
        } else {
            for (AiChatAiRequest.ConversationMessage historyMessage : request.historyMessages()) {
                builder.append(historyMessage.role()).append("：").append(safeText(historyMessage.content())).append('\n');
            }
        }
        builder.append("学生刚刚发送：").append(safeText(request.latestStudentMessage())).append('\n');
        builder.append("请直接以 AI 导师口吻自然回应。");
        return builder.toString();
    }

    private RestClient buildRestClient() {
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private URI buildRequestUri() {
        return UriComponentsBuilder.fromUriString(defaultText(properties.getBaseUrl(), "https://api.deepseek.com").trim())
                .path(normalizePath(defaultText(properties.getPath(), "/v1/chat/completions")))
                .build(true)
                .toUri();
    }

    private String buildAuthorizationValue() {
        return defaultText(properties.getAuthPrefix(), "Bearer ") + defaultText(properties.getApiKey(), "").trim();
    }

    private int resolveTimeoutSeconds() {
        Integer timeoutSeconds = properties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 60 : timeoutSeconds;
    }

    private String extractContent(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师返回空响应");
        }
        JsonNode contentNode = responseBody.at("/choices/0/message/content");
        String content = extractTextContent(contentNode);
        if (!StringUtils.hasText(content)) {
            JsonNode errorMessageNode = responseBody.at("/error/message");
            if (errorMessageNode.isTextual() && StringUtils.hasText(errorMessageNode.asText())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, errorMessageNode.asText().trim());
            }
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师返回空内容");
        }
        return content.trim();
    }

    private String extractTextContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode itemNode : contentNode) {
                JsonNode textNode = itemNode.path("text");
                if (textNode.isTextual() && StringUtils.hasText(textNode.asText())) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
        return null;
    }

    private String extractRemoteErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "AI 导师服务返回错误";
        }
        try {
            JsonNode body = JacksonHolder.MAPPER.readTree(responseBody);
            JsonNode messageNode = body.at("/error/message");
            if (messageNode.isTextual() && StringUtils.hasText(messageNode.asText())) {
                return messageNode.asText().trim();
            }
            JsonNode detailNode = body.path("message");
            if (detailNode.isTextual() && StringUtils.hasText(detailNode.asText())) {
                return detailNode.asText().trim();
            }
        } catch (Exception ignored) {
        }
        return responseBody.trim();
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private record ChatCompletionRequest(String model,
                                         List<ChatMessage> messages,
                                         Double temperature,
                                         @JsonProperty("max_tokens") Integer maxTokens) {
    }

    private record ChatMessage(String role, String content) {
    }

    private static final class JacksonHolder {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
