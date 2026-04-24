package sdu.jiaq.jqpro.service.ai;

/**
 * 管理员 AI 对话消息。
 *
 * @param role    消息角色，仅支持 system、user、assistant
 * @param content 消息内容
 */
public record AdminOpsAiConversationMessage(
        String role,
        String content,
        String kind,
        String toolCallId,
        String toolName,
        String argumentsJson,
        String responseData
) {

    public AdminOpsAiConversationMessage(String role, String content) {
        this(role, content, "text", null, null, null, null);
    }

    public static AdminOpsAiConversationMessage toolCall(String toolCallId, String toolName, String argumentsJson) {
        return new AdminOpsAiConversationMessage("assistant", null, "tool_call", toolCallId, toolName, argumentsJson, null);
    }

    public static AdminOpsAiConversationMessage toolResponse(String toolCallId, String toolName, String responseData) {
        return new AdminOpsAiConversationMessage("tool", null, "tool_response", toolCallId, toolName, null, responseData);
    }
}
