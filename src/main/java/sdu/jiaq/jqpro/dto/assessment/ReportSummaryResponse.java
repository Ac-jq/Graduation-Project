package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报告摘要响应。
 */
@Data
@Builder
public class ReportSummaryResponse {

    private Long reportId;

    private Long scaleId;

    private String scaleName;

    private Integer totalScore;

    private String levelCode;

    private String summaryText;

    private LocalDateTime createdAt;
}
