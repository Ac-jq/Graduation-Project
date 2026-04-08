package sdu.jiaq.jqpro.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天室会话响应。
 */
@Data
@Builder
public class ConsultChatSessionResponse {

    private Long chatSessionId;

    private Long appointmentId;

    private String status;

    private Boolean sealed;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;
}
