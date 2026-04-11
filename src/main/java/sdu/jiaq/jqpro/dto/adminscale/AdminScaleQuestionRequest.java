package sdu.jiaq.jqpro.dto.adminscale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Admin scale question request.
 */
@Data
public class AdminScaleQuestionRequest {

    @NotNull(message = "questionNo is required")
    private Integer questionNo;

    @NotBlank(message = "content is required")
    private String content;

    @NotNull(message = "requiredFlag is required")
    private Integer requiredFlag;

    @Valid
    @NotEmpty(message = "options are required")
    private List<AdminScaleOptionRequest> options;
}
