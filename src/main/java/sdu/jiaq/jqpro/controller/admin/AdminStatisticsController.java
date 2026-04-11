package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.statistics.AppointmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.AssessmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.OverviewStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.ResourceStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.StatisticsExportRowResponse;
import sdu.jiaq.jqpro.service.StatisticsService;

import java.util.List;

/**
 * Admin statistics controller.
 */
@RestController
@RequestMapping("/api/admin/statistics")
@SaCheckRole(RoleConstants.ADMIN)
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    public AdminStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public Result<OverviewStatisticsResponse> getOverview() {
        return Result.success(statisticsService.getOverview());
    }

    @GetMapping("/assessments")
    public Result<AssessmentStatisticsResponse> getAssessments() {
        return Result.success(statisticsService.getAssessments());
    }

    @GetMapping("/resources")
    public Result<ResourceStatisticsResponse> getResources() {
        return Result.success(statisticsService.getResources());
    }

    @GetMapping("/appointments")
    public Result<AppointmentStatisticsResponse> getAppointments() {
        return Result.success(statisticsService.getAppointments());
    }

    @GetMapping("/export")
    public Result<List<StatisticsExportRowResponse>> export(@RequestParam String dimension) {
        return Result.success(statisticsService.exportByDimension(dimension));
    }
}
