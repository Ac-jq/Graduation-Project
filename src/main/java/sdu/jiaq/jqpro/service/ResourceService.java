package sdu.jiaq.jqpro.service;

import org.springframework.web.multipart.MultipartFile;
import sdu.jiaq.jqpro.dto.resource.ResourceCategoryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceDetailResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceUploadResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceTagResponse;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceCategoryRequest;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceRequest;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceTagRequest;

import java.util.List;

/**
 * Resource service.
 */
public interface ResourceService {

    List<ResourceCategoryResponse> listPublishedCategories();

    List<ResourceTagResponse> listTags();

    List<ResourceSummaryResponse> listPublishedResources(Long categoryId, Long tagId, String keyword);

    ResourceDetailResponse getPublishedResourceDetail(Long resourceId);

    void addFavorite(Long resourceId);

    void removeFavorite(Long resourceId);

    List<ResourceSummaryResponse> listCurrentStudentFavorites();

    List<ResourceCategoryResponse> listAdminCategories();

    List<ResourceTagResponse> listAdminTags();

    List<ResourceSummaryResponse> listAdminResources(String status, String keyword);

    ResourceUploadResponse uploadAdminResourceAsset(MultipartFile file, boolean coverOnly);

    ResourceSummaryResponse createResource(UpsertResourceRequest request);

    ResourceSummaryResponse updateResource(Long resourceId, UpsertResourceRequest request);

    ResourceSummaryResponse publishResource(Long resourceId);

    ResourceSummaryResponse offlineResource(Long resourceId);

    ResourceCategoryResponse createCategory(UpsertResourceCategoryRequest request);

    ResourceCategoryResponse updateCategory(Long categoryId, UpsertResourceCategoryRequest request);

    ResourceTagResponse createTag(UpsertResourceTagRequest request);
}
