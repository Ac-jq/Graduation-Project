package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.constant.AssessmentNoticeConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.assessment.ReportDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ReportSummaryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleAnswer;
import sdu.jiaq.jqpro.entity.MentalScaleOption;
import sdu.jiaq.jqpro.entity.MentalScaleQuestion;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleAnswerMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleOptionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleQuestionMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.ReportService;
import sdu.jiaq.jqpro.service.ResourceService;
import sdu.jiaq.jqpro.service.ai.InterpretationAiClient;
import sdu.jiaq.jqpro.service.ai.ResourceRecommendationAiRequest;

/**
 * 测评报告查询服务。
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final String AUDIT_IP = "system";
    private static final int RECOMMENDED_RESOURCE_LIMIT = 3;
    private static final int AI_RESOURCE_CATALOG_LIMIT = 50;
    private static final Set<String> GENERIC_RESOURCE_KEYWORDS = Set.of("校园支持", "呼吸练习", "视觉安抚", "舒缓放松", "图像引导");

    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final MentalScaleMapper mentalScaleMapper;
    private final MentalScaleAnswerMapper mentalScaleAnswerMapper;
    private final MentalScaleQuestionMapper mentalScaleQuestionMapper;
    private final MentalScaleOptionMapper mentalScaleOptionMapper;
    private final SysUserMapper sysUserMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final ResourceService resourceService;
    private final AuditLogService auditLogService;
    private final InterpretationAiClient interpretationAiClient;

    public ReportServiceImpl(MentalScaleReportMapper mentalScaleReportMapper,
                             MentalScaleMapper mentalScaleMapper,
                             MentalScaleAnswerMapper mentalScaleAnswerMapper,
                             MentalScaleQuestionMapper mentalScaleQuestionMapper,
                             MentalScaleOptionMapper mentalScaleOptionMapper,
                             SysUserMapper sysUserMapper,
                             CounselorStudentMapper counselorStudentMapper,
                             ResourceService resourceService,
                             AuditLogService auditLogService,
                             InterpretationAiClient interpretationAiClient) {
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.mentalScaleMapper = mentalScaleMapper;
        this.mentalScaleAnswerMapper = mentalScaleAnswerMapper;
        this.mentalScaleQuestionMapper = mentalScaleQuestionMapper;
        this.mentalScaleOptionMapper = mentalScaleOptionMapper;
        this.sysUserMapper = sysUserMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.resourceService = resourceService;
        this.auditLogService = auditLogService;
        this.interpretationAiClient = interpretationAiClient;
    }

    @Override
    public List<ReportSummaryResponse> listCurrentStudentReports() {
        return buildSummaryList(SecurityUtil.getCurrentUserId());
    }

    @Override
    public ReportDetailResponse getCurrentStudentReportDetail(Long reportId) {
        Long userId = SecurityUtil.getCurrentUserId();
        MentalScaleReport report = getRequiredReport(reportId);
        if (!userId.equals(report.getUserId())) {
            throw new BusinessException("无权查看该报告");
        }
        auditLogService.record(userId, "ASSESSMENT_REPORT_VIEW_SELF", "查看本人测评报告", "reportId=" + reportId, AUDIT_IP);
        return buildDetail(report);
    }

    @Override
    public List<ReportSummaryResponse> listCounselorStudentReports(Long studentUserId) {
        verifyCounselorOwnership(studentUserId);
        return buildSummaryList(studentUserId);
    }

    @Override
    public ReportDetailResponse getCounselorStudentReportDetail(Long studentUserId, Long reportId) {
        verifyCounselorOwnership(studentUserId);
        MentalScaleReport report = getRequiredReport(reportId);
        if (!studentUserId.equals(report.getUserId())) {
            throw new BusinessException("报告与学生不匹配");
        }
        auditLogService.record(
                SecurityUtil.getCurrentUserId(),
                "ASSESSMENT_REPORT_VIEW_COUNSELOR",
                "咨询师查看学生测评报告",
                "studentUserId=" + studentUserId + ", reportId=" + reportId,
                AUDIT_IP
        );
        return buildDetail(report);
    }

    private List<ReportSummaryResponse> buildSummaryList(Long studentUserId) {
        List<MentalScaleReport> reports = mentalScaleReportMapper.selectList(
                new LambdaQueryWrapper<MentalScaleReport>()
                        .eq(MentalScaleReport::getUserId, studentUserId)
                        .orderByDesc(MentalScaleReport::getCreatedAt)
        );
        if (reports.isEmpty()) {
            return List.of();
        }

        Map<Long, MentalScale> scaleMap = mentalScaleMapper.selectBatchIds(
                reports.stream().map(MentalScaleReport::getScaleId).distinct().toList()
        ).stream().collect(Collectors.toMap(MentalScale::getId, Function.identity()));

        return reports.stream()
                .map(report -> ReportSummaryResponse.builder()
                        .reportId(report.getId())
                        .scaleId(report.getScaleId())
                        .scaleName(scaleMap.get(report.getScaleId()).getName())
                        .totalScore(report.getTotalScore())
                        .levelCode(report.getLevelCode())
                        .summaryText(report.getSummaryText())
                        .createdAt(report.getCreatedAt())
                        .build())
                .toList();
    }

    private ReportDetailResponse buildDetail(MentalScaleReport report) {
        MentalScale scale = mentalScaleMapper.selectById((Serializable) report.getScaleId());
        SysUser studentUser = sysUserMapper.selectById((Serializable) report.getUserId());
        if (scale == null || studentUser == null) {
            throw new BusinessException("报告关联数据缺失");
        }

        String assessmentContext = buildDetailedAssessmentContext(scale, report);

        return ReportDetailResponse.builder()
                .reportId(report.getId())
                .sessionId(report.getSessionId())
                .scaleId(report.getScaleId())
                .scaleName(scale.getName())
                .studentUserId(studentUser.getId())
                .studentName(studentUser.getDisplayName())
                .studentNo(studentUser.getStudentNo())
                .totalScore(report.getTotalScore())
                .levelCode(report.getLevelCode())
                .summaryText(report.getSummaryText())
                .aiInterpretation(report.getAiInterpretation())
                .recommendationNote(buildRecommendationNote(report.getLevelCode()))
                .recommendAppointment("HIGH".equals(report.getLevelCode()) || "MEDIUM".equals(report.getLevelCode()))
                .recommendedResources(buildRecommendedResources(scale, report, assessmentContext))
                .noticeText(AssessmentNoticeConstants.NON_DIAGNOSTIC_NOTICE)
                .createdAt(report.getCreatedAt())
                .build();
    }

    private String buildRecommendationNote(String levelCode) {
        if ("HIGH".equals(levelCode)) {
            return "当前结果提示需要尽快结合学校心理老师、辅导员或其他专业支持资源进一步沟通，优先保证休息、安全与现实支持。";
        }
        if ("MEDIUM".equals(levelCode)) {
            return "当前结果建议先结合自助资源持续观察；如果困扰持续存在或明显影响学习生活，建议尽快预约咨询支持。";
        }
        return "当前结果整体相对平稳，可以继续保持规律作息、适度运动和稳定的自我照顾节奏。";
    }

    private List<ResourceSummaryResponse> buildRecommendedResources(MentalScale scale,
                                                                   MentalScaleReport report,
                                                                   String assessmentContext) {
        List<ResourceSummaryResponse> allPublishedResources = resourceService.listPublishedResources(null, null, null);
        if (allPublishedResources.isEmpty()) {
            return List.of();
        }

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
            log.warn("AI resource recommendation failed, fallback to rule-based recommendation. reportId={}", report.getId(), exception);
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
                .toList();
    }

    private String buildDetailedAssessmentContext(MentalScale scale, MentalScaleReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("量表名称：").append(defaultText(scale.getName())).append('\n');
        builder.append("总分：").append(report.getTotalScore() == null ? "未知" : report.getTotalScore()).append('\n');
        builder.append("风险等级：").append(defaultText(report.getLevelCode())).append('\n');
        builder.append("摘要：").append(defaultText(report.getSummaryText())).append('\n');
        builder.append("现有解读：").append(defaultText(report.getAiInterpretation())).append('\n');
        builder.append("详细答题记录：\n");

        List<MentalScaleAnswer> answers = mentalScaleAnswerMapper.selectList(
                new LambdaQueryWrapper<MentalScaleAnswer>()
                        .eq(MentalScaleAnswer::getSessionId, report.getSessionId())
                        .orderByAsc(MentalScaleAnswer::getQuestionId, MentalScaleAnswer::getId)
        );
        if (answers.isEmpty()) {
            builder.append("暂无答题记录。");
            return builder.toString();
        }

        Set<Long> questionIds = answers.stream().map(MentalScaleAnswer::getQuestionId).collect(Collectors.toSet());
        Set<Long> optionIds = answers.stream().map(MentalScaleAnswer::getOptionId).collect(Collectors.toSet());
        Map<Long, MentalScaleQuestion> questionMap = mentalScaleQuestionMapper.selectList(
                new LambdaQueryWrapper<MentalScaleQuestion>()
                        .eq(MentalScaleQuestion::getScaleId, scale.getId())
                        .in(MentalScaleQuestion::getId, questionIds)
        ).stream().collect(Collectors.toMap(MentalScaleQuestion::getId, Function.identity()));
        Map<Long, MentalScaleOption> optionMap = mentalScaleOptionMapper.selectList(
                new LambdaQueryWrapper<MentalScaleOption>().in(MentalScaleOption::getId, optionIds)
        ).stream().collect(Collectors.toMap(MentalScaleOption::getId, Function.identity()));

        String detailLines = answers.stream()
                .sorted(Comparator.comparing(answer -> {
                    MentalScaleQuestion question = questionMap.get(answer.getQuestionId());
                    return question == null ? Integer.MAX_VALUE : question.getQuestionNo();
                }))
                .map(answer -> {
                    MentalScaleQuestion question = questionMap.get(answer.getQuestionId());
                    MentalScaleOption option = optionMap.get(answer.getOptionId());
                    String questionNo = question == null || question.getQuestionNo() == null ? "?" : String.valueOf(question.getQuestionNo());
                    String questionContent = question == null ? "未知题目" : defaultText(question.getContent());
                    String optionContent = option == null ? "未知选项" : defaultText(option.getContent());
                    Integer score = option == null ? answer.getScore() : option.getScore();
                    return "题目%s：%s；学生选择：%s；该题得分：%s。"
                            .formatted(questionNo, questionContent, optionContent, score == null ? "未知" : score);
                })
                .collect(Collectors.joining("\n"));
        builder.append(detailLines);
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
        List<String> primaryKeywords = new ArrayList<>();
        String scaleCode = defaultText(scale.getCode()).toUpperCase();
        String contentText = assessmentContext.toLowerCase();
        boolean lowRisk = "LOW".equals(report.getLevelCode());

        if (!lowRisk) {
            if (scaleCode.contains("GAD") || containsAny(contentText, "焦虑", "紧张", "担忧")) {
                primaryKeywords.addAll(List.of("呼吸练习", "舒缓放松", "视觉安抚", "校园支持"));
            }
            if (scaleCode.contains("PHQ") || containsAny(contentText, "低落", "兴趣减退", "无助", "情绪")) {
                primaryKeywords.addAll(List.of("睡前放松", "视觉安抚", "校园支持", "图像引导"));
            }
            if (scaleCode.contains("SLEEP") || containsAny(contentText, "睡眠", "入睡", "失眠", "夜间")) {
                primaryKeywords.addAll(List.of("睡前放松", "睡眠修复", "视觉安抚"));
            }
            if (scaleCode.contains("STRESS") || containsAny(contentText, "压力", "考试", "专注", "学业")) {
                primaryKeywords.addAll(List.of("考试周", "专注恢复", "校园支持", "学习节律"));
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

    private MentalScaleReport getRequiredReport(Long reportId) {
        MentalScaleReport report = mentalScaleReportMapper.selectById((Serializable) reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        return report;
    }

    private void verifyCounselorOwnership(Long studentUserId) {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        CounselorStudent relation = counselorStudentMapper.selectOne(
                new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                        .eq(CounselorStudent::getStudentUserId, studentUserId)
                        .last("limit 1")
        );
        if (relation == null) {
            throw new BusinessException("无权查看该学生报告");
        }
    }

    private record RecommendationProfile(List<String> primaryKeywords, boolean preferArticle) {
    }

    private record ScoredResource(ResourceSummaryResponse resource, int score) {
    }
}
