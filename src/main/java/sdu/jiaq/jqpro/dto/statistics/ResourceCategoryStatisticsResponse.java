package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Resource category statistics.
 */
@Data
@Builder
public class ResourceCategoryStatisticsResponse {

    private Long categoryId;

    private String categoryName;

    private long resourceCount;

    private long publishedCount;

    private long viewCount;

    private long favoriteCount;
}
