package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.dto.statistics.AppointmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.AssessmentCompareSummaryResponse;
import sdu.jiaq.jqpro.dto.statistics.AssessmentScaleSummaryResponse;
import sdu.jiaq.jqpro.dto.statistics.AssessmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.CounselorAppointmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.NamedMetricResponse;
import sdu.jiaq.jqpro.dto.statistics.OverviewStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.ResourceCategoryStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.ResourceStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.StatisticsExportRowResponse;
import sdu.jiaq.jqpro.dto.statistics.TopResourceStatisticsResponse;
import sdu.jiaq.jqpro.entity.AiChatSession;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.MentalResource;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.ResourceCategory;
import sdu.jiaq.jqpro.entity.ResourceFavorite;
import sdu.jiaq.jqpro.entity.ResourceViewLog;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AiChatSessionMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.ResourceCategoryMapper;
import sdu.jiaq.jqpro.mapper.ResourceFavoriteMapper;
import sdu.jiaq.jqpro.mapper.ResourceViewLogMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.StatisticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Statistics service implementation.
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final MentalScaleMapper mentalScaleMapper;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final ResourceCategoryMapper resourceCategoryMapper;
    private final ResourceFavoriteMapper resourceFavoriteMapper;
    private final ResourceViewLogMapper resourceViewLogMapper;

    public StatisticsServiceImpl(SysUserMapper sysUserMapper,
                                 StudentProfileMapper studentProfileMapper,
                                 MentalScaleReportMapper mentalScaleReportMapper,
                                 MentalScaleMapper mentalScaleMapper,
                                 AiChatSessionMapper aiChatSessionMapper,
                                 ConsultAppointmentMapper consultAppointmentMapper,
                                 MentalResourceMapper mentalResourceMapper,
                                 ResourceCategoryMapper resourceCategoryMapper,
                                 ResourceFavoriteMapper resourceFavoriteMapper,
                                 ResourceViewLogMapper resourceViewLogMapper) {
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.mentalScaleMapper = mentalScaleMapper;
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.mentalResourceMapper = mentalResourceMapper;
        this.resourceCategoryMapper = resourceCategoryMapper;
        this.resourceFavoriteMapper = resourceFavoriteMapper;
        this.resourceViewLogMapper = resourceViewLogMapper;
    }

    @Override
    public OverviewStatisticsResponse getOverview() {
        List<SysUser> students = listUsersByRole(RoleConstants.STUDENT);
        List<SysUser> counselors = listUsersByRole(RoleConstants.COUNSELOR);
        List<MentalScaleReport> reports = mentalScaleReportMapper.selectList(null);
        List<AiChatSession> aiSessions = aiChatSessionMapper.selectList(null);
        List<ConsultAppointment> appointments = consultAppointmentMapper.selectList(null);
        List<MentalResource> resources = mentalResourceMapper.selectList(null);
        List<ResourceFavorite> favorites = resourceFavoriteMapper.selectList(null);
        List<ResourceViewLog> views = resourceViewLogMapper.selectList(null);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Set<Long> activeUsers = new HashSet<>();
        aiSessions.stream()
                .filter(item -> item.getLastActiveAt() != null && !item.getLastActiveAt().isBefore(todayStart))
                .map(AiChatSession::getStudentUserId)
                .forEach(activeUsers::add);
        appointments.stream()
                .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isBefore(todayStart))
                .map(ConsultAppointment::getStudentUserId)
                .forEach(activeUsers::add);
        reports.stream()
                .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isBefore(todayStart))
                .map(MentalScaleReport::getUserId)
                .forEach(activeUsers::add);
        views.stream()
                .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isBefore(todayStart))
                .map(ResourceViewLog::getStudentUserId)
                .forEach(activeUsers::add);

        return OverviewStatisticsResponse.builder()
                .studentCount(students.size())
                .counselorCount(counselors.size())
                .scaleReportCount(reports.size())
                .aiSessionCount(aiSessions.size())
                .appointmentCount(appointments.size())
                .resourceCount(resources.size())
                .publishedResourceCount(resources.stream().filter(item -> "PUBLISHED".equals(item.getStatus())).count())
                .resourceViewCount(views.size())
                .favoriteCount(favorites.size())
                .dailyActiveUsers(activeUsers.size())
                .build();
    }

    @Override
    public AssessmentStatisticsResponse getAssessments() {
        List<MentalScaleReport> reports = mentalScaleReportMapper.selectList(new LambdaQueryWrapper<MentalScaleReport>()
                .orderByAsc(MentalScaleReport::getCreatedAt, MentalScaleReport::getId));
        if (reports.isEmpty()) {
            return AssessmentStatisticsResponse.builder()
                    .totalReports(0)
                    .participantCount(0)
                    .averageScore(0)
                    .levelDistribution(List.of())
                    .scales(List.of())
                    .compareSummary(AssessmentCompareSummaryResponse.builder()
                            .sampleCount(0)
                            .averageDelta(0)
                            .improvedCount(0)
                            .stableCount(0)
                            .worsenedCount(0)
                            .smallSampleWarning(true)
                            .build())
                    .build();
        }

        Map<Long, MentalScale> scaleMap = mentalScaleMapper.selectBatchIds(reports.stream()
                        .map(MentalScaleReport::getScaleId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(MentalScale::getId, Function.identity()));

        List<NamedMetricResponse> levelDistribution = reports.stream()
                .collect(Collectors.groupingBy(MentalScaleReport::getLevelCode, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> NamedMetricResponse.builder().name(entry.getKey()).count(entry.getValue()).build())
                .toList();

        List<AssessmentScaleSummaryResponse> scales = reports.stream()
                .collect(Collectors.groupingBy(MentalScaleReport::getScaleId))
                .entrySet().stream()
                .map(entry -> AssessmentScaleSummaryResponse.builder()
                        .scaleId(entry.getKey())
                        .scaleName(scaleMap.containsKey(entry.getKey()) ? scaleMap.get(entry.getKey()).getName() : "未知量表")
                        .participantCount(entry.getValue().stream().map(MentalScaleReport::getUserId).distinct().count())
                        .reportCount(entry.getValue().size())
                        .averageScore(entry.getValue().stream().mapToInt(MentalScaleReport::getTotalScore).average().orElse(0))
                        .build())
                .sorted(Comparator.comparing(AssessmentScaleSummaryResponse::getScaleId))
                .toList();

        List<Integer> deltas = reports.stream()
                .collect(Collectors.groupingBy(item -> item.getUserId() + "-" + item.getScaleId()))
                .values().stream()
                .filter(group -> group.size() >= 2)
                .map(group -> group.get(group.size() - 1).getTotalScore() - group.get(0).getTotalScore())
                .toList();

        return AssessmentStatisticsResponse.builder()
                .totalReports(reports.size())
                .participantCount(reports.stream().map(MentalScaleReport::getUserId).distinct().count())
                .averageScore(reports.stream().mapToInt(MentalScaleReport::getTotalScore).average().orElse(0))
                .levelDistribution(levelDistribution)
                .scales(scales)
                .compareSummary(AssessmentCompareSummaryResponse.builder()
                        .sampleCount(deltas.size())
                        .averageDelta(deltas.stream().mapToInt(Integer::intValue).average().orElse(0))
                        .improvedCount(deltas.stream().filter(delta -> delta < 0).count())
                        .stableCount(deltas.stream().filter(delta -> delta == 0).count())
                        .worsenedCount(deltas.stream().filter(delta -> delta > 0).count())
                        .smallSampleWarning(deltas.size() < 3)
                        .build())
                .build();
    }

    @Override
    public ResourceStatisticsResponse getResources() {
        List<MentalResource> resources = mentalResourceMapper.selectList(null);
        List<ResourceCategory> categories = resourceCategoryMapper.selectList(null);
        List<ResourceFavorite> favorites = resourceFavoriteMapper.selectList(null);
        List<ResourceViewLog> views = resourceViewLogMapper.selectList(null);

        Map<Long, Long> favoriteCountMap = favorites.stream()
                .collect(Collectors.groupingBy(ResourceFavorite::getResourceId, Collectors.counting()));
        Map<Long, Long> viewCountMap = views.stream()
                .collect(Collectors.groupingBy(ResourceViewLog::getResourceId, Collectors.counting()));
        Map<Long, List<MentalResource>> resourceByCategory = resources.stream()
                .collect(Collectors.groupingBy(MentalResource::getCategoryId));

        List<ResourceCategoryStatisticsResponse> categoryStats = categories.stream()
                .map(category -> {
                    List<MentalResource> categoryResources = resourceByCategory.getOrDefault(category.getId(), List.of());
                    List<Long> resourceIds = categoryResources.stream().map(MentalResource::getId).toList();
                    return ResourceCategoryStatisticsResponse.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .resourceCount(categoryResources.size())
                            .publishedCount(categoryResources.stream().filter(item -> "PUBLISHED".equals(item.getStatus())).count())
                            .viewCount(resourceIds.stream().mapToLong(resourceId -> viewCountMap.getOrDefault(resourceId, 0L)).sum())
                            .favoriteCount(resourceIds.stream().mapToLong(resourceId -> favoriteCountMap.getOrDefault(resourceId, 0L)).sum())
                            .build();
                })
                .sorted(Comparator.comparing(ResourceCategoryStatisticsResponse::getCategoryId))
                .toList();

        Map<Long, ResourceCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ResourceCategory::getId, Function.identity()));
        List<TopResourceStatisticsResponse> topResources = resources.stream()
                .map(resource -> TopResourceStatisticsResponse.builder()
                        .resourceId(resource.getId())
                        .title(resource.getTitle())
                        .categoryName(categoryMap.containsKey(resource.getCategoryId()) ? categoryMap.get(resource.getCategoryId()).getName() : "未分类")
                        .viewCount(viewCountMap.getOrDefault(resource.getId(), 0L))
                        .favoriteCount(favoriteCountMap.getOrDefault(resource.getId(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(TopResourceStatisticsResponse::getViewCount).reversed()
                        .thenComparingLong(TopResourceStatisticsResponse::getFavoriteCount).reversed())
                .limit(5)
                .toList();

        return ResourceStatisticsResponse.builder()
                .resourceCount(resources.size())
                .publishedCount(resources.stream().filter(item -> "PUBLISHED".equals(item.getStatus())).count())
                .totalViews(views.size())
                .totalFavorites(favorites.size())
                .categories(categoryStats)
                .topResources(topResources)
                .build();
    }

    @Override
    public AppointmentStatisticsResponse getAppointments() {
        List<ConsultAppointment> appointments = consultAppointmentMapper.selectList(null);
        Map<Long, StudentProfile> profileMap = studentProfileMapper.selectList(null).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        Map<Long, SysUser> counselorMap = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleCode, RoleConstants.COUNSELOR))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<NamedMetricResponse> collegeDistribution = appointments.stream()
                .collect(Collectors.groupingBy(item -> {
                    StudentProfile profile = profileMap.get(item.getStudentUserId());
                    return profile == null || profile.getCollege() == null || profile.getCollege().isBlank() ? "未填写" : profile.getCollege();
                }, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> NamedMetricResponse.builder().name(entry.getKey()).count(entry.getValue()).build())
                .toList();

        List<CounselorAppointmentStatisticsResponse> counselorLoads = appointments.stream()
                .collect(Collectors.groupingBy(ConsultAppointment::getCounselorUserId))
                .entrySet().stream()
                .map(entry -> {
                    List<ConsultAppointment> data = entry.getValue();
                    SysUser counselor = counselorMap.get(entry.getKey());
                    return CounselorAppointmentStatisticsResponse.builder()
                            .counselorUserId(entry.getKey())
                            .counselorName(counselor == null ? "未知咨询师" : counselor.getDisplayName())
                            .totalCount(data.size())
                            .acceptedCount(data.stream().filter(item -> "ACCEPTED".equals(item.getStatus())).count())
                            .rejectedCount(data.stream().filter(item -> "REJECTED".equals(item.getStatus())).count())
                            .pendingCount(data.stream().filter(item -> "PENDING".equals(item.getStatus())).count())
                            .build();
                })
                .sorted(Comparator.comparingLong(CounselorAppointmentStatisticsResponse::getTotalCount).reversed())
                .toList();

        return AppointmentStatisticsResponse.builder()
                .totalCount(appointments.size())
                .acceptedCount(appointments.stream().filter(item -> "ACCEPTED".equals(item.getStatus())).count())
                .rejectedCount(appointments.stream().filter(item -> "REJECTED".equals(item.getStatus())).count())
                .pendingCount(appointments.stream().filter(item -> "PENDING".equals(item.getStatus())).count())
                .collegeDistribution(collegeDistribution)
                .counselorLoads(counselorLoads)
                .build();
    }

    @Override
    public List<StatisticsExportRowResponse> exportByDimension(String dimension) {
        String normalizedDimension = dimension == null ? "" : dimension.trim().toLowerCase();
        if (!Set.of("college", "grade", "gender").contains(normalizedDimension)) {
            throw new BusinessException("不支持的导出维度，仅支持 college / grade / gender");
        }

        List<StudentProfile> profiles = studentProfileMapper.selectList(null);
        List<MentalScaleReport> reports = mentalScaleReportMapper.selectList(null);
        List<AiChatSession> aiSessions = aiChatSessionMapper.selectList(null);
        List<ConsultAppointment> appointments = consultAppointmentMapper.selectList(null);
        List<ResourceFavorite> favorites = resourceFavoriteMapper.selectList(null);
        List<ResourceViewLog> views = resourceViewLogMapper.selectList(null);

        Map<Long, List<MentalScaleReport>> reportMap = reports.stream().collect(Collectors.groupingBy(MentalScaleReport::getUserId));
        Map<Long, Long> aiSessionCountMap = aiSessions.stream().collect(Collectors.groupingBy(AiChatSession::getStudentUserId, Collectors.counting()));
        Map<Long, Long> appointmentCountMap = appointments.stream().collect(Collectors.groupingBy(ConsultAppointment::getStudentUserId, Collectors.counting()));
        Map<Long, Long> favoriteCountMap = favorites.stream().collect(Collectors.groupingBy(ResourceFavorite::getStudentUserId, Collectors.counting()));
        Map<Long, Long> viewCountMap = views.stream().collect(Collectors.groupingBy(ResourceViewLog::getStudentUserId, Collectors.counting()));

        return profiles.stream()
                .collect(Collectors.groupingBy(profile -> switch (normalizedDimension) {
                    case "grade" -> normalizeDimensionValue(profile.getGrade());
                    case "gender" -> normalizeDimensionValue(profile.getGender());
                    default -> normalizeDimensionValue(profile.getCollege());
                }))
                .entrySet().stream()
                .map(entry -> {
                    List<Long> userIds = entry.getValue().stream().map(StudentProfile::getUserId).toList();
                    List<MentalScaleReport> scopedReports = userIds.stream()
                            .map(reportMap::get)
                            .filter(Objects::nonNull)
                            .flatMap(Collection::stream)
                            .toList();
                    return StatisticsExportRowResponse.builder()
                            .dimension(normalizedDimension)
                            .dimensionValue(entry.getKey())
                            .studentCount(userIds.size())
                            .reportCount(scopedReports.size())
                            .averageScore(scopedReports.stream().mapToInt(MentalScaleReport::getTotalScore).average().orElse(0))
                            .aiSessionCount(userIds.stream().mapToLong(userId -> aiSessionCountMap.getOrDefault(userId, 0L)).sum())
                            .appointmentCount(userIds.stream().mapToLong(userId -> appointmentCountMap.getOrDefault(userId, 0L)).sum())
                            .resourceViewCount(userIds.stream().mapToLong(userId -> viewCountMap.getOrDefault(userId, 0L)).sum())
                            .favoriteCount(userIds.stream().mapToLong(userId -> favoriteCountMap.getOrDefault(userId, 0L)).sum())
                            .build();
                })
                .sorted(Comparator.comparing(StatisticsExportRowResponse::getDimensionValue))
                .toList();
    }

    private List<SysUser> listUsersByRole(String roleCode) {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleCode, roleCode));
    }

    private String normalizeDimensionValue(String value) {
        return value == null || value.isBlank() ? "未填写" : value;
    }
}
