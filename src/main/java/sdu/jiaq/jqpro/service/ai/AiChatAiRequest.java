package sdu.jiaq.jqpro.service.ai;

import java.util.List;

/**
 * Request payload for AI mentor chat generation.
 */
public record AiChatAiRequest(
        String sessionTitle,
        String riskLevel,
        boolean riskFlag,
        List<ConversationMessage> historyMessages,
        String latestStudentMessage
) {

    public record ConversationMessage(String role, String content) {
    }
}
