package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户参与情况统计。
 */
@Data
@Builder
public class UserEngagementStatisticsResponse {

    private long totalStudents;

    private long activeStudents;

    private long highlyEngagedStudents;

    private List<UserEngagementItemResponse> items;
}
