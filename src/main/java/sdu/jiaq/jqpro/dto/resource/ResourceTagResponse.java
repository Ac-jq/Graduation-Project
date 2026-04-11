package sdu.jiaq.jqpro.dto.resource;

import lombok.Builder;
import lombok.Data;

/**
 * Resource tag response.
 */
@Data
@Builder
public class ResourceTagResponse {

    private Long tagId;

    private String name;

    private String description;
}
