package sdu.jiaq.jqpro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import sdu.jiaq.jqpro.websocket.ConsultChatHandshakeInterceptor;
import sdu.jiaq.jqpro.websocket.ConsultChatWebSocketHandler;

/**
 * WebSocket 配置。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConsultChatWebSocketHandler consultChatWebSocketHandler;
    private final ConsultChatHandshakeInterceptor consultChatHandshakeInterceptor;

    public WebSocketConfig(ConsultChatWebSocketHandler consultChatWebSocketHandler,
                           ConsultChatHandshakeInterceptor consultChatHandshakeInterceptor) {
        this.consultChatWebSocketHandler = consultChatWebSocketHandler;
        this.consultChatHandshakeInterceptor = consultChatHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consultChatWebSocketHandler, "/ws/consult-chat")
                .addInterceptors(consultChatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
