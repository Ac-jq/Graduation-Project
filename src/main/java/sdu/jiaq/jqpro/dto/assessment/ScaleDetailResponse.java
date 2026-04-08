package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

/**
 * 量表详情响应。
 */
@Data
@Builder
public class ScaleDetailResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    private String introduction;

    private Integer totalQuestions;

    private Integer pageSize;
}
