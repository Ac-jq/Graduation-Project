package sdu.jiaq.jqpro.service.impl;

import org.springframework.stereotype.Service;
import sdu.jiaq.jqpro.entity.SysAuditLog;
import sdu.jiaq.jqpro.mapper.SysAuditLogMapper;
import sdu.jiaq.jqpro.service.AuditLogService;

/**
 * 审计日志服务实现。
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final SysAuditLogMapper sysAuditLogMapper;

    public AuditLogServiceImpl(SysAuditLogMapper sysAuditLogMapper) {
        this.sysAuditLogMapper = sysAuditLogMapper;
    }

    @Override
    public void record(Long userId, String actionCode, String actionName, String detailText, String ipAddress) {
        SysAuditLog sysAuditLog = new SysAuditLog();
        sysAuditLog.setUserId(userId);
        sysAuditLog.setActionCode(actionCode);
        sysAuditLog.setActionName(actionName);
        sysAuditLog.setDetailText(detailText);
        sysAuditLog.setIpAddress(ipAddress);
        sysAuditLogMapper.insert(sysAuditLog);
    }
}
