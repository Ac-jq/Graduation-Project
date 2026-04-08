package sdu.jiaq.jqpro.websocket;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import sdu.jiaq.jqpro.common.exception.BusinessException;

import java.util.Map;

/**
 * 聊天室握手拦截器。
 */
@Component
public class ConsultChatHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest httpServletRequest = servletServerHttpRequest.getServletRequest();
        String token = httpServletRequest.getParameter("token");
        String appointmentId = httpServletRequest.getParameter("appointmentId");
        if (token == null || token.isBlank() || appointmentId == null || appointmentId.isBlank()) {
            return false;
        }

        Object loginId;
        try {
            loginId = StpUtil.getLoginIdByToken(token);
        } catch (Exception exception) {
            throw new BusinessException("WebSocket登录态无效");
        }
        if (loginId == null) {
            return false;
        }
        attributes.put("userId", Long.parseLong(String.valueOf(loginId)));
        attributes.put("appointmentId", Long.parseLong(appointmentId));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
