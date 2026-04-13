package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sdu.jiaq.jqpro.common.constant.AdminAiTaskConstants;
import sdu.jiaq.jqpro.common.constant.ResourceConstants;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.constant.UserStatusConstants;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.PasswordCryptoUtil;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskItemResponse;
import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskResponse;
import sdu.jiaq.jqpro.dto.adminai.AdminAiTaskSummaryResponse;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskRequest;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskResponse;
import sdu.jiaq.jqpro.entity.AdminAiTask;
import sdu.jiaq.jqpro.entity.AdminAiTaskItem;
import sdu.jiaq.jqpro.entity.MentalResource;
import sdu.jiaq.jqpro.entity.SysAuditLog;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AdminAiTaskItemMapper;
import sdu.jiaq.jqpro.mapper.AdminAiTaskMapper;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.SysAuditLogMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AdminAiTaskService;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiAction;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiClient;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiPlan;

/**
 * Administrator AI task service.
 * The model generates a reviewed execution plan, while all real data changes
 * are still validated and executed on the server after explicit confirmation.
 */
@Slf4j
@Service
public class AdminAiTaskServiceImpl implements AdminAiTaskService {

    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("(?:resource|资源|ID|id)\\s*[:：#-]?\\s*(\\d+)");
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("(?:\"|“|‘|《)(.*?)(?:\"|”|’|》)");
    private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d+)\\s*(?:个月|月|month|months)");
    private static final String FIELD_VALUE_TEMPLATE = "(?i)%s\\s*[:：=]\\s*([\\p{L}\\p{N}_-]+)";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ACCOUNT = "account";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_COUNSELOR_NO = "counselorNo";
    private static final String FIELD_ROLE_CODE = "roleCode";
    private static final String DEFAULT_COUNSELOR_PASSWORD = "Jqpro@123";

    private final AdminAiTaskMapper adminAiTaskMapper;
    private final AdminAiTaskItemMapper adminAiTaskItemMapper;
    private final SysUserMapper sysUserMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final SysAuditLogMapper sysAuditLogMapper;
    private final AuditLogService auditLogService;
    private final AdminOpsAiClient adminOpsAiClient;

    public AdminAiTaskServiceImpl(AdminAiTaskMapper adminAiTaskMapper,
                                  AdminAiTaskItemMapper adminAiTaskItemMapper,
                                  SysUserMapper sysUserMapper,
                                  MentalResourceMapper mentalResourceMapper,
                                  SysAuditLogMapper sysAuditLogMapper,
                                  AuditLogService auditLogService,
                                  AdminOpsAiClient adminOpsAiClient) {
        this.adminAiTaskMapper = adminAiTaskMapper;
        this.adminAiTaskItemMapper = adminAiTaskItemMapper;
        this.sysUserMapper = sysUserMapper;
        this.mentalResourceMapper = mentalResourceMapper;
        this.sysAuditLogMapper = sysAuditLogMapper;
        this.auditLogService = auditLogService;
        this.adminOpsAiClient = adminOpsAiClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseAdminAiTaskResponse parse(ParseAdminAiTaskRequest request) {
        Long adminUserId = SecurityUtil.getCurrentUserId();
        String instruction = request.getInstruction().trim();
        ParsedTask parsedTask = parseInstruction(instruction);

        AdminAiTask task = new AdminAiTask();
        task.setAdminUserId(adminUserId);
        task.setInstructionText(instruction);
        task.setTaskType(parsedTask.taskType);
        task.setParseStatus(parsedTask.parseStatus);
        task.setConfirmStatus(AdminAiTaskConstants.CONFIRM_PENDING);
        task.setExecuteStatus(AdminAiTaskConstants.EXECUTE_WAITING);
        task.setSummaryText(parsedTask.summaryText);
        task.setFailureReason(parsedTask.failureReason);
        adminAiTaskMapper.insert(task);

        int sortNo = 1;
        for (AdminAiTaskItem item : parsedTask.items) {
            item.setTaskId(task.getId());
            item.setSortNo(sortNo++);
            item.setExecuteStatus(AdminAiTaskConstants.EXECUTE_WAITING);
            adminAiTaskItemMapper.insert(item);
        }

        auditLogService.record(adminUserId, "ADMIN_AI_PARSE", "Admin AI parse task",
                "Parsed task #" + task.getId() + " from instruction: " + instruction, null);

        return ParseAdminAiTaskResponse.builder()
                .ready(AdminAiTaskConstants.PARSE_READY.equals(task.getParseStatus()))
                .message(AdminAiTaskConstants.PARSE_READY.equals(task.getParseStatus())
                        ? "已生成待确认执行计划"
                        : task.getFailureReason())
                .task(getTask(task.getId()))
                .build();
    }

    @Override
    public List<AdminAiTaskSummaryResponse> listTasks() {
        return adminAiTaskMapper.selectList(new LambdaQueryWrapper<AdminAiTask>()
                        .orderByDesc(AdminAiTask::getCreatedAt, AdminAiTask::getId))
                .stream()
                .map(this::buildTaskSummaryResponse)
                .toList();
    }

    @Override
    public AdminAiTaskResponse getTask(Long taskId) {
        AdminAiTask task = getRequiredTask(taskId);
        List<AdminAiTaskItemResponse> items = adminAiTaskItemMapper.selectList(new LambdaQueryWrapper<AdminAiTaskItem>()
                        .eq(AdminAiTaskItem::getTaskId, taskId)
                        .orderByAsc(AdminAiTaskItem::getSortNo, AdminAiTaskItem::getId))
                .stream()
                .map(this::buildTaskItemResponse)
                .toList();
        return buildTaskResponse(task, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminAiTaskResponse confirm(Long taskId) {
        Long adminUserId = SecurityUtil.getCurrentUserId();
        AdminAiTask task = getRequiredTask(taskId);
        if (!AdminAiTaskConstants.PARSE_READY.equals(task.getParseStatus())) {
            throw new BusinessException("当前任务尚未形成可执行计划");
        }
        if (!AdminAiTaskConstants.CONFIRM_PENDING.equals(task.getConfirmStatus())) {
            throw new BusinessException("当前任务已经处理过，不能重复确认");
        }

        List<AdminAiTaskItem> items = adminAiTaskItemMapper.selectList(new LambdaQueryWrapper<AdminAiTaskItem>()
                .eq(AdminAiTaskItem::getTaskId, taskId)
                .orderByAsc(AdminAiTaskItem::getSortNo, AdminAiTaskItem::getId));
        if (items.isEmpty()) {
            throw new BusinessException("当前任务没有待执行明细，无法确认执行");
        }

        validateItemsBeforeExecution(items);
        for (AdminAiTaskItem item : items) {
            executeItem(item, adminUserId);
            item.setExecuteStatus(AdminAiTaskConstants.EXECUTE_EXECUTED);
            adminAiTaskItemMapper.updateById(item);
        }

        LocalDateTime now = LocalDateTime.now();
        task.setConfirmStatus(AdminAiTaskConstants.CONFIRM_CONFIRMED);
        task.setExecuteStatus(AdminAiTaskConstants.EXECUTE_EXECUTED);
        task.setConfirmedAt(now);
        task.setExecutedAt(now);
        adminAiTaskMapper.updateById(task);
        auditLogService.record(adminUserId, "ADMIN_AI_CONFIRM", "Admin AI confirm task",
                "Confirmed and executed task #" + taskId, null);
        return getTask(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminAiTaskResponse cancel(Long taskId) {
        Long adminUserId = SecurityUtil.getCurrentUserId();
        AdminAiTask task = getRequiredTask(taskId);
        if (!AdminAiTaskConstants.CONFIRM_PENDING.equals(task.getConfirmStatus())) {
            throw new BusinessException("当前任务不能取消");
        }
        task.setConfirmStatus(AdminAiTaskConstants.CONFIRM_CANCELED);
        task.setExecuteStatus(AdminAiTaskConstants.EXECUTE_CANCELED);
        adminAiTaskMapper.updateById(task);

        List<AdminAiTaskItem> items = adminAiTaskItemMapper.selectList(new LambdaQueryWrapper<AdminAiTaskItem>()
                .eq(AdminAiTaskItem::getTaskId, taskId));
        for (AdminAiTaskItem item : items) {
            item.setExecuteStatus(AdminAiTaskConstants.EXECUTE_CANCELED);
            adminAiTaskItemMapper.updateById(item);
        }
        auditLogService.record(adminUserId, "ADMIN_AI_CANCEL", "Admin AI cancel task",
                "Canceled task #" + taskId, null);
        return getTask(taskId);
    }

    private ParsedTask parseInstruction(String instruction) {
        if (adminOpsAiClient.isEnabled()) {
            try {
                ParsedTask aiParsedTask = parseInstructionByAi(instruction);
                if (aiParsedTask != null) {
                    return aiParsedTask;
                }
            } catch (BusinessException exception) {
                log.warn("Administrator AI parse failed, fallback to local rules: {}", exception.getMessage());
            } catch (Exception exception) {
                log.warn("Administrator AI parse failed unexpectedly, fallback to local rules", exception);
            }
        }
        return parseInstructionByRules(instruction);
    }

    private ParsedTask parseInstructionByAi(String instruction) {
        AdminOpsAiPlan plan = adminOpsAiClient.parseInstruction(instruction);
        if (plan == null) {
            return null;
        }
        String parseStatus = normalizeText(plan.parseStatus());
        if (!isReadyParseStatus(parseStatus)) {
            return null;
        }
        List<AdminOpsAiAction> actions = plan.actions() == null ? List.of() : plan.actions();
        if (actions.isEmpty()) {
            return null;
        }

        String taskType = normalizeTaskType(plan.taskType());
        return switch (taskType) {
            case AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS -> parseAccountStatusActions(actions, plan.summaryText(), instruction);
            case AdminAiTaskConstants.TASK_TYPE_COUNSELOR_CREATE -> parseCounselorCreateActions(actions, plan.summaryText());
            case AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS -> parseResourceStatusActions(actions, plan.summaryText(), instruction);
            default -> ParsedTask.needMoreInfo(firstText(plan.failureReason(), "AI 返回了当前系统不支持的操作类型"));
        };
    }

    private ParsedTask parseInstructionByRules(String instruction) {
        String normalized = instruction.trim();
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (containsAny(lowered, "禁用", "停用", "封禁", "disable account", "suspend account", "ban account")) {
            if (containsAny(lowered, "未登录", "inactive", "inactive student") && containsAny(lowered, "学生", "student")) {
                return parseInactiveStudentTask(normalized);
            }
            return parseAccountStatusTask(normalized, UserStatusConstants.DISABLED);
        }
        if (containsAny(lowered, "启用", "恢复", "enable account", "activate account", "restore account")) {
            return parseAccountStatusTask(normalized, UserStatusConstants.ACTIVE);
        }
        if (containsAny(lowered, "创建咨询师", "新增咨询师", "新建一个心理咨询师", "create counselor", "add counselor")) {
            return parseCounselorCreateTask(normalized);
        }
        if (containsAny(lowered, "上架资源", "发布资源", "publish resource", "publish article", "online resource")) {
            return parseResourceStatusTask(normalized, ResourceConstants.RESOURCE_PUBLISHED, AdminAiTaskConstants.OP_PUBLISH);
        }
        if (containsAny(lowered, "下架资源", "停止资源", "下架文章", "offline resource", "unpublish resource", "take resource offline")) {
            return parseResourceStatusTask(normalized, ResourceConstants.RESOURCE_OFFLINE, AdminAiTaskConstants.OP_OFFLINE);
        }
        return ParsedTask.needMoreInfo("当前只支持账号启停、批量禁用长期未登录学生、创建咨询师、资源上架/下架");
    }

    private ParsedTask parseAccountStatusActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        Map<Long, AdminAiTaskItem> uniqueItems = new LinkedHashMap<>();
        for (AdminOpsAiAction action : actions) {
            Integer inactiveMonths = action.inactiveMonths();
            String roleCode = normalizeUpper(action.roleCode());
            if (inactiveMonths != null && inactiveMonths > 0 && RoleConstants.STUDENT.equals(roleCode)) {
                ParsedTask parsedTask = parseInactiveStudentTask(inactiveMonths, summaryText);
                if (!AdminAiTaskConstants.PARSE_READY.equals(parsedTask.parseStatus)) {
                    return parsedTask;
                }
                for (AdminAiTaskItem item : parsedTask.items) {
                    uniqueItems.put(item.getTargetId(), item);
                }
                continue;
            }

            String account = normalizeText(action.account());
            if (account == null) {
                SysUser mentionedUser = findMentionedUser(instruction);
                account = mentionedUser == null ? null : mentionedUser.getAccount();
            }
            if (account == null) {
                return ParsedTask.needMoreInfo("请明确要操作的账号");
            }
            SysUser user = findUserByAccount(account);
            if (user == null) {
                return ParsedTask.needMoreInfo("未找到账号 " + account);
            }
            String targetStatus = resolveAccountTargetStatus(action, summaryText, instruction);
            if (targetStatus == null) {
                return ParsedTask.needMoreInfo("AI 未能识别账号状态变更目标");
            }
            uniqueItems.put(user.getId(), buildUserStatusItem(user, targetStatus));
        }

        if (uniqueItems.isEmpty()) {
            return ParsedTask.needMoreInfo("未匹配到可执行的账号状态变更对象");
        }
        String summary = StringUtils.hasText(summaryText)
                ? summaryText.trim()
                : "待确认 " + uniqueItems.size() + " 条账号状态变更";
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS, summary, new ArrayList<>(uniqueItems.values()));
    }

    private ParsedTask parseCounselorCreateActions(List<AdminOpsAiAction> actions, String summaryText) {
        AdminOpsAiAction primaryAction = actions.stream().findFirst().orElse(null);
        if (primaryAction == null) {
            return ParsedTask.needMoreInfo("AI 未生成创建咨询师所需字段");
        }
        String displayName = normalizeText(primaryAction.displayName());
        String counselorNo = normalizeText(primaryAction.counselorNo());
        if (!StringUtils.hasText(displayName) || !StringUtils.hasText(counselorNo)) {
            return ParsedTask.needMoreInfo("创建咨询师至少需要姓名和工号");
        }
        if (existsCounselorNo(counselorNo)) {
            return ParsedTask.needMoreInfo("工号 " + counselorNo + " 已存在");
        }
        String account = normalizeText(primaryAction.account());
        if (!StringUtils.hasText(account)) {
            account = generateCounselorAccount(counselorNo);
        }
        if (findUserByAccount(account) != null) {
            return ParsedTask.needMoreInfo("自动生成的账号 " + account + " 已存在，请重新指定账号或工号");
        }

        List<AdminAiTaskItem> items = List.of(
                buildCreateItem(account, FIELD_ACCOUNT, account),
                buildCreateItem(account, FIELD_DISPLAY_NAME, displayName),
                buildCreateItem(account, FIELD_COUNSELOR_NO, counselorNo),
                buildCreateItem(account, FIELD_ROLE_CODE, RoleConstants.COUNSELOR),
                buildCreateItem(account, FIELD_STATUS, UserStatusConstants.ACTIVE)
        );
        String summary = StringUtils.hasText(summaryText)
                ? summaryText.trim()
                : "待创建咨询师账号 " + account + "（" + displayName + " / " + counselorNo + "）";
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_COUNSELOR_CREATE, summary, items);
    }

    private ParsedTask parseResourceStatusActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        AdminOpsAiAction primaryAction = actions.stream().findFirst().orElse(null);
        if (primaryAction == null) {
            return ParsedTask.needMoreInfo("AI 未生成资源状态变更信息");
        }
        MentalResource resource = findResource(primaryAction.resourceId(), primaryAction.resourceTitle(), instruction);
        if (resource == null) {
            return ParsedTask.needMoreInfo("请提供准确的资源标题或资源 ID");
        }
        String nextStatus = normalizeResourceStatus(
                primaryAction.newValue(),
                firstText(primaryAction.operationType(), primaryAction.actionType(), summaryText)
        );
        if (nextStatus == null) {
            return ParsedTask.needMoreInfo("AI 未能识别资源上架/下架目标状态");
        }
        String operationType = ResourceConstants.RESOURCE_PUBLISHED.equals(nextStatus)
                ? AdminAiTaskConstants.OP_PUBLISH
                : AdminAiTaskConstants.OP_OFFLINE;
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_RESOURCE);
        item.setTargetId(resource.getId());
        item.setTargetLabel(buildResourceLabel(resource));
        item.setOperationType(operationType);
        item.setFieldName(FIELD_STATUS);
        item.setOldValue(resource.getStatus());
        item.setNewValue(nextStatus);
        String summary = StringUtils.hasText(summaryText)
                ? summaryText.trim()
                : "待将资源《" + resource.getTitle() + "》状态调整为 " + nextStatus;
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS, summary, List.of(item));
    }

    private ParsedTask parseAccountStatusTask(String instruction, String targetStatus) {
        String account = extractFieldValue(instruction, "账号", "账户", "account");
        if (account == null) {
            SysUser mentionedUser = findMentionedUser(instruction);
            account = mentionedUser == null ? null : mentionedUser.getAccount();
        }
        if (account == null) {
            return ParsedTask.needMoreInfo("请提供要操作的账号");
        }
        SysUser user = findUserByAccount(account);
        if (user == null) {
            return ParsedTask.needMoreInfo("目标账号不存在");
        }
        AdminAiTaskItem item = buildUserStatusItem(user, targetStatus);
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS,
                "待将账号 " + user.getAccount() + " 状态变更为 " + targetStatus,
                List.of(item));
    }

    private ParsedTask parseInactiveStudentTask(String instruction) {
        Matcher matcher = MONTH_PATTERN.matcher(instruction);
        if (!matcher.find()) {
            return ParsedTask.needMoreInfo("请明确未登录时长，例如“三个月未登录的学生账号”");
        }
        int inactiveMonths = Integer.parseInt(matcher.group(1));
        return parseInactiveStudentTask(inactiveMonths, null);
    }

    private ParsedTask parseInactiveStudentTask(int inactiveMonths, String summaryText) {
        if (inactiveMonths <= 0) {
            return ParsedTask.needMoreInfo("未登录时长必须大于 0 个月");
        }
        List<SysUser> targetUsers = findInactiveStudents(inactiveMonths);
        if (targetUsers.isEmpty()) {
            return ParsedTask.needMoreInfo("当前没有符合“连续 " + inactiveMonths + " 个月未登录”的学生账号");
        }
        List<AdminAiTaskItem> items = targetUsers.stream()
                .map(user -> buildUserStatusItem(user, UserStatusConstants.DISABLED))
                .toList();
        String summary = StringUtils.hasText(summaryText)
                ? summaryText.trim()
                : "待禁用 " + targetUsers.size() + " 个连续 " + inactiveMonths + " 个月未登录的学生账号";
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS, summary, items);
    }

    private ParsedTask parseCounselorCreateTask(String instruction) {
        String account = extractFieldValue(instruction, "账号", "账户", "account");
        String displayName = extractFieldValue(instruction, "显示名", "姓名", "名字", "displayName", "name");
        String counselorNo = extractFieldValue(instruction, "工号", "counselorNo", "staffNo");
        if (!StringUtils.hasText(displayName) || !StringUtils.hasText(counselorNo)) {
            return ParsedTask.needMoreInfo("创建咨询师需要至少提供姓名和工号");
        }
        if (existsCounselorNo(counselorNo)) {
            return ParsedTask.needMoreInfo("工号 " + counselorNo + " 已存在");
        }
        if (!StringUtils.hasText(account)) {
            account = generateCounselorAccount(counselorNo);
        }
        if (findUserByAccount(account) != null) {
            return ParsedTask.needMoreInfo("账号 " + account + " 已存在");
        }
        List<AdminAiTaskItem> items = List.of(
                buildCreateItem(account, FIELD_ACCOUNT, account),
                buildCreateItem(account, FIELD_DISPLAY_NAME, displayName),
                buildCreateItem(account, FIELD_COUNSELOR_NO, counselorNo),
                buildCreateItem(account, FIELD_ROLE_CODE, RoleConstants.COUNSELOR),
                buildCreateItem(account, FIELD_STATUS, UserStatusConstants.ACTIVE)
        );
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_COUNSELOR_CREATE,
                "待创建咨询师账号 " + account + "（" + displayName + " / " + counselorNo + "）",
                items);
    }

    private ParsedTask parseResourceStatusTask(String instruction, String targetStatus, String operationType) {
        MentalResource resource = findResourceByInstruction(instruction);
        if (resource == null) {
            return ParsedTask.needMoreInfo("请提供资源 ID 或完整标题");
        }
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_RESOURCE);
        item.setTargetId(resource.getId());
        item.setTargetLabel(buildResourceLabel(resource));
        item.setOperationType(operationType);
        item.setFieldName(FIELD_STATUS);
        item.setOldValue(resource.getStatus());
        item.setNewValue(targetStatus);
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS,
                "待将资源《" + resource.getTitle() + "》状态调整为 " + targetStatus,
                List.of(item));
    }

    private void validateItemsBeforeExecution(List<AdminAiTaskItem> items) {
        for (AdminAiTaskItem item : items) {
            if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                    && AdminAiTaskConstants.OP_UPDATE.equals(item.getOperationType())
                    && FIELD_STATUS.equals(item.getFieldName())) {
                SysUser user = getRequiredUser(item.getTargetId());
                if (!Objects.equals(user.getStatus(), item.getOldValue())) {
                    throw new BusinessException("账号状态已发生变化，请重新解析指令后再确认");
                }
                continue;
            }

            if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                    && AdminAiTaskConstants.OP_CREATE.equals(item.getOperationType())
                    && FIELD_ACCOUNT.equals(item.getFieldName())) {
                if (findUserByAccount(item.getNewValue()) != null) {
                    throw new BusinessException("待创建的咨询师账号已存在");
                }
                String counselorNo = getOptionalItemValue(items, FIELD_COUNSELOR_NO);
                if (StringUtils.hasText(counselorNo) && existsCounselorNo(counselorNo)) {
                    throw new BusinessException("待创建咨询师的工号已存在");
                }
                continue;
            }

            if (AdminAiTaskConstants.TARGET_RESOURCE.equals(item.getTargetType())
                    && FIELD_STATUS.equals(item.getFieldName())) {
                MentalResource resource = getRequiredResource(item.getTargetId());
                if (!Objects.equals(resource.getStatus(), item.getOldValue())) {
                    throw new BusinessException("资源状态已发生变化，请重新解析指令后再确认");
                }
            }
        }
    }

    private void executeItem(AdminAiTaskItem item, Long adminUserId) {
        if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                && AdminAiTaskConstants.OP_UPDATE.equals(item.getOperationType())
                && FIELD_STATUS.equals(item.getFieldName())) {
            SysUser user = getRequiredUser(item.getTargetId());
            user.setStatus(item.getNewValue());
            sysUserMapper.updateById(user);
            auditLogService.record(adminUserId, "ADMIN_AI_USER_STATUS", "Admin AI update user status",
                    "Account " + user.getAccount() + " status changed from " + item.getOldValue() + " to " + item.getNewValue(), null);
            return;
        }

        if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                && AdminAiTaskConstants.OP_CREATE.equals(item.getOperationType())) {
            executeCreateCounselor(item, adminUserId);
            return;
        }

        if (AdminAiTaskConstants.TARGET_RESOURCE.equals(item.getTargetType())
                && FIELD_STATUS.equals(item.getFieldName())) {
            MentalResource resource = getRequiredResource(item.getTargetId());
            resource.setStatus(item.getNewValue());
            if (ResourceConstants.RESOURCE_PUBLISHED.equals(item.getNewValue()) && resource.getPublishedAt() == null) {
                resource.setPublishedAt(LocalDateTime.now());
            }
            mentalResourceMapper.updateById(resource);
            auditLogService.record(adminUserId, "ADMIN_AI_RESOURCE_STATUS", "Admin AI update resource status",
                    "Resource #" + resource.getId() + " status changed from " + item.getOldValue() + " to " + item.getNewValue(), null);
        }
    }

    private void executeCreateCounselor(AdminAiTaskItem currentItem, Long adminUserId) {
        if (!FIELD_ACCOUNT.equals(currentItem.getFieldName())) {
            return;
        }
        List<AdminAiTaskItem> peerItems = adminAiTaskItemMapper.selectList(new LambdaQueryWrapper<AdminAiTaskItem>()
                .eq(AdminAiTaskItem::getTaskId, currentItem.getTaskId())
                .orderByAsc(AdminAiTaskItem::getSortNo, AdminAiTaskItem::getId));

        String account = getRequiredItemValue(peerItems, FIELD_ACCOUNT);
        String displayName = getRequiredItemValue(peerItems, FIELD_DISPLAY_NAME);
        String counselorNo = getRequiredItemValue(peerItems, FIELD_COUNSELOR_NO);
        String roleCode = firstText(getOptionalItemValue(peerItems, FIELD_ROLE_CODE), RoleConstants.COUNSELOR);
        String status = firstText(getOptionalItemValue(peerItems, FIELD_STATUS), UserStatusConstants.ACTIVE);

        String salt = PasswordCryptoUtil.generateSalt();
        SysUser counselor = new SysUser();
        counselor.setAccount(account);
        counselor.setPasswordSalt(salt);
        counselor.setPasswordHash(PasswordCryptoUtil.hashPassword(DEFAULT_COUNSELOR_PASSWORD, salt));
        counselor.setRoleCode(roleCode);
        counselor.setRealName(displayName);
        counselor.setDisplayName(displayName);
        counselor.setCounselorNo(counselorNo);
        counselor.setStatus(status);
        sysUserMapper.insert(counselor);

        auditLogService.record(adminUserId, "ADMIN_AI_COUNSELOR_CREATE", "Admin AI create counselor",
                "Created counselor account " + account + " with counselorNo " + counselorNo, null);
    }

    private List<SysUser> findInactiveStudents(int inactiveMonths) {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(inactiveMonths);
        List<SysUser> students = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, RoleConstants.STUDENT)
                .eq(SysUser::getStatus, UserStatusConstants.ACTIVE)
                .orderByAsc(SysUser::getCreatedAt, SysUser::getId));
        if (students.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = students.stream().map(SysUser::getId).toList();
        List<SysAuditLog> loginLogs = sysAuditLogMapper.selectList(new LambdaQueryWrapper<SysAuditLog>()
                .in(SysAuditLog::getUserId, studentIds)
                .eq(SysAuditLog::getActionCode, "LOGIN")
                .orderByDesc(SysAuditLog::getCreatedAt));
        Map<Long, LocalDateTime> latestLoginMap = new HashMap<>();
        for (SysAuditLog log : loginLogs) {
            if (log.getUserId() == null || log.getCreatedAt() == null) {
                continue;
            }
            latestLoginMap.merge(log.getUserId(), log.getCreatedAt(),
                    (left, right) -> left.isAfter(right) ? left : right);
        }

        return students.stream()
                .filter(user -> {
                    LocalDateTime latestLoginAt = latestLoginMap.get(user.getId());
                    if (latestLoginAt != null) {
                        return latestLoginAt.isBefore(cutoff);
                    }
                    return user.getCreatedAt() != null && user.getCreatedAt().isBefore(cutoff);
                })
                .sorted(Comparator.comparing(SysUser::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SysUser::getId))
                .toList();
    }

    private AdminAiTaskItem buildUserStatusItem(SysUser user, String targetStatus) {
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_USER);
        item.setTargetId(user.getId());
        item.setTargetLabel(buildUserLabel(user));
        item.setOperationType(AdminAiTaskConstants.OP_UPDATE);
        item.setFieldName(FIELD_STATUS);
        item.setOldValue(user.getStatus());
        item.setNewValue(targetStatus);
        return item;
    }

    private String buildUserLabel(SysUser user) {
        String displayName = firstText(user.getDisplayName(), user.getRealName(), user.getAccount());
        return user.getAccount() + " / " + displayName;
    }

    private String buildResourceLabel(MentalResource resource) {
        return "#" + resource.getId() + " / " + resource.getTitle();
    }

    private AdminAiTaskItem buildCreateItem(String targetLabel, String fieldName, String newValue) {
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_USER);
        item.setTargetLabel(targetLabel);
        item.setOperationType(AdminAiTaskConstants.OP_CREATE);
        item.setFieldName(fieldName);
        item.setOldValue(null);
        item.setNewValue(newValue);
        return item;
    }

    private String getRequiredItemValue(List<AdminAiTaskItem> items, String fieldName) {
        String value = getOptionalItemValue(items, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("任务明细缺少必要字段：" + fieldName);
        }
        return value;
    }

    private String getOptionalItemValue(List<AdminAiTaskItem> items, String fieldName) {
        return items.stream()
                .filter(item -> fieldName.equals(item.getFieldName()))
                .map(AdminAiTaskItem::getNewValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String normalizeTaskType(String taskType) {
        String normalized = normalizeUpper(taskType);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.contains("ACCOUNT")) {
            return AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS;
        }
        if (normalized.contains("COUNSELOR")) {
            return AdminAiTaskConstants.TASK_TYPE_COUNSELOR_CREATE;
        }
        if (normalized.contains("RESOURCE")) {
            return AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS;
        }
        return normalized;
    }

    private String resolveAccountTargetStatus(AdminOpsAiAction action, String summaryText, String instruction) {
        String targetStatus = normalizeUserStatus(action.newValue());
        if (targetStatus != null) {
            return targetStatus;
        }
        targetStatus = normalizeUserStatus(firstText(action.operationType(), action.actionType()));
        if (targetStatus != null) {
            return targetStatus;
        }
        String mergedText = (firstText(summaryText, instruction) == null ? "" : firstText(summaryText, instruction)).toLowerCase(Locale.ROOT);
        if (mergedText.contains("disable") || mergedText.contains("suspend") || mergedText.contains("禁用") || mergedText.contains("停用")) {
            return UserStatusConstants.DISABLED;
        }
        if (mergedText.contains("enable") || mergedText.contains("restore") || mergedText.contains("启用") || mergedText.contains("恢复")) {
            return UserStatusConstants.ACTIVE;
        }
        return null;
    }

    private boolean isReadyParseStatus(String parseStatus) {
        String normalized = normalizeUpper(parseStatus);
        return AdminAiTaskConstants.PARSE_READY.equals(normalized)
                || "SUCCESS".equals(normalized)
                || "EXECUTABLE".equals(normalized)
                || "PARSED".equals(normalized);
    }

    private String normalizeUserStatus(String value) {
        String normalized = normalizeUpper(value);
        if ("ENABLE".equals(normalized) || "ENABLED".equals(normalized) || "ACTIVE".equals(normalized)) {
            return UserStatusConstants.ACTIVE;
        }
        if ("DISABLE".equals(normalized) || "DISABLED".equals(normalized) || "INACTIVE".equals(normalized)) {
            return UserStatusConstants.DISABLED;
        }
        return null;
    }

    private String normalizeResourceStatus(String newValue, String operationType) {
        String normalizedValue = normalizeUpper(newValue);
        if (ResourceConstants.RESOURCE_PUBLISHED.equals(normalizedValue) || "ONLINE".equals(normalizedValue)) {
            return ResourceConstants.RESOURCE_PUBLISHED;
        }
        if (ResourceConstants.RESOURCE_OFFLINE.equals(normalizedValue) || "UNPUBLISHED".equals(normalizedValue)) {
            return ResourceConstants.RESOURCE_OFFLINE;
        }

        String normalizedOp = normalizeUpper(operationType);
        if (AdminAiTaskConstants.OP_PUBLISH.equals(normalizedOp) || "PUBLISH".equals(normalizedOp)) {
            return ResourceConstants.RESOURCE_PUBLISHED;
        }
        if (AdminAiTaskConstants.OP_OFFLINE.equals(normalizedOp) || "OFFLINE".equals(normalizedOp)) {
            return ResourceConstants.RESOURCE_OFFLINE;
        }
        if (normalizedOp != null && normalizedOp.contains("OFFLINE")) {
            return ResourceConstants.RESOURCE_OFFLINE;
        }
        if (normalizedOp != null && (normalizedOp.contains("PUBLISH") || normalizedOp.contains("ONLINE"))) {
            return ResourceConstants.RESOURCE_PUBLISHED;
        }
        return null;
    }

    private String normalizeUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean existsCounselorNo(String counselorNo) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getCounselorNo, counselorNo)
                .last("limit 1")) > 0;
    }

    private String generateCounselorAccount(String counselorNo) {
        String sanitized = counselorNo.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (!StringUtils.hasText(sanitized)) {
            throw new BusinessException("无法根据工号生成账号，请在指令中明确提供账号");
        }
        return "c_" + sanitized;
    }

    private SysUser findUserByAccount(String account) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account)
                .last("limit 1"));
    }

    private SysUser findMentionedUser(String instruction) {
        return sysUserMapper.selectList(null).stream()
                .filter(user -> instruction.contains(user.getAccount()))
                .findFirst()
                .orElse(null);
    }

    private MentalResource findResource(Long resourceId, String resourceTitle, String instruction) {
        if (resourceId != null) {
            MentalResource resource = mentalResourceMapper.selectById(resourceId);
            if (resource != null) {
                return resource;
            }
        }
        if (StringUtils.hasText(resourceTitle)) {
            MentalResource resource = mentalResourceMapper.selectOne(new LambdaQueryWrapper<MentalResource>()
                    .eq(MentalResource::getTitle, resourceTitle.trim())
                    .last("limit 1"));
            if (resource != null) {
                return resource;
            }
        }
        return findResourceByInstruction(instruction);
    }

    private MentalResource findResourceByInstruction(String instruction) {
        Matcher idMatcher = RESOURCE_ID_PATTERN.matcher(instruction);
        if (idMatcher.find()) {
            return mentalResourceMapper.selectById(Long.parseLong(idMatcher.group(1)));
        }
        Matcher titleMatcher = QUOTED_VALUE_PATTERN.matcher(instruction);
        if (titleMatcher.find()) {
            return mentalResourceMapper.selectOne(new LambdaQueryWrapper<MentalResource>()
                    .eq(MentalResource::getTitle, titleMatcher.group(1).trim())
                    .last("limit 1"));
        }
        return null;
    }

    private String extractFieldValue(String instruction, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile(String.format(FIELD_VALUE_TEMPLATE, Pattern.quote(label))).matcher(instruction);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private boolean containsAny(String text, String... fragments) {
        String lowered = text.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (lowered.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private AdminAiTaskResponse buildTaskResponse(AdminAiTask task, List<AdminAiTaskItemResponse> items) {
        return AdminAiTaskResponse.builder()
                .taskId(task.getId())
                .adminUserId(task.getAdminUserId())
                .instructionText(task.getInstructionText())
                .taskType(task.getTaskType())
                .parseStatus(task.getParseStatus())
                .confirmStatus(task.getConfirmStatus())
                .executeStatus(task.getExecuteStatus())
                .summaryText(task.getSummaryText())
                .failureReason(task.getFailureReason())
                .createdAt(task.getCreatedAt())
                .confirmedAt(task.getConfirmedAt())
                .executedAt(task.getExecutedAt())
                .items(items)
                .build();
    }

    private AdminAiTaskSummaryResponse buildTaskSummaryResponse(AdminAiTask task) {
        return AdminAiTaskSummaryResponse.builder()
                .taskId(task.getId())
                .instructionText(task.getInstructionText())
                .taskType(task.getTaskType())
                .parseStatus(task.getParseStatus())
                .confirmStatus(task.getConfirmStatus())
                .executeStatus(task.getExecuteStatus())
                .summaryText(task.getSummaryText())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private AdminAiTaskItemResponse buildTaskItemResponse(AdminAiTaskItem item) {
        return AdminAiTaskItemResponse.builder()
                .itemId(item.getId())
                .targetType(item.getTargetType())
                .targetId(item.getTargetId())
                .targetLabel(item.getTargetLabel())
                .operationType(item.getOperationType())
                .fieldName(item.getFieldName())
                .oldValue(item.getOldValue())
                .newValue(item.getNewValue())
                .sortNo(item.getSortNo())
                .executeStatus(item.getExecuteStatus())
                .build();
    }

    private AdminAiTask getRequiredTask(Long taskId) {
        AdminAiTask task = adminAiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("目标用户不存在");
        }
        return user;
    }

    private MentalResource getRequiredResource(Long resourceId) {
        MentalResource resource = mentalResourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("目标资源不存在");
        }
        return resource;
    }

    private static final class ParsedTask {

        private final String taskType;
        private final String parseStatus;
        private final String summaryText;
        private final String failureReason;
        private final List<AdminAiTaskItem> items;

        private ParsedTask(String taskType,
                           String parseStatus,
                           String summaryText,
                           String failureReason,
                           List<AdminAiTaskItem> items) {
            this.taskType = taskType;
            this.parseStatus = parseStatus;
            this.summaryText = summaryText;
            this.failureReason = failureReason;
            this.items = items;
        }

        private static ParsedTask ready(String taskType, String summaryText, List<AdminAiTaskItem> items) {
            return new ParsedTask(taskType, AdminAiTaskConstants.PARSE_READY, summaryText, null, items);
        }

        private static ParsedTask needMoreInfo(String failureReason) {
            return new ParsedTask(null, AdminAiTaskConstants.PARSE_NEED_MORE_INFO, null, failureReason, List.of());
        }
    }
}
