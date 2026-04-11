package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

/**
 * Counselor appointment load response.
 */
@Data
@Builder
public class CounselorAppointmentStatisticsResponse {

    private Long counselorUserId;

    private String counselorName;

    private long totalCount;

    private long acceptedCount;

    private long rejectedCount;

    private long pendingCount;
}
