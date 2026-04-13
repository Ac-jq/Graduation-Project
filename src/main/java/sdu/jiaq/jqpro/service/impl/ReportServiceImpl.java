package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.constant.AssessmentNoticeConstants;
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
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.ReportService;
import sdu.jiaq.jqpro.service.ResourceService;

/**
 * 测评报告查询服务。
 */
@Service
public class ReportServiceImpl implements ReportService {
    private static final String AUDIT_IP = "system";

    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final MentalScaleMapper mentalScaleMapper;
    private final SysUserMapper sysUserMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final ResourceService resourceService;
    private final AuditLogService auditLogService;

    public ReportServiceImpl(
            MentalScaleReportMapper mentalScaleReportMapper,
            MentalScaleMapper mentalScaleMapper,
            SysUserMapper sysUserMapper,
            CounselorStudentMapper counselorStudentMapper,
            ResourceService resourceService,
            AuditLogService auditLogService
    ) {
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.mentalScaleMapper = mentalScaleMapper;
        this.sysUserMapper = sysUserMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.resourceService = resourceService;
        this.auditLogService = auditLogService;
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
                .recommendedResources(buildRecommendedResources())
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

    private List<ResourceSummaryResponse> buildRecommendedResources() {
        return resourceService.listPublishedResources(null, null, null).stream().limit(3).toList();
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
}
