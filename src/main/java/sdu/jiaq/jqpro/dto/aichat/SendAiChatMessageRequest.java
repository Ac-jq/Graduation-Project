package sdu.jiaq.jqpro.dto.aichat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送 AI 消息请求。
 */
@Data
public class SendAiChatMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容长度不能超过1000")
    private String content;
}
