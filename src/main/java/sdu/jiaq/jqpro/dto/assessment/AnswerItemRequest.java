package sdu.jiaq.jqpro.dto.assessment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单题答案保存请求。
 */
@Data
public class AnswerItemRequest {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "选项ID不能为空")
    private Long optionId;
}
