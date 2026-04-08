package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Assessment statistics response.
 */
@Data
@Builder
public class AssessmentStatisticsResponse {

    private long totalReports;

    private long participantCount;

    private double averageScore;

    private List<NamedMetricResponse> levelDistribution;

    private List<AssessmentScaleSummaryResponse> scales;

    private AssessmentCompareSummaryResponse compareSummary;
}
