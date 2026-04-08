package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

/**
 * 量表摘要响应。
 */
@Data
@Builder
public class ScaleSummaryResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer totalQuestions;

    private Integer pageSize;
}
