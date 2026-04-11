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

    private String operationType;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private Integer sortNo;

    private String executeStatus;
}
