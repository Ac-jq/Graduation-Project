package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.AiChatConstants;
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
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AiChatMessageMapper;
import sdu.jiaq.jqpro.mapper.AiChatSessionMapper;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AiChatService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 会话服务实现。
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of("自杀", "轻生", "活不下去", "结束生命", "伤害自己", "不想活了");

    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectProvider<ChatModel> chatModelProvider;

    public AiChatServiceImpl(AiChatSessionMapper aiChatSessionMapper,
                             AiChatMessageMapper aiChatMessageMapper,
                             CounselorStudentMapper counselorStudentMapper,
                             SysUserMapper sysUserMapper,
                             ObjectProvider<ChatModel> chatModelProvider) {
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.aiChatMessageMapper = aiChatMessageMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.sysUserMapper = sysUserMapper;
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSessionResponse createSession(CreateAiChatSessionRequest request) {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        AiChatSession session = new AiChatSession();
        session.setStudentUserId(studentUserId);
        session.setTitle(resolveTitle(request.getTitle()));
        session.setStatus(AiChatConstants.SESSION_ACTIVE);
        session.setRiskFlag(0);
        session.setLastActiveAt(LocalDateTime.now());
        aiChatSessionMapper.insert(session);
        return buildSessionResponse(session, getUserMap(studentUserId));
    }

    @Override
    public List<AiChatSessionResponse> listCurrentStudentSessions() {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        return buildSessionResponses(aiChatSessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentUserId, studentUserId)
                .orderByDesc(AiChatSession::getLastActiveAt)));
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
        RiskAnalysis risk = analyzeRisk(request.getContent());

        AiChatMessage studentMessage = new AiChatMessage();
        studentMessage.setSessionId(sessionId);
        studentMessage.setSenderType(AiChatConstants.SENDER_STUDENT);
        studentMessage.setContentText(ChatCryptoUtil.encryptWithPrefix(request.getContent()));
        studentMessage.setRiskLevel(risk.level());
        studentMessage.setHitKeywords(risk.hitKeywords());
        aiChatMessageMapper.insert(studentMessage);

        String aiReplyText = generateAiReply(request.getContent(), risk.level());
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setSenderType(AiChatConstants.SENDER_AI);
        aiMessage.setContentText(ChatCryptoUtil.encryptWithPrefix(aiReplyText));
        aiMessage.setRiskLevel(risk.level());
        aiMessage.setHitKeywords(risk.hitKeywords());
        aiChatMessageMapper.insert(aiMessage);

        session.setSummaryText(request.getContent().length() > 60 ? request.getContent().substring(0, 60) : request.getContent());
        session.setRiskFlag(risk.riskFlag() ? 1 : 0);
        session.setRiskLevel(risk.level());
        session.setLastActiveAt(LocalDateTime.now());
        aiChatSessionMapper.updateById(session);

        return SendAiChatMessageResponse.builder()
                .studentMessage(buildMessageResponse(studentMessage))
                .aiMessage(buildMessageResponse(aiMessage))
                .riskFlag(risk.riskFlag())
                .riskLevel(risk.level())
                .build();
    }

    @Override
    public List<AiChatSessionResponse> listCounselorStudentSessions(Long studentUserId) {
        verifyCounselorStudentOwnership(studentUserId);
        return buildSessionResponses(aiChatSessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentUserId, studentUserId)
                .orderByDesc(AiChatSession::getLastActiveAt)));
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
        return sessions.stream()
                .map(session -> buildSessionResponse(session, getUserMap(studentIds)))
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
        if (relation == null) {
            throw new BusinessException("无权查看该学生AI会话");
        }
    }

    private String generateAiReply(String content, String riskLevel) {
        if (ReportLevelConstants.HIGH.equals(riskLevel)) {
            return "我注意到你刚刚提到的内容可能说明你正处在非常辛苦的状态。现在最重要的是先保证你的人身安全，请立刻联系学校心理老师、辅导员、家人或身边可信任的人，不要独自承受。";
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel != null) {
            try {
                String prompt = """
                        你是高校心理自助平台中的AI导师。
                        请用温和、倾听、非诊断性的方式回复学生。
                        不要下医学诊断，不要夸张承诺，先共情，再给2条简单建议。
                        学生消息：%s
                        """.formatted(content);
                String reply = ChatClient.create(chatModel).prompt().user(prompt).call().content();
                if (reply != null && !reply.isBlank()) {
                    return reply;
                }
            } catch (Exception ignored) {
            }
        }
        return "我听到了你的压力和不舒服。你可以先把最困扰你的那件事拆成一个最小问题，再试着给自己留出一小段休息时间；如果这种状态持续影响到学习和睡眠，建议尽快预约咨询师进一步聊一聊。";
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
        return title;
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
