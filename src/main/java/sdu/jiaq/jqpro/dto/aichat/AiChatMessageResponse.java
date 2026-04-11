package sdu.jiaq.jqpro.dto.aichat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 消息响应。
 */
@Data
@Builder
public class AiChatMessageResponse {

    private Long messageId;

    private Long sessionId;

    private String senderType;

    private String content;

    private String riskLevel;

    private String hitKeywords;

    private LocalDateTime createdAt;
}
