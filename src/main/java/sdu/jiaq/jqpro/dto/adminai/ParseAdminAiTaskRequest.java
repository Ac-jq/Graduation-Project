package sdu.jiaq.jqpro.dto.adminai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Parse admin instruction request.
 */
@Data
public class ParseAdminAiTaskRequest {

    @NotBlank(message = "指令不能为空")
    private String instruction;

    private Long taskId;
}
