package sdu.jiaq.jqpro.service.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.service.ReportRecommendationService;
import sdu.jiaq.jqpro.service.ResourceService;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;
import sdu.jiaq.jqpro.service.ai.ResourceRecommendationAiRequest;

/**
 * 负责在报告生成时固化推荐资源，避免历史报告详情页重复请求 AI。
 */
@Service
public class ReportRecommendationServiceImpl implements ReportRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ReportRecommendationServiceImpl.class);
    private static final int RECOMMENDED_RESOURCE_LIMIT = 3;
    private static final int AI_RESOURCE_CATALOG_LIMIT = 50;
    private static final Set<String> GENERIC_RESOURCE_KEYWORDS = Set.of("校园支持", "呼吸练习", "视觉安抚", "舒缓放松", "图像引导");

    private final ResourceService resourceService;
    private final InterpretationAiClient interpretationAiClient;

    public ReportRecommendationServiceImpl(ResourceService resourceService, InterpretationAiClient interpretationAiClient) {
        this.resourceService = resourceService;
        this.interpretationAiClient = interpretationAiClient;
    }

    @Override
    public String buildRecommendedResourceIdSnapshot(MentalScale scale, MentalScaleReport report, String detailedAnswerContext) {
        List<ResourceSummaryResponse> allPublishedResources = resourceService.listPublishedResources(null, null, null);
        if (allPublishedResources.isEmpty()) {
            return "";
        }

        String assessmentContext = buildAssessmentContext(scale, report, detailedAnswerContext);
        LinkedHashMap<Long, ResourceSummaryResponse> mergedResources = new LinkedHashMap<>();

        try {
            List<ResourceSummaryResponse> latestResources = allPublishedResources.stream()
                    .sorted(Comparator.comparing(ResourceSummaryResponse::getPublishedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .limit(AI_RESOURCE_CATALOG_LIMIT)
                    .toList();
            String resourceCatalog = buildResourceCatalog(latestResources);
            List<Long> aiResourceIds = interpretationAiClient.selectRecommendedResourceIds(
                    new ResourceRecommendationAiRequest(assessmentContext, resourceCatalog)
            );
            latestResources.stream()
                    .filter(resource -> aiResourceIds.contains(resource.getResourceId()))
                    .sorted(Comparator.comparingInt(resource -> aiResourceIds.indexOf(resource.getResourceId())))
                    .forEach(resource -> mergedResources.put(resource.getResourceId(), resource));
        } catch (Exception exception) {
            log.warn("AI resource recommendation failed during report generation, fallback to rule-based recommendation. sessionId={}", report.getSessionId(), exception);
        }

        if (mergedResources.size() < RECOMMENDED_RESOURCE_LIMIT) {
            RecommendationProfile profile = buildRecommendationProfile(scale, report, assessmentContext);

            for (String keyword : profile.primaryKeywords()) {
                allPublishedResources.stream()
                        .map(resource -> new ScoredResource(resource, scoreResource(resource, List.of(keyword), profile.preferArticle())))
                        .filter(scoredResource -> scoredResource.score() > 0)
                        .sorted(Comparator
                                .comparingInt(ScoredResource::score).reversed()
                                .thenComparing(scoredResource -> scoredResource.resource().getPublishedAt(),
                                        Comparator.nullsLast(LocalDateTime::compareTo).reversed())
                                .thenComparing(scoredResource -> scoredResource.resource().getFavoriteCount(), Comparator.reverseOrder())
                                .thenComparing(scoredResource -> scoredResource.resource().getViewCount(), Comparator.reverseOrder()))
                        .map(ScoredResource::resource)
                        .filter(resource -> !mergedResources.containsKey(resource.getResourceId()))
                        .findFirst()
                        .ifPresent(resource -> mergedResources.put(resource.getResourceId(), resource));
                if (mergedResources.size() >= RECOMMENDED_RESOURCE_LIMIT) {
                    break;
                }
            }

            List<ResourceSummaryResponse> targetedResources = allPublishedResources.stream()
                    .map(resource -> new ScoredResource(resource, scoreResource(resource, profile.primaryKeywords(), profile.preferArticle())))
                    .filter(scoredResource -> scoredResource.score() > 0)
                    .sorted(Comparator
                            .comparingInt(ScoredResource::score).reversed()
                            .thenComparing(scoredResource -> scoredResource.resource().getPublishedAt(),
                                    Comparator.nullsLast(LocalDateTime::compareTo).reversed())
                            .thenComparing(scoredResource -> scoredResource.resource().getFavoriteCount(), Comparator.reverseOrder())
                            .thenComparing(scoredResource -> scoredResource.resource().getViewCount(), Comparator.reverseOrder()))
                    .map(ScoredResource::resource)
                    .toList();
            targetedResources.forEach(resource -> mergedResources.putIfAbsent(resource.getResourceId(), resource));

            if (mergedResources.size() < RECOMMENDED_RESOURCE_LIMIT) {
                buildGenericFallbackResources(allPublishedResources).forEach(resource -> mergedResources.putIfAbsent(resource.getResourceId(), resource));
            }
        }

        return mergedResources.values().stream()
                .limit(RECOMMENDED_RESOURCE_LIMIT)
                .map(ResourceSummaryResponse::getResourceId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<ResourceSummaryResponse> listSnapshotResources(String recommendedResourceIds) {
        List<Long> resourceIds = parseResourceIds(recommendedResourceIds);
        if (resourceIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ResourceSummaryResponse> publishedResourceMap = resourceService.listPublishedResources(null, null, null)
                .stream()
                .collect(Collectors.toMap(ResourceSummaryResponse::getResourceId, resource -> resource, (left, right) -> left));

        return resourceIds.stream()
                .map(publishedResourceMap::get)
                .filter(Objects::nonNull)
                .limit(RECOMMENDED_RESOURCE_LIMIT)
                .toList();
    }

    private List<Long> parseResourceIds(String recommendedResourceIds) {
        if (!StringUtils.hasText(recommendedResourceIds)) {
            return List.of();
        }
        return List.of(recommendedResourceIds.split(","))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String buildAssessmentContext(MentalScale scale, MentalScaleReport report, String detailedAnswerContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("量表名称：").append(defaultText(scale.getName())).append('\n');
        builder.append("总分：").append(report.getTotalScore() == null ? "未知" : report.getTotalScore()).append('\n');
        builder.append("风险等级：").append(defaultText(report.getLevelCode())).append('\n');
        builder.append("摘要：").append(defaultText(report.getSummaryText())).append('\n');
        builder.append("AI解读：").append(defaultText(report.getAiInterpretation())).append('\n');
        builder.append("详细答题记录：\n").append(defaultText(detailedAnswerContext));
        return builder.toString();
    }

    private String buildResourceCatalog(List<ResourceSummaryResponse> resources) {
        return resources.stream()
                .map(resource -> {
                    String tags = resource.getTags() == null ? "" : resource.getTags().stream()
                            .map(tag -> defaultText(tag.getName()))
                            .filter(name -> !name.isBlank())
                            .collect(Collectors.joining("、"));
                    return "资源ID=%d；标题=%s；分类=%s；摘要=%s；标签=%s"
                            .formatted(
                                    resource.getResourceId(),
                                    defaultText(resource.getTitle()),
                                    defaultText(resource.getCategoryName()),
                                    defaultText(resource.getSummaryText()),
                                    defaultText(tags)
                            );
                })
                .collect(Collectors.joining("\n"));
    }

    private RecommendationProfile buildRecommendationProfile(MentalScale scale,
                                                             MentalScaleReport report,
                                                             String assessmentContext) {
        List<String> primaryKeywords = new java.util.ArrayList<>();
        String scaleCode = defaultText(scale.getCode()).toUpperCase();
        String contentText = assessmentContext.toLowerCase();
        boolean lowRisk = "LOW".equals(report.getLevelCode());

        if (!lowRisk) {
            if (scaleCode.contains("GAD") || containsAny(contentText, "焦虑", "紧张", "担心")) {
                primaryKeywords.addAll(List.of("呼吸练习", "舒缓放松", "视觉安抚", "校园支持"));
            }
            if (scaleCode.contains("PHQ") || containsAny(contentText, "低落", "兴趣减退", "无助", "情绪")) {
                primaryKeywords.addAll(List.of("睡前放松", "视觉安抚", "校园支持", "图像引导"));
            }
            if (scaleCode.contains("SLEEP") || containsAny(contentText, "睡眠", "入睡", "失眠", "夜间")) {
                primaryKeywords.addAll(List.of("睡前放松", "睡眠修复", "视觉安抚"));
            }
            if (scaleCode.contains("STRESS") || containsAny(contentText, "压力", "考试", "专注", "学业")) {
                primaryKeywords.addAll(List.of("考试", "专注恢复", "校园支持", "学习节律"));
            }
        }

        if ("HIGH".equals(report.getLevelCode()) || "MEDIUM".equals(report.getLevelCode())) {
            primaryKeywords.add("校园支持");
        }

        return new RecommendationProfile(
                primaryKeywords.stream().filter(Objects::nonNull).distinct().toList(),
                lowRisk || primaryKeywords.isEmpty()
        );
    }

    private List<ResourceSummaryResponse> buildGenericFallbackResources(List<ResourceSummaryResponse> allPublishedResources) {
        return allPublishedResources.stream()
                .map(resource -> new ScoredResource(resource, scoreResource(resource, GENERIC_RESOURCE_KEYWORDS, true)))
                .sorted(Comparator
                        .comparingInt(ScoredResource::score).reversed()
                        .thenComparing(scoredResource -> "ARTICLE".equals(scoredResource.resource().getResourceType()) ? 1 : 0, Comparator.reverseOrder())
                        .thenComparing(scoredResource -> scoredResource.resource().getFavoriteCount(), Comparator.reverseOrder())
                        .thenComparing(scoredResource -> scoredResource.resource().getViewCount(), Comparator.reverseOrder()))
                .map(ScoredResource::resource)
                .toList();
    }

    private int scoreResource(ResourceSummaryResponse resource, Collection<String> keywords, boolean preferArticle) {
        int score = 0;
        String title = defaultText(resource.getTitle()).toLowerCase();
        String summary = defaultText(resource.getSummaryText()).toLowerCase();
        String category = defaultText(resource.getCategoryName()).toLowerCase();

        for (String keyword : keywords) {
            String normalizedKeyword = defaultText(keyword).toLowerCase();
            if (normalizedKeyword.isEmpty()) {
                continue;
            }
            if (category.contains(normalizedKeyword)) {
                score += 5;
            }
            if (title.contains(normalizedKeyword)) {
                score += 4;
            }
            if (summary.contains(normalizedKeyword)) {
                score += 3;
            }
            if (resource.getTags() != null && resource.getTags().stream()
                    .map(tag -> defaultText(tag.getName()).toLowerCase())
                    .anyMatch(tagName -> tagName.contains(normalizedKeyword))) {
                score += 6;
            }
        }

        if (preferArticle && "ARTICLE".equals(resource.getResourceType())) {
            score += 2;
        }
        if ("PUBLISHED".equals(resource.getStatus())) {
            score += 1;
        }
        return score;
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private record RecommendationProfile(List<String> primaryKeywords, boolean preferArticle) {
    }

    private record ScoredResource(ResourceSummaryResponse resource, int score) {
    }
}
