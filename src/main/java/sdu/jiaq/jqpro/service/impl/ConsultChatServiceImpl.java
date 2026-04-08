package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.AppointmentConstants;
import sdu.jiaq.jqpro.common.constant.ChatConstants;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.ChatCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.chat.ConsultChatMessageResponse;
import sdu.jiaq.jqpro.dto.chat.ConsultChatSessionResponse;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.ConsultChatMessage;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatMessageMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.ConsultChatService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 私密聊天室服务实现。
 */
@Service
public class ConsultChatServiceImpl implements ConsultChatService {

    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final ConsultChatMessageMapper consultChatMessageMapper;
    private final SysUserMapper sysUserMapper;

    public ConsultChatServiceImpl(ConsultAppointmentMapper consultAppointmentMapper,
                                  ConsultChatSessionMapper consultChatSessionMapper,
                                  ConsultChatMessageMapper consultChatMessageMapper,
                                  SysUserMapper sysUserMapper) {
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.consultChatMessageMapper = consultChatMessageMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public ConsultChatSessionResponse getAppointmentChatSession(Long appointmentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        ConsultChatSession chatSession = getAndVerifyChatSession(appointmentId, userId);
        archiveIfExpired(appointmentId);
        chatSession = consultChatSessionMapper.selectById(chatSession.getId());
        return buildSessionResponse(chatSession);
    }

    @Override
    public List<ConsultChatMessageResponse> listAppointmentMessages(Long appointmentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        ConsultChatSession chatSession = getAndVerifyChatSession(appointmentId, userId);
        archiveIfExpired(appointmentId);
        return consultChatMessageMapper.selectList(new LambdaQueryWrapper<ConsultChatMessage>()
                        .eq(ConsultChatMessage::getChatSessionId, chatSession.getId())
                        .orderByAsc(ConsultChatMessage::getCreatedAt, ConsultChatMessage::getId))
                .stream()
                .map(this::buildMessageResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsultChatSessionResponse validateAndActivateChat(Long appointmentId, Long userId) {
        ConsultChatSession chatSession = getAndVerifyChatSession(appointmentId, userId);
        archiveIfExpired(appointmentId);
        chatSession = consultChatSessionMapper.selectById(chatSession.getId());
        LocalDateTime now = LocalDateTime.now();
        if (chatSession.getSealedFlag() == 1 || ChatConstants.CHAT_ARCHIVED.equals(chatSession.getStatus())) {
            throw new BusinessException("当前聊天室已封存");
        }
        if (now.isBefore(chatSession.getOpenTime()) || now.isAfter(chatSession.getCloseTime())) {
            throw new BusinessException("当前不在预约有效时间内");
        }
        if (!ChatConstants.CHAT_ACTIVE.equals(chatSession.getStatus())) {
            chatSession.setStatus(ChatConstants.CHAT_ACTIVE);
            consultChatSessionMapper.updateById(chatSession);
        }
        return buildSessionResponse(chatSession);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsultChatMessageResponse sendMessage(Long appointmentId, Long userId, String content) {
        ConsultChatSession chatSession = getAndVerifyChatSession(appointmentId, userId);
        validateAndActivateChat(appointmentId, userId);
        SysUser user = getRequiredUser(userId);

        ConsultChatMessage message = new ConsultChatMessage();
        message.setChatSessionId(chatSession.getId());
        message.setSenderUserId(userId);
        message.setSenderType(resolveSenderType(user.getRoleCode()));
        message.setContentCipherText(ChatCryptoUtil.encrypt(content));
        consultChatMessageMapper.insert(message);
        return buildMessageResponse(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveIfExpired(Long appointmentId) {
        ConsultChatSession chatSession = consultChatSessionMapper.selectOne(new LambdaQueryWrapper<ConsultChatSession>()
                .eq(ConsultChatSession::getAppointmentId, appointmentId)
                .last("limit 1"));
        if (chatSession == null) {
            return;
        }
        if (chatSession.getSealedFlag() == 0 && LocalDateTime.now().isAfter(chatSession.getCloseTime())) {
            chatSession.setStatus(ChatConstants.CHAT_ARCHIVED);
            chatSession.setSealedFlag(1);
            consultChatSessionMapper.updateById(chatSession);

            ConsultAppointment appointment = consultAppointmentMapper.selectById(chatSession.getAppointmentId());
            if (appointment != null && AppointmentConstants.APPOINTMENT_ACCEPTED.equals(appointment.getStatus())) {
                appointment.setStatus(AppointmentConstants.APPOINTMENT_COMPLETED);
                consultAppointmentMapper.updateById(appointment);
            }
        }
    }

    private ConsultChatSession getAndVerifyChatSession(Long appointmentId, Long userId) {
        ConsultAppointment appointment = consultAppointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        if (!userId.equals(appointment.getStudentUserId()) && !userId.equals(appointment.getCounselorUserId())) {
            throw new BusinessException("无权访问该聊天室");
        }
        ConsultChatSession chatSession = consultChatSessionMapper.selectOne(new LambdaQueryWrapper<ConsultChatSession>()
                .eq(ConsultChatSession::getAppointmentId, appointmentId)
                .last("limit 1"));
        if (chatSession == null) {
            throw new BusinessException("聊天室尚未开放");
        }
        return chatSession;
    }

    private ConsultChatSessionResponse buildSessionResponse(ConsultChatSession chatSession) {
        return ConsultChatSessionResponse.builder()
                .chatSessionId(chatSession.getId())
                .appointmentId(chatSession.getAppointmentId())
                .status(chatSession.getStatus())
                .sealed(chatSession.getSealedFlag() != null && chatSession.getSealedFlag() == 1)
                .openTime(chatSession.getOpenTime())
                .closeTime(chatSession.getCloseTime())
                .build();
    }

    private ConsultChatMessageResponse buildMessageResponse(ConsultChatMessage message) {
        return ConsultChatMessageResponse.builder()
                .messageId(message.getId())
                .chatSessionId(message.getChatSessionId())
                .senderUserId(message.getSenderUserId())
                .senderType(message.getSenderType())
                .content(ChatCryptoUtil.decrypt(message.getContentCipherText()))
                .createdAt(message.getCreatedAt())
                .build();
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String resolveSenderType(String roleCode) {
        return RoleConstants.COUNSELOR.equals(roleCode) ? ChatConstants.SENDER_COUNSELOR : ChatConstants.SENDER_STUDENT;
    }
}
