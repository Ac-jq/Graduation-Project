package sdu.jiaq.jqpro.dto.aichat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话响应。
 */
@Data
@Builder
public class AiChatSessionResponse {

    private Long sessionId;

    private Long studentUserId;

    private String studentName;

    private String title;

    private String status;

    private String summaryText;

    private Boolean riskFlag;

    private String riskLevel;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;
}
