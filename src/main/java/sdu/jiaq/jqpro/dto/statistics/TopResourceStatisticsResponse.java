package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Hot resource statistics.
 */
@Data
@Builder
public class TopResourceStatisticsResponse {

    private Long resourceId;

    private String title;

    private String categoryName;

    private long viewCount;

    private long favoriteCount;
}
