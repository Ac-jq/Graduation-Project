package sdu.jiaq.jqpro.controller.counselor;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.assessment.ReportDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ReportSummaryResponse;
import sdu.jiaq.jqpro.service.ReportService;

import java.util.List;

/**
 * 咨询师查看学生报告接口。
 */
@RestController
@RequestMapping("/api/counselor/students/{studentUserId}/reports")
@SaCheckRole(RoleConstants.COUNSELOR)
public class CounselorReportController {

    private final ReportService reportService;

    public CounselorReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Result<List<ReportSummaryResponse>> listStudentReports(@PathVariable Long studentUserId) {
        return Result.success(reportService.listCounselorStudentReports(studentUserId));
    }

    @GetMapping("/{reportId}")
    public Result<ReportDetailResponse> getStudentReportDetail(@PathVariable Long studentUserId,
                                                               @PathVariable Long reportId) {
        return Result.success(reportService.getCounselorStudentReportDetail(studentUserId, reportId));
    }
}
