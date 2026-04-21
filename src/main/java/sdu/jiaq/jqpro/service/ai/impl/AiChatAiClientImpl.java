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
import sdu.jiaq.jqpro.config.ai.AiChatAiProperties;
import sdu.jiaq.jqpro.service.ai.AiChatAiClient;
import sdu.jiaq.jqpro.service.ai.AiChatAiRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生 AI 导师客户端。
 * 本地演示优先保证页面闭环：远端错误会转成一条 AI 回复，而不是抛出 400 中断聊天。
 */
@Slf4j
@Service
public class AiChatAiClientImpl implements AiChatAiClient {

    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_PATH = "/v1/chat/completions";

    private final AiChatAiProperties properties;

    public AiChatAiClientImpl(AiChatAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateReply(AiChatAiRequest request) {
        String apiKey = defaultText(properties.getApiKey(), "").trim();
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Student AI mentor API key is empty. Return demo-visible configuration message.");
            return "AI 导师接口还没有读取到 DeepSeek API Key。请确认后端启动脚本已经加载 .local/deepseek.env.ps1，"
                    + "或在当前终端设置 JQPRO_AI_CHAT_API_KEY / JQPRO_AI_INTERPRETATION_API_KEY 后重启后端。";
        }

        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                defaultText(properties.getModel(), DEFAULT_MODEL),
                buildMessages(request),
                properties.getTemperature(),
                properties.getMaxTokens()
        );

        try {
            JsonNode responseBody = buildRestClient().post()
                    .uri(buildRequestUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(defaultText(properties.getAuthHeaderName(), "Authorization"), buildAuthorizationValue(apiKey))
                    .body(completionRequest)
                    .retrieve()
                    .body(JsonNode.class);
            return extractContent(responseBody);
        } catch (RestClientResponseException exception) {
            String message = extractRemoteErrorMessage(exception.getResponseBodyAsString());
            log.warn("Student AI mentor remote error: status={}, message={}", exception.getStatusCode(), message);
            return buildRemoteErrorReply(message);
        } catch (ResourceAccessException exception) {
            log.warn("Student AI mentor request timed out", exception);
            return "AI 导师请求 DeepSeek 超时。当前聊天记录已保存，请稍后再试，或检查本机网络到 api.deepseek.com 的连通性。";
        } catch (RestClientException exception) {
            log.warn("Student AI mentor API invocation failed", exception);
            return "AI 导师调用 DeepSeek 失败：" + defaultText(exception.getMessage(), "未知网络错误");
        } catch (Exception exception) {
            log.warn("Student AI mentor response parsing failed", exception);
            return "AI 导师返回内容解析失败：" + defaultText(exception.getMessage(), "未知解析错误");
        }
    }

    private List<ChatMessage> buildMessages(AiChatAiRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", resolveSystemPrompt()));
        messages.add(new ChatMessage("user", buildContextPrompt(request)));
        return messages;
    }

    private String buildContextPrompt(AiChatAiRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("会话标题：").append(safeText(request.sessionTitle())).append('\n');
        builder.append("当前风险等级：").append(safeText(request.riskLevel())).append('\n');
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
        builder.append("请直接以 AI 导师口吻自然回应，重点结合学生最新表达，不要输出系统配置说明。");
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
        return UriComponentsBuilder.fromUriString(defaultText(properties.getBaseUrl(), DEFAULT_BASE_URL).trim())
                .path(normalizePath(defaultText(properties.getPath(), DEFAULT_PATH)))
                .build(true)
                .toUri();
    }

    private String buildAuthorizationValue(String apiKey) {
        return defaultText(properties.getAuthPrefix(), "Bearer ") + apiKey;
    }

    private int resolveTimeoutSeconds() {
        Integer timeoutSeconds = properties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 60 : timeoutSeconds;
    }

    private String extractContent(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            return "AI 导师返回了空响应。请稍后再试。";
        }
        JsonNode contentNode = responseBody.at("/choices/0/message/content");
        String content = extractTextContent(contentNode);
        if (StringUtils.hasText(content)) {
            return content.trim();
        }
        JsonNode errorMessageNode = responseBody.at("/error/message");
        if (errorMessageNode.isTextual() && StringUtils.hasText(errorMessageNode.asText())) {
            return buildRemoteErrorReply(errorMessageNode.asText().trim());
        }
        return "AI 导师没有返回有效内容。请稍后再试。";
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
            return "远端没有返回错误详情";
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

    private String buildRemoteErrorReply(String remoteMessage) {
        String normalized = defaultText(remoteMessage, "未知远端错误");
        String lower = normalized.toLowerCase();
        if (lower.contains("insufficient") || normalized.contains("余额") || normalized.contains("quota")) {
            return "DeepSeek 接口返回余额或额度不足：" + normalized + "。当前学生消息已保存，请充值或更换可用 API Key 后再继续测试。";
        }
        if (lower.contains("auth") || lower.contains("unauthorized") || lower.contains("invalid api key")
                || normalized.contains("认证") || normalized.contains("鉴权")) {
            return "DeepSeek 接口鉴权失败：" + normalized + "。请检查本地 API Key 是否正确加载。";
        }
        return "DeepSeek 接口返回错误：" + normalized;
    }

    private String defaultSystemPrompt() {
        return """
                你是高校心理自助服务平台中的 AI 导师。
                请用简体中文回应学生，语气自然、具体、温和，优先结合学生最新输入和最近几轮上下文。
                不要输出 Markdown 标题，不要只说模板化安慰。
                如果学生提到自伤、自杀、无法保证安全，请明确建议立刻联系老师、家人或当地紧急支持。
                """;
    }

    private String resolveSystemPrompt() {
        String configuredPrompt = properties.getSystemPrompt();
        if (!StringUtils.hasText(configuredPrompt) || looksLikeMojibake(configuredPrompt)) {
            return defaultSystemPrompt();
        }
        return configuredPrompt;
    }

    private boolean looksLikeMojibake(String value) {
        return value.contains("浣犳槸") || value.contains("鐢ㄧ畝") || value.contains("瀵煎笀");
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
