package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.adminai.AuditLogResponse;
import sdu.jiaq.jqpro.service.AdminAuditService;

import java.util.List;

/**
 * Admin audit query controller.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@SaCheckRole(RoleConstants.ADMIN)
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public Result<List<AuditLogResponse>> listLogs(@RequestParam(required = false) String actionCode,
                                                   @RequestParam(required = false) String keyword) {
        return Result.success(adminAuditService.listLogs(actionCode, keyword));
    }
}
