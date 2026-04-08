package sdu.jiaq.jqpro.dto.aichat;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建 AI 会话请求。
 */
@Data
public class CreateAiChatSessionRequest {

    @Size(max = 128, message = "会话标题长度不能超过128")
    private String title;
}
