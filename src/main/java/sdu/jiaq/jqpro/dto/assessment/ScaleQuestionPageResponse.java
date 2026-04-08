package sdu.jiaq.jqpro.dto.assessment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页题目响应。
 */
@Data
@Builder
public class ScaleQuestionPageResponse {

    private Long sessionId;

    private Integer pageNum;

    private Integer pageSize;

    private Long total;

    private Integer answeredCount;

    private Integer totalQuestions;

    private List<QuestionResponse> records;
}
