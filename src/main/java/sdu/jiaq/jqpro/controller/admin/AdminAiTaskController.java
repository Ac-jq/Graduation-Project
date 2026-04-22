package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskResponse;
import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskSummaryResponse;
import sdu.jiaq.jqpro.dto.adminai.ConfirmAdminAiTaskRequest;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskRequest;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskResponse;
import sdu.jiaq.jqpro.service.AdminAiTaskService;

/**
 * Administrator AI task controller.
 */
@RestController
@RequestMapping("/api/admin/ai-tasks")
@SaCheckRole(RoleConstants.ADMIN)
public class AdminAiTaskController {

    private final AdminAiTaskService adminAiTaskService;

    public AdminAiTaskController(AdminAiTaskService adminAiTaskService) {
        this.adminAiTaskService = adminAiTaskService;
    }

    @PostMapping("/parse")
    public Result<ParseAdminAiTaskResponse> parse(@Valid @RequestBody ParseAdminAiTaskRequest request) {
        return Result.success(adminAiTaskService.parse(request));
    }

    @GetMapping
    public Result<List<AdminAiTaskSummaryResponse>> listTasks() {
        return Result.success(adminAiTaskService.listTasks());
    }

    @GetMapping("/{taskId}")
    public Result<AdminAiTaskResponse> getTask(@PathVariable Long taskId) {
        return Result.success(adminAiTaskService.getTask(taskId));
    }

    @PostMapping("/{taskId}/confirm")
    public Result<AdminAiTaskResponse> confirm(@PathVariable Long taskId,
                                               @RequestBody(required = false) ConfirmAdminAiTaskRequest request) {
        return Result.success("任务执行成功", adminAiTaskService.confirm(taskId, request));
    }

    @PostMapping("/{taskId}/cancel")
    public Result<AdminAiTaskResponse> cancel(@PathVariable Long taskId) {
        return Result.success("任务已取消", adminAiTaskService.cancel(taskId));
    }
}
