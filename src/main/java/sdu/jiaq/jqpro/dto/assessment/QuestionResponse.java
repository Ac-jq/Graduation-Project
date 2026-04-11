package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 题目响应。
 */
@Data
@Builder
public class QuestionResponse {

    private Long questionId;

    private Integer questionNo;

    private String content;

    private Long selectedOptionId;

    private List<QuestionOptionResponse> options;
}
