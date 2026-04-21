package sdu.jiaq.jqpro.dto.counselor;

import lombok.Builder;
import lombok.Data;

/**
 * 咨询师个人资料响应。
 */
@Data
@Builder
public class CounselorProfileResponse {

    private Long userId;

    private String account;

    private String realName;

    private String displayName;

    private String counselorNo;

    private String roleCode;

    private String avatarUrl;
}
