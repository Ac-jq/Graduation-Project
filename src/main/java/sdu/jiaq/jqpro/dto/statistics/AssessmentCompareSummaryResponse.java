package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Assessment compare summary.
 */
@Data
@Builder
public class AssessmentCompareSummaryResponse {

    private long sampleCount;

    private double averageDelta;

    private long improvedCount;

    private long stableCount;

    private long worsenedCount;

    private boolean smallSampleWarning;
}
