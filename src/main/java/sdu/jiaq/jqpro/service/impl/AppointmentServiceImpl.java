package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.AppointmentConstants;
import sdu.jiaq.jqpro.common.constant.ChatConstants;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.appointment.AppointmentActionRequest;
import sdu.jiaq.jqpro.dto.appointment.AppointmentCounselorOptionResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约服务实现。
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final List<FixedSlotDefinition> FIXED_DAILY_SLOTS = List.of(
            new FixedSlotDefinition(LocalTime.of(8, 30), LocalTime.of(9, 30), "08:30~09:30"),
            new FixedSlotDefinition(LocalTime.of(10, 30), LocalTime.of(11, 30), "10:30~11:30"),
            new FixedSlotDefinition(LocalTime.of(14, 0), LocalTime.of(15, 0), "14:00~15:00"),
            new FixedSlotDefinition(LocalTime.of(16, 0), LocalTime.of(17, 0), "16:00~17:00"),
            new FixedSlotDefinition(LocalTime.of(19, 0), LocalTime.of(20, 0), "19:00~20:00"),
            new FixedSlotDefinition(LocalTime.of(20, 30), LocalTime.of(21, 30), "20:30~21:30")
    );

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
    public List<AppointmentCounselorOptionResponse> listAvailableCounselors() {
        List<SysUser> counselors = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, RoleConstants.COUNSELOR)
                .eq(SysUser::getStatus, UserStatusConstants.ACTIVE)
                .orderByAsc(SysUser::getDisplayName, SysUser::getId));
        if (counselors.isEmpty()) {
            return List.of();
        }
        return counselors.stream()
                .map(counselor -> AppointmentCounselorOptionResponse.builder()
                        .counselorUserId(counselor.getId())
                        .counselorName(counselor.getDisplayName())
                        .counselorNo(counselor.getCounselorNo())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AppointmentSlotResponse> listDailySlots(Long counselorUserId, LocalDate date) {
        if (counselorUserId == null) {
            throw new BusinessException("请选择咨询师");
        }
        if (date == null) {
            throw new BusinessException("请选择预约日期");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException("预约日期不能早于今天");
        }

        SysUser counselor = getRequiredCounselor(counselorUserId);
        List<ConsultAppointmentSlot> slots = ensureFixedDailySlots(counselorUserId, date);
        Map<String, ConsultAppointmentSlot> slotMap = slots.stream()
                .collect(Collectors.toMap(this::buildSlotKey, Function.identity(), (left, right) -> left));
        LocalDateTime now = LocalDateTime.now();

        return FIXED_DAILY_SLOTS.stream()
                .map(definition -> {
                    LocalDateTime startTime = LocalDateTime.of(date, definition.startTime());
                    LocalDateTime endTime = LocalDateTime.of(date, definition.endTime());
                    ConsultAppointmentSlot slot = slotMap.get(buildSlotKey(counselorUserId, startTime, endTime));
                    boolean isExpired = !endTime.isAfter(now);
                    boolean isBooked = slot == null || !AppointmentConstants.SLOT_OPEN.equals(slot.getStatus());
                    return AppointmentSlotResponse.builder()
                            .slotId(slot == null ? null : slot.getId())
                            .counselorUserId(counselorUserId)
                            .counselorName(counselor.getDisplayName())
                            .startTime(startTime)
                            .endTime(endTime)
                            .status(isExpired ? AppointmentConstants.SLOT_CLOSED : slot.getStatus())
                            .booked(isBooked)
                            .selectable(!isExpired && !isBooked)
                            .timeLabel(definition.label())
                            .build();
                })
                .toList();
    }

    @Override
    public List<AppointmentResponse> listStudentAppointments() {
        Long studentUserId = SecurityUtil.getCurrentUserId();
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

        return buildAppointmentResponse(appointment, slot, getUserMap(List.of(slot.getCounselorUserId())), null);
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
        ConsultChatSession chatSession = consultChatSessionMapper.selectOne(new LambdaQueryWrapper<ConsultChatSession>()
                .eq(ConsultChatSession::getAppointmentId, appointment.getId())
                .orderByDesc(ConsultChatSession::getId)
                .last("limit 1"));
        return buildAppointmentResponse(appointment, getRequiredSlot(appointment.getSlotId()), getUserMap(List.of(appointment.getCounselorUserId())), chatSession);
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
        return buildAppointmentResponse(appointment, slot, getUserMap(List.of(appointment.getCounselorUserId())), null);
    }

    private List<AppointmentResponse> buildAppointmentResponses(List<ConsultAppointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        Map<Long, ConsultAppointment> appointmentMap = appointments.stream()
                .collect(Collectors.toMap(ConsultAppointment::getId, Function.identity()));
        Map<Long, ConsultAppointmentSlot> slotMap = consultAppointmentSlotMapper.selectBatchIds(appointments.stream()
                        .map(ConsultAppointment::getSlotId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ConsultAppointmentSlot::getId, Function.identity()));
        Map<Long, SysUser> counselorMap = getUserMap(appointments.stream()
                .map(ConsultAppointment::getCounselorUserId).distinct().toList());
        Map<Long, ConsultChatSession> chatSessionMap = consultChatSessionMapper.selectList(new LambdaQueryWrapper<ConsultChatSession>()
                        .in(ConsultChatSession::getAppointmentId, appointmentMap.keySet())
                        .orderByDesc(ConsultChatSession::getId))
                .stream()
                .collect(Collectors.toMap(ConsultChatSession::getAppointmentId, Function.identity(), (left, right) -> left));

        chatSessionMap.values().forEach(chatSession -> applyChatExpiryIfNeeded(chatSession, appointmentMap.get(chatSession.getAppointmentId())));
        return appointments.stream()
                .map(appointment -> buildAppointmentResponse(
                        appointment,
                        slotMap.get(appointment.getSlotId()),
                        counselorMap,
                        chatSessionMap.get(appointment.getId())))
                .toList();
    }

    private AppointmentResponse buildAppointmentResponse(ConsultAppointment appointment,
                                                         ConsultAppointmentSlot slot,
                                                         Map<Long, SysUser> counselorMap,
                                                         ConsultChatSession chatSession) {
        SysUser counselor = counselorMap.get(appointment.getCounselorUserId());
        LocalDateTime now = LocalDateTime.now();
        boolean chatSealed = chatSession != null && chatSession.getSealedFlag() != null && chatSession.getSealedFlag() == 1;
        boolean chatEnded = AppointmentConstants.APPOINTMENT_COMPLETED.equals(appointment.getStatus())
                || AppointmentConstants.APPOINTMENT_REJECTED.equals(appointment.getStatus())
                || AppointmentConstants.APPOINTMENT_CANCELED.equals(appointment.getStatus())
                || (slot != null && !slot.getEndTime().isAfter(now))
                || chatSealed
                || (chatSession != null && (ChatConstants.CHAT_ARCHIVED.equals(chatSession.getStatus()) || ChatConstants.CHAT_CLOSED.equals(chatSession.getStatus())));
        boolean chatAvailable = chatSession != null
                && !chatSealed
                && slot != null
                && slot.getEndTime().isAfter(now)
                && (AppointmentConstants.APPOINTMENT_ACCEPTED.equals(appointment.getStatus())
                || ChatConstants.CHAT_ACTIVE.equals(chatSession.getStatus()));
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
                .chatAvailable(chatAvailable)
                .chatEnded(chatEnded)
                .chatStatus(chatSession == null ? null : chatSession.getStatus())
                .chatSealed(chatSealed)
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

    private void applyChatExpiryIfNeeded(ConsultChatSession chatSession, ConsultAppointment appointment) {
        if (chatSession == null || appointment == null) {
            return;
        }
        if (chatSession.getSealedFlag() != null && chatSession.getSealedFlag() == 1) {
            return;
        }
        if (chatSession.getCloseTime() != null && LocalDateTime.now().isAfter(chatSession.getCloseTime())) {
            chatSession.setStatus(ChatConstants.CHAT_ARCHIVED);
            chatSession.setSealedFlag(1);
            consultChatSessionMapper.updateById(chatSession);

            appointment.setStatus(AppointmentConstants.APPOINTMENT_COMPLETED);
            consultAppointmentMapper.updateById(appointment);

            if (appointment.getSlotId() != null) {
                ConsultAppointmentSlot slot = consultAppointmentSlotMapper.selectById(appointment.getSlotId());
                if (slot != null && !AppointmentConstants.SLOT_CLOSED.equals(slot.getStatus())) {
                    slot.setStatus(AppointmentConstants.SLOT_CLOSED);
                    consultAppointmentSlotMapper.updateById(slot);
                }
            }
        }
    }

    private Map<Long, SysUser> getUserMap(List<Long> userIds) {
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private SysUser getRequiredCounselor(Long counselorUserId) {
        SysUser counselor = sysUserMapper.selectById(counselorUserId);
        if (counselor == null || !RoleConstants.COUNSELOR.equals(counselor.getRoleCode())) {
            throw new BusinessException("咨询师不存在");
        }
        if (!UserStatusConstants.ACTIVE.equals(counselor.getStatus())) {
            throw new BusinessException("当前咨询师不可预约");
        }
        return counselor;
    }

    private List<ConsultAppointmentSlot> ensureFixedDailySlots(Long counselorUserId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<ConsultAppointmentSlot> existingSlots = consultAppointmentSlotMapper.selectList(
                new LambdaQueryWrapper<ConsultAppointmentSlot>()
                        .eq(ConsultAppointmentSlot::getCounselorUserId, counselorUserId)
                        .ge(ConsultAppointmentSlot::getStartTime, dayStart)
                        .lt(ConsultAppointmentSlot::getStartTime, dayEnd)
                        .orderByAsc(ConsultAppointmentSlot::getStartTime, ConsultAppointmentSlot::getId)
        );
        Map<String, ConsultAppointmentSlot> slotMap = existingSlots.stream()
                .collect(Collectors.toMap(this::buildSlotKey, Function.identity(), (left, right) -> left));

        for (FixedSlotDefinition definition : FIXED_DAILY_SLOTS) {
            LocalDateTime startTime = LocalDateTime.of(date, definition.startTime());
            LocalDateTime endTime = LocalDateTime.of(date, definition.endTime());
            String key = buildSlotKey(counselorUserId, startTime, endTime);
            if (!slotMap.containsKey(key)) {
                ConsultAppointmentSlot slot = new ConsultAppointmentSlot();
                slot.setCounselorUserId(counselorUserId);
                slot.setStartTime(startTime);
                slot.setEndTime(endTime);
                slot.setStatus(AppointmentConstants.SLOT_OPEN);
                consultAppointmentSlotMapper.insert(slot);
                slotMap.put(key, slot);
            }
        }

        return slotMap.values().stream()
                .sorted(Comparator.comparing(ConsultAppointmentSlot::getStartTime))
                .toList();
    }

    private String buildSlotKey(ConsultAppointmentSlot slot) {
        return buildSlotKey(slot.getCounselorUserId(), slot.getStartTime(), slot.getEndTime());
    }

    private String buildSlotKey(Long counselorUserId, LocalDateTime startTime, LocalDateTime endTime) {
        return counselorUserId + "|" + startTime + "|" + endTime;
    }

    private record FixedSlotDefinition(LocalTime startTime, LocalTime endTime, String label) {
    }
}
