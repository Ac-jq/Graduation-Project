package sdu.jiaq.jqpro.dto.aichat;

import lombok.Builder;
import lombok.Data;

/**
 * Student-scoped AI mentor persona response.
 */
@Data
@Builder
public class AiPersonaSettingResponse {

    private Long studentUserId;

    private String mentorName;

    private String avatarText;
}
