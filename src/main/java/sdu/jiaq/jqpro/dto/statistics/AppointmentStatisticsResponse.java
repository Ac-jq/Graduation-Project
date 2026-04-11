package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Appointment statistics response.
 */
@Data
@Builder
public class AppointmentStatisticsResponse {

    private long totalCount;

    private long acceptedCount;

    private long rejectedCount;

    private long pendingCount;

    private List<NamedMetricResponse> collegeDistribution;

    private List<CounselorAppointmentStatisticsResponse> counselorLoads;
}
