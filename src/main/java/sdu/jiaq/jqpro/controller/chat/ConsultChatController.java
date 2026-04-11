package sdu.jiaq.jqpro.controller.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.chat.ConsultChatMessageResponse;
import sdu.jiaq.jqpro.dto.chat.ConsultChatSessionResponse;
import sdu.jiaq.jqpro.service.ConsultChatService;

import java.util.List;

/**
 * 私密聊天室查询接口。
 */
@RestController
@RequestMapping("/api/chat/appointments/{appointmentId}")
public class ConsultChatController {

    private final ConsultChatService consultChatService;

    public ConsultChatController(ConsultChatService consultChatService) {
        this.consultChatService = consultChatService;
    }

    @GetMapping("/session")
    public Result<ConsultChatSessionResponse> getChatSession(@PathVariable Long appointmentId) {
        return Result.success(consultChatService.getAppointmentChatSession(appointmentId));
    }

    @GetMapping("/messages")
    public Result<List<ConsultChatMessageResponse>> listMessages(@PathVariable Long appointmentId) {
        return Result.success(consultChatService.listAppointmentMessages(appointmentId));
    }
}
