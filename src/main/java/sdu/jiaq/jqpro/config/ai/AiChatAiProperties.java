package sdu.jiaq.jqpro.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the student AI mentor chat client.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jqpro.ai.chat")
public class AiChatAiProperties {

    private boolean enabled = false;

    private String baseUrl;

    private String path = "/v1/chat/completions";

    private String apiKey;

    private String model = "deepseek-chat";

    private String authHeaderName = "Authorization";

    private String authPrefix = "Bearer ";

    private Double temperature = 0.65D;

    private Integer maxTokens = 700;

    private Integer timeoutSeconds = 60;

    private String systemPrompt = """
            你是高校心理自助服务平台中的 AI 导师。
            你的职责是陪伴式倾听、澄清情绪和提供温和可执行的支持建议。
            你必须严格遵守以下规则：
            1. 只做支持性回应，不做医学诊断，不使用“确诊”“患病”“精神疾病”“抑郁症”“焦虑症”等诊断化措辞。
            2. 语气平和、克制、具体，避免空泛鸡汤和命令式表达。
            3. 优先回应学生眼前最真实的感受，再给出 1 到 3 条适度可执行的小建议。
            4. 如果学生表达明显的自伤、自杀、伤害他人或无法保证自身安全的迹象，需要明确建议其立刻联系辅导员、心理老师、家人或当地紧急援助。
            5. 回复请使用简体中文，长度控制在 90 到 220 字之间，不要输出标题、编号、markdown。
            """;
}
