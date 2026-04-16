package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
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
import sdu.jiaq.jqpro.dto.statistics.UserInterventionEffectExportRow;
import sdu.jiaq.jqpro.dto.statistics.UserEngagementStatisticsResponse;
import sdu.jiaq.jqpro.service.StatisticsService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/engagements")
    public Result<UserEngagementStatisticsResponse> getUserEngagements() {
        return Result.success(statisticsService.getUserEngagements());
    }

    @GetMapping("/export")
    public Result<List<StatisticsExportRowResponse>> export(@RequestParam String dimension) {
        return Result.success(statisticsService.exportByDimension(dimension));
    }

    @GetMapping("/export/intervention-effect")
    public void exportInterventionEffect(HttpServletResponse response) throws IOException {
        List<UserInterventionEffectExportRow> rows = statisticsService.listUserInterventionEffectRows();
        String fileName = URLEncoder.encode("用户个体心理健康干预效果评估报表", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), UserInterventionEffectExportRow.class)
                .autoCloseStream(false)
                .sheet("干预效果评估")
                .doWrite(rows);
    }
}
