package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Overview statistics response.
 */
@Data
@Builder
public class OverviewStatisticsResponse {

    private long studentCount;

    private long counselorCount;

    private long scaleReportCount;

    private long aiSessionCount;

    private long appointmentCount;

    private long resourceCount;

    private long publishedResourceCount;

    private long resourceViewCount;

    private long favoriteCount;

    private long dailyActiveUsers;
}
