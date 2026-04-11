package sdu.jiaq.jqpro.dto.appointment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约响应。
 */
@Data
@Builder
public class AppointmentResponse {

    private Long appointmentId;

    private Long slotId;

    private Long studentUserId;

    private String anonymousName;

    private Long counselorUserId;

    private String counselorName;

    private String issueSummary;

    private String status;

    private String resultMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;
}
