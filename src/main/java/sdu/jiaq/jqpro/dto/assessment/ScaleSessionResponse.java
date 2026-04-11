package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

/**
 * 作答会话响应。
 */
@Data
@Builder
public class ScaleSessionResponse {

    private Long sessionId;

    private Long scaleId;

    private Integer answeredCount;

    private Integer totalQuestions;

    private String status;
}
