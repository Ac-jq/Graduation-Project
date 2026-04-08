package sdu.jiaq.jqpro.service;

/**
 * 审计日志服务。
 */
public interface AuditLogService {

    void record(Long userId, String actionCode, String actionName, String detailText, String ipAddress);
}
