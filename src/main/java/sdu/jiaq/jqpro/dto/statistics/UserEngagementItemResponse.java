package sdu.jiaq.jqpro.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户参与情况明细。
 */
@Data
@Builder
public class UserEngagementItemResponse {

    private Long userId;

    private String displayName;

    private String studentNo;

    private String college;

    private String grade;

    private long assessmentCount;

    private double averageScore;

    private long aiSessionCount;

    private long appointmentCount;

    private long resourceViewCount;

    private long favoriteCount;

    private long engagementScore;

    private LocalDateTime latestActivityAt;
}
