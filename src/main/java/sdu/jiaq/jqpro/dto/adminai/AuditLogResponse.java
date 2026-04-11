package sdu.jiaq.jqpro.dto.adminai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Audit log query response.
 */
@Data
@Builder
public class AuditLogResponse {

    private Long logId;

    private Long userId;

    private String userDisplayName;

    private String actionCode;

    private String actionName;

    private String detailText;

    private String ipAddress;

    private LocalDateTime createdAt;
}
