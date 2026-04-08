package sdu.jiaq.jqpro.dto.adminscale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Admin scale option request.
 */
@Data
public class AdminScaleOptionRequest {

    @NotBlank(message = "optionCode is required")
    private String optionCode;

    @NotBlank(message = "content is required")
    private String content;

    @NotNull(message = "score is required")
    private Integer score;

    @NotNull(message = "sortNo is required")
    private Integer sortNo;
}
