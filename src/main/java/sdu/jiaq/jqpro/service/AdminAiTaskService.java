package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskResponse;
import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskSummaryResponse;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskResponse;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskRequest;

import java.util.List;

/**
 * Admin AI task service.
 */
public interface AdminAiTaskService {

    ParseAdminAiTaskResponse parse(ParseAdminAiTaskRequest request);

    List<AdminAiTaskSummaryResponse> listTasks();

    AdminAiTaskResponse getTask(Long taskId);

    AdminAiTaskResponse confirm(Long taskId);

    AdminAiTaskResponse cancel(Long taskId);
}
