package sdu.jiaq.jqpro.dto.adminai;

import lombok.Builder;
import lombok.Data;

/**
 * Task item response.
 */
@Data
@Builder
public class AdminAiTaskItemResponse {

    private Long itemId;

    private String targetType;

    private Long targetId;

    private String targetLabel;

    private String account;

    private String displayName;

    private String realName;

    private String studentNo;

    private String counselorNo;

    private String college;

    private String grade;

    private String operationType;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private Integer sortNo;

    private String executeStatus;
}
