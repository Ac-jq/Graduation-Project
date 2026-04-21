package sdu.jiaq.jqpro.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import sdu.jiaq.jqpro.dto.chat.ConsultChatMessageResponse;
import sdu.jiaq.jqpro.dto.chat.ConsultChatSessionResponse;
import sdu.jiaq.jqpro.dto.chat.WebSocketChatPayload;
import sdu.jiaq.jqpro.service.ConsultChatService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私密聊天室 WebSocket 处理器。
 */
@Component
public class ConsultChatWebSocketHandler extends TextWebSocketHandler {

    private final ConsultChatService consultChatService;
    private final ObjectMapper objectMapper;
    private final Map<Long, Map<String, WebSocketSession>> appointmentSessions = new ConcurrentHashMap<>();

    public ConsultChatWebSocketHandler(ConsultChatService consultChatService, ObjectMapper objectMapper) {
        this.consultChatService = consultChatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long appointmentId = (Long) session.getAttributes().get("appointmentId");
        Long userId = (Long) session.getAttributes().get("userId");
        consultChatService.validateAndActivateChat(appointmentId, userId);

        Map<String, WebSocketSession> sessionMap = appointmentSessions.computeIfAbsent(appointmentId, key -> new ConcurrentHashMap<>());
        sessionMap.put(session.getId(), session);
        int onlineCount = countOpenSessions(sessionMap);

        sendPayload(session, WebSocketChatPayload.builder()
                .type("CONNECTED")
                .tip("聊天室连接成功")
                .onlineCount(onlineCount)
                .build());

        if (onlineCount > 1) {
            broadcast(appointmentId, WebSocketChatPayload.builder()
                    .type("SYSTEM")
                    .action("USER_JOINED")
                    .tip("对方已上线，可以开始聊天")
                    .onlineCount(onlineCount)
                    .build());
        } else {
            sendPayload(session, WebSocketChatPayload.builder()
                    .type("SYSTEM")
                    .action("WAITING_PEER")
                    .tip("你已进入聊天室，正在等待对方进入")
                    .onlineCount(onlineCount)
                    .build());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long appointmentId = (Long) session.getAttributes().get("appointmentId");
        Long userId = (Long) session.getAttributes().get("userId");
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String content = node.path("content").asText(null);
            if (content == null || content.isBlank()) {
                sendPayload(session, WebSocketChatPayload.builder()
                        .type("ERROR")
                        .tip("消息内容不能为空")
                        .build());
                return;
            }
            ConsultChatMessageResponse response = consultChatService.sendMessage(appointmentId, userId, content);
            broadcast(appointmentId, WebSocketChatPayload.builder()
                    .type("MESSAGE")
                    .message(response)
                    .onlineCount(countOpenSessions(appointmentSessions.get(appointmentId)))
                    .build());
        } catch (Exception exception) {
            sendPayload(session, WebSocketChatPayload.builder()
                    .type("ERROR")
                    .tip(exception.getMessage())
                    .build());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long appointmentId = (Long) session.getAttributes().get("appointmentId");
        if (appointmentId == null) {
            return;
        }
        Map<String, WebSocketSession> sessionMap = appointmentSessions.get(appointmentId);
        if (sessionMap == null) {
            return;
        }

        sessionMap.remove(session.getId());
        int onlineCount = countOpenSessions(sessionMap);
        if (onlineCount == 0) {
            appointmentSessions.remove(appointmentId);
            return;
        }

        broadcast(appointmentId, WebSocketChatPayload.builder()
                .type("SYSTEM")
                .action("USER_LEFT")
                .tip("对方暂时离开了聊天室")
                .onlineCount(onlineCount)
                .build());
    }

    public void broadcastChatClosed(Long appointmentId, ConsultChatSessionResponse session) throws IOException {
        broadcast(appointmentId, WebSocketChatPayload.builder()
                .type("SYSTEM")
                .action("CHAT_CLOSED")
                .tip("咨询师已结束本次聊天")
                .session(session)
                .onlineCount(countOpenSessions(appointmentSessions.get(appointmentId)))
                .build());
    }

    private int countOpenSessions(Map<String, WebSocketSession> sessionMap) {
        if (sessionMap == null) {
            return 0;
        }
        return (int) sessionMap.values().stream().filter(WebSocketSession::isOpen).count();
    }

    private void broadcast(Long appointmentId, WebSocketChatPayload payload) throws IOException {
        Map<String, WebSocketSession> sessionMap = appointmentSessions.get(appointmentId);
        if (sessionMap == null) {
            return;
        }
        String text = objectMapper.writeValueAsString(payload);
        for (WebSocketSession webSocketSession : sessionMap.values()) {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(text));
            }
        }
    }

    private void sendPayload(WebSocketSession session, WebSocketChatPayload payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }
}
