package sdu.jiaq.jqpro.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Resource create or update request.
 */
@Data
public class UpsertResourceRequest {

    @NotBlank(message = "资源标题不能为空")
    private String title;

    @NotBlank(message = "资源简介不能为空")
    private String summaryText;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @NotBlank(message = "资源地址不能为空")
    private String contentUrl;

    private String coverUrl;

    @NotNull(message = "资源分类不能为空")
    private Long categoryId;

    private List<Long> tagIds;
}
