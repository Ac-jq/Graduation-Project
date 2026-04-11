package sdu.jiaq.jqpro.dto.adminai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Task list response.
 */
@Data
@Builder
public class AdminAiTaskSummaryResponse {

    private Long taskId;

    private String instructionText;

    private String taskType;

    private String parseStatus;

    private String confirmStatus;

    private String executeStatus;

    private String summaryText;

    private LocalDateTime createdAt;
}
