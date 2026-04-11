package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.AppointmentConstants;
import sdu.jiaq.jqpro.common.constant.ChatConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.appointment.AppointmentActionRequest;
import sdu.jiaq.jqpro.dto.appointment.AppointmentResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentSlotResponse;
import sdu.jiaq.jqpro.dto.appointment.CreateAppointmentRequest;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.ConsultAppointmentSlot;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentSlotMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AppointmentService;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约服务实现。
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final String[] ANONYMOUS_PREFIX = {"向日葵", "银杏", "云杉", "海棠", "木槿", "晚风"};

    private final ConsultAppointmentSlotMapper consultAppointmentSlotMapper;
    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AppointmentServiceImpl(ConsultAppointmentSlotMapper consultAppointmentSlotMapper,
                                  ConsultAppointmentMapper consultAppointmentMapper,
                                  ConsultChatSessionMapper consultChatSessionMapper,
                                  SysUserMapper sysUserMapper,
                                  NotificationService notificationService,
                                  AuditLogService auditLogService) {
        this.consultAppointmentSlotMapper = consultAppointmentSlotMapper;
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.sysUserMapper = sysUserMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<AppointmentSlotResponse> listOpenSlots() {
        List<ConsultAppointmentSlot> slots = consultAppointmentSlotMapper.selectList(new LambdaQueryWrapper<ConsultAppointmentSlot>()
                .eq(ConsultAppointmentSlot::getStatus, AppointmentConstants.SLOT_OPEN)
                .ge(ConsultAppointmentSlot::getEndTime, LocalDateTime.now())
                .orderByAsc(ConsultAppointmentSlot::getStartTime));
        if (slots.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> counselorMap = sysUserMapper.selectBatchIds(slots.stream()
                        .map(ConsultAppointmentSlot::getCounselorUserId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return slots.stream().map(slot -> AppointmentSlotResponse.builder()
                .slotId(slot.getId())
                .counselorUserId(slot.getCounselorUserId())
                .counselorName(counselorMap.get(slot.getCounselorUserId()).getDisplayName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .build()).toList();
    }

    @Override
    public List<AppointmentResponse> listStudentAppointments() {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        // Keep student list isolated to current account so the frontend can safely render "My Appointments".
        List<ConsultAppointment> appointments = consultAppointmentMapper.selectList(new LambdaQueryWrapper<ConsultAppointment>()
                .eq(ConsultAppointment::getStudentUserId, studentUserId)
                .orderByDesc(ConsultAppointment::getCreatedAt));
        return buildAppointmentResponses(appointments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        Long studentUserId = SecurityUtil.getCurrentUserId();
        ConsultAppointmentSlot slot = getRequiredSlot(request.getSlotId());
        if (!AppointmentConstants.SLOT_OPEN.equals(slot.getStatus())) {
            throw new BusinessException("该时段已不可预约");
        }
        if (slot.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该时段已过期");
        }

        ConsultAppointment appointment = new ConsultAppointment();
        appointment.setSlotId(slot.getId());
        appointment.setStudentUserId(studentUserId);
        appointment.setCounselorUserId(slot.getCounselorUserId());
        appointment.setAnonymousName(generateAnonymousName(studentUserId));
        appointment.setIssueSummary(request.getIssueSummary());
        appointment.setStatus(AppointmentConstants.APPOINTMENT_PENDING);
        consultAppointmentMapper.insert(appointment);

        slot.setStatus(AppointmentConstants.SLOT_RESERVED);
        consultAppointmentSlotMapper.updateById(slot);
        notificationService.pushNotification(slot.getCounselorUserId(), "新的咨询预约", "你收到一条新的匿名预约，请及时处理。");
        auditLogService.record(studentUserId, "APPOINTMENT_CREATE", "发起匿名预约",
                "创建预约#" + appointment.getId() + " 并锁定时段#" + slot.getId(), "system");
        return buildAppointmentResponse(appointment, slot, getUserMap(List.of(slot.getCounselorUserId())));
    }

    @Override
    public List<AppointmentResponse> listCounselorAppointments() {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        List<ConsultAppointment> appointments = consultAppointmentMapper.selectList(new LambdaQueryWrapper<ConsultAppointment>()
                .eq(ConsultAppointment::getCounselorUserId, counselorUserId)
                .orderByDesc(ConsultAppointment::getCreatedAt));
        return buildAppointmentResponses(appointments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentResponse acceptAppointment(Long appointmentId, AppointmentActionRequest request) {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        ConsultAppointment appointment = getRequiredAppointment(appointmentId);
        if (!counselorUserId.equals(appointment.getCounselorUserId())) {
            throw new BusinessException("无权处理该预约");
        }
        if (!AppointmentConstants.APPOINTMENT_PENDING.equals(appointment.getStatus())) {
            throw new BusinessException("当前预约状态不允许接单");
        }

        appointment.setStatus(AppointmentConstants.APPOINTMENT_ACCEPTED);
        appointment.setResultMessage(defaultResultMessage(request.getResultMessage(), "咨询师已接单，请按预约时间进入私密聊天室。"));
        consultAppointmentMapper.updateById(appointment);
        createOrUpdateChatSession(appointment);
        notificationService.pushNotification(appointment.getStudentUserId(), "预约已接单", appointment.getResultMessage());
        auditLogService.record(counselorUserId, "APPOINTMENT_ACCEPT", "咨询师接单", "接单预约#" + appointmentId, "system");
        return buildAppointmentResponse(appointment, getRequiredSlot(appointment.getSlotId()), getUserMap(List.of(appointment.getCounselorUserId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentResponse rejectAppointment(Long appointmentId, AppointmentActionRequest request) {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        ConsultAppointment appointment = getRequiredAppointment(appointmentId);
        if (!counselorUserId.equals(appointment.getCounselorUserId())) {
            throw new BusinessException("无权处理该预约");
        }
        if (!AppointmentConstants.APPOINTMENT_PENDING.equals(appointment.getStatus())) {
            throw new BusinessException("当前预约状态不允许拒绝");
        }
        appointment.setStatus(AppointmentConstants.APPOINTMENT_REJECTED);
        appointment.setResultMessage(defaultResultMessage(request.getResultMessage(), "当前预约未被接单，请重新选择其他时段。"));
        consultAppointmentMapper.updateById(appointment);

        ConsultAppointmentSlot slot = getRequiredSlot(appointment.getSlotId());
        slot.setStatus(AppointmentConstants.SLOT_OPEN);
        consultAppointmentSlotMapper.updateById(slot);

        notificationService.pushNotification(appointment.getStudentUserId(), "预约未通过", appointment.getResultMessage());
        auditLogService.record(counselorUserId, "APPOINTMENT_REJECT", "咨询师拒绝预约", "拒绝预约#" + appointmentId, "system");
        return buildAppointmentResponse(appointment, slot, getUserMap(List.of(appointment.getCounselorUserId())));
    }

    private List<AppointmentResponse> buildAppointmentResponses(List<ConsultAppointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        Map<Long, ConsultAppointmentSlot> slotMap = consultAppointmentSlotMapper.selectBatchIds(appointments.stream()
                        .map(ConsultAppointment::getSlotId).distinct().toList())
                .stream().collect(Collectors.toMap(ConsultAppointmentSlot::getId, Function.identity()));
        Map<Long, SysUser> counselorMap = getUserMap(appointments.stream()
                .map(ConsultAppointment::getCounselorUserId).distinct().toList());
        return appointments.stream()
                .map(appointment -> buildAppointmentResponse(appointment, slotMap.get(appointment.getSlotId()), counselorMap))
                .toList();
    }

    private AppointmentResponse buildAppointmentResponse(ConsultAppointment appointment,
                                                         ConsultAppointmentSlot slot,
                                                         Map<Long, SysUser> counselorMap) {
        SysUser counselor = counselorMap.get(appointment.getCounselorUserId());
        return AppointmentResponse.builder()
                .appointmentId(appointment.getId())
                .slotId(appointment.getSlotId())
                .studentUserId(appointment.getStudentUserId())
                .anonymousName(appointment.getAnonymousName())
                .counselorUserId(appointment.getCounselorUserId())
                .counselorName(counselor == null ? null : counselor.getDisplayName())
                .issueSummary(appointment.getIssueSummary())
                .status(appointment.getStatus())
                .resultMessage(appointment.getResultMessage())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    private ConsultAppointmentSlot getRequiredSlot(Long slotId) {
        ConsultAppointmentSlot slot = consultAppointmentSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException("预约时段不存在");
        }
        return slot;
    }

    private ConsultAppointment getRequiredAppointment(Long appointmentId) {
        ConsultAppointment appointment = consultAppointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        return appointment;
    }

    private String generateAnonymousName(Long studentUserId) {
        int index = Math.floorMod(studentUserId.intValue(), ANONYMOUS_PREFIX.length);
        return ANONYMOUS_PREFIX[index] + "同学";
    }

    private String defaultResultMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private void createOrUpdateChatSession(ConsultAppointment appointment) {
        ConsultAppointmentSlot slot = getRequiredSlot(appointment.getSlotId());
        ConsultChatSession chatSession = consultChatSessionMapper.selectOne(new LambdaQueryWrapper<ConsultChatSession>()
                .eq(ConsultChatSession::getAppointmentId, appointment.getId())
                .last("limit 1"));
        if (chatSession == null) {
            chatSession = new ConsultChatSession();
            chatSession.setAppointmentId(appointment.getId());
            chatSession.setStudentUserId(appointment.getStudentUserId());
            chatSession.setCounselorUserId(appointment.getCounselorUserId());
            chatSession.setOpenTime(slot.getStartTime());
            chatSession.setCloseTime(slot.getEndTime());
            chatSession.setStatus(ChatConstants.CHAT_PENDING);
            chatSession.setSealedFlag(0);
            consultChatSessionMapper.insert(chatSession);
        } else {
            chatSession.setOpenTime(slot.getStartTime());
            chatSession.setCloseTime(slot.getEndTime());
            chatSession.setStatus(ChatConstants.CHAT_PENDING);
            chatSession.setSealedFlag(0);
            consultChatSessionMapper.updateById(chatSession);
        }
    }

    private Map<Long, SysUser> getUserMap(List<Long> userIds) {
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }
}
