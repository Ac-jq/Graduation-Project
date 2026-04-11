package sdu.jiaq.jqpro.controller.resource;

import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.resource.ResourceCategoryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceDetailResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceTagResponse;
import sdu.jiaq.jqpro.service.ResourceService;

import java.util.List;

/**
 * Student-side resource browse controller.
 */
@RestController
@RequestMapping("/api/resources")
@SaCheckLogin
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/categories")
    public Result<List<ResourceCategoryResponse>> listCategories() {
        return Result.success(resourceService.listPublishedCategories());
    }

    @GetMapping("/tags")
    public Result<List<ResourceTagResponse>> listTags() {
        return Result.success(resourceService.listTags());
    }

    @GetMapping
    public Result<List<ResourceSummaryResponse>> listResources(@RequestParam(name = "categoryId", required = false) Long categoryId,
                                                               @RequestParam(name = "tagId", required = false) Long tagId,
                                                               @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(resourceService.listPublishedResources(categoryId, tagId, keyword));
    }

    @GetMapping("/{resourceId}")
    public Result<ResourceDetailResponse> getResourceDetail(@PathVariable("resourceId") Long resourceId) {
        return Result.success(resourceService.getPublishedResourceDetail(resourceId));
    }
}
