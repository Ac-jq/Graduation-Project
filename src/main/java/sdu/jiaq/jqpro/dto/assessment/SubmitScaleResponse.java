package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

/**
 * 量表提交响应。
 */
@Data
@Builder
public class SubmitScaleResponse {

    private Long sessionId;

    private Long reportId;

    private Integer totalScore;

    private String levelCode;

    private String summaryText;

    private String noticeText;
}
