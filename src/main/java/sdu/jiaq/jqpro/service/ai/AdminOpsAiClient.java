package sdu.jiaq.jqpro.service.ai;

import java.util.List;

/**
 * 管理员 AI 运维助手客户端。
 */
public interface AdminOpsAiClient {

    boolean isEnabled();

    AdminOpsAiPlan parseInstruction(String instruction);

    AdminOpsAiPlan parseConversation(List<AdminOpsAiConversationMessage> conversationHistory);

    AdminOpsAiChatResponse chatWithTools(List<AdminOpsAiConversationMessage> conversationHistory);
}
