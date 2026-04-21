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

    private String counselorAvatarUrl;

    private String issueSummary;

    private String status;

    private String resultMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    /**
     * 当前是否允许直接进入聊天室。
     */
    private Boolean chatAvailable;

    /**
     * 聊天室是否已经结束。
     */
    private Boolean chatEnded;

    /**
     * 聊天室状态。
     */
    private String chatStatus;

    /**
     * 聊天室是否已封存。
     */
    private Boolean chatSealed;
}
