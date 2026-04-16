package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.student.AvatarUploadResponse;
import sdu.jiaq.jqpro.dto.student.StudentProfileResponse;
import sdu.jiaq.jqpro.dto.student.UpdateStudentProfileRequest;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.StudentProfileService;

/**
 * 学生个人资料服务实现。
 */
@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Path AVATAR_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), ".local", "user-assets", "avatars");

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
        auditLogService.record(userId, "PROFILE_UPDATE", "更新学生档案", "更新个人资料与紧急联系人信息", "system");
        return buildProfileResponse(userId);
    }

    @Override
    public AvatarUploadResponse uploadCurrentStudentAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像文件不能超过 5MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        String extension = resolveExtension(contentType, file.getOriginalFilename());
        String fileName = "student-avatar-" + SecurityUtil.getCurrentUserId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(AVATAR_UPLOAD_DIR);
            Path target = AVATAR_UPLOAD_DIR.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("头像上传失败，请稍后重试");
        }

        return AvatarUploadResponse.builder()
                .avatarUrl("http://127.0.0.1:8080/user-assets/avatars/" + fileName)
                .build();
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

    private String resolveExtension(String contentType, String originalFilename) {
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedType.contains("png")) {
            return ".png";
        }
        if (normalizedType.contains("webp")) {
            return ".webp";
        }
        if (normalizedType.contains("gif")) {
            return ".gif";
        }
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String suffix = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(suffix)) {
                return suffix.equals(".jpeg") ? ".jpg" : suffix;
            }
        }
        return ".jpg";
    }
}
