package sdu.jiaq.jqpro.dto.adminscale;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin scale response.
 */
@Data
@Builder
public class AdminScaleResponse {

    private Long scaleId;

    private String code;

    private String name;

    private String description;

    private String introduction;

    private Integer totalQuestions;

    private Integer pageSize;

    private Integer lowThreshold;

    private Integer mediumThreshold;

    private Integer highThreshold;

    private String status;

    private boolean inUse;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<AdminScaleQuestionResponse> questions;
}
