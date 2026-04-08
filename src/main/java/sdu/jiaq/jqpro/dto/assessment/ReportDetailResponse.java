package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报告详情响应。
 */
@Data
@Builder
public class ReportDetailResponse {

    private Long reportId;

    private Long sessionId;

    private Long scaleId;

    private String scaleName;

    private Long studentUserId;

    private String studentName;

    private String studentNo;

    private Integer totalScore;

    private String levelCode;

    private String summaryText;

    private String aiInterpretation;

    private String recommendationNote;

    private boolean recommendAppointment;

    private List<ResourceSummaryResponse> recommendedResources;

    private LocalDateTime createdAt;
}
