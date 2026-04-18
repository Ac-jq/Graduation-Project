package sdu.jiaq.jqpro.dto.adminuser;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin-side user summary.
 */
@Data
@Builder
public class AdminUserSummaryResponse {

    private Long userId;

    private String account;

    private String roleCode;

    private String realName;

    private String displayName;

    private String studentNo;

    private String counselorNo;

    private String status;

    private String college;

    private String grade;

    private String phone;

    private LocalDateTime createdAt;
}
