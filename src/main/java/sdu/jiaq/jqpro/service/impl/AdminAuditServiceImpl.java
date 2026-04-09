package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.dto.adminai.AuditLogResponse;
import sdu.jiaq.jqpro.entity.SysAuditLog;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.SysAuditLogMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AdminAuditService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin audit query implementation.
 */
@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final SysUserMapper sysUserMapper;

    public AdminAuditServiceImpl(SysAuditLogMapper sysAuditLogMapper, SysUserMapper sysUserMapper) {
        this.sysAuditLogMapper = sysAuditLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<AuditLogResponse> listLogs(String actionCode, String keyword) {
        LambdaQueryWrapper<SysAuditLog> queryWrapper = new LambdaQueryWrapper<SysAuditLog>()
                .eq(actionCode != null && !actionCode.isBlank(), SysAuditLog::getActionCode, actionCode);
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.nested(wrapper -> wrapper
                    .like(SysAuditLog::getActionName, keyword)
                    .or()
                    .like(SysAuditLog::getDetailText, keyword));
        }
        queryWrapper.orderByDesc(SysAuditLog::getCreatedAt, SysAuditLog::getId);

        List<SysAuditLog> logs = sysAuditLogMapper.selectList(queryWrapper);
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(logs.stream()
                        .map(SysAuditLog::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return logs.stream()
                .map(log -> AuditLogResponse.builder()
                        .logId(log.getId())
                        .userId(log.getUserId())
                        .userDisplayName(log.getUserId() == null || !userMap.containsKey(log.getUserId()) ? null : userMap.get(log.getUserId()).getDisplayName())
                        .actionCode(log.getActionCode())
                        .actionName(log.getActionName())
                        .detailText(log.getDetailText())
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }
}