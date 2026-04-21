package sdu.jiaq.jqpro.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天室消息响应。
 */
@Data
@Builder
public class ConsultChatMessageResponse {

    private Long messageId;

    private Long chatSessionId;

    private Long senderUserId;

    private String senderType;

    private String senderDisplayName;

    private String senderAvatarUrl;

    private String content;

    private LocalDateTime createdAt;
}
