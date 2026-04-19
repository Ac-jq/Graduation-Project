package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.resource.ResourceCategoryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceTagResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceUploadResponse;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceCategoryRequest;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceRequest;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceTagRequest;
import sdu.jiaq.jqpro.service.ResourceService;

import java.util.List;

/**
 * Admin resource management controller.
 */
@RestController
@RequestMapping("/api/admin")
@SaCheckRole(RoleConstants.ADMIN)
public class AdminResourceController {

    private final ResourceService resourceService;

    public AdminResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/resources")
    public Result<List<ResourceSummaryResponse>> listResources(@RequestParam(name = "status", required = false) String status,
                                                               @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(resourceService.listAdminResources(status, keyword));
    }

    @PostMapping(value = "/resources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ResourceUploadResponse> uploadResourceAsset(@RequestParam("file") MultipartFile file,
                                                              @RequestParam(name = "coverOnly", defaultValue = "false") boolean coverOnly) {
        return Result.success("资源文件上传成功", resourceService.uploadAdminResourceAsset(file, coverOnly));
    }

    @PostMapping("/resources")
    public Result<ResourceSummaryResponse> createResource(@Valid @RequestBody UpsertResourceRequest request) {
        return Result.success("资源创建成功", resourceService.createResource(request));
    }

    @PutMapping("/resources/{resourceId}")
    public Result<ResourceSummaryResponse> updateResource(@PathVariable("resourceId") Long resourceId,
                                                          @Valid @RequestBody UpsertResourceRequest request) {
        return Result.success("资源更新成功", resourceService.updateResource(resourceId, request));
    }

    @PostMapping("/resources/{resourceId}/publish")
    public Result<ResourceSummaryResponse> publishResource(@PathVariable("resourceId") Long resourceId) {
        return Result.success("资源发布成功", resourceService.publishResource(resourceId));
    }

    @PostMapping("/resources/{resourceId}/offline")
    public Result<ResourceSummaryResponse> offlineResource(@PathVariable("resourceId") Long resourceId) {
        return Result.success("资源下线成功", resourceService.offlineResource(resourceId));
    }

    @GetMapping("/resource-categories")
    public Result<List<ResourceCategoryResponse>> listCategories() {
        return Result.success(resourceService.listAdminCategories());
    }

    @PostMapping("/resource-categories")
    public Result<ResourceCategoryResponse> createCategory(@Valid @RequestBody UpsertResourceCategoryRequest request) {
        return Result.success("资源分类创建成功", resourceService.createCategory(request));
    }

    @PutMapping("/resource-categories/{categoryId}")
    public Result<ResourceCategoryResponse> updateCategory(@PathVariable("categoryId") Long categoryId,
                                                           @Valid @RequestBody UpsertResourceCategoryRequest request) {
        return Result.success("资源分类更新成功", resourceService.updateCategory(categoryId, request));
    }

    @GetMapping("/resource-tags")
    public Result<List<ResourceTagResponse>> listTags() {
        return Result.success(resourceService.listAdminTags());
    }

    @PostMapping("/resource-tags")
    public Result<ResourceTagResponse> createTag(@Valid @RequestBody UpsertResourceTagRequest request) {
        return Result.success("资源标签创建成功", resourceService.createTag(request));
    }
}
