package sdu.jiaq.jqpro.service.ai.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
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
import sdu.jiaq.jqpro.config.ai.AdminOpsAiProperties;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiClient;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiPlan;

/**
 * OpenAI-compatible client for administrator operation planning.
 */
@Slf4j
@Service
public class AdminOpsAiClientImpl implements AdminOpsAiClient {

    private final AdminOpsAiProperties properties;
    private final ObjectMapper objectMapper;

    public AdminOpsAiClientImpl(AdminOpsAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    @Override
    public AdminOpsAiPlan parseInstruction(String instruction) {
        validateConfiguration();

        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                properties.getModel(),
                List.of(
                        new ChatMessage("system", properties.getSystemPrompt()),
                        new ChatMessage("user", properties.getUserPromptTemplate().replace("{instruction}", instruction.trim()))
                ),
                properties.getTemperature(),
                properties.getMaxTokens(),
                new ResponseFormat("json_object")
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
            String content = extractContent(responseBody);
            return objectMapper.readValue(stripCodeFence(content), AdminOpsAiPlan.class);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.error("Administrator AI planning request timed out", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维解析超时，请稍后重试");
        } catch (RestClientException exception) {
            log.error("Administrator AI planning API invocation failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维解析失败，请检查模型服务配置");
        } catch (Exception exception) {
            log.error("Administrator AI planning response parsing failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维返回内容无法解析为执行计划");
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维未启用");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getPath())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getModel())
                || !StringUtils.hasText(properties.getAuthHeaderName())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维配置不完整");
        }
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
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维返回空响应");
        }

        JsonNode contentNode = responseBody.at("/choices/0/message/content");
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维未返回可解析内容");
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
            if (builder.length() > 0) {
                return builder.toString();
            }
        }
        throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维返回格式不受支持");
    }

    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private String normalizePath(String path) {
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private record ChatCompletionRequest(String model,
                                         List<ChatMessage> messages,
                                         Double temperature,
                                         @JsonProperty("max_tokens") Integer maxTokens,
                                         @JsonProperty("response_format") ResponseFormat responseFormat) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }
}
