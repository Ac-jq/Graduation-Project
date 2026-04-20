package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.counselor.CounselorStudentSummaryResponse;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.CounselorStudentService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 咨询师学生服务实现。
 */
@Service
public class CounselorStudentServiceImpl implements CounselorStudentService {

    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;

    public CounselorStudentServiceImpl(ConsultAppointmentMapper consultAppointmentMapper,
                                       ConsultChatSessionMapper consultChatSessionMapper,
                                       SysUserMapper sysUserMapper,
                                       StudentProfileMapper studentProfileMapper) {
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
    }

    @Override
    public List<CounselorStudentSummaryResponse> listCurrentCounselorStudents() {
        Long counselorUserId = SecurityUtil.getCurrentUserId();

        List<ConsultAppointment> appointments = consultAppointmentMapper.selectList(new LambdaQueryWrapper<ConsultAppointment>()
                .eq(ConsultAppointment::getCounselorUserId, counselorUserId)
                .orderByDesc(ConsultAppointment::getUpdatedAt, ConsultAppointment::getCreatedAt, ConsultAppointment::getId));
        List<ConsultChatSession> chatSessions = consultChatSessionMapper.selectList(new LambdaQueryWrapper<ConsultChatSession>()
                .eq(ConsultChatSession::getCounselorUserId, counselorUserId)
                .orderByDesc(ConsultChatSession::getUpdatedAt, ConsultChatSession::getCreatedAt, ConsultChatSession::getId));

        Map<Long, LocalDateTime> lastContactMap = appointments.stream()
                .filter(appointment -> appointment.getStudentUserId() != null)
                .collect(Collectors.toMap(
                        ConsultAppointment::getStudentUserId,
                        appointment -> appointment.getUpdatedAt() == null ? appointment.getCreatedAt() : appointment.getUpdatedAt(),
                        this::pickLatest
                ));

        for (ConsultChatSession session : chatSessions) {
            if (session.getStudentUserId() == null) {
                continue;
            }
            LocalDateTime activityAt = session.getUpdatedAt() == null ? session.getCreatedAt() : session.getUpdatedAt();
            lastContactMap.merge(session.getStudentUserId(), activityAt, this::pickLatest);
        }

        if (lastContactMap.isEmpty()) {
            return List.of();
        }

        List<Long> studentUserIds = lastContactMap.entrySet().stream()
                .sorted(Map.Entry.<Long, LocalDateTime>comparingByValue(Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(studentUserIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, StudentProfile> profileMap = studentProfileMapper.selectList(new LambdaQueryWrapper<StudentProfile>()
                        .in(StudentProfile::getUserId, studentUserIds))
                .stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));

        return studentUserIds.stream()
                .map(studentUserId -> {
                    SysUser user = userMap.get(studentUserId);
                    StudentProfile profile = profileMap.get(studentUserId);
                    return CounselorStudentSummaryResponse.builder()
                            .studentUserId(studentUserId)
                            .studentName(user == null ? "未知学生" : user.getDisplayName())
                            .studentNo(user == null ? null : user.getStudentNo())
                            .college(profile == null ? null : profile.getCollege())
                            .grade(profile == null ? null : profile.getGrade())
                            .gender(profile == null ? null : profile.getGender())
                            .build();
                })
                .toList();
    }

    private LocalDateTime pickLatest(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }
}
