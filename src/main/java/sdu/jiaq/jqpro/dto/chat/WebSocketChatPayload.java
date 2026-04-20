package sdu.jiaq.jqpro.dto.chat;

import lombok.Builder;
import lombok.Data;

/**
 * WebSocket 聊天消息载荷。
 */
@Data
@Builder
public class WebSocketChatPayload {

    private String type;

    private String action;

    private ConsultChatMessageResponse message;

    private String tip;

    private Integer onlineCount;
}
