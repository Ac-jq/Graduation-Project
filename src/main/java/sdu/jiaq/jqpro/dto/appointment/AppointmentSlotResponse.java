package sdu.jiaq.jqpro.dto.appointment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生预约页的固定时段响应。
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

    @JsonProperty("isBooked")
    private boolean booked;

    @JsonProperty("isSelectable")
    private boolean selectable;

    private String timeLabel;
}
