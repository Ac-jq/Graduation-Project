package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

/**
 * 题目选项响应。
 */
@Data
@Builder
public class QuestionOptionResponse {

    private Long id;

    private String optionCode;

    private String content;

    private Integer score;
}
