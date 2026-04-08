package sdu.jiaq.jqpro.controller.student;

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
 * 学生报告接口。
 */
@RestController
@RequestMapping("/api/student/reports")
@SaCheckRole(RoleConstants.STUDENT)
public class StudentReportController {

    private final ReportService reportService;

    public StudentReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Result<List<ReportSummaryResponse>> listCurrentStudentReports() {
        return Result.success(reportService.listCurrentStudentReports());
    }

    @GetMapping("/{reportId}")
    public Result<ReportDetailResponse> getCurrentStudentReportDetail(@PathVariable Long reportId) {
        return Result.success(reportService.getCurrentStudentReportDetail(reportId));
    }
}
