package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Per-scale assessment summary.
 */
@Data
@Builder
public class AssessmentScaleSummaryResponse {

    private Long scaleId;

    private String scaleName;

    private long participantCount;

    private long reportCount;

    private double averageScore;
}
