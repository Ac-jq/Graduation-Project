package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.adminai.AuditLogResponse;

import java.util.List;

/**
 * Admin audit query service.
 */
public interface AdminAuditService {

    List<AuditLogResponse> listLogs(String actionCode, String keyword);
}
