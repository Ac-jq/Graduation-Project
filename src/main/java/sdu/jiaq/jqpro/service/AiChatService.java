package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.aichat.AiChatMessageResponse;
import sdu.jiaq.jqpro.dto.aichat.AiChatSessionResponse;
import sdu.jiaq.jqpro.dto.aichat.CreateAiChatSessionRequest;
import sdu.jiaq.jqpro.dto.aichat.SendAiChatMessageRequest;
import sdu.jiaq.jqpro.dto.aichat.SendAiChatMessageResponse;

import java.util.List;

/**
 * AI 会话服务。
 */
public interface AiChatService {

    AiChatSessionResponse createSession(CreateAiChatSessionRequest request);

    List<AiChatSessionResponse> listCurrentStudentSessions();

    List<AiChatMessageResponse> listCurrentStudentMessages(Long sessionId);

    SendAiChatMessageResponse sendMessage(Long sessionId, SendAiChatMessageRequest request);

    List<AiChatSessionResponse> listCounselorStudentSessions(Long studentUserId);

    List<AiChatMessageResponse> listCounselorStudentMessages(Long studentUserId, Long sessionId);
}
