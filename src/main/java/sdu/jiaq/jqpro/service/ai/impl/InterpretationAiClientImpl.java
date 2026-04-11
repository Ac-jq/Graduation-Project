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
import org.springframework.web.util.UriComponentsBuilder;
import sdu.jiaq.jqpro.common.enums.ResultCode;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.config.ai.InterpretationAiProperties;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;
import sdu.jiaq.jqpro.service.ai.InterpretationAiRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI 兼容协议客户端。
 */
@Slf4j
@Service
public class InterpretationAiClientImpl implements InterpretationAiClient {

    private final InterpretationAiProperties properties;

    public InterpretationAiClientImpl(InterpretationAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateInterpretation(InterpretationAiRequest request) {
        validateConfiguration();

        RestClient restClient = buildRestClient();
        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                properties.getModel(),
                List.of(
                        new ChatMessage("system", properties.getSystemPrompt()),
                        new ChatMessage("user", buildUserPrompt(request))
                ),
                properties.getTemperature(),
                properties.getMaxTokens()
        );

        try {
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
            log.error("AI interpretation request timed out or network is unavailable", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读生成失败，请稍后重试");
        } catch (RestClientException exception) {
            log.error("AI interpretation API invocation failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读生成失败，请稍后重试");
        } catch (Exception exception) {
            log.error("AI interpretation response parsing failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务返回格式无法解析，请检查模型接口兼容性");
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务未启用，请联系管理员配置");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务未配置完整：缺少 base-url");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务未配置完整：缺少 api-key");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务未配置完整：缺少 model");
        }
        if (!StringUtils.hasText(properties.getPath())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务未配置完整：缺少 path");
        }
        if (!StringUtils.hasText(properties.getAuthHeaderName())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务未配置完整：缺少 auth-header-name");
        }
    }

    private RestClient buildRestClient() {
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
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
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return 60;
        }
        return timeoutSeconds;
    }

    private String buildUserPrompt(InterpretationAiRequest request) {
        return properties.getUserPromptTemplate()
                .replace("{scaleName}", safeText(request.scaleName()))
                .replace("{scaleDescription}", safeText(request.scaleDescription()))
                .replace("{scaleIntroduction}", safeText(request.scaleIntroduction()))
                .replace("{totalScore}", request.totalScore() == null ? "未知" : request.totalScore().toString())
                .replace("{riskLevel}", safeText(request.riskLevel()))
                .replace("{thresholdSummary}", safeText(request.thresholdSummary()));
    }

    private String extractContent(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务返回格式无法解析，请检查模型接口兼容性");
        }

        JsonNode contentNode = responseBody.at("/choices/0/message/content");
        String content = extractTextContent(contentNode);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI解读服务返回空内容，请检查模型配置");
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
