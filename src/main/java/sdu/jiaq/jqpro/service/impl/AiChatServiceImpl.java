package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.AiChatConstants;
import sdu.jiaq.jqpro.common.constant.AppointmentConstants;
import sdu.jiaq.jqpro.common.constant.ReportLevelConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.ChatCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.aichat.AiChatMessageResponse;
import sdu.jiaq.jqpro.dto.aichat.AiChatSessionResponse;
import sdu.jiaq.jqpro.dto.aichat.CreateAiChatSessionRequest;
import sdu.jiaq.jqpro.dto.aichat.SendAiChatMessageRequest;
import sdu.jiaq.jqpro.dto.aichat.SendAiChatMessageResponse;
import sdu.jiaq.jqpro.entity.AiChatMessage;
import sdu.jiaq.jqpro.entity.AiChatSession;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AiChatMessageMapper;
import sdu.jiaq.jqpro.mapper.AiChatSessionMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AiChatService;
import sdu.jiaq.jqpro.service.ai.AiChatAiClient;
import sdu.jiaq.jqpro.service.ai.AiChatAiRequest;

/**
 * Student AI mentor session service implementation.
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of("自杀", "轻生", "活不下去", "结束生命", "伤害自己", "不想活了");
    private static final int MAX_HISTORY_MESSAGES = 10;

    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final SysUserMapper sysUserMapper;
    private final AiChatAiClient aiChatAiClient;

    public AiChatServiceImpl(AiChatSessionMapper aiChatSessionMapper,
                             AiChatMessageMapper aiChatMessageMapper,
                             CounselorStudentMapper counselorStudentMapper,
                             ConsultAppointmentMapper consultAppointmentMapper,
                             SysUserMapper sysUserMapper,
                             AiChatAiClient aiChatAiClient) {
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.aiChatMessageMapper = aiChatMessageMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.aiChatAiClient = aiChatAiClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSessionResponse createSession(CreateAiChatSessionRequest request) {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        AiChatSession session = new AiChatSession();
        session.setStudentUserId(studentUserId);
        session.setTitle(resolveTitle(request == null ? null : request.getTitle()));
        session.setStatus(AiChatConstants.SESSION_ACTIVE);
        session.setRiskFlag(0);
        session.setRiskLevel(ReportLevelConstants.LOW);
        session.setLastActiveAt(LocalDateTime.now());
        aiChatSessionMapper.insert(session);
        return buildSessionResponse(session, getUserMap(studentUserId));
    }

    @Override
    public List<AiChatSessionResponse> listCurrentStudentSessions() {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        return buildSessionResponses(aiChatSessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentUserId, studentUserId)
                .orderByDesc(AiChatSession::getLastActiveAt, AiChatSession::getId)));
    }

    @Override
    public List<AiChatMessageResponse> listCurrentStudentMessages(Long sessionId) {
        AiChatSession session = getOwnedStudentSession(sessionId);
        return listMessagesBySession(session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendAiChatMessageResponse sendMessage(Long sessionId, SendAiChatMessageRequest request) {
        AiChatSession session = getOwnedStudentSession(sessionId);
        if (!AiChatConstants.SESSION_ACTIVE.equals(session.getStatus())) {
            throw new BusinessException("该会话已归档，请开启新对话");
        }
        String content = request.getContent().trim();
        RiskAnalysis risk = analyzeRisk(content);
        List<AiChatAiRequest.ConversationMessage> historyMessages = loadConversationHistory(sessionId);

        AiChatMessage studentMessage = new AiChatMessage();
        studentMessage.setSessionId(sessionId);
        studentMessage.setSenderType(AiChatConstants.SENDER_STUDENT);
        studentMessage.setContentText(ChatCryptoUtil.encryptWithPrefix(content));
        studentMessage.setRiskLevel(risk.level());
        studentMessage.setHitKeywords(risk.hitKeywords());
        studentMessage.setCreatedAt(LocalDateTime.now());
        aiChatMessageMapper.insert(studentMessage);

        String aiReplyText = aiChatAiClient.generateReply(new AiChatAiRequest(
                session.getTitle(),
                risk.level(),
                risk.riskFlag(),
                historyMessages,
                content
        ));

        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setSenderType(AiChatConstants.SENDER_AI);
        aiMessage.setContentText(ChatCryptoUtil.encryptWithPrefix(aiReplyText));
        aiMessage.setRiskLevel(risk.level());
        aiMessage.setHitKeywords(risk.hitKeywords());
        aiMessage.setCreatedAt(LocalDateTime.now());
        aiChatMessageMapper.insert(aiMessage);

        session.setSummaryText(content.length() > 80 ? content.substring(0, 80) : content);
        session.setRiskFlag(risk.riskFlag() ? 1 : 0);
        session.setRiskLevel(risk.level());
        session.setLastActiveAt(aiMessage.getCreatedAt());
        aiChatSessionMapper.updateById(session);

        return SendAiChatMessageResponse.builder()
                .studentMessage(buildMessageResponse(studentMessage))
                .aiMessage(buildMessageResponse(aiMessage))
                .riskFlag(risk.riskFlag())
                .riskLevel(risk.level())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSessionResponse archiveCurrentStudentSession(Long sessionId) {
        AiChatSession session = getOwnedStudentSession(sessionId);
        if (!AiChatConstants.SESSION_ARCHIVED.equals(session.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            session.setStatus(AiChatConstants.SESSION_ARCHIVED);
            session.setArchivedAt(now);
            session.setLastActiveAt(now);
            aiChatSessionMapper.updateById(session);
        }
        return buildSessionResponse(session, getUserMap(session.getStudentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmptyCurrentStudentSession(Long sessionId) {
        AiChatSession session = getOwnedStudentSession(sessionId);
        Long messageCount = aiChatMessageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, session.getId()));
        if (messageCount != null && messageCount > 0) {
            return;
        }
        aiChatSessionMapper.deleteById(session.getId());
    }

    @Override
    public List<AiChatSessionResponse> listCounselorStudentSessions(Long studentUserId) {
        verifyCounselorStudentOwnership(studentUserId);
        return buildSessionResponses(aiChatSessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentUserId, studentUserId)
                .orderByDesc(AiChatSession::getLastActiveAt, AiChatSession::getId)));
    }

    @Override
    public List<AiChatMessageResponse> listCounselorStudentMessages(Long studentUserId, Long sessionId) {
        verifyCounselorStudentOwnership(studentUserId);
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        if (session == null || !studentUserId.equals(session.getStudentUserId())) {
            throw new BusinessException("AI会话不存在");
        }
        return listMessagesBySession(sessionId);
    }

    private List<AiChatSessionResponse> buildSessionResponses(List<AiChatSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = sessions.stream().map(AiChatSession::getStudentUserId).distinct().toList();
        Map<Long, SysUser> userMap = getUserMap(studentIds);
        return sessions.stream()
                .map(session -> buildSessionResponse(session, userMap))
                .toList();
    }

    private AiChatSessionResponse buildSessionResponse(AiChatSession session, Map<Long, SysUser> userMap) {
        SysUser student = userMap.get(session.getStudentUserId());
        return AiChatSessionResponse.builder()
                .sessionId(session.getId())
                .studentUserId(session.getStudentUserId())
                .studentName(student == null ? null : student.getDisplayName())
                .title(session.getTitle())
                .status(session.getStatus())
                .archivedAt(session.getArchivedAt())
                .summaryText(session.getSummaryText())
                .riskFlag(session.getRiskFlag() != null && session.getRiskFlag() == 1)
                .riskLevel(session.getRiskLevel())
                .lastActiveAt(session.getLastActiveAt())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private List<AiChatMessageResponse> listMessagesBySession(Long sessionId) {
        return aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByAsc(AiChatMessage::getCreatedAt, AiChatMessage::getId))
                .stream()
                .map(this::buildMessageResponse)
                .toList();
    }

    private List<AiChatAiRequest.ConversationMessage> loadConversationHistory(Long sessionId) {
        return aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByDesc(AiChatMessage::getCreatedAt, AiChatMessage::getId)
                        .last("limit " + MAX_HISTORY_MESSAGES))
                .stream()
                .sorted(Comparator
                        .comparing(AiChatMessage::getCreatedAt)
                        .thenComparing(AiChatMessage::getId))
                .map(message -> new AiChatAiRequest.ConversationMessage(
                        AiChatConstants.SENDER_AI.equals(message.getSenderType()) ? "AI导师" : "学生",
                        ChatCryptoUtil.decryptCompat(message.getContentText())
                ))
                .toList();
    }

    private AiChatMessageResponse buildMessageResponse(AiChatMessage message) {
        return AiChatMessageResponse.builder()
                .messageId(message.getId())
                .sessionId(message.getSessionId())
                .senderType(message.getSenderType())
                .content(ChatCryptoUtil.decryptCompat(message.getContentText()))
                .riskLevel(message.getRiskLevel())
                .hitKeywords(message.getHitKeywords())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private AiChatSession getOwnedStudentSession(Long sessionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getStudentUserId())) {
            throw new BusinessException("AI会话不存在");
        }
        return session;
    }

    private void verifyCounselorStudentOwnership(Long studentUserId) {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        CounselorStudent relation = counselorStudentMapper.selectOne(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentUserId, studentUserId)
                .last("limit 1"));
        if (relation != null) {
            return;
        }

        Long appointmentCount = consultAppointmentMapper.selectCount(new LambdaQueryWrapper<ConsultAppointment>()
                .eq(ConsultAppointment::getCounselorUserId, counselorUserId)
                .eq(ConsultAppointment::getStudentUserId, studentUserId)
                .in(ConsultAppointment::getStatus, List.of(
                        AppointmentConstants.APPOINTMENT_ACCEPTED,
                        AppointmentConstants.APPOINTMENT_COMPLETED
                )));
        if (appointmentCount == null || appointmentCount <= 0) {
            throw new BusinessException("无权查看该学生AI会话");
        }
    }

    private RiskAnalysis analyzeRisk(String content) {
        List<String> hitKeywords = HIGH_RISK_KEYWORDS.stream()
                .filter(content::contains)
                .toList();
        if (!hitKeywords.isEmpty()) {
            return new RiskAnalysis(true, ReportLevelConstants.HIGH, String.join(",", hitKeywords));
        }
        return new RiskAnalysis(false, ReportLevelConstants.LOW, null);
    }

    private String resolveTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新的倾诉会话";
        }
        return title.trim();
    }

    private Map<Long, SysUser> getUserMap(Long userId) {
        return getUserMap(List.of(userId));
    }

    private Map<Long, SysUser> getUserMap(List<Long> userIds) {
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private record RiskAnalysis(boolean riskFlag, String level, String hitKeywords) {
    }
}
