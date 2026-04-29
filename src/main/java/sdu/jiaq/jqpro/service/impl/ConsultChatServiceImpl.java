package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.AppointmentConstants;
import sdu.jiaq.jqpro.common.constant.ChatConstants;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.util.AvatarUrlUtil;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.ChatCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.chat.ConsultChatMessageResponse;
import sdu.jiaq.jqpro.dto.chat.ConsultChatSessionResponse;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.ConsultAppointmentSlot;
import sdu.jiaq.jqpro.entity.ConsultChatMessage;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentSlotMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatMessageMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
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
    private final ConsultAppointmentSlotMapper consultAppointmentSlotMapper;
    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final ConsultChatMessageMapper consultChatMessageMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;

    public ConsultChatServiceImpl(ConsultAppointmentMapper consultAppointmentMapper,
                                  ConsultAppointmentSlotMapper consultAppointmentSlotMapper,
                                  ConsultChatSessionMapper consultChatSessionMapper,
                                  ConsultChatMessageMapper consultChatMessageMapper,
                                  StudentProfileMapper studentProfileMapper,
                                  SysUserMapper sysUserMapper) {
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.consultAppointmentSlotMapper = consultAppointmentSlotMapper;
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.consultChatMessageMapper = consultChatMessageMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public ConsultChatSessionResponse getAppointmentChatSession(Long appointmentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        ConsultChatSession chatSession = getAndVerifyChatSession(appointmentId, userId);
        archiveIfExpired(appointmentId);
        return buildSessionResponse(consultChatSessionMapper.selectById(chatSession.getId()));
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

        if (chatSession.getSealedFlag() != null && chatSession.getSealedFlag() == 1) {
            throw new BusinessException("当前聊天室已结束");
        }
        if (ChatConstants.CHAT_ARCHIVED.equals(chatSession.getStatus()) || ChatConstants.CHAT_CLOSED.equals(chatSession.getStatus())) {
            throw new BusinessException("当前聊天室已结束");
        }
        if (chatSession.getCloseTime() != null && now.isAfter(chatSession.getCloseTime())) {
            throw new BusinessException("当前预约已真实过期，聊天室已关闭");
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
        ConsultChatSession chatSession = getLatestChatSession(appointmentId);
        if (chatSession == null) {
            return;
        }
        if (chatSession.getSealedFlag() != null && chatSession.getSealedFlag() == 1) {
            return;
        }
        if (chatSession.getCloseTime() != null && LocalDateTime.now().isAfter(chatSession.getCloseTime())) {
            ConsultAppointment appointment = consultAppointmentMapper.selectById(chatSession.getAppointmentId());
            sealChatSession(chatSession, appointment, ChatConstants.CHAT_ARCHIVED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsultChatSessionResponse closeAppointmentChat(Long appointmentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        ConsultAppointment appointment = consultAppointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        if (!userId.equals(appointment.getCounselorUserId())) {
            throw new BusinessException("仅咨询师可结束当前聊天室");
        }

        ConsultChatSession chatSession = getAndVerifyChatSession(appointmentId, userId);
        if (chatSession.getSealedFlag() != null && chatSession.getSealedFlag() == 1) {
            return buildSessionResponse(chatSession);
        }
        sealChatSession(chatSession, appointment, ChatConstants.CHAT_CLOSED);
        return buildSessionResponse(consultChatSessionMapper.selectById(chatSession.getId()));
    }

    private ConsultChatSession getAndVerifyChatSession(Long appointmentId, Long userId) {
        ConsultAppointment appointment = consultAppointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        if (!userId.equals(appointment.getStudentUserId()) && !userId.equals(appointment.getCounselorUserId())) {
            throw new BusinessException("无权访问该聊天室");
        }
        ConsultChatSession chatSession = getLatestChatSession(appointmentId);
        if (chatSession == null) {
            throw new BusinessException("聊天室尚未创建");
        }
        return chatSession;
    }

    private ConsultChatSession getLatestChatSession(Long appointmentId) {
        return consultChatSessionMapper.selectOne(new LambdaQueryWrapper<ConsultChatSession>()
                .eq(ConsultChatSession::getAppointmentId, appointmentId)
                .orderByDesc(ConsultChatSession::getId)
                .last("limit 1"));
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
        SysUser sender = sysUserMapper.selectById(message.getSenderUserId());
        String avatarUrl = null;
        if (sender != null && RoleConstants.STUDENT.equals(sender.getRoleCode())) {
            StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                    .eq(StudentProfile::getUserId, sender.getId())
                    .last("limit 1"));
            avatarUrl = profile == null ? null : profile.getAvatarUrl();
        } else if (sender != null && RoleConstants.COUNSELOR.equals(sender.getRoleCode())) {
            avatarUrl = sender.getAvatarUrl();
        }
        return ConsultChatMessageResponse.builder()
                .messageId(message.getId())
                .chatSessionId(message.getChatSessionId())
                .senderUserId(message.getSenderUserId())
                .senderType(message.getSenderType())
                .senderDisplayName(resolveSenderDisplayName(sender))
                .senderAvatarUrl(AvatarUrlUtil.toPublicUrl(avatarUrl))
                .content(ChatCryptoUtil.decrypt(message.getContentCipherText()))
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String resolveSenderDisplayName(SysUser sender) {
        if (sender == null) {
            return "未知用户";
        }
        if (sender.getDisplayName() != null && !sender.getDisplayName().isBlank()) {
            return sender.getDisplayName();
        }
        if (sender.getRealName() != null && !sender.getRealName().isBlank()) {
            return sender.getRealName();
        }
        return sender.getAccount();
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

    private void sealChatSession(ConsultChatSession chatSession, ConsultAppointment appointment, String chatStatus) {
        chatSession.setStatus(chatStatus);
        chatSession.setSealedFlag(1);
        if (!LocalDateTime.now().isBefore(chatSession.getOpenTime())) {
            chatSession.setCloseTime(LocalDateTime.now());
        }
        consultChatSessionMapper.updateById(chatSession);

        if (appointment != null
                && !AppointmentConstants.APPOINTMENT_COMPLETED.equals(appointment.getStatus())
                && !AppointmentConstants.APPOINTMENT_REJECTED.equals(appointment.getStatus())
                && !AppointmentConstants.APPOINTMENT_CANCELED.equals(appointment.getStatus())) {
            appointment.setStatus(AppointmentConstants.APPOINTMENT_COMPLETED);
            consultAppointmentMapper.updateById(appointment);
        }

        if (appointment != null && appointment.getSlotId() != null) {
            ConsultAppointmentSlot slot = consultAppointmentSlotMapper.selectById(appointment.getSlotId());
            if (slot != null && !AppointmentConstants.SLOT_CLOSED.equals(slot.getStatus())) {
                slot.setStatus(AppointmentConstants.SLOT_CLOSED);
                consultAppointmentSlotMapper.updateById(slot);
            }
        }
    }
}
