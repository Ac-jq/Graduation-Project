package sdu.jiaq.jqpro.dto.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Tag create or update request.
 */
@Data
public class UpsertResourceTagRequest {

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String description;
}
