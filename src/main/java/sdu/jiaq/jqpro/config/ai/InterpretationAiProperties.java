package sdu.jiaq.jqpro.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 测评报告 AI 解读配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jqpro.ai.interpretation")
public class InterpretationAiProperties {

    private boolean enabled = true;

    private String baseUrl = "https://api.deepseek.com";

    private String path = "/v1/chat/completions";

    private String apiKey;

    private String model = "deepseek-chat";

    private String authHeaderName = "Authorization";

    private String authPrefix = "Bearer ";

    private Double temperature = 0.45D;

    private Integer maxTokens = 900;

    private Integer timeoutSeconds = 60;

    private String systemPrompt = """
            你是一位资深心理学专家，同时熟悉高校心理支持场景。
            你的任务是根据学生这次测评的量表信息、总分、风险等级，以及逐题答题细节，生成一段深度、个性化、具体的心理剖析。
            请严格遵守：
            1. 不要只复述总分和风险等级，要结合题目和作答细节指出学生具体表现在哪些方面存在明显波动或困扰。
            2. 语言自然、专业、有人味，避免模板化套话。
            3. 允许做深度分析，但不要写成“无法判断”“无法生成”这类推脱式表述。
            4. 输出使用简体中文，不要输出标题、markdown 或编号。
            """;

    private String userPromptTemplate = """
            请根据以下测评上下文，生成定制化心理剖析：
            量表名称：{scaleName}
            量表简介：{scaleDescription}
            量表说明：{scaleIntroduction}
            总分：{totalScore}
            风险等级：{riskLevel}
            阈值信息：{thresholdSummary}
            详细答题记录：
            {detailedAnswerContext}
            """;

    private String recommendationSystemPrompt = """
            你是一位高校心理支持平台的资源推荐助手。
            你会收到学生的测评详细诊断结果，以及一批可供选择的心理资源摘要。
            你的任务是从提供的资源中选出最适合当前学生的 3 个资源 ID。
            输出要求：
            1. 只输出 JSON。
            2. JSON 格式固定为：{"resourceIds":[1,2,3]}
            3. 只能从给定资源列表里挑选 ID，不能虚构 ID。
            4. 优先挑选与学生当前困扰最贴近、最适合高校场景的资源。
            """;
}
