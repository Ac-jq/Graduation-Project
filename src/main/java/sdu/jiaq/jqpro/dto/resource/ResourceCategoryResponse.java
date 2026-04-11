package sdu.jiaq.jqpro.dto.resource;

import lombok.Builder;
import lombok.Data;

/**
 * Resource category response.
 */
@Data
@Builder
public class ResourceCategoryResponse {

    private Long categoryId;

    private String name;

    private String description;

    private Integer sortNo;

    private String status;
}
