package sdu.jiaq.jqpro.dto.adminai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Task detail response.
 */
@Data
@Builder
public class AdminAiTaskResponse {

    private Long taskId;

    private Long adminUserId;

    private String instructionText;

    private String taskType;

    private String parseStatus;

    private String workflowStatus;

    private String agentStatus;

    private String confirmStatus;

    private String executeStatus;

    private String summaryText;

    private String failureReason;

    private String pendingPrompt;

    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime executedAt;

    private List<AdminAiConversationMessageResponse> conversation;

    private List<AdminAiTaskItemResponse> items;
}
