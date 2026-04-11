package sdu.jiaq.jqpro.dto.adminscale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Admin scale upsert request.
 */
@Data
public class UpsertAdminScaleRequest {

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private String introduction;

    @NotNull(message = "pageSize is required")
    private Integer pageSize;

    @NotNull(message = "lowThreshold is required")
    private Integer lowThreshold;

    @NotNull(message = "mediumThreshold is required")
    private Integer mediumThreshold;

    @NotNull(message = "highThreshold is required")
    private Integer highThreshold;

    @Valid
    @NotEmpty(message = "questions are required")
    private List<AdminScaleQuestionRequest> questions;
}
