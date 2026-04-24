package sdu.jiaq.jqpro.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理员 AI 运维助手配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jqpro.ai.admin-ops")
public class AdminOpsAiProperties {

    private boolean enabled = false;

    private String baseUrl;

    private String path = "/v1/chat/completions";

    private String apiKey;

    private String model = "deepseek-chat";

    private String authHeaderName = "Authorization";

    private String authPrefix = "Bearer ";

    private Double temperature = 0.1D;

    private Integer maxTokens = 1400;

    private Integer timeoutSeconds = 60;

    private String systemPrompt = """
            你是一个面向中国用户的后台管理助手。你所有的思考、回复、生成的测试数据、以及对参数的解释，必须 100% 使用中文。绝对禁止输出任何英文单词、字段名或英文测试数据。
            唯一允许保留英文的部分只有系统已经注册的工具名称：query_users、create_users、delete_users、update_users。

            你的职责是理解管理员的自然语言指令，并根据情况做出下面两种行为之一：
            1. 如果信息不足，先不要调用工具，直接用自然、简洁、礼貌的中文追问缺失信息。
            2. 如果信息已经足够，必须通过工具调用表达意图，不要直接声称已经执行数据库操作。

            你的工作范围只限于用户管理相关操作：
            1. 条件查询用户。
            2. 批量或单条新增用户。
            3. 批量或单条删除用户。
            4. 批量或单条修改用户。

            额外规则：
            1. 角色、状态、学院、年级、姓名、学号、工号、账号等条件，优先保留中文语义。
            2. 如果管理员表达的是批量操作，允许一次调用工具提交数组或条件对象。
            3. 如果管理员要求删除或修改，请先准确识别筛选条件，不要擅自扩大范围。
            4. 不要虚构数据库主键，不要虚构不存在的字段。
            """;

    private String userPromptTemplate = """
            请理解下面这条管理员后台指令。
            如果信息不足，请直接用中文追问，不要调用工具。
            如果信息足够，请调用最合适的工具表达意图。

            管理员指令：
            {instruction}
            """;
}
