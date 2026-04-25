package sdu.jiaq.jqpro.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import sdu.jiaq.jqpro.common.constant.AdminAiTaskConstants;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.enums.ResultCode;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.config.ai.AdminOpsAiProperties;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiAction;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiChatResponse;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiClient;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiConversationMessage;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiPlan;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiToolCall;

/**
 * 基于 Spring AI + DeepSeek(OpenAI 兼容协议) 的管理员 AI 客户端。
 */
@Slf4j
@Service
public class AdminOpsAiClientImpl implements AdminOpsAiClient {

    private static final String TOOL_QUERY_USERS = "query_users";
    private static final String TOOL_CREATE_USERS = "create_users";
    private static final String TOOL_DELETE_USERS = "delete_users";
    private static final String TOOL_UPDATE_USERS = "update_users";

    private static final String QUERY_USERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "条件": {
                  "type": "object",
                  "description": "用户筛选条件字典，可包含角色、账号、显示名、真实姓名、学号、工号、学院、年级、状态等中文条件"
                }
              },
              "required": ["条件"],
              "additionalProperties": false
            }
            """;

    private static final String CREATE_USERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "用户数组": {
                  "type": "array",
                  "description": "待新增的用户数组",
                  "items": {
                    "type": "object",
                    "properties": {
                      "角色": { "type": "string" },
                      "账号": { "type": "string" },
                      "显示名": { "type": "string" },
                      "真实姓名": { "type": "string" },
                      "学号": { "type": "string" },
                      "工号": { "type": "string" },
                      "学院": { "type": "string" },
                      "年级": { "type": "string" },
                      "状态": { "type": "string" }
                    },
                    "additionalProperties": true
                  }
                }
              },
              "required": ["用户数组"],
              "additionalProperties": false
            }
            """;

    private static final String DELETE_USERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "条件": {
                  "type": "object",
                  "description": "删除目标的筛选条件字典，可包含角色、姓名、学号、工号、学院、年级、状态等条件"
                }
              },
              "required": ["条件"],
              "additionalProperties": false
            }
            """;

    private static final String UPDATE_USERS_SCHEMA = """
            {
              "type": "object",
              "description": "修改用户信息。优先提取平铺参数，不要遗漏目标定位与新值。示例一：把庄文轩的学院改成法学院 -> target_name=庄文轩, new_college=法学院。示例二：把学号20233071的年级改成2025 -> target_student_no=20233071, new_grade=2025。只有当目标条件和至少一个新值明确时才调用本工具。",
              "properties": {
                "target_account": { "type": "string", "description": "目标账号" },
                "target_display_name": { "type": "string", "description": "目标显示名" },
                "target_name": { "type": "string", "description": "目标真实姓名或常用姓名" },
                "target_student_no": { "type": "string", "description": "目标学号" },
                "target_counselor_no": { "type": "string", "description": "目标工号" },
                "target_college": { "type": "string", "description": "目标学院" },
                "target_grade": { "type": "string", "description": "目标年级" },
                "target_role": { "type": "string", "description": "目标角色，如学生、咨询师、管理员" },
                "target_status": { "type": "string", "description": "目标当前状态，如启用、禁用" },
                "new_account": { "type": "string", "description": "新账号" },
                "new_display_name": { "type": "string", "description": "新的显示名" },
                "new_real_name": { "type": "string", "description": "新的真实姓名" },
                "new_student_no": { "type": "string", "description": "新的学号" },
                "new_counselor_no": { "type": "string", "description": "新的工号" },
                "new_college": { "type": "string", "description": "新的学院" },
                "new_grade": { "type": "string", "description": "新的年级" },
                "new_status": { "type": "string", "description": "新的状态，如启用、禁用" },
                "new_role": { "type": "string", "description": "新的角色" },
                "目标条件": {
                  "type": "object",
                  "description": "兼容旧版嵌套结构。把用户想要修改的目标条件放入这里，例如账号、姓名、学号、工号、学院、年级、角色、状态。"
                },
                "修改内容": {
                  "type": "object",
                  "description": "兼容旧版嵌套结构。把用户想要改成的新值放入这里，例如新学院、新年级、新状态、新显示名。"
                }
              },
              "additionalProperties": false
            }
            """;

    private static final String TOOL_QUERY_STUDENT = "query_student";
    private static final String TOOL_CREATE_STUDENT = "create_student";
    private static final String TOOL_DELETE_STUDENT = "delete_student";
    private static final String TOOL_UPDATE_STUDENT = "update_student";
    private static final String TOOL_CREATE_USER = "create_user";

    private static final String STATELESS_QUERY_USERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "account": { "type": "string" },
                "display_name": { "type": "string" },
                "name": { "type": "string" },
                "student_no": { "type": "string" },
                "college": { "type": "string" },
                "grade": { "type": "string" },
                "role_code": { "type": "string" },
                "status": { "type": "string" }
              },
              "additionalProperties": false
            }
            """;

    private static final String STATELESS_CREATE_USER_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "role_code": { "type": "string" },
                "account": { "type": "string" },
                "display_name": { "type": "string" },
                "real_name": { "type": "string" },
                "student_no": { "type": "string" },
                "counselor_no": { "type": "string" },
                "college": { "type": "string" },
                "grade": { "type": "string" },
                "phone": { "type": "string" }
              },
              "additionalProperties": false
            }
            """;

    private static final String STATELESS_DELETE_USERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "account": { "type": "string" },
                "name": { "type": "string" },
                "student_no": { "type": "string" },
                "college": { "type": "string" },
                "grade": { "type": "string" },
                "role_code": { "type": "string" },
                "status": { "type": "string" }
              },
              "additionalProperties": false
            }
            """;

    private static final String STATELESS_UPDATE_USERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "target_account": { "type": "string" },
                "target_display_name": { "type": "string" },
                "target_name": { "type": "string" },
                "target_student_no": { "type": "string" },
                "target_college": { "type": "string" },
                "target_grade": { "type": "string" },
                "new_account": { "type": "string" },
                "new_display_name": { "type": "string" },
                "new_real_name": { "type": "string" },
                "new_student_no": { "type": "string" },
                "new_college": { "type": "string" },
                "new_grade": { "type": "string" },
                "new_status": { "type": "string" }
              },
              "additionalProperties": false
            }
            """;

    private static final String QUERY_STUDENT_SCHEMA = """
            {
              "type": "object",
              "description": "查询学生。所有参数都是平铺的 String 类型，没有就传 null，绝对不要套额外的 JSON 对象。",
              "properties": {
                "name": { "type": "string", "description": "学生姓名，可为空" },
                "student_no": { "type": "string", "description": "学生学号，可为空" },
                "college": { "type": "string", "description": "学生学院，可为空" }
              },
              "additionalProperties": false
            }
            """;

    private static final String CREATE_STUDENT_SCHEMA = """
            {
              "type": "object",
              "description": "新增学生。所有参数都是平铺的 String 类型，没有就传 null，绝对不要套额外的 JSON 对象。",
              "properties": {
                "name": { "type": "string", "description": "学生姓名" },
                "student_no": { "type": "string", "description": "学生学号" },
                "college": { "type": "string", "description": "学院" },
                "grade": { "type": "string", "description": "年级" }
              },
              "additionalProperties": false
            }
            """;

    private static final String DELETE_STUDENT_SCHEMA = """
            {
              "type": "object",
              "description": "删除学生。所有参数都是平铺的 String 类型，没有就传 null，绝对不要套额外的 JSON 对象。",
              "properties": {
                "name": { "type": "string", "description": "学生姓名，可为空" },
                "student_no": { "type": "string", "description": "学生学号，可为空" },
                "account": { "type": "string", "description": "学生账号，可为空" }
              },
              "additionalProperties": false
            }
            """;

    private static final String UPDATE_STUDENT_SCHEMA = """
            {
              "type": "object",
              "description": "修改学生信息。所有参数都是平铺的 String 类型，没有就传 null，绝对不要套额外的 JSON 对象。",
              "properties": {
                "target_name": { "type": "string", "description": "目标学生姓名，可为空" },
                "target_student_no": { "type": "string", "description": "目标学生学号，可为空" },
                "new_college": { "type": "string", "description": "新的学院，可为空" },
                "new_grade": { "type": "string", "description": "新的年级，可为空" },
                "new_name": { "type": "string", "description": "新的姓名，可为空" }
              },
              "additionalProperties": false
            }
            """;

    private final AdminOpsAiProperties properties;
    private final ObjectMapper objectMapper;
    private final OpenAiChatModel chatModel;

    public AdminOpsAiClientImpl(AdminOpsAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.chatModel = buildChatModel();
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getPath())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    @Override
    public AdminOpsAiPlan parseInstruction(String instruction) {
        String normalizedInstruction = normalizeText(instruction);
        if (!StringUtils.hasText(normalizedInstruction)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员指令不能为空");
        }

        return parseConversation(List.of(new AdminOpsAiConversationMessage("user", renderStatelessInstructionPrompt(normalizedInstruction))));
    }

    @Override
    public AdminOpsAiPlan parseConversation(List<AdminOpsAiConversationMessage> conversationHistory) {
        String latestInstruction = extractLatestUserInstruction(conversationHistory);
        AdminOpsAiChatResponse chatResponse = chatWithTools(List.of(
                new AdminOpsAiConversationMessage("user", renderStatelessInstructionPrompt(latestInstruction))
        ));

        if (chatResponse.toolCalls() == null || chatResponse.toolCalls().isEmpty()) {
            return new AdminOpsAiPlan(
                    null,
                    AdminAiTaskConstants.PARSE_NEED_MORE_INFO,
                    null,
                    firstText(chatResponse.content(), "请补充更具体的用户信息后再试"),
                    List.of(),
                    List.of()
            );
        }

        List<AdminOpsAiAction> actions = mapStatelessToolCalls(chatResponse.toolCalls());
        List<AdminOpsAiConversationMessage> traceMessages = buildTraceMessages(chatResponse.toolCalls(), actions);
        if (actions.isEmpty()) {
            return new AdminOpsAiPlan(
                    null,
                    AdminAiTaskConstants.PARSE_NEED_MORE_INFO,
                    null,
                    firstText(chatResponse.content(), "当前指令仍缺少可执行的关键信息"),
                    List.of(),
                    traceMessages
            );
        }

        return new AdminOpsAiPlan(
                AdminAiTaskConstants.TASK_TYPE_USER_CRUD,
                AdminAiTaskConstants.PARSE_READY,
                buildSummaryText(chatResponse.content(), actions),
                null,
                actions,
                traceMessages
        );
    }

    @Override
    public AdminOpsAiChatResponse chatWithTools(List<AdminOpsAiConversationMessage> conversationHistory) {
        validateConfiguration();
        try {
            Prompt prompt = new Prompt(buildMessages(conversationHistory));
            ChatResponse response = chatModel.call(prompt);
            AssistantMessage assistantMessage = response == null || response.getResult() == null
                    ? null
                    : response.getResult().getOutput();
            if (assistantMessage == null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 未返回有效响应");
            }

            List<AdminOpsAiToolCall> toolCalls = assistantMessage.getToolCalls() == null
                    ? List.of()
                    : assistantMessage.getToolCalls().stream()
                    .map(toolCall -> new AdminOpsAiToolCall(toolCall.id(), toolCall.name(), toolCall.arguments()))
                    .toList();
            return new AdminOpsAiChatResponse(normalizeText(assistantMessage.getText()), toolCalls);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("管理员 AI 调用 DeepSeek 失败", exception);
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "管理员 AI 调用 DeepSeek 失败：" + firstText(normalizeText(exception.getMessage()), "模型服务暂时不可用"));
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维助手未启用");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getPath())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getModel())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 运维助手配置不完整");
        }
    }

    private OpenAiChatModel buildChatModel() {
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(firstText(properties.getBaseUrl(), "https://api.deepseek.com"))
                .completionsPath(firstText(properties.getPath(), "/v1/chat/completions"))
                .apiKey(firstText(properties.getApiKey(), ""))
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model(firstText(properties.getModel(), "deepseek-chat"))
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .parallelToolCalls(Boolean.TRUE)
                .internalToolExecutionEnabled(Boolean.FALSE)
                .toolCallbacks(buildStatelessUserToolCallbacks())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultOptions)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private ToolCallback[] buildFlatStudentToolCallbacks() {
        ToolCallback queryStudentTool = FunctionToolCallback
                .builder(TOOL_QUERY_STUDENT, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("查询学生。参数只能是平铺字符串 name、student_no、college；没有值传 null；绝对不要嵌套对象。")
                .inputType(JsonNode.class)
                .inputSchema(QUERY_STUDENT_SCHEMA)
                .build();

        ToolCallback createStudentTool = FunctionToolCallback
                .builder(TOOL_CREATE_STUDENT, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("新增学生。参数只能是平铺字符串 name、student_no、college、grade；没有值传 null；绝对不要嵌套对象。")
                .inputType(JsonNode.class)
                .inputSchema(CREATE_STUDENT_SCHEMA)
                .build();

        ToolCallback deleteStudentTool = FunctionToolCallback
                .builder(TOOL_DELETE_STUDENT, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("删除学生。参数只能是平铺字符串 name、student_no、account；没有值传 null；绝对不要嵌套对象。")
                .inputType(JsonNode.class)
                .inputSchema(DELETE_STUDENT_SCHEMA)
                .build();

        ToolCallback updateStudentTool = FunctionToolCallback
                .builder(TOOL_UPDATE_STUDENT, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("修改学生信息。参数只能是平铺字符串 target_name、target_student_no、new_college、new_grade、new_name；没有值传 null；绝对不要嵌套对象。")
                .inputType(JsonNode.class)
                .inputSchema(UPDATE_STUDENT_SCHEMA)
                .build();

        return new ToolCallback[]{queryStudentTool, createStudentTool, deleteStudentTool, updateStudentTool};
    }

    private ToolCallback[] buildStatelessUserToolCallbacks() {
        ToolCallback queryUsersTool = FunctionToolCallback
                .builder(TOOL_QUERY_USERS, (JsonNode input) -> Map.of("status", "registered"))
                .description("查询用户。所有参数必须平铺，缺值传 null，禁止嵌套 JSON。")
                .inputType(JsonNode.class)
                .inputSchema(STATELESS_QUERY_USERS_SCHEMA)
                .build();

        ToolCallback createUserTool = FunctionToolCallback
                .builder(TOOL_CREATE_USER, (JsonNode input) -> Map.of("status", "registered"))
                .description("新增用户。所有参数必须平铺，缺值传 null，禁止嵌套 JSON。")
                .inputType(JsonNode.class)
                .inputSchema(STATELESS_CREATE_USER_SCHEMA)
                .build();

        ToolCallback updateUsersTool = FunctionToolCallback
                .builder(TOOL_UPDATE_USERS, (JsonNode input) -> Map.of("status", "registered"))
                .description("修改用户。所有参数必须平铺，缺值传 null，禁止嵌套 JSON。")
                .inputType(JsonNode.class)
                .inputSchema(STATELESS_UPDATE_USERS_SCHEMA)
                .build();

        ToolCallback deleteUsersTool = FunctionToolCallback
                .builder(TOOL_DELETE_USERS, (JsonNode input) -> Map.of("status", "registered"))
                .description("删除用户。所有参数必须平铺，缺值传 null，禁止嵌套 JSON。")
                .inputType(JsonNode.class)
                .inputSchema(STATELESS_DELETE_USERS_SCHEMA)
                .build();

        return new ToolCallback[]{queryUsersTool, createUserTool, updateUsersTool, deleteUsersTool};
    }

    private ToolCallback[] buildToolCallbacks() {
        ToolCallback queryUsersTool = FunctionToolCallback
                .builder(TOOL_QUERY_USERS, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("按条件查询用户，支持角色、账号、姓名、学号、工号、学院、年级、状态等条件")
                .inputType(JsonNode.class)
                .inputSchema(QUERY_USERS_SCHEMA)
                .build();

        ToolCallback createUsersTool = FunctionToolCallback
                .builder(TOOL_CREATE_USERS, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("批量或单条新增用户，支持学生、咨询师、系统管理员")
                .inputType(JsonNode.class)
                .inputSchema(CREATE_USERS_SCHEMA)
                .build();

        ToolCallback deleteUsersTool = FunctionToolCallback
                .builder(TOOL_DELETE_USERS, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("按条件批量或单条删除用户")
                .inputType(JsonNode.class)
                .inputSchema(DELETE_USERS_SCHEMA)
                .build();

        ToolCallback updateUsersTool = FunctionToolCallback
                .builder(TOOL_UPDATE_USERS, (JsonNode input) -> Map.of("状态", "仅注册工具定义，等待上层业务执行"))
                .description("按目标条件批量或单条修改用户信息")
                .inputType(JsonNode.class)
                .inputSchema(UPDATE_USERS_SCHEMA)
                .build();

        return new ToolCallback[]{queryUsersTool, createUsersTool, deleteUsersTool, updateUsersTool};
    }

    private List<Message> buildMessages(List<AdminOpsAiConversationMessage> conversationHistory) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(resolveStatelessSystemPrompt()));
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return messages;
        }
        for (AdminOpsAiConversationMessage message : conversationHistory) {
            if (message == null) {
                continue;
            }
            String kind = normalizeConversationKind(message.kind());
            String role = normalizeConversationRole(message.role());
            String content = StringUtils.hasText(message.content()) ? message.content().trim() : "";
            if ("tool_call".equals(kind) && StringUtils.hasText(message.toolName()) && StringUtils.hasText(message.argumentsJson())) {
                String toolCallId = firstText(message.toolCallId(), "call_" + Math.abs(message.argumentsJson().hashCode()));
                messages.add(new AssistantMessage(content, Map.of(), List.of(
                        new AssistantMessage.ToolCall(toolCallId, "function", message.toolName(), message.argumentsJson())
                )));
            } else if ("tool_response".equals(kind) && StringUtils.hasText(message.toolName()) && StringUtils.hasText(message.responseData())) {
                String toolCallId = firstText(message.toolCallId(), "call_" + Math.abs(message.responseData().hashCode()));
                messages.add(new ToolResponseMessage(List.of(
                        new ToolResponseMessage.ToolResponse(toolCallId, message.toolName(), message.responseData())
                )));
            } else if ("assistant".equals(role) && StringUtils.hasText(content)) {
                messages.add(new AssistantMessage(content));
            } else if ("user".equals(role) && StringUtils.hasText(content)) {
                messages.add(new UserMessage(content));
            }
        }
        return messages;
    }

    private List<AdminOpsAiAction> mapFlatStudentToolCalls(List<AdminOpsAiToolCall> toolCalls) {
        List<AdminOpsAiAction> actions = new ArrayList<>();
        for (AdminOpsAiToolCall toolCall : toolCalls) {
            JsonNode argumentsNode = parseArguments(toolCall.argumentsJson());
            log.info("Admin AI raw tool call: tool={}, args={}", toolCall.name(), toolCall.argumentsJson());
            switch (toolCall.name()) {
                case TOOL_QUERY_STUDENT -> actions.add(mapQueryStudentAction(argumentsNode));
                case TOOL_CREATE_STUDENT -> actions.add(mapCreateStudentAction(argumentsNode));
                case TOOL_DELETE_STUDENT -> actions.add(mapDeleteStudentAction(argumentsNode));
                case TOOL_UPDATE_STUDENT -> actions.add(mapUpdateStudentAction(argumentsNode));
                default -> log.warn("管理员 AI 返回了未注册工具: {}", toolCall.name());
            }
        }
        return actions.stream().filter(action -> action != null).toList();
    }

    private AdminOpsAiAction mapQueryStudentAction(JsonNode argumentsNode) {
        String name = textValue(argumentsNode, "name");
        return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_QUERY, null,
                null, null, null, name, name, textValue(argumentsNode, "student_no", "studentNo"),
                null, textValue(argumentsNode, "college"), null, null, null, null, null, RoleConstants.STUDENT);
    }

    private AdminOpsAiAction mapCreateStudentAction(JsonNode argumentsNode) {
        String name = textValue(argumentsNode, "name");
        return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_CREATE, null,
                null, null, null, name, name, textValue(argumentsNode, "student_no", "studentNo"),
                null, textValue(argumentsNode, "college"), textValue(argumentsNode, "grade"),
                UserStatusConstants.ACTIVE, null, null, null, RoleConstants.STUDENT);
    }

    private AdminOpsAiAction mapDeleteStudentAction(JsonNode argumentsNode) {
        String name = textValue(argumentsNode, "name");
        return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_DELETE, null,
                null, null, textValue(argumentsNode, "account"), name, name,
                textValue(argumentsNode, "student_no", "studentNo"), null, null, null,
                null, null, null, null, RoleConstants.STUDENT);
    }

    private AdminOpsAiAction mapUpdateStudentAction(JsonNode argumentsNode) {
        String targetName = textValue(argumentsNode, "target_name", "targetName");
        String targetStudentNo = textValue(argumentsNode, "target_student_no", "targetStudentNo");
        String newCollege = textValue(argumentsNode, "new_college", "newCollege");
        String newGrade = textValue(argumentsNode, "new_grade", "newGrade");
        String newName = textValue(argumentsNode, "new_name", "newName");
        if (StringUtils.hasText(newCollege)) {
            return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_UPDATE, null,
                    "college", newCollege, null, targetName, targetName, targetStudentNo, null,
                    null, null, null, null, null, null, RoleConstants.STUDENT);
        }
        if (StringUtils.hasText(newGrade)) {
            return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_UPDATE, null,
                    "grade", newGrade, null, targetName, targetName, targetStudentNo, null,
                    null, null, null, null, null, null, RoleConstants.STUDENT);
        }
        if (StringUtils.hasText(newName)) {
            return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_UPDATE, null,
                    "realName", newName, null, targetName, targetName, targetStudentNo, null,
                    null, null, null, null, null, null, RoleConstants.STUDENT);
        }
        return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_UPDATE, null,
                null, null, null, targetName, targetName, targetStudentNo, null,
                null, null, null, null, null, null, RoleConstants.STUDENT);
    }

    private List<AdminOpsAiAction> mapStatelessToolCalls(List<AdminOpsAiToolCall> toolCalls) {
        List<AdminOpsAiAction> actions = new ArrayList<>();
        for (AdminOpsAiToolCall toolCall : toolCalls) {
            JsonNode argumentsNode = parseArguments(toolCall.argumentsJson());
            log.info("Admin AI raw tool call: tool={}, args={}", toolCall.name(), toolCall.argumentsJson());
            switch (toolCall.name()) {
                case TOOL_QUERY_USERS -> actions.add(new AdminOpsAiAction(
                        AdminAiTaskConstants.TARGET_USER,
                        AdminAiTaskConstants.OP_QUERY,
                        null,
                        null,
                        null,
                        textValue(argumentsNode, "account"),
                        textValue(argumentsNode, "display_name", "displayName"),
                        textValue(argumentsNode, "name", "real_name", "realName"),
                        textValue(argumentsNode, "student_no", "studentNo"),
                        textValue(argumentsNode, "counselor_no", "counselorNo"),
                        textValue(argumentsNode, "college"),
                        textValue(argumentsNode, "grade"),
                        normalizeStatusValue(textValue(argumentsNode, "status")),
                        null,
                        null,
                        null,
                        normalizeBusinessRole(textValue(argumentsNode, "role_code", "roleCode"))
                ));
                case TOOL_CREATE_USER -> actions.add(new AdminOpsAiAction(
                        AdminAiTaskConstants.TARGET_USER,
                        AdminAiTaskConstants.OP_CREATE,
                        null,
                        null,
                        null,
                        textValue(argumentsNode, "account"),
                        textValue(argumentsNode, "display_name", "displayName"),
                        textValue(argumentsNode, "real_name", "realName", "name"),
                        textValue(argumentsNode, "student_no", "studentNo"),
                        textValue(argumentsNode, "counselor_no", "counselorNo"),
                        textValue(argumentsNode, "college"),
                        textValue(argumentsNode, "grade"),
                        normalizeStatusValue(firstText(textValue(argumentsNode, "status"), UserStatusConstants.ACTIVE)),
                        null,
                        null,
                        null,
                        normalizeBusinessRole(textValue(argumentsNode, "role_code", "roleCode"))
                ));
                case TOOL_DELETE_USERS -> actions.add(new AdminOpsAiAction(
                        AdminAiTaskConstants.TARGET_USER,
                        AdminAiTaskConstants.OP_DELETE,
                        null,
                        null,
                        null,
                        textValue(argumentsNode, "account"),
                        null,
                        textValue(argumentsNode, "name"),
                        textValue(argumentsNode, "student_no", "studentNo"),
                        null,
                        textValue(argumentsNode, "college"),
                        textValue(argumentsNode, "grade"),
                        normalizeStatusValue(textValue(argumentsNode, "status")),
                        null,
                        null,
                        null,
                        normalizeBusinessRole(textValue(argumentsNode, "role_code", "roleCode"))
                ));
                case TOOL_UPDATE_USERS -> actions.addAll(mapStatelessUpdateActions(argumentsNode));
                default -> log.warn("管理员 AI 返回了未注册工具: {}", toolCall.name());
            }
        }
        return actions.stream().filter(action -> action != null).toList();
    }

    private List<AdminOpsAiAction> mapStatelessUpdateActions(JsonNode argumentsNode) {
        List<AdminOpsAiAction> actions = new ArrayList<>();
        addStatelessUpdateAction(actions, argumentsNode, "account", textValue(argumentsNode, "new_account", "newAccount"));
        addStatelessUpdateAction(actions, argumentsNode, "displayName", textValue(argumentsNode, "new_display_name", "newDisplayName"));
        addStatelessUpdateAction(actions, argumentsNode, "realName", textValue(argumentsNode, "new_real_name", "newRealName"));
        addStatelessUpdateAction(actions, argumentsNode, "studentNo", textValue(argumentsNode, "new_student_no", "newStudentNo"));
        addStatelessUpdateAction(actions, argumentsNode, "college", textValue(argumentsNode, "new_college", "newCollege"));
        addStatelessUpdateAction(actions, argumentsNode, "grade", textValue(argumentsNode, "new_grade", "newGrade"));
        addStatelessUpdateAction(actions, argumentsNode, "status", normalizeStatusValue(textValue(argumentsNode, "new_status", "newStatus")));
        return actions;
    }

    private void addStatelessUpdateAction(List<AdminOpsAiAction> actions, JsonNode argumentsNode, String fieldName, String newValue) {
        if (!StringUtils.hasText(newValue)) {
            return;
        }
        actions.add(new AdminOpsAiAction(
                AdminAiTaskConstants.TARGET_USER,
                AdminAiTaskConstants.OP_UPDATE,
                null,
                fieldName,
                newValue,
                textValue(argumentsNode, "target_account", "targetAccount"),
                textValue(argumentsNode, "target_display_name", "targetDisplayName"),
                textValue(argumentsNode, "target_name", "targetName"),
                textValue(argumentsNode, "target_student_no", "targetStudentNo"),
                null,
                textValue(argumentsNode, "target_college", "targetCollege"),
                textValue(argumentsNode, "target_grade", "targetGrade"),
                null,
                null,
                null,
                null,
                RoleConstants.STUDENT
        ));
    }

    private List<AdminOpsAiAction> mapToolCalls(List<AdminOpsAiToolCall> toolCalls) {
        List<AdminOpsAiAction> actions = new ArrayList<>();
        for (AdminOpsAiToolCall toolCall : toolCalls) {
            JsonNode argumentsNode = parseArguments(toolCall.argumentsJson());
            switch (toolCall.name()) {
                case TOOL_QUERY_USERS -> actions.add(buildUserAction(AdminAiTaskConstants.OP_QUERY,
                        extractObject(argumentsNode, "条件", "conditions"), null, null));
                case TOOL_DELETE_USERS -> actions.add(buildUserAction(AdminAiTaskConstants.OP_DELETE,
                        extractObject(argumentsNode, "条件", "conditions"), null, null));
                case TOOL_CREATE_USERS -> actions.addAll(mapCreateActions(argumentsNode));
                case TOOL_UPDATE_USERS -> actions.addAll(mapUpdateActionsFlexibleV2(argumentsNode));
                default -> log.warn("管理员 AI 返回了未注册工具: {}", toolCall.name());
            }
        }
        return actions.stream().filter(action -> action != null).toList();
    }

    private List<AdminOpsAiAction> mapCreateActions(JsonNode argumentsNode) {
        JsonNode usersNode = extractArray(argumentsNode, "用户数组", "users");
        if (usersNode == null || !usersNode.isArray()) {
            return List.of();
        }
        List<AdminOpsAiAction> actions = new ArrayList<>();
        for (JsonNode userNode : usersNode) {
            actions.add(new AdminOpsAiAction(
                    AdminAiTaskConstants.TARGET_USER,
                    AdminAiTaskConstants.OP_CREATE,
                    null,
                    null,
                    null,
                    textValue(userNode, "账号", "account"),
                    textValue(userNode, "显示名", "昵称", "displayName"),
                    textValue(userNode, "真实姓名", "姓名", "realName"),
                    textValue(userNode, "学号", "studentNo"),
                    textValue(userNode, "工号", "counselorNo"),
                    textValue(userNode, "学院", "college"),
                    textValue(userNode, "年级", "grade"),
                    normalizeStatusValue(textValue(userNode, "状态", "status")),
                    null,
                    null,
                    null,
                    normalizeBusinessRole(textValue(userNode, "角色", "role", "roleCode"))
            ));
        }
        return actions;
    }

    private List<AdminOpsAiAction> mapUpdateActions(JsonNode argumentsNode) {
        JsonNode targetNode = extractObject(argumentsNode, "目标条件", "targetConditions");
        JsonNode updatesNode = extractObject(argumentsNode, "修改内容", "updates");
        if (updatesNode == null || !updatesNode.isObject()) {
            return List.of();
        }
        List<AdminOpsAiAction> actions = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = updatesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fields.next();
            String fieldName = normalizeFieldName(fieldEntry.getKey());
            String newValue = stringifySimpleValue(fieldEntry.getValue());
            if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(newValue)) {
                continue;
            }
            if ("roleCode".equals(fieldName)) {
                newValue = normalizeBusinessRole(newValue);
            }
            if ("status".equals(fieldName)) {
                newValue = normalizeStatusValue(newValue);
            }
            actions.add(buildUserAction(AdminAiTaskConstants.OP_UPDATE, targetNode, fieldName, newValue));
        }
        return actions;
    }

    private List<AdminOpsAiAction> mapUpdateActionsFlexible(JsonNode argumentsNode) {
        var targetNode = objectMapper.createObjectNode();
        JsonNode nestedTargetNode = extractObject(argumentsNode, "目标条件", "targetConditions");
        if (nestedTargetNode != null && nestedTargetNode.isObject()) {
            targetNode.setAll((com.fasterxml.jackson.databind.node.ObjectNode) nestedTargetNode);
        }
        copyIfPresent(argumentsNode, targetNode, "target_account", "account");
        copyIfPresent(argumentsNode, targetNode, "targetAccount", "account");
        copyIfPresent(argumentsNode, targetNode, "target_display_name", "displayName");
        copyIfPresent(argumentsNode, targetNode, "targetDisplayName", "displayName");
        copyIfPresent(argumentsNode, targetNode, "target_name", "realName");
        copyIfPresent(argumentsNode, targetNode, "target_real_name", "realName");
        copyIfPresent(argumentsNode, targetNode, "targetRealName", "realName");
        copyIfPresent(argumentsNode, targetNode, "target_student_no", "studentNo");
        copyIfPresent(argumentsNode, targetNode, "targetStudentNo", "studentNo");
        copyIfPresent(argumentsNode, targetNode, "target_counselor_no", "counselorNo");
        copyIfPresent(argumentsNode, targetNode, "targetCounselorNo", "counselorNo");
        copyIfPresent(argumentsNode, targetNode, "target_college", "college");
        copyIfPresent(argumentsNode, targetNode, "targetCollege", "college");
        copyIfPresent(argumentsNode, targetNode, "target_grade", "grade");
        copyIfPresent(argumentsNode, targetNode, "targetGrade", "grade");
        copyIfPresent(argumentsNode, targetNode, "target_role", "roleCode");
        copyIfPresent(argumentsNode, targetNode, "targetRole", "roleCode");
        copyIfPresent(argumentsNode, targetNode, "target_status", "status");
        copyIfPresent(argumentsNode, targetNode, "targetStatus", "status");

        var updatesNode = objectMapper.createObjectNode();
        JsonNode nestedUpdatesNode = extractObject(argumentsNode, "修改内容", "updates");
        if (nestedUpdatesNode != null && nestedUpdatesNode.isObject()) {
            updatesNode.setAll((com.fasterxml.jackson.databind.node.ObjectNode) nestedUpdatesNode);
        }
        copyIfPresent(argumentsNode, updatesNode, "new_account", "account");
        copyIfPresent(argumentsNode, updatesNode, "newAccount", "account");
        copyIfPresent(argumentsNode, updatesNode, "new_display_name", "displayName");
        copyIfPresent(argumentsNode, updatesNode, "newDisplayName", "displayName");
        copyIfPresent(argumentsNode, updatesNode, "new_real_name", "realName");
        copyIfPresent(argumentsNode, updatesNode, "newRealName", "realName");
        copyIfPresent(argumentsNode, updatesNode, "new_name", "realName");
        copyIfPresent(argumentsNode, updatesNode, "new_student_no", "studentNo");
        copyIfPresent(argumentsNode, updatesNode, "newStudentNo", "studentNo");
        copyIfPresent(argumentsNode, updatesNode, "new_counselor_no", "counselorNo");
        copyIfPresent(argumentsNode, updatesNode, "newCounselorNo", "counselorNo");
        copyIfPresent(argumentsNode, updatesNode, "new_college", "college");
        copyIfPresent(argumentsNode, updatesNode, "newCollege", "college");
        copyIfPresent(argumentsNode, updatesNode, "new_grade", "grade");
        copyIfPresent(argumentsNode, updatesNode, "newGrade", "grade");
        copyIfPresent(argumentsNode, updatesNode, "new_status", "status");
        copyIfPresent(argumentsNode, updatesNode, "newStatus", "status");
        copyIfPresent(argumentsNode, updatesNode, "new_role", "roleCode");
        copyIfPresent(argumentsNode, updatesNode, "newRole", "roleCode");

        if (!updatesNode.fields().hasNext()) {
            return List.of();
        }

        List<AdminOpsAiAction> actions = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = updatesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fields.next();
            String fieldName = normalizeFieldName(fieldEntry.getKey());
            String newValue = stringifySimpleValue(fieldEntry.getValue());
            if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(newValue)) {
                continue;
            }
            if ("roleCode".equals(fieldName)) {
                newValue = normalizeBusinessRole(newValue);
            }
            if ("status".equals(fieldName)) {
                newValue = normalizeStatusValue(newValue);
            }
            actions.add(buildUserAction(AdminAiTaskConstants.OP_UPDATE, targetNode, fieldName, newValue));
        }
        return actions;
    }

    private List<AdminOpsAiAction> mapUpdateActionsFlexibleV2(JsonNode argumentsNode) {
        var targetNode = objectMapper.createObjectNode();
        JsonNode nestedTargetNode = extractNamedObject(argumentsNode, "鐩爣鏉′欢", "targetConditions");
        if (nestedTargetNode.isObject()) {
            targetNode.setAll((com.fasterxml.jackson.databind.node.ObjectNode) nestedTargetNode);
        }
        copyIfPresent(argumentsNode, targetNode, "target_account", "account");
        copyIfPresent(argumentsNode, targetNode, "targetAccount", "account");
        copyIfPresent(argumentsNode, targetNode, "target_display_name", "displayName");
        copyIfPresent(argumentsNode, targetNode, "targetDisplayName", "displayName");
        copyIfPresent(argumentsNode, targetNode, "target_name", "realName");
        copyIfPresent(argumentsNode, targetNode, "target_real_name", "realName");
        copyIfPresent(argumentsNode, targetNode, "targetRealName", "realName");
        copyIfPresent(argumentsNode, targetNode, "target_student_no", "studentNo");
        copyIfPresent(argumentsNode, targetNode, "targetStudentNo", "studentNo");
        copyIfPresent(argumentsNode, targetNode, "target_counselor_no", "counselorNo");
        copyIfPresent(argumentsNode, targetNode, "targetCounselorNo", "counselorNo");
        copyIfPresent(argumentsNode, targetNode, "target_college", "college");
        copyIfPresent(argumentsNode, targetNode, "targetCollege", "college");
        copyIfPresent(argumentsNode, targetNode, "target_grade", "grade");
        copyIfPresent(argumentsNode, targetNode, "targetGrade", "grade");
        copyIfPresent(argumentsNode, targetNode, "target_role", "roleCode");
        copyIfPresent(argumentsNode, targetNode, "targetRole", "roleCode");
        copyIfPresent(argumentsNode, targetNode, "target_status", "status");
        copyIfPresent(argumentsNode, targetNode, "targetStatus", "status");

        var updatesNode = objectMapper.createObjectNode();
        JsonNode nestedUpdatesNode = extractNamedObject(argumentsNode, "淇敼鍐呭", "updates");
        if (nestedUpdatesNode.isObject()) {
            updatesNode.setAll((com.fasterxml.jackson.databind.node.ObjectNode) nestedUpdatesNode);
        }
        copyIfPresent(argumentsNode, updatesNode, "new_account", "account");
        copyIfPresent(argumentsNode, updatesNode, "newAccount", "account");
        copyIfPresent(argumentsNode, updatesNode, "new_display_name", "displayName");
        copyIfPresent(argumentsNode, updatesNode, "newDisplayName", "displayName");
        copyIfPresent(argumentsNode, updatesNode, "new_real_name", "realName");
        copyIfPresent(argumentsNode, updatesNode, "newRealName", "realName");
        copyIfPresent(argumentsNode, updatesNode, "new_name", "realName");
        copyIfPresent(argumentsNode, updatesNode, "new_student_no", "studentNo");
        copyIfPresent(argumentsNode, updatesNode, "newStudentNo", "studentNo");
        copyIfPresent(argumentsNode, updatesNode, "new_counselor_no", "counselorNo");
        copyIfPresent(argumentsNode, updatesNode, "newCounselorNo", "counselorNo");
        copyIfPresent(argumentsNode, updatesNode, "new_college", "college");
        copyIfPresent(argumentsNode, updatesNode, "newCollege", "college");
        copyIfPresent(argumentsNode, updatesNode, "new_grade", "grade");
        copyIfPresent(argumentsNode, updatesNode, "newGrade", "grade");
        copyIfPresent(argumentsNode, updatesNode, "new_status", "status");
        copyIfPresent(argumentsNode, updatesNode, "newStatus", "status");
        copyIfPresent(argumentsNode, updatesNode, "new_role", "roleCode");
        copyIfPresent(argumentsNode, updatesNode, "newRole", "roleCode");

        if (!updatesNode.fields().hasNext()) {
            return List.of();
        }

        List<AdminOpsAiAction> actions = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = updatesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fields.next();
            String fieldName = normalizeFieldName(fieldEntry.getKey());
            String newValue = stringifySimpleValue(fieldEntry.getValue());
            if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(newValue)) {
                continue;
            }
            if ("roleCode".equals(fieldName)) {
                newValue = normalizeBusinessRole(newValue);
            }
            if ("status".equals(fieldName)) {
                newValue = normalizeStatusValue(newValue);
            }
            actions.add(buildUserAction(AdminAiTaskConstants.OP_UPDATE, targetNode, fieldName, newValue));
        }
        return actions;
    }

    private JsonNode extractNamedObject(JsonNode root, String primaryKey, String fallbackKey) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return objectMapper.createObjectNode();
        }
        JsonNode primary = root.path(primaryKey);
        if (primary.isObject()) {
            return primary;
        }
        JsonNode fallback = root.path(fallbackKey);
        if (fallback.isObject()) {
            return fallback;
        }
        return objectMapper.createObjectNode();
    }

    private void copyIfPresent(JsonNode source, com.fasterxml.jackson.databind.node.ObjectNode target, String sourceFieldName,
                               String targetFieldName) {
        JsonNode valueNode = source == null ? null : source.get(sourceFieldName);
        if (valueNode == null || valueNode.isNull() || valueNode.isMissingNode()) {
            return;
        }
        String value = stringifySimpleValue(valueNode);
        if (StringUtils.hasText(value)) {
            target.put(targetFieldName, value);
        }
    }

    private AdminOpsAiAction buildUserAction(String operationType, JsonNode conditionsNode, String fieldName, String newValue) {
        JsonNode sourceNode = conditionsNode == null || conditionsNode.isMissingNode() ? objectMapper.createObjectNode() : conditionsNode;
        return new AdminOpsAiAction(
                AdminAiTaskConstants.TARGET_USER,
                operationType,
                null,
                fieldName,
                newValue,
                textValue(sourceNode, "账号", "account"),
                textValue(sourceNode, "显示名", "昵称", "displayName"),
                textValue(sourceNode, "真实姓名", "姓名", "realName"),
                textValue(sourceNode, "学号", "studentNo"),
                textValue(sourceNode, "工号", "counselorNo"),
                textValue(sourceNode, "学院", "college"),
                textValue(sourceNode, "年级", "grade"),
                normalizeStatusValue(textValue(sourceNode, "状态", "status")),
                null,
                null,
                null,
                normalizeBusinessRole(textValue(sourceNode, "角色", "role", "roleCode"))
        );
    }

    private JsonNode parseArguments(String argumentsJson) {
        if (!StringUtils.hasText(argumentsJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(argumentsJson);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员 AI 返回的工具参数不是合法 JSON");
        }
    }

    private JsonNode extractObject(JsonNode root, String primaryKey, String fallbackKey) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return objectMapper.createObjectNode();
        }
        JsonNode primary = root.path(primaryKey);
        if (!primary.isMissingNode() && !primary.isNull()) {
            return primary;
        }
        JsonNode fallback = root.path(fallbackKey);
        if (!fallback.isMissingNode() && !fallback.isNull()) {
            return fallback;
        }
        return root.isObject() ? root : objectMapper.createObjectNode();
    }

    private JsonNode extractArray(JsonNode root, String primaryKey, String fallbackKey) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        JsonNode primary = root.path(primaryKey);
        if (primary.isArray()) {
            return primary;
        }
        JsonNode fallback = root.path(fallbackKey);
        return fallback.isArray() ? fallback : null;
    }

    private String textValue(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (valueNode.isMissingNode() || valueNode.isNull()) {
                continue;
            }
            String value = stringifySimpleValue(valueNode);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String stringifySimpleValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isTextual() || valueNode.isNumber() || valueNode.isBoolean()) {
            return normalizeText(valueNode.asText());
        }
        return normalizeText(valueNode.toString());
    }

    private String buildSummaryText(String assistantContent, List<AdminOpsAiAction> actions) {
        if (StringUtils.hasText(assistantContent)) {
            return assistantContent.trim();
        }
        String operationType = actions.stream()
                .map(AdminOpsAiAction::operationType)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
        return switch (operationType) {
            case AdminAiTaskConstants.OP_CREATE -> "AI 已解析新增用户请求";
            case AdminAiTaskConstants.OP_QUERY -> "AI 已解析查询用户请求";
            case AdminAiTaskConstants.OP_DELETE -> "AI 已解析删除用户请求";
            default -> "AI 已解析修改用户请求";
        };
    }

    private List<AdminOpsAiConversationMessage> buildTraceMessages(List<AdminOpsAiToolCall> toolCalls, List<AdminOpsAiAction> actions) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        List<AdminOpsAiConversationMessage> traceMessages = new ArrayList<>();
        for (AdminOpsAiToolCall toolCall : toolCalls) {
            if (toolCall == null || !StringUtils.hasText(toolCall.name())) {
                continue;
            }
            String toolCallId = firstText(toolCall.id(), "call_" + Math.abs(toolCall.argumentsJson() == null ? toolCall.name().hashCode() : toolCall.argumentsJson().hashCode()));
            traceMessages.add(AdminOpsAiConversationMessage.toolCall(toolCallId, toolCall.name(), firstText(toolCall.argumentsJson(), "{}")));
            traceMessages.add(AdminOpsAiConversationMessage.toolResponse(toolCallId, toolCall.name(), buildToolResponsePayload(toolCall, actions)));
        }
        return traceMessages;
    }

    private String buildToolResponsePayload(AdminOpsAiToolCall toolCall, List<AdminOpsAiAction> actions) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", "captured",
                    "tool", toolCall.name(),
                    "arguments", parseArguments(toolCall.argumentsJson()),
                    "actionCount", actions == null ? 0 : actions.size()
            ));
        } catch (Exception exception) {
            return "{\"status\":\"captured\"}";
        }
    }

    private String normalizeFieldName(String rawFieldName) {
        String value = normalizeText(rawFieldName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value) {
            case "账号", "帐号", "account" -> "account";
            case "显示名", "昵称", "displayName" -> "displayName";
            case "真实姓名", "姓名", "realName" -> "realName";
            case "学号", "studentNo" -> "studentNo";
            case "工号", "counselorNo" -> "counselorNo";
            case "学院", "college" -> "college";
            case "年级", "grade" -> "grade";
            case "状态", "status" -> "status";
            case "角色", "role", "roleCode" -> "roleCode";
            default -> value;
        };
    }

    private String normalizeBusinessRole(String rawRole) {
        String role = normalizeText(rawRole);
        if (!StringUtils.hasText(role)) {
            return null;
        }
        String lower = role.toLowerCase(Locale.ROOT);
        if (lower.contains("student") || role.contains("学生")) {
            return RoleConstants.STUDENT;
        }
        if (lower.contains("counselor") || role.contains("咨询")) {
            return RoleConstants.COUNSELOR;
        }
        if (lower.contains("admin") || role.contains("管理员")) {
            return RoleConstants.ADMIN;
        }
        return role;
    }

    private String normalizeStatusValue(String rawStatus) {
        String status = normalizeText(rawStatus);
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String lower = status.toLowerCase(Locale.ROOT);
        if (lower.contains("active") || status.contains("正常") || status.contains("启用")) {
            return UserStatusConstants.ACTIVE;
        }
        if (lower.contains("disable") || status.contains("禁用") || status.contains("停用")) {
            return UserStatusConstants.DISABLED;
        }
        return status;
    }

    private String normalizeConversationRole(String role) {
        String normalizedRole = normalizeText(role);
        if (!StringUtils.hasText(normalizedRole)) {
            return "user";
        }
        String lower = normalizedRole.toLowerCase(Locale.ROOT);
        if ("assistant".equals(lower)) {
            return "assistant";
        }
        if ("tool".equals(lower)) {
            return "tool";
        }
        if ("system".equals(lower)) {
            return "system";
        }
        return "user";
    }

    private String normalizeConversationKind(String kind) {
        String normalizedKind = normalizeText(kind);
        return StringUtils.hasText(normalizedKind) ? normalizedKind.toLowerCase(Locale.ROOT) : "text";
    }

    private String resolveSystemPrompt() {
        return firstText(normalizeText(properties.getSystemPrompt()),
                "你是一个面向中国用户的后台管理助手，所有回复与解释必须使用中文。");
    }

    private String renderInstructionPrompt(String instruction) {
        String template = firstText(normalizeText(properties.getUserPromptTemplate()),
                """
                请理解下面这条管理员后台指令。
                如果信息不足，请直接用中文追问，不要调用工具。
                如果信息足够，请调用最合适的工具表达意图。

                管理员指令：
                {instruction}
                """);
        return template.replace("{instruction}", instruction);
    }

    private String resolveStatelessSystemPrompt() {
        return """
                你是一个面向中国用户的后台管理助手。
                你所有回复必须使用中文。
                每次只解析当前这一条输入，不要参考历史对话，不要做上下文记忆。
                你只能处理后台用户管理任务。
                你只能使用四个工具：query_users、create_user、update_users、delete_users。
                所有工具参数必须是平铺字段，禁止嵌套对象。
                如果用户要新增、修改或删除，但缺少关键参数，请直接返回：
                【缺少参数，请补充：参数1、参数2】
                缺参数时不要调用工具。
                """;
    }

    private String renderStatelessInstructionPrompt(String instruction) {
        return "请只解析这一条后台管理指令：\n" + instruction;
    }

    private String extractLatestUserInstruction(List<AdminOpsAiConversationMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员指令不能为空");
        }
        for (int index = conversationHistory.size() - 1; index >= 0; index--) {
            AdminOpsAiConversationMessage message = conversationHistory.get(index);
            if (message != null && StringUtils.hasText(message.content())) {
                return message.content().trim();
            }
        }
        throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员指令不能为空");
    }

    private int resolveTimeoutSeconds() {
        Integer timeoutSeconds = properties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 60 : timeoutSeconds;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean shouldRetryForToolCall(AdminOpsAiChatResponse chatResponse) {
        if (chatResponse == null) {
            return false;
        }
        if (chatResponse.toolCalls() != null && !chatResponse.toolCalls().isEmpty()) {
            return false;
        }
        String content = normalizeText(chatResponse.content());
        if (!StringUtils.hasText(content)) {
            return true;
        }
        return !looksLikeClarification(content);
    }

    private boolean looksLikeClarification(String content) {
        String normalized = normalizeText(content);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        return normalized.contains("?")
                || normalized.contains("？")
                || containsAny(normalized, "请问", "请补充", "请提供", "请说明", "还需要", "还缺少", "信息不足", "无法判断", "不够明确");
    }

    private List<AdminOpsAiConversationMessage> buildToolRetryConversation(List<AdminOpsAiConversationMessage> conversationHistory,
                                                                           String assistantContent) {
        List<AdminOpsAiConversationMessage> retryConversation = new ArrayList<>();
        if (conversationHistory != null) {
            retryConversation.addAll(conversationHistory);
        }
        if (StringUtils.hasText(assistantContent)) {
            retryConversation.add(new AdminOpsAiConversationMessage("assistant", assistantContent));
        }
        retryConversation.add(new AdminOpsAiConversationMessage("user",
                "请重新判断上一轮对话。如果信息已经足够，请必须调用最合适的工具；如果信息仍不足，请只输出中文追问，不要直接表示会执行。"));
        return retryConversation;
    }

    private boolean containsAny(String source, String... keywords) {
        if (!StringUtils.hasText(source) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
