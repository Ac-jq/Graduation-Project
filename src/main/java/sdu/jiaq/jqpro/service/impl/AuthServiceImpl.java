package sdu.jiaq.jqpro.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.IpUtil;
import sdu.jiaq.jqpro.common.util.PasswordCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.auth.ChangePasswordRequest;
import sdu.jiaq.jqpro.dto.auth.CurrentUserResponse;
import sdu.jiaq.jqpro.dto.auth.LoginRequest;
import sdu.jiaq.jqpro.dto.auth.LoginResponse;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.AuthService;

import java.util.List;

/**
 * 认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(SysUserMapper sysUserMapper, AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, request.getAccount())
                .last("limit 1"));
        if (sysUser == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!UserStatusConstants.ACTIVE.equals(sysUser.getStatus())) {
            throw new BusinessException("当前账号已被禁用");
        }
        if (!PasswordCryptoUtil.matches(request.getPassword(), sysUser.getPasswordSalt(), sysUser.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }

        StpUtil.login(sysUser.getId());
        auditLogService.record(sysUser.getId(), "LOGIN", "用户登录", "账号登录成功", IpUtil.resolveClientIp(httpServletRequest));

        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .userId(sysUser.getId())
                .account(sysUser.getAccount())
                .roleCode(sysUser.getRoleCode())
                .displayName(sysUser.getDisplayName())
                .roles(List.of(sysUser.getRoleCode()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(HttpServletRequest httpServletRequest) {
        Long userId = SecurityUtil.getCurrentUserId();
        auditLogService.record(userId, "LOGOUT", "用户登出", "账号主动退出登录", IpUtil.resolveClientIp(httpServletRequest));
        StpUtil.logout(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpServletRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        SysUser sysUser = getRequiredUser(userId);
        if (!PasswordCryptoUtil.matches(request.getOldPassword(), sysUser.getPasswordSalt(), sysUser.getPasswordHash())) {
            throw new BusinessException("旧密码不正确");
        }
        String newSalt = PasswordCryptoUtil.generateSalt();
        sysUser.setPasswordSalt(newSalt);
        sysUser.setPasswordHash(PasswordCryptoUtil.hashPassword(request.getNewPassword(), newSalt));
        sysUserMapper.updateById(sysUser);

        auditLogService.record(userId, "CHANGE_PASSWORD", "修改密码", "用户修改登录密码", IpUtil.resolveClientIp(httpServletRequest));
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        SysUser sysUser = getRequiredUser(SecurityUtil.getCurrentUserId());
        return CurrentUserResponse.builder()
                .userId(sysUser.getId())
                .account(sysUser.getAccount())
                .roleCode(sysUser.getRoleCode())
                .realName(sysUser.getRealName())
                .displayName(sysUser.getDisplayName())
                .studentNo(sysUser.getStudentNo())
                .counselorNo(sysUser.getCounselorNo())
                .roles(List.of(sysUser.getRoleCode()))
                .build();
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }
        return sysUser;
    }
}
