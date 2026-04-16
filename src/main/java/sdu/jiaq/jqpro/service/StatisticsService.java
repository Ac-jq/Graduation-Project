package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.statistics.AppointmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.AssessmentStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.OverviewStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.ResourceStatisticsResponse;
import sdu.jiaq.jqpro.dto.statistics.StatisticsExportRowResponse;
import sdu.jiaq.jqpro.dto.statistics.UserInterventionEffectExportRow;
import sdu.jiaq.jqpro.dto.statistics.UserEngagementStatisticsResponse;

import java.util.List;

/**
 * Statistics service.
 */
public interface StatisticsService {

    OverviewStatisticsResponse getOverview();

    AssessmentStatisticsResponse getAssessments();

    ResourceStatisticsResponse getResources();

    AppointmentStatisticsResponse getAppointments();

    UserEngagementStatisticsResponse getUserEngagements();

    List<StatisticsExportRowResponse> exportByDimension(String dimension);

    List<UserInterventionEffectExportRow> listUserInterventionEffectRows();
}
