package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.PasswordCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.adminuser.AdminUserSummaryResponse;
import sdu.jiaq.jqpro.dto.adminuser.CreateAdminUserRequest;
import sdu.jiaq.jqpro.dto.adminuser.CreateCounselorRequest;
import sdu.jiaq.jqpro.dto.adminuser.UpdateAdminUserRequest;
import sdu.jiaq.jqpro.entity.AiChatSession;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.ConsultAppointmentSlot;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.MentalScaleSession;
import sdu.jiaq.jqpro.entity.ResourceFavorite;
import sdu.jiaq.jqpro.entity.ResourceViewLog;
import sdu.jiaq.jqpro.entity.SiteNotification;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AiChatSessionMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentSlotMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleSessionMapper;
import sdu.jiaq.jqpro.mapper.ResourceFavoriteMapper;
import sdu.jiaq.jqpro.mapper.ResourceViewLogMapper;
import sdu.jiaq.jqpro.mapper.SiteNotificationMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AdminUserService;
import sdu.jiaq.jqpro.service.AuditLogService;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin user service implementation.
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final String DEFAULT_PASSWORD = "Jqpro@123";

    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final ConsultAppointmentSlotMapper consultAppointmentSlotMapper;
    private final SiteNotificationMapper siteNotificationMapper;
    private final ResourceFavoriteMapper resourceFavoriteMapper;
    private final ResourceViewLogMapper resourceViewLogMapper;
    private final MentalScaleSessionMapper mentalScaleSessionMapper;
    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final AuditLogService auditLogService;

    public AdminUserServiceImpl(SysUserMapper sysUserMapper,
                                StudentProfileMapper studentProfileMapper,
                                CounselorStudentMapper counselorStudentMapper,
                                ConsultAppointmentSlotMapper consultAppointmentSlotMapper,
                                SiteNotificationMapper siteNotificationMapper,
                                ResourceFavoriteMapper resourceFavoriteMapper,
                                ResourceViewLogMapper resourceViewLogMapper,
                                MentalScaleSessionMapper mentalScaleSessionMapper,
                                MentalScaleReportMapper mentalScaleReportMapper,
                                AiChatSessionMapper aiChatSessionMapper,
                                ConsultAppointmentMapper consultAppointmentMapper,
                                ConsultChatSessionMapper consultChatSessionMapper,
                                AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.consultAppointmentSlotMapper = consultAppointmentSlotMapper;
        this.siteNotificationMapper = siteNotificationMapper;
        this.resourceFavoriteMapper = resourceFavoriteMapper;
        this.resourceViewLogMapper = resourceViewLogMapper;
        this.mentalScaleSessionMapper = mentalScaleSessionMapper;
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<AdminUserSummaryResponse> listUsers(String roleCode, String status, String keyword, String grade, String college) {
        String normalizedRole = normalize(roleCode);
        String normalizedStatus = normalize(status);
        String normalizedKeyword = normalize(keyword);
        String normalizedGrade = normalize(grade);
        String normalizedCollege = normalize(college);
        return sysUserMapper.selectAdminUserSummaries(
                normalizedRole == null ? null : normalizedRole.toUpperCase(Locale.ROOT),
                normalizedStatus == null ? null : normalizedStatus.toUpperCase(Locale.ROOT),
                normalizedKeyword,
                normalizedGrade,
                normalizedCollege
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserSummaryResponse createUser(CreateAdminUserRequest request) {
        String account = request.getAccount().trim();
        String roleCode = normalizeRoleCode(request.getRoleCode());
        String displayName = request.getDisplayName().trim();
        String realName = blankToDefault(request.getRealName(), displayName);
        String password = normalizeOptional(request.getPassword());

        if (!RoleConstants.STUDENT.equals(roleCode)
                && !RoleConstants.COUNSELOR.equals(roleCode)
                && !RoleConstants.ADMIN.equals(roleCode)) {
            throw new BusinessException("用户角色不受支持");
        }
        validateUniqueAccount(account, null);

        SysUser user = new SysUser();
        String salt = PasswordCryptoUtil.generateSalt();
        user.setAccount(account);
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordCryptoUtil.hashPassword(password == null ? DEFAULT_PASSWORD : password, salt));
        user.setRoleCode(roleCode);
        user.setRealName(realName);
        user.setDisplayName(displayName);
        user.setStatus(UserStatusConstants.ACTIVE);

        StudentProfile profile = null;
        if (RoleConstants.STUDENT.equals(roleCode)) {
            String studentNo = requireRoleIdentity(request.getStudentNo(), "学生学号不能为空");
            validateUniqueStudentNo(studentNo, null);
            user.setStudentNo(studentNo);
            sysUserMapper.insert(user);
            profile = createStudentProfile(user.getId(), request);
        } else if (RoleConstants.COUNSELOR.equals(roleCode)) {
            String counselorNo = requireRoleIdentity(request.getCounselorNo(), "咨询师工号不能为空");
            validateUniqueCounselorNo(counselorNo, null);
            user.setCounselorNo(counselorNo);
            sysUserMapper.insert(user);
        } else {
            sysUserMapper.insert(user);
        }

        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_USER_CREATE", "创建用户账号",
                "创建" + roleCode + "账号 " + user.getAccount(), null);
        return buildUserSummary(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserSummaryResponse createCounselor(CreateCounselorRequest request) {
        CreateAdminUserRequest genericRequest = new CreateAdminUserRequest();
        genericRequest.setAccount(request.getAccount());
        genericRequest.setRoleCode(RoleConstants.COUNSELOR);
        genericRequest.setDisplayName(request.getDisplayName());
        genericRequest.setRealName(request.getRealName());
        genericRequest.setCounselorNo(request.getCounselorNo());
        return createUser(genericRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserSummaryResponse updateUser(Long userId, UpdateAdminUserRequest request) {
        SysUser user = getRequiredUser(userId);
        StudentProfile profile = findStudentProfile(userId);

        String account = request.getAccount().trim();
        String displayName = request.getDisplayName().trim();
        String realName = normalizeOptional(request.getRealName());
        String password = normalizeOptional(request.getPassword());

        validateUniqueAccount(account, userId);

        user.setAccount(account);
        user.setDisplayName(displayName);
        user.setRealName(realName);

        if (StringUtils.hasText(password)) {
            String salt = PasswordCryptoUtil.generateSalt();
            user.setPasswordSalt(salt);
            user.setPasswordHash(PasswordCryptoUtil.hashPassword(password, salt));
        }

        if (RoleConstants.STUDENT.equals(user.getRoleCode())) {
            String studentNo = requireRoleIdentity(request.getStudentNo(), "学生学号不能为空");
            validateUniqueStudentNo(studentNo, userId);
            user.setStudentNo(studentNo);
            user.setCounselorNo(null);
            profile = saveStudentProfile(userId, profile, request);
        } else if (RoleConstants.COUNSELOR.equals(user.getRoleCode())) {
            String counselorNo = requireRoleIdentity(request.getCounselorNo(), "咨询师工号不能为空");
            validateUniqueCounselorNo(counselorNo, userId);
            user.setCounselorNo(counselorNo);
            user.setStudentNo(null);
        } else {
            user.setStudentNo(null);
            user.setCounselorNo(null);
        }

        sysUserMapper.updateById(user);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_USER_UPDATE", "Update user",
                "Updated account " + user.getAccount() + " profile", null);
        return buildUserSummary(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserSummaryResponse enableUser(Long userId) {
        return updateUserStatus(userId, UserStatusConstants.ACTIVE, "ADMIN_USER_ENABLE", "Enable user");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserSummaryResponse disableUser(Long userId) {
        return updateUserStatus(userId, UserStatusConstants.DISABLED, "ADMIN_USER_DISABLE", "Disable user");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId) {
        SysUser user = getRequiredUser(userId);
        String salt = PasswordCryptoUtil.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordCryptoUtil.hashPassword(DEFAULT_PASSWORD, salt));
        sysUserMapper.updateById(user);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_USER_RESET_PASSWORD", "Reset password",
                "Reset password for account " + user.getAccount(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        validateDeletion(userId);
        SysUser user = getRequiredUser(userId);
        if (RoleConstants.STUDENT.equals(user.getRoleCode())) {
            studentProfileMapper.delete(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
            resourceFavoriteMapper.delete(new LambdaQueryWrapper<ResourceFavorite>().eq(ResourceFavorite::getStudentUserId, userId));
            resourceViewLogMapper.delete(new LambdaQueryWrapper<ResourceViewLog>().eq(ResourceViewLog::getStudentUserId, userId));
            counselorStudentMapper.delete(new LambdaQueryWrapper<CounselorStudent>().eq(CounselorStudent::getStudentUserId, userId));
        }
        if (RoleConstants.COUNSELOR.equals(user.getRoleCode())) {
            counselorStudentMapper.delete(new LambdaQueryWrapper<CounselorStudent>().eq(CounselorStudent::getCounselorUserId, userId));
            consultAppointmentSlotMapper.delete(new LambdaQueryWrapper<ConsultAppointmentSlot>().eq(ConsultAppointmentSlot::getCounselorUserId, userId));
        }
        siteNotificationMapper.delete(new LambdaQueryWrapper<SiteNotification>().eq(SiteNotification::getReceiverUserId, userId));
        sysUserMapper.deleteById(userId);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_USER_DELETE", "Delete user",
                "Deleted user account " + user.getAccount(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("至少需要选择一个用户");
        }
        for (Long userId : userIds) {
            if (userId != null) {
                deleteUser(userId);
            }
        }
    }

    private AdminUserSummaryResponse updateUserStatus(Long userId, String status, String actionCode, String actionName) {
        SysUser user = getRequiredUser(userId);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId)
                .last("limit 1"));
        auditLogService.record(SecurityUtil.getCurrentUserId(), actionCode, actionName,
                "Changed account " + user.getAccount() + " status to " + status, null);
        return buildUserSummary(user, profile);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRoleCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeGrade(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("20\\d{2}").matcher(normalized);
        return matcher.find() ? matcher.group() : normalized;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SysUser findByAccount(String account) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account.trim())
                .last("limit 1"));
    }

    private StudentProfile findStudentProfile(Long userId) {
        return studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId)
                .last("limit 1"));
    }

    private void validateUniqueAccount(String account, Long currentUserId) {
        SysUser existingUser = findByAccount(account);
        if (existingUser != null && !existingUser.getId().equals(currentUserId)) {
            throw new BusinessException("Account already exists");
        }
    }

    private void validateUniqueStudentNo(String studentNo, Long currentUserId) {
        SysUser existingUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStudentNo, studentNo)
                .last("limit 1"));
        if (existingUser != null && !existingUser.getId().equals(currentUserId)) {
            throw new BusinessException("Student number already exists");
        }
    }

    private void validateUniqueCounselorNo(String counselorNo, Long currentUserId) {
        SysUser existingUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getCounselorNo, counselorNo)
                .last("limit 1"));
        if (existingUser != null && !existingUser.getId().equals(currentUserId)) {
            throw new BusinessException("Counselor number already exists");
        }
    }

    private String requireRoleIdentity(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private StudentProfile createStudentProfile(Long userId, CreateAdminUserRequest request) {
        String college = normalizeOptional(request.getCollege());
        String grade = normalizeOptional(request.getGrade());
        String phone = normalizeOptional(request.getPhone());

        if (!StringUtils.hasText(college)) {
            throw new BusinessException("学生学院不能为空");
        }
        if (!StringUtils.hasText(grade)) {
            throw new BusinessException("学生年级不能为空");
        }

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setCollege(college);
        profile.setGrade(grade);
        profile.setPhone(phone);
        studentProfileMapper.insert(profile);
        return profile;
    }

    private StudentProfile saveStudentProfile(Long userId, StudentProfile profile, UpdateAdminUserRequest request) {
        String college = normalizeOptional(request.getCollege());
        String grade = normalizeOptional(request.getGrade());
        String phone = normalizeOptional(request.getPhone());

        if (profile == null && !StringUtils.hasText(college) && !StringUtils.hasText(grade) && !StringUtils.hasText(phone)) {
            return null;
        }

        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setCollege(college);
            profile.setGrade(grade);
            profile.setPhone(phone);
            studentProfileMapper.insert(profile);
            return profile;
        }

        profile.setCollege(college);
        profile.setGrade(grade);
        profile.setPhone(phone);
        studentProfileMapper.updateById(profile);
        return profile;
    }

    private void validateDeletion(Long userId) {
        SysUser user = getRequiredUser(userId);
        if (RoleConstants.ADMIN.equals(user.getRoleCode())) {
            throw new BusinessException("管理员账号不允许直接删除");
        }
        long scaleSessionCount = mentalScaleSessionMapper.selectCount(new LambdaQueryWrapper<MentalScaleSession>()
                .eq(MentalScaleSession::getUserId, userId));
        long reportCount = mentalScaleReportMapper.selectCount(new LambdaQueryWrapper<MentalScaleReport>()
                .eq(MentalScaleReport::getUserId, userId));
        long aiSessionCount = RoleConstants.STUDENT.equals(user.getRoleCode())
                ? aiChatSessionMapper.selectCount(new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getStudentUserId, userId))
                : 0L;
        long appointmentCount = consultAppointmentMapper.selectCount(new LambdaQueryWrapper<ConsultAppointment>()
                .and(wrapper -> wrapper.eq(ConsultAppointment::getStudentUserId, userId).or().eq(ConsultAppointment::getCounselorUserId, userId)));
        long chatSessionCount = consultChatSessionMapper.selectCount(new LambdaQueryWrapper<ConsultChatSession>()
                .and(wrapper -> wrapper.eq(ConsultChatSession::getStudentUserId, userId).or().eq(ConsultChatSession::getCounselorUserId, userId)));
        if (scaleSessionCount > 0 || reportCount > 0 || aiSessionCount > 0 || appointmentCount > 0 || chatSessionCount > 0) {
            throw new BusinessException("该账号已有业务数据，不能直接删除，请改为禁用账号");
        }
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        return user;
    }

    private AdminUserSummaryResponse buildUserSummary(SysUser user, StudentProfile profile) {
        return AdminUserSummaryResponse.builder()
                .userId(user.getId())
                .account(user.getAccount())
                .roleCode(user.getRoleCode())
                .realName(user.getRealName())
                .displayName(user.getDisplayName())
                .studentNo(user.getStudentNo())
                .counselorNo(user.getCounselorNo())
                .status(user.getStatus())
                .college(profile == null ? null : profile.getCollege())
                .grade(profile == null ? null : profile.getGrade())
                .phone(profile == null ? null : profile.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
