package sdu.jiaq.jqpro.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.ResourceConstants;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.resource.ResourceCategoryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceDetailResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceTagResponse;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceCategoryRequest;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceRequest;
import sdu.jiaq.jqpro.dto.resource.UpsertResourceTagRequest;
import sdu.jiaq.jqpro.entity.MentalResource;
import sdu.jiaq.jqpro.entity.ResourceCategory;
import sdu.jiaq.jqpro.entity.ResourceFavorite;
import sdu.jiaq.jqpro.entity.ResourceTag;
import sdu.jiaq.jqpro.entity.ResourceTagRelation;
import sdu.jiaq.jqpro.entity.ResourceViewLog;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.ResourceCategoryMapper;
import sdu.jiaq.jqpro.mapper.ResourceFavoriteMapper;
import sdu.jiaq.jqpro.mapper.ResourceTagMapper;
import sdu.jiaq.jqpro.mapper.ResourceTagRelationMapper;
import sdu.jiaq.jqpro.mapper.ResourceViewLogMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.ResourceService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resource module service implementation.
 */
@Service
public class ResourceServiceImpl implements ResourceService {

    private final ResourceCategoryMapper resourceCategoryMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final ResourceTagRelationMapper resourceTagRelationMapper;
    private final ResourceFavoriteMapper resourceFavoriteMapper;
    private final ResourceViewLogMapper resourceViewLogMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;

    public ResourceServiceImpl(ResourceCategoryMapper resourceCategoryMapper,
                               ResourceTagMapper resourceTagMapper,
                               MentalResourceMapper mentalResourceMapper,
                               ResourceTagRelationMapper resourceTagRelationMapper,
                               ResourceFavoriteMapper resourceFavoriteMapper,
                               ResourceViewLogMapper resourceViewLogMapper,
                               SysUserMapper sysUserMapper,
                               AuditLogService auditLogService) {
        this.resourceCategoryMapper = resourceCategoryMapper;
        this.resourceTagMapper = resourceTagMapper;
        this.mentalResourceMapper = mentalResourceMapper;
        this.resourceTagRelationMapper = resourceTagRelationMapper;
        this.resourceFavoriteMapper = resourceFavoriteMapper;
        this.resourceViewLogMapper = resourceViewLogMapper;
        this.sysUserMapper = sysUserMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ResourceCategoryResponse> listPublishedCategories() {
        return resourceCategoryMapper.selectList(new LambdaQueryWrapper<ResourceCategory>()
                        .eq(ResourceCategory::getStatus, ResourceConstants.CATEGORY_ACTIVE)
                        .orderByAsc(ResourceCategory::getSortNo, ResourceCategory::getId))
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    public List<ResourceTagResponse> listTags() {
        return resourceTagMapper.selectList(new LambdaQueryWrapper<ResourceTag>()
                        .orderByAsc(ResourceTag::getName, ResourceTag::getId))
                .stream()
                .map(this::toTagResponse)
                .toList();
    }

    @Override
    public List<ResourceSummaryResponse> listPublishedResources(Long categoryId, Long tagId, String keyword) {
        List<Long> tagFilteredResourceIds = filterResourceIdsByTag(tagId);
        if (tagId != null && tagFilteredResourceIds.isEmpty()) {
            return List.of();
        }

        List<MentalResource> resources = mentalResourceMapper.selectList(new LambdaQueryWrapper<MentalResource>()
                .eq(MentalResource::getStatus, ResourceConstants.RESOURCE_PUBLISHED)
                .eq(categoryId != null, MentalResource::getCategoryId, categoryId)
                .and(hasText(keyword), wrapper -> wrapper
                        .like(MentalResource::getTitle, keyword.trim())
                        .or()
                        .like(MentalResource::getSummaryText, keyword.trim()))
                .in(tagId != null, MentalResource::getId, tagFilteredResourceIds)
                .orderByDesc(MentalResource::getPublishedAt, MentalResource::getId));

        return buildSummaryResponses(resources, getCurrentStudentUserIdOrNull());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceDetailResponse getPublishedResourceDetail(Long resourceId) {
        MentalResource resource = getRequiredResource(resourceId);
        if (!ResourceConstants.RESOURCE_PUBLISHED.equals(resource.getStatus())) {
            throw new BusinessException("资源未发布");
        }

        Long studentUserId = getCurrentStudentUserIdOrNull();
        if (studentUserId != null) {
            ResourceViewLog viewLog = new ResourceViewLog();
            viewLog.setResourceId(resourceId);
            viewLog.setStudentUserId(studentUserId);
            resourceViewLogMapper.insert(viewLog);
        }

        ResourceSummaryResponse summary = buildSummaryResponses(List.of(resource), studentUserId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("资源不存在"));

        return ResourceDetailResponse.builder()
                .resourceId(summary.getResourceId())
                .title(summary.getTitle())
                .summaryText(summary.getSummaryText())
                .resourceType(summary.getResourceType())
                .contentUrl(summary.getContentUrl())
                .coverUrl(summary.getCoverUrl())
                .status(summary.getStatus())
                .publishedAt(summary.getPublishedAt())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .categoryId(summary.getCategoryId())
                .categoryName(summary.getCategoryName())
                .tags(summary.getTags())
                .favorite(summary.isFavorite())
                .favoriteCount(summary.getFavoriteCount())
                .viewCount(summary.getViewCount())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long resourceId) {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        MentalResource resource = getRequiredResource(resourceId);
        if (!ResourceConstants.RESOURCE_PUBLISHED.equals(resource.getStatus())) {
            throw new BusinessException("仅可收藏已发布资源");
        }

        ResourceFavorite existing = resourceFavoriteMapper.selectOne(new LambdaQueryWrapper<ResourceFavorite>()
                .eq(ResourceFavorite::getResourceId, resourceId)
                .eq(ResourceFavorite::getStudentUserId, studentUserId)
                .last("limit 1"));
        if (existing != null) {
            return;
        }

        ResourceFavorite favorite = new ResourceFavorite();
        favorite.setResourceId(resourceId);
        favorite.setStudentUserId(studentUserId);
        resourceFavoriteMapper.insert(favorite);

        auditLogService.record(studentUserId, "RESOURCE_FAVORITE_ADD", "资源收藏", "收藏资源#" + resourceId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long resourceId) {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        List<ResourceFavorite> favorites = resourceFavoriteMapper.selectList(new LambdaQueryWrapper<ResourceFavorite>()
                .eq(ResourceFavorite::getResourceId, resourceId)
                .eq(ResourceFavorite::getStudentUserId, studentUserId));
        if (favorites.isEmpty()) {
            return;
        }

        favorites.forEach(item -> resourceFavoriteMapper.deleteById(item.getId()));
        auditLogService.record(studentUserId, "RESOURCE_FAVORITE_REMOVE", "取消收藏", "取消收藏资源#" + resourceId, null);
    }

    @Override
    public List<ResourceSummaryResponse> listCurrentStudentFavorites() {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        List<ResourceFavorite> favorites = resourceFavoriteMapper.selectList(new LambdaQueryWrapper<ResourceFavorite>()
                .eq(ResourceFavorite::getStudentUserId, studentUserId)
                .orderByDesc(ResourceFavorite::getCreatedAt, ResourceFavorite::getId));
        if (favorites.isEmpty()) {
            return List.of();
        }

        List<Long> resourceIds = favorites.stream()
                .map(ResourceFavorite::getResourceId)
                .distinct()
                .toList();
        List<MentalResource> resources = mentalResourceMapper.selectList(new LambdaQueryWrapper<MentalResource>()
                .in(MentalResource::getId, resourceIds)
                .eq(MentalResource::getStatus, ResourceConstants.RESOURCE_PUBLISHED));

        Map<Long, Integer> sortIndex = new HashMap<>();
        for (int i = 0; i < resourceIds.size(); i++) {
            sortIndex.put(resourceIds.get(i), i);
        }

        return buildSummaryResponses(resources, studentUserId).stream()
                .sorted((left, right) -> Integer.compare(
                        sortIndex.getOrDefault(left.getResourceId(), Integer.MAX_VALUE),
                        sortIndex.getOrDefault(right.getResourceId(), Integer.MAX_VALUE)))
                .toList();
    }

    @Override
    public List<ResourceCategoryResponse> listAdminCategories() {
        return resourceCategoryMapper.selectList(new LambdaQueryWrapper<ResourceCategory>()
                        .orderByAsc(ResourceCategory::getSortNo, ResourceCategory::getId))
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    public List<ResourceSummaryResponse> listAdminResources(String status, String keyword) {
        List<MentalResource> resources = mentalResourceMapper.selectList(new LambdaQueryWrapper<MentalResource>()
                .eq(hasText(status), MentalResource::getStatus, status == null ? null : status.trim().toUpperCase())
                .and(hasText(keyword), wrapper -> wrapper
                        .like(MentalResource::getTitle, keyword.trim())
                        .or()
                        .like(MentalResource::getSummaryText, keyword.trim()))
                .orderByDesc(MentalResource::getUpdatedAt, MentalResource::getId));

        return buildSummaryResponses(resources, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSummaryResponse createResource(UpsertResourceRequest request) {
        validateCategory(request.getCategoryId());

        MentalResource resource = new MentalResource();
        applyRequest(resource, request);
        resource.setStatus(ResourceConstants.RESOURCE_DRAFT);
        mentalResourceMapper.insert(resource);
        syncTagRelations(resource.getId(), request.getTagIds());

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_CREATE", "创建资源", "创建资源#" + resource.getId(), null);
        return buildSummaryResponses(List.of(getRequiredResource(resource.getId())), null).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSummaryResponse updateResource(Long resourceId, UpsertResourceRequest request) {
        MentalResource resource = getRequiredResource(resourceId);
        validateCategory(request.getCategoryId());

        applyRequest(resource, request);
        mentalResourceMapper.updateById(resource);
        syncTagRelations(resourceId, request.getTagIds());

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_UPDATE", "编辑资源", "编辑资源#" + resourceId, null);
        return buildSummaryResponses(List.of(getRequiredResource(resourceId)), null).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSummaryResponse publishResource(Long resourceId) {
        MentalResource resource = getRequiredResource(resourceId);
        resource.setStatus(ResourceConstants.RESOURCE_PUBLISHED);
        resource.setPublishedAt(LocalDateTime.now());
        mentalResourceMapper.updateById(resource);

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_PUBLISH", "发布资源", "发布资源#" + resourceId, null);
        return buildSummaryResponses(List.of(resource), null).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSummaryResponse offlineResource(Long resourceId) {
        MentalResource resource = getRequiredResource(resourceId);
        resource.setStatus(ResourceConstants.RESOURCE_OFFLINE);
        mentalResourceMapper.updateById(resource);

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_OFFLINE", "下线资源", "下线资源#" + resourceId, null);
        return buildSummaryResponses(List.of(resource), null).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceCategoryResponse createCategory(UpsertResourceCategoryRequest request) {
        ensureCategoryNameUnique(request.getName(), null);

        ResourceCategory category = new ResourceCategory();
        category.setName(request.getName().trim());
        category.setDescription(blankToNull(request.getDescription()));
        category.setSortNo(request.getSortNo() == null ? 99 : request.getSortNo());
        category.setStatus(hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase() : ResourceConstants.CATEGORY_ACTIVE);
        resourceCategoryMapper.insert(category);

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_CATEGORY_CREATE", "创建资源分类", "创建分类#" + category.getId(), null);
        return toCategoryResponse(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceCategoryResponse updateCategory(Long categoryId, UpsertResourceCategoryRequest request) {
        ResourceCategory category = getRequiredCategory(categoryId);
        ensureCategoryNameUnique(request.getName(), categoryId);

        category.setName(request.getName().trim());
        category.setDescription(blankToNull(request.getDescription()));
        if (request.getSortNo() != null) {
            category.setSortNo(request.getSortNo());
        }
        if (hasText(request.getStatus())) {
            category.setStatus(request.getStatus().trim().toUpperCase());
        }
        resourceCategoryMapper.updateById(category);

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_CATEGORY_UPDATE", "编辑资源分类", "编辑分类#" + categoryId, null);
        return toCategoryResponse(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceTagResponse createTag(UpsertResourceTagRequest request) {
        ensureTagNameUnique(request.getName());

        ResourceTag tag = new ResourceTag();
        tag.setName(request.getName().trim());
        tag.setDescription(blankToNull(request.getDescription()));
        resourceTagMapper.insert(tag);

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_RESOURCE_TAG_CREATE", "创建资源标签", "创建标签#" + tag.getId(), null);
        return toTagResponse(tag);
    }

    private List<ResourceSummaryResponse> buildSummaryResponses(List<MentalResource> resources, Long currentStudentUserId) {
        if (resources.isEmpty()) {
            return List.of();
        }

        List<Long> resourceIds = resources.stream().map(MentalResource::getId).toList();
        ResourceContext context = preloadContext(resourceIds);

        List<ResourceSummaryResponse> responses = new ArrayList<>();
        for (MentalResource resource : resources) {
            ResourceCategory category = context.categoryMap.get(resource.getCategoryId());
            List<ResourceTagResponse> tags = context.resourceTags.getOrDefault(resource.getId(), List.of());
            boolean favorite = currentStudentUserId != null
                    && context.favoriteStudentIds.getOrDefault(resource.getId(), Set.of()).contains(currentStudentUserId);

            responses.add(ResourceSummaryResponse.builder()
                    .resourceId(resource.getId())
                    .title(resource.getTitle())
                    .summaryText(resource.getSummaryText())
                    .resourceType(resource.getResourceType())
                    .contentUrl(resource.getContentUrl())
                    .coverUrl(resource.getCoverUrl())
                    .status(resource.getStatus())
                    .publishedAt(resource.getPublishedAt())
                    .categoryId(resource.getCategoryId())
                    .categoryName(category == null ? null : category.getName())
                    .tags(tags)
                    .favorite(favorite)
                    .favoriteCount(context.favoriteCounts.getOrDefault(resource.getId(), 0L))
                    .viewCount(context.viewCounts.getOrDefault(resource.getId(), 0L))
                    .build());
        }
        return responses;
    }

    private ResourceContext preloadContext(List<Long> resourceIds) {
        ResourceContext context = new ResourceContext();

        List<MentalResource> resources = mentalResourceMapper.selectBatchIds(resourceIds);
        List<Long> categoryIds = resources.stream()
                .map(MentalResource::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!categoryIds.isEmpty()) {
            context.categoryMap = resourceCategoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(ResourceCategory::getId, item -> item, (left, right) -> left, HashMap::new));
        }

        List<ResourceTagRelation> relations = resourceTagRelationMapper.selectList(new LambdaQueryWrapper<ResourceTagRelation>()
                .in(ResourceTagRelation::getResourceId, resourceIds));
        List<Long> tagIds = relations.stream().map(ResourceTagRelation::getTagId).distinct().toList();
        Map<Long, ResourceTag> tagMap = tagIds.isEmpty()
                ? Map.of()
                : resourceTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(ResourceTag::getId, item -> item, (left, right) -> left, HashMap::new));
        for (ResourceTagRelation relation : relations) {
            ResourceTag tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            context.resourceTags.computeIfAbsent(relation.getResourceId(), key -> new ArrayList<>()).add(toTagResponse(tag));
        }

        List<ResourceFavorite> favorites = resourceFavoriteMapper.selectList(new LambdaQueryWrapper<ResourceFavorite>()
                .in(ResourceFavorite::getResourceId, resourceIds));
        for (ResourceFavorite favorite : favorites) {
            context.favoriteCounts.merge(favorite.getResourceId(), 1L, Long::sum);
            context.favoriteStudentIds.computeIfAbsent(favorite.getResourceId(), key -> new HashSet<>()).add(favorite.getStudentUserId());
        }

        List<ResourceViewLog> viewLogs = resourceViewLogMapper.selectList(new LambdaQueryWrapper<ResourceViewLog>()
                .in(ResourceViewLog::getResourceId, resourceIds));
        for (ResourceViewLog viewLog : viewLogs) {
            context.viewCounts.merge(viewLog.getResourceId(), 1L, Long::sum);
        }

        return context;
    }

    private void applyRequest(MentalResource resource, UpsertResourceRequest request) {
        resource.setTitle(request.getTitle().trim());
        resource.setSummaryText(request.getSummaryText().trim());
        resource.setResourceType(request.getResourceType().trim().toUpperCase());
        resource.setContentUrl(request.getContentUrl().trim());
        resource.setCoverUrl(blankToNull(request.getCoverUrl()));
        resource.setCategoryId(request.getCategoryId());
    }

    private void syncTagRelations(Long resourceId, List<Long> tagIds) {
        List<ResourceTagRelation> existing = resourceTagRelationMapper.selectList(new LambdaQueryWrapper<ResourceTagRelation>()
                .eq(ResourceTagRelation::getResourceId, resourceId));
        existing.forEach(item -> resourceTagRelationMapper.deleteById(item.getId()));

        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        Set<Long> distinctTagIds = tagIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (distinctTagIds.isEmpty()) {
            return;
        }

        List<ResourceTag> tags = resourceTagMapper.selectBatchIds(distinctTagIds);
        if (tags.size() != distinctTagIds.size()) {
            throw new BusinessException("存在无效资源标签");
        }

        for (Long tagId : distinctTagIds) {
            ResourceTagRelation relation = new ResourceTagRelation();
            relation.setResourceId(resourceId);
            relation.setTagId(tagId);
            resourceTagRelationMapper.insert(relation);
        }
    }

    private void validateCategory(Long categoryId) {
        ResourceCategory category = getRequiredCategory(categoryId);
        if (ResourceConstants.CATEGORY_DISABLED.equals(category.getStatus())) {
            throw new BusinessException("当前分类已停用");
        }
    }

    private Long getCurrentStudentUserIdOrNull() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            Long userId = Long.parseLong(String.valueOf(loginId));
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || !RoleConstants.STUDENT.equals(user.getRoleCode())) {
                return null;
            }
            return userId;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Long> filterResourceIdsByTag(Long tagId) {
        if (tagId == null) {
            return List.of();
        }
        return resourceTagRelationMapper.selectList(new LambdaQueryWrapper<ResourceTagRelation>()
                        .eq(ResourceTagRelation::getTagId, tagId))
                .stream()
                .map(ResourceTagRelation::getResourceId)
                .distinct()
                .toList();
    }

    private void ensureCategoryNameUnique(String name, Long excludeId) {
        List<ResourceCategory> categories = resourceCategoryMapper.selectList(new LambdaQueryWrapper<ResourceCategory>()
                .eq(ResourceCategory::getName, name.trim()));
        boolean conflict = categories.stream().anyMatch(item -> !Objects.equals(item.getId(), excludeId));
        if (conflict) {
            throw new BusinessException("资源分类名称已存在");
        }
    }

    private void ensureTagNameUnique(String name) {
        Long count = resourceTagMapper.selectCount(new LambdaQueryWrapper<ResourceTag>()
                .eq(ResourceTag::getName, name.trim()));
        if (count != null && count > 0) {
            throw new BusinessException("资源标签名称已存在");
        }
    }

    private ResourceCategory getRequiredCategory(Long categoryId) {
        ResourceCategory category = resourceCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("资源分类不存在");
        }
        return category;
    }

    private MentalResource getRequiredResource(Long resourceId) {
        MentalResource resource = mentalResourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        return resource;
    }

    private ResourceCategoryResponse toCategoryResponse(ResourceCategory category) {
        return ResourceCategoryResponse.builder()
                .categoryId(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortNo(category.getSortNo())
                .status(category.getStatus())
                .build();
    }

    private ResourceTagResponse toTagResponse(ResourceTag tag) {
        return ResourceTagResponse.builder()
                .tagId(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .build();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static class ResourceContext {
        private Map<Long, ResourceCategory> categoryMap = new HashMap<>();
        private Map<Long, List<ResourceTagResponse>> resourceTags = new HashMap<>();
        private Map<Long, Long> favoriteCounts = new HashMap<>();
        private Map<Long, Set<Long>> favoriteStudentIds = new HashMap<>();
        private Map<Long, Long> viewCounts = new HashMap<>();
    }
}
