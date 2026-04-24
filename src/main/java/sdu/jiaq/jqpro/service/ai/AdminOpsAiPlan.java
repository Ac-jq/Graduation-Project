package sdu.jiaq.jqpro.service.ai;

import java.util.List;

/**
 * Structured administrator operation plan returned by the model.
 */
public record AdminOpsAiPlan(
        String taskType,
        String parseStatus,
        String summaryText,
        String failureReason,
        List<AdminOpsAiAction> actions,
        List<AdminOpsAiConversationMessage> traceMessages
) {
}
