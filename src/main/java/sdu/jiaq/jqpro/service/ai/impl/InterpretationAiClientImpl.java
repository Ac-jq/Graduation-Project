package sdu.jiaq.jqpro.service.ai.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import sdu.jiaq.jqpro.config.ai.InterpretationAiProperties;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;
import sdu.jiaq.jqpro.service.ai.InterpretationAiRequest;
import sdu.jiaq.jqpro.service.ai.ResourceRecommendationAiRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI-compatible interpretation client.
 */
@Slf4j
@Service
public class InterpretationAiClientImpl implements InterpretationAiClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final InterpretationAiProperties properties;

    public InterpretationAiClientImpl(InterpretationAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateInterpretation(InterpretationAiRequest request) {
        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                defaultText(properties.getModel(), "deepseek-chat"),
                List.of(
                        new ChatMessage("system", defaultText(properties.getSystemPrompt(), "你是一位心理学专家。")),
                        new ChatMessage("user", buildInterpretationPrompt(request))
                ),
                properties.getTemperature(),
                properties.getMaxTokens()
        );
        return executeForText(completionRequest);
    }

    @Override
    public List<Long> selectRecommendedResourceIds(ResourceRecommendationAiRequest request) {
        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                defaultText(properties.getModel(), "deepseek-chat"),
                List.of(
                        new ChatMessage("system", defaultText(properties.getRecommendationSystemPrompt(), "只输出资源 ID 的 JSON。")),
                        new ChatMessage("user", buildRecommendationPrompt(request))
                ),
                0.2D,
                400
        );
        String jsonText = executeForText(completionRequest);
        try {
            JsonNode root = OBJECT_MAPPER.readTree(jsonText);
            JsonNode idsNode = root.path("resourceIds");
            if (!idsNode.isArray()) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 资源推荐返回格式不正确");
            }
            return java.util.stream.StreamSupport.stream(idsNode.spliterator(), false)
                    .filter(JsonNode::isIntegralNumber)
                    .map(JsonNode::asLong)
                    .distinct()
                    .limit(3)
                    .toList();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("AI resource recommendation parse failed, raw={}", jsonText, exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 资源推荐返回内容无法解析");
        }
    }

    private String executeForText(ChatCompletionRequest completionRequest) {
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
            log.warn("Interpretation AI remote error: status={}, message={}", exception.getStatusCode(), message);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, message);
        } catch (ResourceAccessException exception) {
            log.error("Interpretation AI request timed out", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 服务请求超时，请稍后重试");
        } catch (RestClientException exception) {
            log.error("Interpretation AI invocation failed", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 服务调用失败：" + defaultText(exception.getMessage(), "未知错误"));
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

    private String buildInterpretationPrompt(InterpretationAiRequest request) {
        return defaultText(properties.getUserPromptTemplate(), "")
                .replace("{scaleName}", safeText(request.scaleName()))
                .replace("{scaleDescription}", safeText(request.scaleDescription()))
                .replace("{scaleIntroduction}", safeText(request.scaleIntroduction()))
                .replace("{totalScore}", request.totalScore() == null ? "未知" : request.totalScore().toString())
                .replace("{riskLevel}", safeText(request.riskLevel()))
                .replace("{thresholdSummary}", safeText(request.thresholdSummary()))
                .replace("{detailedAnswerContext}", safeText(request.detailedAnswerContext()));
    }

    private String buildRecommendationPrompt(ResourceRecommendationAiRequest request) {
        return """
                学生测评详细诊断结果：
                %s

                可供选择的资源清单：
                %s
                """.formatted(safeText(request.assessmentContext()), safeText(request.resourceCatalog()));
    }

    private String extractContent(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 服务返回空响应");
        }
        JsonNode contentNode = responseBody.at("/choices/0/message/content");
        String content = extractTextContent(contentNode);
        if (!StringUtils.hasText(content)) {
            JsonNode errorMessageNode = responseBody.at("/error/message");
            if (errorMessageNode.isTextual() && StringUtils.hasText(errorMessageNode.asText())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, errorMessageNode.asText().trim());
            }
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 服务返回空内容");
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
            return "AI 服务返回错误";
        }
        try {
            JsonNode body = OBJECT_MAPPER.readTree(responseBody);
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
}
