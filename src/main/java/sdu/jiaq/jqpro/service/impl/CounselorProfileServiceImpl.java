package sdu.jiaq.jqpro.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.counselor.CounselorProfileResponse;
import sdu.jiaq.jqpro.dto.counselor.UpdateCounselorProfileRequest;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.CounselorProfileService;

/**
 * 咨询师个人资料服务实现。
 */
@Service
public class CounselorProfileServiceImpl implements CounselorProfileService {

    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;

    public CounselorProfileServiceImpl(SysUserMapper sysUserMapper, AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public CounselorProfileResponse getCurrentCounselorProfile() {
        return buildProfileResponse(getRequiredCurrentCounselor());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CounselorProfileResponse updateCurrentCounselorProfile(UpdateCounselorProfileRequest request) {
        SysUser counselor = getRequiredCurrentCounselor();
        counselor.setAvatarUrl(request.getAvatarUrl());
        sysUserMapper.updateById(counselor);
        auditLogService.record(counselor.getId(), "COUNSELOR_PROFILE_UPDATE", "更新咨询师资料",
                "更新咨询师头像地址", "system");
        return buildProfileResponse(counselor);
    }

    private SysUser getRequiredCurrentCounselor() {
        SysUser counselor = sysUserMapper.selectById(SecurityUtil.getCurrentUserId());
        if (counselor == null) {
            throw new BusinessException("用户不存在");
        }
        if (!RoleConstants.COUNSELOR.equals(counselor.getRoleCode())) {
            throw new BusinessException("仅咨询师可以维护该资料");
        }
        return counselor;
    }

    private CounselorProfileResponse buildProfileResponse(SysUser counselor) {
        return CounselorProfileResponse.builder()
                .userId(counselor.getId())
                .account(counselor.getAccount())
                .realName(counselor.getRealName())
                .displayName(counselor.getDisplayName())
                .counselorNo(counselor.getCounselorNo())
                .roleCode(counselor.getRoleCode())
                .avatarUrl(counselor.getAvatarUrl())
                .build();
    }
}
