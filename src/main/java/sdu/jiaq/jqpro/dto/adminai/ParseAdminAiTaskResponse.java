package sdu.jiaq.jqpro.dto.adminai;

import lombok.Builder;
import lombok.Data;

/**
 * Parse response wrapper.
 */
@Data
@Builder
public class ParseAdminAiTaskResponse {

    private boolean ready;

    private String message;

    private AdminAiTaskResponse task;
}
