package sdu.jiaq.jqpro.dto.aichat;

import lombok.Builder;
import lombok.Data;

/**
 * 发送 AI 消息响应。
 */
@Data
@Builder
public class SendAiChatMessageResponse {

    private AiChatMessageResponse studentMessage;

    private AiChatMessageResponse aiMessage;

    private Boolean riskFlag;

    private String riskLevel;
}
