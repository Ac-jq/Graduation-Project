package sdu.jiaq.jqpro.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 测评报告 AI 解读专用配置。
 * 采用 OpenAI 兼容协议，可通过配置切换到 DeepSeek 等兼容模型服务。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jqpro.ai.interpretation")
public class InterpretationAiProperties {

    private boolean enabled = false;

    private String baseUrl;

    private String path = "/v1/chat/completions";

    private String apiKey;

    private String model;

    private String authHeaderName = "Authorization";

    private String authPrefix = "Bearer ";

    private Double temperature = 0.4D;

    private Integer maxTokens = 600;

    private Integer timeoutSeconds = 60;

    /**
     * 系统提示词，用于约束输出风格与安全边界。
     */
    private String systemPrompt = """
            你是高校心理自助服务平台中的测评解读助手。
            你的任务是根据量表信息生成一段适合直接展示给学生的中文解释文本。
            你必须严格遵守以下要求：
            1. 语气温和、克制、支持性强，不制造恐慌。
            2. 只能做辅助解释，不得使用“确诊”“患病”“精神疾病”“抑郁症”“焦虑症”等诊断化表述。
            3. 先说明当前状态，再给出 2 到 3 条可执行建议。
            4. 建议必须符合高校心理支持场景，可建议联系辅导员、心理老师或校园支持资源，但不得代替危机处置流程。
            5. 只输出最终解释正文，不要输出标题、称呼、免责声明、markdown 或编号列表。
            6. 文本长度控制在 120 到 220 字之间。
            """;

    /**
     * 用户提示词模板，由业务服务替换占位符后发送给模型。
     */
    private String userPromptTemplate = """
            请基于以下测评结果生成解读正文。
            量表名称：{scaleName}
            量表简介：{scaleDescription}
            量表说明：{scaleIntroduction}
            总分：{totalScore}
            风险等级：{riskLevel}
            阈值信息：{thresholdSummary}
            """;
}
