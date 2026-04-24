package sdu.jiaq.jqpro.dto.adminai;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Conversation message used by the administrator AI task dialog.
 */
@Data
@Builder
public class AdminAiConversationMessageResponse {

    private String role;

    private String content;

    private LocalDateTime createdAt;
}
