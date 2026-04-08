package sdu.jiaq.jqpro.dto.assessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 答案批量保存请求。
 */
@Data
public class AnswerSaveRequest {

    @Valid
    @NotEmpty(message = "至少保存一条答案")
    private List<AnswerItemRequest> answers;
}
