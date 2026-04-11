package sdu.jiaq.jqpro.dto.adminscale;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Admin scale question response.
 */
@Data
@Builder
public class AdminScaleQuestionResponse {

    private Long questionId;

    private Integer questionNo;

    private String content;

    private Integer requiredFlag;

    private List<AdminScaleOptionResponse> options;
}
