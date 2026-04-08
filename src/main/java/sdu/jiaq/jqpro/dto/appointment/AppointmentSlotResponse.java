package sdu.jiaq.jqpro.dto.appointment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约时段响应。
 */
@Data
@Builder
public class AppointmentSlotResponse {

    private Long slotId;

    private Long counselorUserId;

    private String counselorName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;
}
