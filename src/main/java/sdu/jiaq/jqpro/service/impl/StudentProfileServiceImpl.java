package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.student.StudentProfileResponse;
import sdu.jiaq.jqpro.dto.student.UpdateStudentProfileRequest;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.StudentProfileService;

/**
 * 学生档案服务实现。
 */
@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;

    public StudentProfileServiceImpl(StudentProfileMapper studentProfileMapper,
                                     SysUserMapper sysUserMapper,
                                     AuditLogService auditLogService) {
        this.studentProfileMapper = studentProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public StudentProfileResponse getCurrentStudentProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        return buildProfileResponse(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileResponse updateCurrentStudentProfile(UpdateStudentProfileRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        StudentProfile studentProfile = getRequiredProfile(userId);
        studentProfile.setAvatarUrl(request.getAvatarUrl());
        studentProfile.setCollege(request.getCollege());
        studentProfile.setGrade(request.getGrade());
        studentProfile.setGender(request.getGender());
        studentProfile.setPhone(request.getPhone());
        studentProfile.setEmergencyContact(request.getEmergencyContact());
        studentProfile.setEmergencyPhone(request.getEmergencyPhone());
        studentProfileMapper.updateById(studentProfile);
        auditLogService.record(userId, "PROFILE_UPDATE", "更新学生档案", "更新个人档案与紧急联系人信息", "system");
        return buildProfileResponse(userId);
    }

    private StudentProfileResponse buildProfileResponse(Long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }
        StudentProfile studentProfile = getRequiredProfile(userId);
        return StudentProfileResponse.builder()
                .userId(sysUser.getId())
                .account(sysUser.getAccount())
                .realName(sysUser.getRealName())
                .displayName(sysUser.getDisplayName())
                .studentNo(sysUser.getStudentNo())
                .avatarUrl(studentProfile.getAvatarUrl())
                .college(studentProfile.getCollege())
                .grade(studentProfile.getGrade())
                .gender(studentProfile.getGender())
                .phone(studentProfile.getPhone())
                .emergencyContact(studentProfile.getEmergencyContact())
                .emergencyPhone(studentProfile.getEmergencyPhone())
                .counselorUserId(studentProfile.getCounselorUserId())
                .build();
    }

    private StudentProfile getRequiredProfile(Long userId) {
        StudentProfile studentProfile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId)
                .last("limit 1"));
        if (studentProfile == null) {
            throw new BusinessException("学生档案不存在");
        }
        return studentProfile;
    }
}
