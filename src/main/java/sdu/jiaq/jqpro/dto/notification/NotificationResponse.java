package sdu.jiaq.jqpro.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应。
 */
@Data
@Builder
public class NotificationResponse {

    private Long notificationId;

    private String title;

    private String contentText;

    private Boolean read;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
