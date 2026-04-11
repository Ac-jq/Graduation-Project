package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.chat.ConsultChatMessageResponse;
import sdu.jiaq.jqpro.dto.chat.ConsultChatSessionResponse;

import java.util.List;

/**
 * 私密聊天室服务。
 */
public interface ConsultChatService {

    ConsultChatSessionResponse getAppointmentChatSession(Long appointmentId);

    List<ConsultChatMessageResponse> listAppointmentMessages(Long appointmentId);

    ConsultChatSessionResponse validateAndActivateChat(Long appointmentId, Long userId);

    ConsultChatMessageResponse sendMessage(Long appointmentId, Long userId, String content);

    void archiveIfExpired(Long appointmentId);
}
