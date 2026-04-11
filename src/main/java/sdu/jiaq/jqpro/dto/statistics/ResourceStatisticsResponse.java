package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resource statistics response.
 */
@Data
@Builder
public class ResourceStatisticsResponse {

    private long resourceCount;

    private long publishedCount;

    private long totalViews;

    private long totalFavorites;

    private List<ResourceCategoryStatisticsResponse> categories;

    private List<TopResourceStatisticsResponse> topResources;
}
