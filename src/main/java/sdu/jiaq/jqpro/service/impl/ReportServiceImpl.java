package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.assessment.ReportDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ReportSummaryResponse;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.MentalScale;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.ReportService;
import sdu.jiaq.jqpro.service.ResourceService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报告服务实现。
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final MentalScaleMapper mentalScaleMapper;
    private final SysUserMapper sysUserMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final ResourceService resourceService;

    public ReportServiceImpl(MentalScaleReportMapper mentalScaleReportMapper,
                             MentalScaleMapper mentalScaleMapper,
                             SysUserMapper sysUserMapper,
                             CounselorStudentMapper counselorStudentMapper,
                             ResourceService resourceService) {
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.mentalScaleMapper = mentalScaleMapper;
        this.sysUserMapper = sysUserMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.resourceService = resourceService;
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
        return buildDetail(report);
    }

    private List<ReportSummaryResponse> buildSummaryList(Long studentUserId) {
        List<MentalScaleReport> reports = mentalScaleReportMapper.selectList(new LambdaQueryWrapper<MentalScaleReport>()
                .eq(MentalScaleReport::getUserId, studentUserId)
                .orderByDesc(MentalScaleReport::getCreatedAt));
        if (reports.isEmpty()) {
            return List.of();
        }
        Map<Long, MentalScale> scaleMap = mentalScaleMapper.selectBatchIds(reports.stream()
                        .map(MentalScaleReport::getScaleId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(MentalScale::getId, Function.identity()));
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
        MentalScale scale = mentalScaleMapper.selectById(report.getScaleId());
        SysUser studentUser = sysUserMapper.selectById(report.getUserId());
        if (scale == null || studentUser == null) {
            throw new BusinessException("报告关联数据缺失");
        }
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
                .recommendAppointment(ReportLevelConstants.HIGH.equals(report.getLevelCode())
                        || ReportLevelConstants.MEDIUM.equals(report.getLevelCode()))
                .recommendedResources(buildRecommendedResources(report))
                .createdAt(report.getCreatedAt())
                .build();
    }

    private String buildRecommendationNote(String levelCode) {
        if (ReportLevelConstants.HIGH.equals(levelCode)) {
            return "Current result suggests that you should combine self-help resources with counselor support as soon as possible.";
        }
        if (ReportLevelConstants.MEDIUM.equals(levelCode)) {
            return "Current result suggests starting with self-help resources and considering an appointment if distress continues.";
        }
        return "Current result is relatively stable. You can keep following self-help resources for daily mental maintenance.";
    }

    private List<ResourceSummaryResponse> buildRecommendedResources(MentalScaleReport report) {
        return resourceService.listPublishedResources(null, null, null).stream()
                .limit(3)
                .toList();
    }

    private MentalScaleReport getRequiredReport(Long reportId) {
        MentalScaleReport report = mentalScaleReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        return report;
    }

    private void verifyCounselorOwnership(Long studentUserId) {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        CounselorStudent relation = counselorStudentMapper.selectOne(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentUserId, studentUserId)
                .last("limit 1"));
        if (relation == null) {
            throw new BusinessException("无权查看该学生报告");
        }
    }
}
