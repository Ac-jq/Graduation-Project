package sdu.jiaq.jqpro.dto.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Category create or update request.
 */
@Data
public class UpsertResourceCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;

    private Integer sortNo;

    private String status;
}
