package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.PasswordCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.adminuser.AdminUserSummaryResponse;
import sdu.jiaq.jqpro.dto.adminuser.CreateCounselorRequest;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysUser;
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
    private final AuditLogService auditLogService;

    public AdminUserServiceImpl(SysUserMapper sysUserMapper,
                                StudentProfileMapper studentProfileMapper,
                                AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<AdminUserSummaryResponse> listUsers(String roleCode, String status, String keyword) {
        String normalizedRole = normalize(roleCode);
        String normalizedStatus = normalize(status);
        String normalizedKeyword = normalize(keyword);
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreatedAt, SysUser::getId));
        Map<Long, StudentProfile> profileMap = studentProfileMapper.selectList(null).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity(), (left, right) -> left));
        return users.stream()
                .filter(user -> normalizedRole == null || normalizedRole.equalsIgnoreCase(user.getRoleCode()))
                .filter(user -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(user.getStatus()))
                .filter(user -> matchKeyword(user, profileMap.get(user.getId()), normalizedKeyword))
                .map(user -> buildUserSummary(user, profileMap.get(user.getId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserSummaryResponse createCounselor(CreateCounselorRequest request) {
        if (findByAccount(request.getAccount()) != null) {
            throw new BusinessException("Account already exists");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getCounselorNo, request.getCounselorNo())) > 0) {
            throw new BusinessException("Counselor number already exists");
        }
        String salt = PasswordCryptoUtil.generateSalt();
        SysUser user = new SysUser();
        user.setAccount(request.getAccount().trim());
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordCryptoUtil.hashPassword(DEFAULT_PASSWORD, salt));
        user.setRoleCode(RoleConstants.COUNSELOR);
        user.setRealName(blankToDefault(request.getRealName(), request.getDisplayName().trim()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setCounselorNo(request.getCounselorNo().trim());
        user.setStatus(UserStatusConstants.ACTIVE);
        sysUserMapper.insert(user);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "ADMIN_USER_CREATE_COUNSELOR", "Create counselor",
                "Created counselor account " + user.getAccount(), null);
        return buildUserSummary(user, null);
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

    private boolean matchKeyword(SysUser user, StudentProfile profile, String keyword) {
        if (keyword == null) {
            return true;
        }
        return contains(user.getAccount(), keyword)
                || contains(user.getDisplayName(), keyword)
                || contains(user.getRealName(), keyword)
                || contains(user.getStudentNo(), keyword)
                || contains(user.getCounselorNo(), keyword)
                || contains(profile == null ? null : profile.getCollege(), keyword)
                || contains(profile == null ? null : profile.getGrade(), keyword);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private SysUser findByAccount(String account) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account.trim())
                .last("limit 1"));
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
                .createdAt(user.getCreatedAt())
                .build();
    }
}
