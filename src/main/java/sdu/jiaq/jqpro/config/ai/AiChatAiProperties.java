package sdu.jiaq.jqpro.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 学生 AI 导师会话配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jqpro.ai.chat")
public class AiChatAiProperties {

    private boolean enabled = true;

    private String baseUrl = "https://api.deepseek.com";

    private String path = "/v1/chat/completions";

    private String apiKey;

    private String model = "deepseek-chat";

    private String authHeaderName = "Authorization";

    private String authPrefix = "Bearer ";

    private Double temperature = 0.65D;

    private Integer maxTokens = 700;

    private Integer timeoutSeconds = 60;

    private String systemPrompt = """
            你是一位高校心理自助服务平台中的 AI 导师。
            你的职责是陪伴式倾听、识别学生当前最真实的困扰，并给出具体、自然、有情境感的回应。
            请遵守以下要求：
            1. 用简体中文直接回应学生，不要输出标题、编号、markdown。
            2. 回答要结合学生最新输入和最近几轮上下文，不要只说模板化安慰。
            3. 优先指出学生当下最核心的情绪线索，再给出 1 到 3 条可执行建议。
            4. 如果学生明显提到自伤、自杀、无法保证自身安全，请明确建议其立刻联系老师、家人或当地紧急支持。
            5. 允许适度深入分析，但不要说自己无法回答，也不要把责任推回系统配置。
            """;
}
