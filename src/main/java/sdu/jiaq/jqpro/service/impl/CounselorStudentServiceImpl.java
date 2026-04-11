package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.counselor.CounselorStudentSummaryResponse;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.CounselorStudentService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Counselor student service implementation.
 */
@Service
public class CounselorStudentServiceImpl implements CounselorStudentService {

    private final CounselorStudentMapper counselorStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;

    public CounselorStudentServiceImpl(CounselorStudentMapper counselorStudentMapper,
                                       SysUserMapper sysUserMapper,
                                       StudentProfileMapper studentProfileMapper) {
        this.counselorStudentMapper = counselorStudentMapper;
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
    }

    @Override
    public List<CounselorStudentSummaryResponse> listCurrentCounselorStudents() {
        Long counselorUserId = SecurityUtil.getCurrentUserId();
        List<CounselorStudent> relations = counselorStudentMapper.selectList(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .orderByDesc(CounselorStudent::getCreatedAt, CounselorStudent::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> studentUserIds = relations.stream().map(CounselorStudent::getStudentUserId).distinct().toList();
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
                            .studentName(user == null ? "Unknown Student" : user.getDisplayName())
                            .studentNo(user == null ? null : user.getStudentNo())
                            .college(profile == null ? null : profile.getCollege())
                            .grade(profile == null ? null : profile.getGrade())
                            .gender(profile == null ? null : profile.getGender())
                            .build();
                })
                .toList();
    }
}
