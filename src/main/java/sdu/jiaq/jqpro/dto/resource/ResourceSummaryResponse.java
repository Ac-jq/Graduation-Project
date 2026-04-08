package sdu.jiaq.jqpro.dto.resource;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resource card response.
 */
@Data
@Builder
public class ResourceSummaryResponse {

    private Long resourceId;

    private String title;

    private String summaryText;

    private String resourceType;

    private String contentUrl;

    private String coverUrl;

    private String status;

    private LocalDateTime publishedAt;

    private Long categoryId;

    private String categoryName;

    private List<ResourceTagResponse> tags;

    private boolean favorite;

    private long favoriteCount;

    private long viewCount;
}
