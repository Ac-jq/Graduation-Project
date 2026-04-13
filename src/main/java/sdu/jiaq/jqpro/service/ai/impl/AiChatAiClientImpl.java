package sdu.jiaq.jqpro.service.ai.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import sdu.jiaq.jqpro.common.enums.ResultCode;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.config.ai.AiChatAiProperties;
import sdu.jiaq.jqpro.service.ai.AiChatAiClient;
import sdu.jiaq.jqpro.service.ai.AiChatAiRequest;

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
        validateConfiguration();

        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                properties.getModel(),
                buildMessages(request),
                properties.getTemperature(),
                properties.getMaxTokens()
        );

        try {
            RestClient restClient = buildRestClient();
            JsonNode responseBody = restClient.post()
                    .uri(buildRequestUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(properties.getAuthHeaderName(), buildAuthorizationValue())
                    .body(completionRequest)
                    .retrieve()
                    .body(JsonNode.class);
            return extractContent(responseBody);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.error("Student AI mentor request timed out", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师暂时不可用，请稍后再试");
        } catch (RestClientException exception) {
            log.error("Student AI mentor API invocation failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师响应失败，请稍后再试");
        } catch (Exception exception) {
            log.error("Student AI mentor response parsing failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师返回内容无法解析");
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师服务未启用，请联系管理员配置");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getPath())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getModel())
                || !StringUtils.hasText(properties.getAuthHeaderName())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 导师服务配置不完整");
        }
    }

    private List<ChatMessage> buildMessages(AiChatAiRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", properties.getSystemPrompt()));
        messages.add(new ChatMessage("user", buildContextPrompt(request)));
        return messages;
    }

    private String buildContextPrompt(AiChatAiRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("会话标题：").append(safeText(request.sessionTitle())).append('\n');
        builder.append("风险等级：").append(safeText(request.riskLevel())).append('\n');
        builder.append("是否命中风险关键词：").append(request.riskFlag() ? "是" : "否").append('\n');
        builder.append("最近对话：\n");
        if (request.historyMessages() == null || request.historyMessages().isEmpty()) {
            builder.append("暂无历史消息。\n");
        } else {
            for (AiChatAiRequest.ConversationMessage historyMessage : request.historyMessages()) {
                builder.append(historyMessage.role()).append("：").append(safeText(historyMessage.content())).append('\n');
            }
        }
        builder.append("学生刚刚发送：").append(safeText(request.latestStudentMessage())).append('\n');
        builder.append("请直接以 AI 导师身份回应学生。");
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
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl().trim())
                .path(normalizePath(properties.getPath()))
                .build(true)
                .toUri();
    }

    private String buildAuthorizationValue() {
        String prefix = properties.getAuthPrefix() == null ? "" : properties.getAuthPrefix();
        return prefix + properties.getApiKey().trim();
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

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }

    private String normalizePath(String path) {
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private record ChatCompletionRequest(String model,
                                         List<ChatMessage> messages,
                                         Double temperature,
                                         @JsonProperty("max_tokens") Integer maxTokens) {
    }

    private record ChatMessage(String role, String content) {
    }
}
