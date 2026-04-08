package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Export row response.
 */
@Data
@Builder
public class StatisticsExportRowResponse {

    private String dimension;

    private String dimensionValue;

    private long studentCount;

    private long reportCount;

    private double averageScore;

    private long aiSessionCount;

    private long appointmentCount;

    private long resourceViewCount;

    private long favoriteCount;
}
