package sdu.jiaq.jqpro.controller.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.chat.ConsultChatMessageResponse;
import sdu.jiaq.jqpro.dto.chat.ConsultChatSessionResponse;
import sdu.jiaq.jqpro.service.ConsultChatService;
import sdu.jiaq.jqpro.websocket.ConsultChatWebSocketHandler;

import java.util.List;

/**
 * 私密聊天室查询接口。
 */
@RestController
@RequestMapping("/api/chat/appointments/{appointmentId}")
public class ConsultChatController {

    private final ConsultChatService consultChatService;
    private final ConsultChatWebSocketHandler consultChatWebSocketHandler;

    public ConsultChatController(ConsultChatService consultChatService,
                                 ConsultChatWebSocketHandler consultChatWebSocketHandler) {
        this.consultChatService = consultChatService;
        this.consultChatWebSocketHandler = consultChatWebSocketHandler;
    }

    @GetMapping("/session")
    public Result<ConsultChatSessionResponse> getChatSession(@PathVariable("appointmentId") Long appointmentId) {
        return Result.success(consultChatService.getAppointmentChatSession(appointmentId));
    }

    @GetMapping("/messages")
    public Result<List<ConsultChatMessageResponse>> listMessages(@PathVariable("appointmentId") Long appointmentId) {
        return Result.success(consultChatService.listAppointmentMessages(appointmentId));
    }

    @PostMapping("/close")
    public Result<ConsultChatSessionResponse> closeChat(@PathVariable("appointmentId") Long appointmentId) {
        ConsultChatSessionResponse session = consultChatService.closeAppointmentChat(appointmentId);
        try {
            consultChatWebSocketHandler.broadcastChatClosed(appointmentId, session);
        } catch (Exception exception) {
            // 数据库状态已经完成更新，广播失败不应影响关闭接口的业务结果。
        }
        return Result.success(session);
    }
}
