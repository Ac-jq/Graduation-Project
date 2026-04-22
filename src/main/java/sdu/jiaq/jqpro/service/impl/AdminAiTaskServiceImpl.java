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
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
import sdu.jiaq.jqpro.dto.adminai.ConfirmAdminAiTaskRequest;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskRequest;
import sdu.jiaq.jqpro.dto.adminai.ParseAdminAiTaskResponse;
import sdu.jiaq.jqpro.entity.AdminAiTask;
import sdu.jiaq.jqpro.entity.AdminAiTaskItem;
import sdu.jiaq.jqpro.entity.AiChatSession;
import sdu.jiaq.jqpro.entity.ConsultAppointment;
import sdu.jiaq.jqpro.entity.ConsultAppointmentSlot;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.entity.CounselorStudent;
import sdu.jiaq.jqpro.entity.MentalResource;
import sdu.jiaq.jqpro.entity.MentalScaleReport;
import sdu.jiaq.jqpro.entity.MentalScaleSession;
import sdu.jiaq.jqpro.entity.ResourceFavorite;
import sdu.jiaq.jqpro.entity.ResourceViewLog;
import sdu.jiaq.jqpro.entity.SiteNotification;
import sdu.jiaq.jqpro.entity.StudentProfile;
import sdu.jiaq.jqpro.entity.SysAuditLog;
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AdminAiTaskItemMapper;
import sdu.jiaq.jqpro.mapper.AdminAiTaskMapper;
import sdu.jiaq.jqpro.mapper.AiChatSessionMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentMapper;
import sdu.jiaq.jqpro.mapper.ConsultAppointmentSlotMapper;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.mapper.CounselorStudentMapper;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleReportMapper;
import sdu.jiaq.jqpro.mapper.MentalScaleSessionMapper;
import sdu.jiaq.jqpro.mapper.ResourceFavoriteMapper;
import sdu.jiaq.jqpro.mapper.ResourceViewLogMapper;
import sdu.jiaq.jqpro.mapper.SiteNotificationMapper;
import sdu.jiaq.jqpro.mapper.StudentProfileMapper;
import sdu.jiaq.jqpro.mapper.SysAuditLogMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AdminAiTaskService;
import sdu.jiaq.jqpro.service.AuditLogService;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiAction;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiClient;
import sdu.jiaq.jqpro.service.ai.AdminOpsAiPlan;

@Slf4j
@Service
public class AdminAiTaskServiceImpl implements AdminAiTaskService {

    private static final String FIELD_ACCOUNT = "account";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_REAL_NAME = "realName";
    private static final String FIELD_STUDENT_NO = "studentNo";
    private static final String FIELD_COUNSELOR_NO = "counselorNo";
    private static final String FIELD_ROLE_CODE = "roleCode";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_SNAPSHOT = "snapshot";
    private static final String DEFAULT_PASSWORD = "Jqpro@123";
    private static final int MAX_QUERY_ROWS = 20;

    private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d+)\\s*(?:个月|月|month|months)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("(?:account|账号|帐号)\\s*[:：=]?\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STUDENT_NO_PATTERN = Pattern.compile("(?:studentNo|student no|学号)\\s*[:：=]?\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNSELOR_NO_PATTERN = Pattern.compile("(?:counselorNo|counselor no|工号)\\s*[:：=]?\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("(?:displayName|display name|显示名|名字|名为|叫|named)\\s*[:：=]?\\s*[\"“”']?([\\p{L}\\p{N}_\\-·\\s]{2,40})[\"“”']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern REAL_NAME_PATTERN = Pattern.compile("(?:realName|real name|真实姓名|姓名)\\s*[:：=]?\\s*[\"“”']?([\\p{L}\\p{N}_\\-·\\s]{2,40})[\"“”']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("(?:resource|资源|id)\\s*[:：=]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("[\"“”']([^\"“”']+)[\"“”']");
    private static final Pattern GRADE_PATTERN = Pattern.compile("(?:grade|\\u5e74\\u7ea7)\\s*[:：]?\\s*(20\\d{2}|\\d{2})|(?:^|\\D)(20\\d{2}|\\d{2})\\s*\\u7ea7", Pattern.CASE_INSENSITIVE);

    private final AdminAiTaskMapper adminAiTaskMapper;
    private final AdminAiTaskItemMapper adminAiTaskItemMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final MentalScaleSessionMapper mentalScaleSessionMapper;
    private final MentalScaleReportMapper mentalScaleReportMapper;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final ConsultAppointmentMapper consultAppointmentMapper;
    private final ConsultAppointmentSlotMapper consultAppointmentSlotMapper;
    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final SiteNotificationMapper siteNotificationMapper;
    private final ResourceFavoriteMapper resourceFavoriteMapper;
    private final ResourceViewLogMapper resourceViewLogMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final SysAuditLogMapper sysAuditLogMapper;
    private final AuditLogService auditLogService;
    private final AdminOpsAiClient adminOpsAiClient;

    public AdminAiTaskServiceImpl(AdminAiTaskMapper adminAiTaskMapper, AdminAiTaskItemMapper adminAiTaskItemMapper,
                                  SysUserMapper sysUserMapper, StudentProfileMapper studentProfileMapper,
                                  CounselorStudentMapper counselorStudentMapper, MentalScaleSessionMapper mentalScaleSessionMapper,
                                  MentalScaleReportMapper mentalScaleReportMapper, AiChatSessionMapper aiChatSessionMapper,
                                  ConsultAppointmentMapper consultAppointmentMapper, ConsultAppointmentSlotMapper consultAppointmentSlotMapper,
                                  ConsultChatSessionMapper consultChatSessionMapper, SiteNotificationMapper siteNotificationMapper,
                                  ResourceFavoriteMapper resourceFavoriteMapper, ResourceViewLogMapper resourceViewLogMapper,
                                  MentalResourceMapper mentalResourceMapper, SysAuditLogMapper sysAuditLogMapper,
                                  AuditLogService auditLogService, AdminOpsAiClient adminOpsAiClient) {
        this.adminAiTaskMapper = adminAiTaskMapper;
        this.adminAiTaskItemMapper = adminAiTaskItemMapper;
        this.sysUserMapper = sysUserMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.counselorStudentMapper = counselorStudentMapper;
        this.mentalScaleSessionMapper = mentalScaleSessionMapper;
        this.mentalScaleReportMapper = mentalScaleReportMapper;
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.consultAppointmentMapper = consultAppointmentMapper;
        this.consultAppointmentSlotMapper = consultAppointmentSlotMapper;
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.siteNotificationMapper = siteNotificationMapper;
        this.resourceFavoriteMapper = resourceFavoriteMapper;
        this.resourceViewLogMapper = resourceViewLogMapper;
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
                "Parsed admin AI instruction: " + instruction, null);
        return ParseAdminAiTaskResponse.builder()
                .ready(AdminAiTaskConstants.PARSE_READY.equals(parsedTask.parseStatus))
                .message(AdminAiTaskConstants.PARSE_READY.equals(parsedTask.parseStatus) ? "已生成待确认执行计划" : parsedTask.failureReason)
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
    public AdminAiTaskResponse confirm(Long taskId, ConfirmAdminAiTaskRequest request) {
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
            throw new BusinessException("当前任务没有待执行明细");
        }
        Set<Long> selectedItemIds = resolveSelectedItemIds(request);
        List<AdminAiTaskItem> executableItems = selectedItemIds.isEmpty()
                ? items
                : items.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .toList();
        if (executableItems.isEmpty()) {
            throw new BusinessException("请选择至少一条要执行的任务明细");
        }
        validateSelectableCreateGroup(items, executableItems, selectedItemIds);
        validateItemsBeforeExecution(executableItems);

        Set<String> handledGroups = new HashSet<>();
        for (AdminAiTaskItem item : executableItems) {
            String op = normalizeOperation(item.getOperationType());
            if (AdminAiTaskConstants.OP_CREATE.equals(op)) {
                String key = "CREATE:" + firstText(item.getTargetLabel(), "TASK-" + taskId);
                if (handledGroups.add(key)) {
                    executeCreateGroup(executableItems, item.getTargetLabel(), adminUserId);
                }
            } else if (AdminAiTaskConstants.OP_DELETE.equals(op)) {
                String key = "DELETE:" + item.getTargetId();
                if (handledGroups.add(key)) {
                    executeDelete(item, adminUserId);
                }
            } else if (AdminAiTaskConstants.OP_QUERY.equals(op)) {
                auditLogService.record(adminUserId, "ADMIN_AI_QUERY_CONFIRM", "Admin AI query confirm",
                        "Confirmed query result for user #" + item.getTargetId(), null);
            } else {
                executeItem(item, adminUserId);
            }
            item.setExecuteStatus(AdminAiTaskConstants.EXECUTE_EXECUTED);
            adminAiTaskItemMapper.updateById(item);
        }
        if (!selectedItemIds.isEmpty()) {
            markUnselectedItemsCanceled(items, selectedItemIds);
        }

        LocalDateTime now = LocalDateTime.now();
        task.setConfirmStatus(AdminAiTaskConstants.CONFIRM_CONFIRMED);
        task.setExecuteStatus(AdminAiTaskConstants.EXECUTE_EXECUTED);
        task.setConfirmedAt(now);
        task.setExecutedAt(now);
        adminAiTaskMapper.updateById(task);
        auditLogService.record(adminUserId, "ADMIN_AI_CONFIRM", "Admin AI confirm task", "Confirmed task #" + taskId, null);
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
        auditLogService.record(adminUserId, "ADMIN_AI_CANCEL", "Admin AI cancel task", "Canceled task #" + taskId, null);
        return getTask(taskId);
    }

    private Set<Long> resolveSelectedItemIds(ConfirmAdminAiTaskRequest request) {
        if (request == null || request.getSelectedItemIds() == null || request.getSelectedItemIds().isEmpty()) {
            return Set.of();
        }
        return request.getSelectedItemIds().stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void validateSelectableCreateGroup(List<AdminAiTaskItem> allItems,
                                               List<AdminAiTaskItem> executableItems,
                                               Set<Long> selectedItemIds) {
        if (selectedItemIds.isEmpty()) {
            return;
        }
        boolean selectedCreate = executableItems.stream()
                .map(AdminAiTaskItem::getOperationType)
                .map(this::normalizeOperation)
                .anyMatch(AdminAiTaskConstants.OP_CREATE::equals);
        if (!selectedCreate) {
            return;
        }
        Set<String> selectedGroups = executableItems.stream()
                .filter(item -> AdminAiTaskConstants.OP_CREATE.equals(normalizeOperation(item.getOperationType())))
                .map(AdminAiTaskItem::getTargetLabel)
                .collect(java.util.stream.Collectors.toSet());
        boolean missingCreateField = allItems.stream()
                .filter(item -> AdminAiTaskConstants.OP_CREATE.equals(normalizeOperation(item.getOperationType())))
                .filter(item -> selectedGroups.contains(item.getTargetLabel()))
                .anyMatch(item -> !selectedItemIds.contains(item.getId()));
        if (missingCreateField) {
            throw new BusinessException("创建账号任务必须完整勾选同一账号的所有字段");
        }
    }

    private void markUnselectedItemsCanceled(List<AdminAiTaskItem> allItems, Set<Long> selectedItemIds) {
        for (AdminAiTaskItem item : allItems) {
            if (selectedItemIds.contains(item.getId())) {
                continue;
            }
            item.setExecuteStatus(AdminAiTaskConstants.EXECUTE_CANCELED);
            adminAiTaskItemMapper.updateById(item);
        }
    }

    private AdminAiTaskResponse buildTaskResponse(AdminAiTask task, List<AdminAiTaskItemResponse> items) {
        return AdminAiTaskResponse.builder()
                .taskId(task.getId()).adminUserId(task.getAdminUserId()).instructionText(task.getInstructionText())
                .taskType(task.getTaskType()).parseStatus(task.getParseStatus()).confirmStatus(task.getConfirmStatus())
                .executeStatus(task.getExecuteStatus()).summaryText(task.getSummaryText()).failureReason(task.getFailureReason())
                .createdAt(task.getCreatedAt()).confirmedAt(task.getConfirmedAt()).executedAt(task.getExecutedAt()).items(items).build();
    }

    private AdminAiTaskSummaryResponse buildTaskSummaryResponse(AdminAiTask task) {
        return AdminAiTaskSummaryResponse.builder()
                .taskId(task.getId()).instructionText(task.getInstructionText()).taskType(task.getTaskType())
                .parseStatus(task.getParseStatus()).confirmStatus(task.getConfirmStatus()).executeStatus(task.getExecuteStatus())
                .summaryText(task.getSummaryText()).createdAt(task.getCreatedAt()).build();
    }

    private AdminAiTaskItemResponse buildTaskItemResponse(AdminAiTaskItem item) {
        return AdminAiTaskItemResponse.builder()
                .itemId(item.getId()).targetType(item.getTargetType()).targetId(item.getTargetId()).targetLabel(item.getTargetLabel())
                .operationType(item.getOperationType()).fieldName(item.getFieldName()).oldValue(item.getOldValue())
                .newValue(item.getNewValue()).sortNo(item.getSortNo()).executeStatus(item.getExecuteStatus()).build();
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

    private ParsedTask parseInstruction(String instruction) {
        ParsedTask explicitIdentifierTask = parseExplicitIdentifierQuery(instruction);
        if (explicitIdentifierTask != null) {
            return explicitIdentifierTask;
        }
        if (adminOpsAiClient.isEnabled()) {
            try {
                ParsedTask task = parseByAi(instruction);
                if (task != null) {
                    if (AdminAiTaskConstants.PARSE_READY.equals(task.parseStatus) && shouldPreferRuleParser(instruction)) {
                        ParsedTask ruleTask = parseByRules(instruction);
                        if (AdminAiTaskConstants.PARSE_READY.equals(ruleTask.parseStatus)) {
                            return ruleTask;
                        }
                    }
                    if (AdminAiTaskConstants.PARSE_NEED_MORE_INFO.equals(task.parseStatus)) {
                        ParsedTask ruleTask = parseByRules(instruction);
                        if (AdminAiTaskConstants.PARSE_READY.equals(ruleTask.parseStatus)) {
                            return ruleTask;
                        }
                    }
                    return task;
                }
            } catch (BusinessException exception) {
                log.warn("Admin AI parse fallback: {}", exception.getMessage());
            } catch (Exception exception) {
                log.warn("Admin AI parse fallback", exception);
            }
        }
        return parseByRules(instruction);
    }

    private ParsedTask parseExplicitIdentifierQuery(String instruction) {
        String lowered = instruction.toLowerCase(Locale.ROOT);
        if (containsAny(lowered, "create", "add", "new", "update", "change", "modify", "set", "delete", "remove",
                "新增", "创建", "修改", "更改", "删除", "移除", "禁用", "启用")) {
            return null;
        }
        SysUser user = null;
        String account = extractAccount(instruction);
        if (StringUtils.hasText(account)) {
            user = findUserByAccount(account);
        }
        if (user == null && StringUtils.hasText(extractStudentNo(instruction))) {
            String studentNo = extractStudentNo(instruction);
            user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentNo, studentNo).last("limit 1"));
        }
        if (user == null && StringUtils.hasText(extractCounselorNo(instruction))) {
            String counselorNo = extractCounselorNo(instruction);
            user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getCounselorNo, counselorNo).last("limit 1"));
        }
        if (user == null) {
            return null;
        }
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_USER_CRUD,
                "待确认查询结果，共匹配 1 名用户", List.of(buildQueryItem(user)));
    }

    private boolean shouldPreferRuleParser(String instruction) {
        String lowered = instruction.toLowerCase(Locale.ROOT);
        return StringUtils.hasText(extractAccount(instruction))
                || StringUtils.hasText(extractStudentNo(instruction))
                || StringUtils.hasText(extractCounselorNo(instruction))
                || (containsAny(lowered, "delete", "remove", "删除", "移除") && StringUtils.hasText(extractGrade(instruction)));
    }

    private ParsedTask parseByAi(String instruction) {
        AdminOpsAiPlan plan = adminOpsAiClient.parseInstruction(instruction);
        if (plan == null || plan.actions() == null || plan.actions().isEmpty()) {
            return null;
        }
        if (!isReadyParseStatus(plan.parseStatus())) {
            return ParsedTask.needMoreInfo(firstText(plan.failureReason(), "AI 未能形成可执行计划"));
        }
        boolean isResourceTask = plan.actions().stream()
                .map(AdminOpsAiAction::targetType)
                .map(this::normalizeUpper)
                .anyMatch(AdminAiTaskConstants.TARGET_RESOURCE::equals);
        if (isResourceTask || AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS.equals(normalizeTaskType(plan.taskType()))) {
            return parseResourceActions(plan.actions(), plan.summaryText(), instruction);
        }
        return parseUserActions(plan.actions(), plan.summaryText(), instruction);
    }

    private ParsedTask parseByRules(String instruction) {
        String lowered = instruction.toLowerCase(Locale.ROOT);
        if (containsAny(lowered, "publish resource", "offline resource", "take resource offline", "上架资源", "下架资源", "上架文章", "下架文章")) {
            return parseResourceByRules(instruction);
        }
        if (containsAny(lowered, "inactive", "未登录") && containsAny(lowered, "student", "学生") && containsAny(lowered, "disable", "禁用", "停用")) {
            return parseInactiveStudentTask(instruction);
        }
        if (containsAny(lowered, "create", "add", "new", "新建", "新增", "创建")) {
            return parseCreateUserByRules(instruction);
        }
        if (containsAny(lowered, "delete", "remove", "删除", "移除")) {
            if (containsAny(lowered, "student", "学生") && StringUtils.hasText(extractGrade(instruction))) {
                return parseDeleteStudentsByGrade(instruction);
            }
            return parseDeleteUserByRules(instruction);
        }
        if (containsAny(lowered, "query", "find", "list", "show", "查询", "查看", "列出")) {
            return parseQueryUserByRules(instruction);
        }
        if (containsAny(lowered, "update", "change", "modify", "set", "修改", "更改", "改成", "启用", "禁用", "停用", "恢复")) {
            return parseUpdateUserByRules(instruction);
        }
        return ParsedTask.needMoreInfo("当前仅支持学生/老师账号的增删查改、批量禁用未登录学生，以及资源上架下架");
    }

    private ParsedTask parseUserActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        for (AdminOpsAiAction action : actions) {
            if (action.inactiveMonths() != null && action.inactiveMonths() > 0) {
                return parseInactiveStudentTask(action.inactiveMonths(), firstText(summaryText, instruction));
            }
        }
        String operationType = actions.stream()
                .map(AdminOpsAiAction::operationType).map(this::normalizeOperation).filter(StringUtils::hasText).findFirst().orElse(null);
        if (!StringUtils.hasText(operationType)) {
            return ParsedTask.needMoreInfo("AI 未能识别用户操作类型");
        }
        return switch (operationType) {
            case AdminAiTaskConstants.OP_CREATE -> parseCreateUserFromActions(actions, summaryText, instruction);
            case AdminAiTaskConstants.OP_QUERY -> parseQueryUserFromActions(actions, summaryText, instruction);
            case AdminAiTaskConstants.OP_DELETE -> parseDeleteUserFromActions(actions, summaryText, instruction);
            default -> parseUpdateUserFromActions(actions, summaryText, instruction);
        };
    }

    private ParsedTask parseCreateUserByRules(String instruction) {
        UserMutationDraft draft = new UserMutationDraft();
        draft.fillMissingFromInstruction(instruction);
        return parseCreateUserFromActions(List.of(draft.toCreateAction()), null, instruction);
    }

    private ParsedTask parseQueryUserByRules(String instruction) {
        UserFilter filter = new UserFilter();
        filter.fillMissingFromInstruction(instruction);
        return parseQueryUserFromActions(List.of(filter.toAction(AdminAiTaskConstants.OP_QUERY, FIELD_SNAPSHOT, null)), null, instruction);
    }

    private ParsedTask parseDeleteUserByRules(String instruction) {
        UserFilter filter = new UserFilter();
        filter.fillMissingFromInstruction(instruction);
        return parseDeleteUserFromActions(List.of(filter.toAction(AdminAiTaskConstants.OP_DELETE, FIELD_SNAPSHOT, "DELETE")), null, instruction);
    }

    private ParsedTask parseUpdateUserByRules(String instruction) {
        UserFilter filter = new UserFilter();
        filter.fillMissingFromInstruction(instruction);
        Map<String, String> fieldUpdates = new LinkedHashMap<>();
        inferFieldUpdatesFromInstruction(instruction, fieldUpdates);
        List<AdminOpsAiAction> actions = new ArrayList<>();
        if (fieldUpdates.isEmpty()) {
            actions.add(filter.toAction(AdminAiTaskConstants.OP_UPDATE, FIELD_STATUS, normalizeUserStatus(extractStatus(instruction))));
        } else {
            for (Map.Entry<String, String> entry : fieldUpdates.entrySet()) {
                actions.add(filter.toAction(AdminAiTaskConstants.OP_UPDATE, entry.getKey(), entry.getValue()));
            }
        }
        return parseUpdateUserFromActions(actions, null, instruction);
    }

    private ParsedTask parseInactiveStudentTask(String instruction) {
        Matcher matcher = MONTH_PATTERN.matcher(instruction);
        if (!matcher.find()) {
            return ParsedTask.needMoreInfo("请明确长期未登录的月份数，例如“三个月未登录的学生”");
        }
        return parseInactiveStudentTask(Integer.parseInt(matcher.group(1)), null);
    }

    private ParsedTask parseInactiveStudentTask(int inactiveMonths, String summaryText) {
        List<SysUser> users = findInactiveStudents(inactiveMonths);
        if (users.isEmpty()) {
            return ParsedTask.needMoreInfo("没有找到符合条件的长期未登录学生");
        }
        List<AdminAiTaskItem> items = users.stream()
                .map(user -> buildUserItem(user, AdminAiTaskConstants.OP_UPDATE, FIELD_STATUS, user.getStatus(), UserStatusConstants.DISABLED))
                .toList();
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS,
                firstText(summaryText, "待禁用 " + users.size() + " 名连续 " + inactiveMonths + " 个月未登录的学生"), items);
    }

    private ParsedTask parseDeleteStudentsByGrade(String instruction) {
        String grade = extractGrade(instruction);
        if (!StringUtils.hasText(grade)) {
            return ParsedTask.needMoreInfo("请补充要删除的学生年级，例如 2025 级学生");
        }
        List<SysUser> users = findStudentsByGrade(grade);
        if (users.isEmpty()) {
            return ParsedTask.needMoreInfo("没有找到 " + grade + " 级学生");
        }
        if (users.size() > MAX_QUERY_ROWS) {
            return ParsedTask.needMoreInfo(grade + " 级学生数量过多，请补充学院、姓名或学号缩小范围");
        }
        List<AdminAiTaskItem> items = users.stream()
                .map(user -> buildUserItem(user, AdminAiTaskConstants.OP_DELETE, FIELD_SNAPSHOT, buildUserSnapshot(user), "DELETE"))
                .toList();
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_USER_CRUD, "待复核删除 " + grade + " 级学生 " + users.size() + " 人", items);
    }

    private ParsedTask parseResourceByRules(String instruction) {
        MentalResource resource = findResource(null, null, instruction);
        if (resource == null) {
            return ParsedTask.needMoreInfo("请提供准确的资源标题或资源 ID");
        }
        String nextStatus = containsAny(instruction.toLowerCase(Locale.ROOT), "publish", "上架")
                ? ResourceConstants.RESOURCE_PUBLISHED : ResourceConstants.RESOURCE_OFFLINE;
        AdminOpsAiAction action = new AdminOpsAiAction(AdminAiTaskConstants.TARGET_RESOURCE,
                ResourceConstants.RESOURCE_PUBLISHED.equals(nextStatus) ? AdminAiTaskConstants.OP_PUBLISH : AdminAiTaskConstants.OP_OFFLINE,
                null, FIELD_STATUS, nextStatus, null, null, null, null, null, null, resource.getTitle(), resource.getId(), null, null);
        return parseResourceActions(List.of(action), null, instruction);
    }

    private ParsedTask parseCreateUserFromActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        UserMutationDraft draft = new UserMutationDraft();
        for (AdminOpsAiAction action : actions) {
            draft.merge(action);
        }
        draft.fillMissingFromInstruction(instruction);
        String roleCode = normalizeUserRole(draft.roleCode);
        if (!isSupportedUserRole(roleCode)) {
            return ParsedTask.needMoreInfo("仅支持创建学生或老师账号");
        }
        String displayName = normalizeCreateDisplayName(firstText(draft.displayName, draft.realName), draft.realName);
        String realName = firstText(draft.realName, draft.displayName);
        if (!StringUtils.hasText(displayName)) {
            return ParsedTask.needMoreInfo("请明确提供姓名");
        }
        if (RoleConstants.STUDENT.equals(roleCode) && !StringUtils.hasText(draft.studentNo)) {
            return ParsedTask.needMoreInfo("创建学生需要提供学号");
        }
        if (RoleConstants.COUNSELOR.equals(roleCode) && !StringUtils.hasText(draft.counselorNo)) {
            return ParsedTask.needMoreInfo("创建老师需要提供工号");
        }
        String generatedAccount = RoleConstants.STUDENT.equals(roleCode) ? generateStudentAccount(draft.studentNo) : generateCounselorAccount(draft.counselorNo);
        String account = firstText(normalizeCreateAccount(draft.account), generatedAccount);
        if (findUserByAccount(account) != null) {
            return ParsedTask.needMoreInfo("账号 " + account + " 已存在");
        }
        if (RoleConstants.STUDENT.equals(roleCode) && existsStudentNo(draft.studentNo)) {
            return ParsedTask.needMoreInfo("学号 " + draft.studentNo + " 已存在");
        }
        if (RoleConstants.COUNSELOR.equals(roleCode) && existsCounselorNo(draft.counselorNo)) {
            return ParsedTask.needMoreInfo("工号 " + draft.counselorNo + " 已存在");
        }

        List<AdminAiTaskItem> items = new ArrayList<>();
        items.add(buildCreateItem(account, FIELD_ACCOUNT, account));
        items.add(buildCreateItem(account, FIELD_DISPLAY_NAME, displayName));
        items.add(buildCreateItem(account, FIELD_REAL_NAME, realName));
        items.add(buildCreateItem(account, FIELD_ROLE_CODE, roleCode));
        items.add(buildCreateItem(account, FIELD_STATUS, firstText(normalizeUserStatus(draft.status), UserStatusConstants.ACTIVE)));
        if (RoleConstants.STUDENT.equals(roleCode)) {
            items.add(buildCreateItem(account, FIELD_STUDENT_NO, draft.studentNo));
        } else {
            items.add(buildCreateItem(account, FIELD_COUNSELOR_NO, draft.counselorNo));
        }
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_USER_CRUD,
                firstText(summaryText, "待创建" + (RoleConstants.STUDENT.equals(roleCode) ? "学生" : "老师") + "账号 " + account), items);
    }

    private ParsedTask parseQueryUserFromActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        UserFilter filter = new UserFilter();
        for (AdminOpsAiAction action : actions) {
            filter.merge(action);
        }
        filter.fillMissingFromInstruction(instruction);
        filter.roleCode = normalizeUserRole(filter.roleCode);
        List<SysUser> users = findUsers(filter, false);
        if (users.isEmpty()) {
            return ParsedTask.needMoreInfo("没有找到匹配的学生或老师");
        }
        if (users.size() > MAX_QUERY_ROWS) {
            return ParsedTask.needMoreInfo("匹配结果过多，请补充更精确的账号、学号、工号或姓名");
        }
        List<AdminAiTaskItem> items = users.stream().map(this::buildQueryItem).toList();
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_USER_CRUD,
                firstText(summaryText, "待确认查询结果，共匹配 " + users.size() + " 名用户"), items);
    }

    private ParsedTask parseDeleteUserFromActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        UserFilter filter = new UserFilter();
        for (AdminOpsAiAction action : actions) {
            filter.merge(action);
        }
        filter.fillMissingFromInstruction(instruction);
        filter.roleCode = normalizeUserRole(filter.roleCode);
        filter.status = normalizeUserStatus(filter.status);
        SysUser user = resolveSingleUser(filter, true);
        AdminAiTaskItem item = buildUserItem(user, AdminAiTaskConstants.OP_DELETE, FIELD_SNAPSHOT, buildUserSnapshot(user), "DELETE");
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_USER_CRUD,
                firstText(summaryText, "待删除用户 " + buildUserLabel(user)), List.of(item));
    }

    private ParsedTask parseUpdateUserFromActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        UserFilter filter = new UserFilter();
        Map<String, String> fieldUpdates = new LinkedHashMap<>();
        for (AdminOpsAiAction action : actions) {
            filter.merge(action);
            mergeFieldUpdate(fieldUpdates, action.fieldName(), action.newValue());
            mergeFieldUpdate(fieldUpdates, FIELD_STATUS, action.status());
        }
        filter.fillMissingFromInstruction(instruction);
        filter.roleCode = normalizeUserRole(filter.roleCode);
        filter.status = null;
        if (fieldUpdates.isEmpty()) {
            inferFieldUpdatesFromInstruction(instruction, fieldUpdates);
        }
        if (fieldUpdates.isEmpty()) {
            return ParsedTask.needMoreInfo("请明确要修改的字段和值");
        }
        SysUser user = resolveSingleUser(filter, true);
        List<AdminAiTaskItem> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : fieldUpdates.entrySet()) {
            String fieldName = normalizeFieldName(entry.getKey());
            if (!isMutableUserField(fieldName)) {
                continue;
            }
            String oldValue = getUserFieldValue(user, fieldName);
            String newValue = normalizeNewFieldValue(fieldName, entry.getValue());
            if (!StringUtils.hasText(newValue) || Objects.equals(oldValue, newValue)) {
                continue;
            }
            items.add(buildUserItem(user, AdminAiTaskConstants.OP_UPDATE, fieldName, oldValue, newValue));
        }
        if (items.isEmpty()) {
            return ParsedTask.needMoreInfo("没有识别出实际需要更新的字段，或新旧值一致");
        }
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_USER_CRUD,
                firstText(summaryText, "待更新用户 " + buildUserLabel(user) + " 的 " + items.size() + " 个字段"), items);
    }

    private ParsedTask parseResourceActions(List<AdminOpsAiAction> actions, String summaryText, String instruction) {
        AdminOpsAiAction action = actions.get(0);
        MentalResource resource = findResource(action.resourceId(), action.resourceTitle(), instruction);
        if (resource == null) {
            return ParsedTask.needMoreInfo("请提供准确的资源标题或资源 ID");
        }
        String nextStatus = normalizeResourceStatus(action.newValue(), firstText(action.operationType(), action.actionType(), summaryText, instruction));
        if (!StringUtils.hasText(nextStatus)) {
            return ParsedTask.needMoreInfo("未能识别资源目标状态");
        }
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_RESOURCE);
        item.setTargetId(resource.getId());
        item.setTargetLabel(buildResourceLabel(resource));
        item.setOperationType(ResourceConstants.RESOURCE_PUBLISHED.equals(nextStatus) ? AdminAiTaskConstants.OP_PUBLISH : AdminAiTaskConstants.OP_OFFLINE);
        item.setFieldName(FIELD_STATUS);
        item.setOldValue(resource.getStatus());
        item.setNewValue(nextStatus);
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS,
                firstText(summaryText, "待调整资源《" + resource.getTitle() + "》状态为 " + nextStatus), List.of(item));
    }

    private void validateItemsBeforeExecution(List<AdminAiTaskItem> items) {
        Map<String, List<AdminAiTaskItem>> createGroups = new HashMap<>();
        for (AdminAiTaskItem item : items) {
            String op = normalizeOperation(item.getOperationType());
            if (AdminAiTaskConstants.OP_CREATE.equals(op)) {
                createGroups.computeIfAbsent(firstText(item.getTargetLabel(), "UNKNOWN"), key -> new ArrayList<>()).add(item);
                continue;
            }
            if (AdminAiTaskConstants.OP_QUERY.equals(op)) {
                if (item.getTargetId() == null || sysUserMapper.selectById(item.getTargetId()) == null) {
                    throw new BusinessException("查询结果中的用户已不存在，请重新解析指令");
                }
                continue;
            }
            if (AdminAiTaskConstants.OP_DELETE.equals(op)) {
                validateDeletion(item.getTargetId());
                continue;
            }
            if (AdminAiTaskConstants.TARGET_RESOURCE.equals(item.getTargetType())) {
                MentalResource resource = getRequiredResource(item.getTargetId());
                if (!Objects.equals(resource.getStatus(), item.getOldValue())) {
                    throw new BusinessException("资源状态已变化，请重新解析后再确认");
                }
                continue;
            }
            SysUser user = getRequiredUser(item.getTargetId());
            if (!Objects.equals(getUserFieldValue(user, item.getFieldName()), item.getOldValue())) {
                throw new BusinessException("用户数据已变化，请重新解析后再确认");
            }
            validateFieldChange(item.getFieldName(), item.getNewValue(), user.getId(), user.getRoleCode());
        }
        for (List<AdminAiTaskItem> groupItems : createGroups.values()) {
            validateCreateGroup(groupItems);
        }
    }

    private void validateCreateGroup(List<AdminAiTaskItem> items) {
        String account = getRequiredItemValue(items, FIELD_ACCOUNT);
        String roleCode = normalizeUserRole(getRequiredItemValue(items, FIELD_ROLE_CODE));
        if (!isSupportedUserRole(roleCode)) {
            throw new BusinessException("仅支持创建学生或老师账号");
        }
        if (findUserByAccount(account) != null) {
            throw new BusinessException("账号已存在: " + account);
        }
        if (RoleConstants.STUDENT.equals(roleCode) && existsStudentNo(getRequiredItemValue(items, FIELD_STUDENT_NO))) {
            throw new BusinessException("学号已存在");
        }
        if (RoleConstants.COUNSELOR.equals(roleCode) && existsCounselorNo(getRequiredItemValue(items, FIELD_COUNSELOR_NO))) {
            throw new BusinessException("工号已存在");
        }
    }

    private void executeCreateGroup(List<AdminAiTaskItem> allItems, String targetLabel, Long adminUserId) {
        List<AdminAiTaskItem> groupItems = allItems.stream()
                .filter(item -> AdminAiTaskConstants.OP_CREATE.equals(normalizeOperation(item.getOperationType())))
                .filter(item -> Objects.equals(item.getTargetLabel(), targetLabel))
                .toList();
        String account = getRequiredItemValue(groupItems, FIELD_ACCOUNT);
        String displayName = getRequiredItemValue(groupItems, FIELD_DISPLAY_NAME);
        String realName = getRequiredItemValue(groupItems, FIELD_REAL_NAME);
        String roleCode = normalizeUserRole(getRequiredItemValue(groupItems, FIELD_ROLE_CODE));
        String status = firstText(normalizeUserStatus(getOptionalItemValue(groupItems, FIELD_STATUS)), UserStatusConstants.ACTIVE);
        String studentNo = getOptionalItemValue(groupItems, FIELD_STUDENT_NO);
        String counselorNo = getOptionalItemValue(groupItems, FIELD_COUNSELOR_NO);

        String salt = PasswordCryptoUtil.generateSalt();
        SysUser user = new SysUser();
        user.setAccount(account);
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordCryptoUtil.hashPassword(DEFAULT_PASSWORD, salt));
        user.setRoleCode(roleCode);
        user.setDisplayName(displayName);
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        user.setCounselorNo(counselorNo);
        user.setStatus(status);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            SysUser existingUser = findUserByAccount(account);
            if (existingUser == null) {
                throw exception;
            }
            user = existingUser;
        }

        if (RoleConstants.STUDENT.equals(roleCode)) {
            long existingProfileCount = studentProfileMapper.selectCount(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, user.getId()));
            if (existingProfileCount == 0) {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(user.getId());
                studentProfileMapper.insert(profile);
            }
        }
        for (AdminAiTaskItem item : groupItems) {
            item.setTargetId(user.getId());
            item.setTargetLabel(buildUserLabel(user));
            adminAiTaskItemMapper.updateById(item);
        }
        auditLogService.record(adminUserId, "ADMIN_AI_USER_CREATE", "Admin AI create user", "Created " + roleCode + " account " + account, null);
    }

    private void executeDelete(AdminAiTaskItem item, Long adminUserId) {
        Long userId = item.getTargetId();
        validateDeletion(userId);
        SysUser user = getRequiredUser(userId);
        if (RoleConstants.STUDENT.equals(user.getRoleCode())) {
            studentProfileMapper.delete(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
            resourceFavoriteMapper.delete(new LambdaQueryWrapper<ResourceFavorite>().eq(ResourceFavorite::getStudentUserId, userId));
            resourceViewLogMapper.delete(new LambdaQueryWrapper<ResourceViewLog>().eq(ResourceViewLog::getStudentUserId, userId));
            counselorStudentMapper.delete(new LambdaQueryWrapper<CounselorStudent>().eq(CounselorStudent::getStudentUserId, userId));
        }
        if (RoleConstants.COUNSELOR.equals(user.getRoleCode())) {
            counselorStudentMapper.delete(new LambdaQueryWrapper<CounselorStudent>().eq(CounselorStudent::getCounselorUserId, userId));
            consultAppointmentSlotMapper.delete(new LambdaQueryWrapper<ConsultAppointmentSlot>().eq(ConsultAppointmentSlot::getCounselorUserId, userId));
        }
        siteNotificationMapper.delete(new LambdaQueryWrapper<SiteNotification>().eq(SiteNotification::getReceiverUserId, userId));
        sysUserMapper.deleteById(userId);
        auditLogService.record(adminUserId, "ADMIN_AI_USER_DELETE", "Admin AI delete user", "Deleted user " + item.getTargetLabel(), null);
    }

    private void executeItem(AdminAiTaskItem item, Long adminUserId) {
        if (AdminAiTaskConstants.TARGET_RESOURCE.equals(item.getTargetType())) {
            MentalResource resource = getRequiredResource(item.getTargetId());
            resource.setStatus(item.getNewValue());
            if (ResourceConstants.RESOURCE_PUBLISHED.equals(item.getNewValue()) && resource.getPublishedAt() == null) {
                resource.setPublishedAt(LocalDateTime.now());
            }
            mentalResourceMapper.updateById(resource);
            auditLogService.record(adminUserId, "ADMIN_AI_RESOURCE_STATUS", "Admin AI update resource", "Changed resource #" + resource.getId() + " status to " + item.getNewValue(), null);
            return;
        }
        SysUser user = getRequiredUser(item.getTargetId());
        switch (normalizeFieldName(item.getFieldName())) {
            case FIELD_ACCOUNT -> user.setAccount(item.getNewValue());
            case FIELD_DISPLAY_NAME -> user.setDisplayName(item.getNewValue());
            case FIELD_REAL_NAME -> user.setRealName(item.getNewValue());
            case FIELD_STUDENT_NO -> user.setStudentNo(item.getNewValue());
            case FIELD_COUNSELOR_NO -> user.setCounselorNo(item.getNewValue());
            case FIELD_STATUS -> user.setStatus(item.getNewValue());
            default -> throw new BusinessException("不支持更新字段: " + item.getFieldName());
        }
        sysUserMapper.updateById(user);
        auditLogService.record(adminUserId, "ADMIN_AI_USER_UPDATE", "Admin AI update user",
                "Updated " + user.getAccount() + " field " + item.getFieldName(), null);
    }

    private void validateDeletion(Long userId) {
        SysUser user = getRequiredUser(userId);
        if (RoleConstants.ADMIN.equals(user.getRoleCode())) {
            throw new BusinessException("不允许通过 AI 运维删除管理员账号");
        }
        long scaleSessionCount = mentalScaleSessionMapper.selectCount(new LambdaQueryWrapper<MentalScaleSession>().eq(MentalScaleSession::getUserId, userId));
        long reportCount = mentalScaleReportMapper.selectCount(new LambdaQueryWrapper<MentalScaleReport>().eq(MentalScaleReport::getUserId, userId));
        long aiSessionCount = RoleConstants.STUDENT.equals(user.getRoleCode())
                ? aiChatSessionMapper.selectCount(new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getStudentUserId, userId)) : 0L;
        long appointmentCount = consultAppointmentMapper.selectCount(new LambdaQueryWrapper<ConsultAppointment>()
                .and(wrapper -> wrapper.eq(ConsultAppointment::getStudentUserId, userId).or().eq(ConsultAppointment::getCounselorUserId, userId)));
        long chatSessionCount = consultChatSessionMapper.selectCount(new LambdaQueryWrapper<ConsultChatSession>()
                .and(wrapper -> wrapper.eq(ConsultChatSession::getStudentUserId, userId).or().eq(ConsultChatSession::getCounselorUserId, userId)));
        if (scaleSessionCount > 0 || reportCount > 0 || aiSessionCount > 0 || appointmentCount > 0 || chatSessionCount > 0) {
            throw new BusinessException("该账号已有业务数据，不能直接删除，请改为禁用账号");
        }
    }

    private List<SysUser> findInactiveStudents(int inactiveMonths) {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(inactiveMonths);
        List<SysUser> students = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, RoleConstants.STUDENT).eq(SysUser::getStatus, UserStatusConstants.ACTIVE));
        if (students.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = students.stream().map(SysUser::getId).toList();
        List<SysAuditLog> logs = sysAuditLogMapper.selectList(new LambdaQueryWrapper<SysAuditLog>()
                .in(SysAuditLog::getUserId, userIds).eq(SysAuditLog::getActionCode, "LOGIN"));
        Map<Long, LocalDateTime> latestLoginMap = new HashMap<>();
        for (SysAuditLog log : logs) {
            if (log.getUserId() != null && log.getCreatedAt() != null) {
                latestLoginMap.merge(log.getUserId(), log.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return students.stream().filter(user -> {
            LocalDateTime latest = latestLoginMap.get(user.getId());
            return latest != null ? latest.isBefore(cutoff) : user.getCreatedAt() != null && user.getCreatedAt().isBefore(cutoff);
        }).sorted(Comparator.comparing(SysUser::getId)).toList();
    }

    private List<SysUser> findStudentsByGrade(String grade) {
        List<StudentProfile> profiles = studentProfileMapper.selectList(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getGrade, grade));
        if (profiles.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = profiles.stream()
                .map(StudentProfile::getUserId)
                .filter(Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getRoleCode, RoleConstants.STUDENT))
                .stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .toList();
    }

    private List<SysUser> findUsers(UserFilter filter, boolean exactName) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        boolean hasStrongIdentifier = StringUtils.hasText(filter.account)
                || StringUtils.hasText(filter.studentNo)
                || StringUtils.hasText(filter.counselorNo);
        if (StringUtils.hasText(filter.account)) wrapper.eq(SysUser::getAccount, filter.account);
        if (StringUtils.hasText(filter.studentNo)) wrapper.eq(SysUser::getStudentNo, filter.studentNo);
        if (StringUtils.hasText(filter.counselorNo)) wrapper.eq(SysUser::getCounselorNo, filter.counselorNo);
        if (StringUtils.hasText(filter.roleCode)) wrapper.eq(SysUser::getRoleCode, filter.roleCode);
        if (StringUtils.hasText(filter.status)) wrapper.eq(SysUser::getStatus, filter.status);
        if (!hasStrongIdentifier && StringUtils.hasText(filter.displayName)) {
            if (exactName) wrapper.eq(SysUser::getDisplayName, filter.displayName); else wrapper.like(SysUser::getDisplayName, filter.displayName);
        }
        if (!hasStrongIdentifier && StringUtils.hasText(filter.realName)) {
            if (exactName) wrapper.eq(SysUser::getRealName, filter.realName); else wrapper.like(SysUser::getRealName, filter.realName);
        }
        return sysUserMapper.selectList(wrapper).stream().filter(user -> isSupportedUserRole(user.getRoleCode())).toList();
    }

    private SysUser resolveSingleUser(UserFilter filter, boolean exactName) {
        List<SysUser> users = findUsers(filter, exactName);
        if (users.isEmpty()) throw new BusinessException("没有找到匹配的学生或老师");
        if (users.size() > 1) throw new BusinessException("匹配到多名用户，请补充更精确的账号、学号或工号");
        return users.get(0);
    }

    private AdminAiTaskItem buildCreateItem(String targetLabel, String fieldName, String newValue) {
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_USER); item.setTargetLabel(targetLabel); item.setOperationType(AdminAiTaskConstants.OP_CREATE);
        item.setFieldName(fieldName); item.setOldValue(null); item.setNewValue(newValue); return item;
    }

    private AdminAiTaskItem buildUserItem(SysUser user, String operationType, String fieldName, String oldValue, String newValue) {
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_USER); item.setTargetId(user.getId()); item.setTargetLabel(buildUserLabel(user));
        item.setOperationType(operationType); item.setFieldName(fieldName); item.setOldValue(oldValue); item.setNewValue(newValue); return item;
    }

    private AdminAiTaskItem buildQueryItem(SysUser user) { return buildUserItem(user, AdminAiTaskConstants.OP_QUERY, FIELD_SNAPSHOT, null, buildUserSnapshot(user)); }
    private String buildUserLabel(SysUser user) { return user.getAccount() + " / " + firstText(user.getDisplayName(), user.getRealName(), user.getAccount()); }
    private String buildUserSnapshot(SysUser user) { return "account=" + user.getAccount() + ", role=" + user.getRoleCode() + ", displayName=" + firstText(user.getDisplayName(), "NULL") + ", realName=" + firstText(user.getRealName(), "NULL") + ", studentNo=" + firstText(user.getStudentNo(), "NULL") + ", counselorNo=" + firstText(user.getCounselorNo(), "NULL") + ", status=" + firstText(user.getStatus(), "NULL"); }
    private String buildResourceLabel(MentalResource resource) { return "#" + resource.getId() + " / " + resource.getTitle(); }
    private SysUser findUserByAccount(String account) { return StringUtils.hasText(account) ? sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount, account).last("limit 1")) : null; }
    private boolean existsStudentNo(String studentNo) { return StringUtils.hasText(studentNo) && sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentNo, studentNo).last("limit 1")) > 0; }
    private boolean existsCounselorNo(String counselorNo) { return StringUtils.hasText(counselorNo) && sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getCounselorNo, counselorNo).last("limit 1")) > 0; }
    private String getRequiredItemValue(List<AdminAiTaskItem> items, String fieldName) { String v = getOptionalItemValue(items, fieldName); if (!StringUtils.hasText(v)) throw new BusinessException("任务明细缺少必要字段: " + fieldName); return v; }
    private String getOptionalItemValue(List<AdminAiTaskItem> items, String fieldName) { return items.stream().filter(item -> fieldName.equals(item.getFieldName())).map(AdminAiTaskItem::getNewValue).filter(StringUtils::hasText).findFirst().orElse(null); }
    private String firstText(String... values) { for (String value : values) if (StringUtils.hasText(value)) return value.trim(); return null; }
    private String normalizeText(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String normalizeUpper(String value) { String normalized = normalizeText(value); return normalized == null ? null : normalized.toUpperCase(Locale.ROOT); }
    private boolean containsAny(String text, String... fragments) { for (String f : fragments) if (text.contains(f.toLowerCase(Locale.ROOT))) return true; return false; }
    private String normalizeCreateAccount(String account) { String normalized = normalizeText(account); if (!StringUtils.hasText(normalized)) return null; String lowered = normalized.toLowerCase(Locale.ROOT); if (normalized.contains(" ") || containsAny(lowered, "named", "with", "the", "set", "for", "to", "student", "teacher", "counselor", "account", "user", "账号", "学生", "老师", "咨询师")) return null; return normalized; }
    private String normalizeCreateDisplayName(String displayName, String realName) { String normalized = normalizeText(displayName); if (!StringUtils.hasText(normalized)) return normalizeText(realName); String lowered = normalized.toLowerCase(Locale.ROOT); if (containsAny(lowered, "with studentno", "with counselorno", "studentno", "counselorno", " account", "账号", "学号", "工号")) return normalizeText(realName); return normalized; }
    private String normalizeTaskType(String taskType) { String n = normalizeUpper(taskType); if (!StringUtils.hasText(n)) return null; if (n.contains("USER")) return AdminAiTaskConstants.TASK_TYPE_USER_CRUD; if (n.contains("ACCOUNT")) return AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS; if (n.contains("COUNSELOR")) return AdminAiTaskConstants.TASK_TYPE_COUNSELOR_CREATE; if (n.contains("RESOURCE")) return AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS; return n; }
    private String normalizeOperation(String op) { String n = normalizeUpper(op); if (!StringUtils.hasText(n)) return null; if (n.contains("CREATE")) return AdminAiTaskConstants.OP_CREATE; if (n.contains("DELETE") || n.contains("REMOVE")) return AdminAiTaskConstants.OP_DELETE; if (n.contains("QUERY") || n.contains("LIST") || n.contains("SEARCH") || n.contains("FIND")) return AdminAiTaskConstants.OP_QUERY; if (n.contains("PUBLISH") || n.contains("ONLINE")) return AdminAiTaskConstants.OP_PUBLISH; if (n.contains("OFFLINE") || n.contains("UNPUBLISH")) return AdminAiTaskConstants.OP_OFFLINE; return AdminAiTaskConstants.OP_UPDATE; }
    private String normalizeFieldName(String field) { String n = normalizeText(field); if (!StringUtils.hasText(n)) return null; String l = n.toLowerCase(Locale.ROOT); if (l.contains("display") || "显示名".equals(n) || "名字".equals(n)) return FIELD_DISPLAY_NAME; if (l.contains("real") || "真实姓名".equals(n) || "姓名".equals(n)) return FIELD_REAL_NAME; if (l.contains("student") || "学号".equals(n)) return FIELD_STUDENT_NO; if (l.contains("counselor") || "工号".equals(n)) return FIELD_COUNSELOR_NO; if (l.contains("role")) return FIELD_ROLE_CODE; if (l.contains("status") || "状态".equals(n)) return FIELD_STATUS; if (l.contains("snapshot")) return FIELD_SNAPSHOT; if (l.contains("account") || "账号".equals(n) || "帐号".equals(n)) return FIELD_ACCOUNT; return n; }
    private String normalizeUserRole(String role) { String n = normalizeUpper(role); if (!StringUtils.hasText(n)) return null; if (n.contains("STUDENT") || n.contains("学生")) return RoleConstants.STUDENT; if (n.contains("COUNSELOR") || n.contains("TEACHER") || n.contains("老师") || n.contains("咨询师")) return RoleConstants.COUNSELOR; if (n.contains("ADMIN")) return RoleConstants.ADMIN; return null; }
    private boolean isSupportedUserRole(String role) { return RoleConstants.STUDENT.equals(role) || RoleConstants.COUNSELOR.equals(role); }
    private String normalizeUserStatus(String value) { String n = normalizeUpper(value); if (!StringUtils.hasText(n)) return null; if ("ENABLE".equals(n) || "ENABLED".equals(n) || "ACTIVE".equals(n) || "启用".equals(value) || "恢复".equals(value)) return UserStatusConstants.ACTIVE; if ("DISABLE".equals(n) || "DISABLED".equals(n) || "INACTIVE".equals(n) || "禁用".equals(value) || "停用".equals(value)) return UserStatusConstants.DISABLED; return null; }
    private String normalizeResourceStatus(String newValue, String operationText) { String n = normalizeUpper(newValue); if (ResourceConstants.RESOURCE_PUBLISHED.equals(n) || "ONLINE".equals(n)) return ResourceConstants.RESOURCE_PUBLISHED; if (ResourceConstants.RESOURCE_OFFLINE.equals(n) || "UNPUBLISHED".equals(n)) return ResourceConstants.RESOURCE_OFFLINE; String lowered = normalizeText(operationText) == null ? "" : operationText.toLowerCase(Locale.ROOT); if (containsAny(lowered, "publish", "online", "上架")) return ResourceConstants.RESOURCE_PUBLISHED; if (containsAny(lowered, "offline", "unpublish", "下架")) return ResourceConstants.RESOURCE_OFFLINE; return null; }
    private boolean isReadyParseStatus(String parseStatus) { String n = normalizeUpper(parseStatus); return AdminAiTaskConstants.PARSE_READY.equals(n) || "SUCCESS".equals(n) || "EXECUTABLE".equals(n) || "PARSED".equals(n); }
    private String extractStatus(String instruction) { String lowered = instruction.toLowerCase(Locale.ROOT); if (containsAny(lowered, "启用", "恢复", "enable", "activate", "restore")) return UserStatusConstants.ACTIVE; if (containsAny(lowered, "禁用", "停用", "disable", "suspend", "ban")) return UserStatusConstants.DISABLED; return null; }
    private String extractByPattern(Pattern pattern, String text) { Matcher matcher = pattern.matcher(text); return matcher.find() ? normalizeText(matcher.group(1)) : null; }
    private String extractAccount(String instruction) {
        String asciiAccount = extractByPattern(Pattern.compile("\\baccount\\s*[:：]?\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE), instruction);
        String chineseAccount = extractByPattern(Pattern.compile("(?:账号|帐号)\\s*[:：]\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE), instruction);
        return normalizeCreateAccount(firstText(asciiAccount, chineseAccount, extractByPattern(ACCOUNT_PATTERN, instruction)));
    }
    private String extractStudentNo(String instruction) {
        return firstText(extractByPattern(Pattern.compile("(?:studentNo|student no|学号)\\s*[:：]?\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE), instruction), extractByPattern(STUDENT_NO_PATTERN, instruction));
    }
    private String extractCounselorNo(String instruction) {
        return firstText(extractByPattern(Pattern.compile("(?:counselorNo|counselor no|工号)\\s*[:：]?\\s*([\\w-]+)", Pattern.CASE_INSENSITIVE), instruction), extractByPattern(COUNSELOR_NO_PATTERN, instruction));
    }
    private String extractDisplayName(String instruction) { String value = extractByPattern(DISPLAY_NAME_PATTERN, instruction); return StringUtils.hasText(value) ? value.replaceAll("\\s+", " ").trim() : null; }
    private String extractRealName(String instruction) { String value = extractByPattern(REAL_NAME_PATTERN, instruction); return StringUtils.hasText(value) ? value.replaceAll("\\s+", " ").trim() : null; }
    private String extractQuotedValue(String instruction) { Matcher matcher = QUOTED_VALUE_PATTERN.matcher(instruction); return matcher.find() ? normalizeText(matcher.group(1)) : null; }
    private String extractGrade(String instruction) {
        Pattern robustGradePattern = Pattern.compile("(?:grade|年级)\\s*[:：]?\\s*(20\\d{2}|\\d{2})|(?:^|\\D)(20\\d{2}|\\d{2})\\s*级", Pattern.CASE_INSENSITIVE);
        Matcher matcher = robustGradePattern.matcher(instruction);
        if (!matcher.find()) {
            return null;
        }
        String value = firstText(matcher.group(1), matcher.group(2));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() == 2 ? "20" + value : value;
    }
    private String resolveRoleFromInstruction(String instruction) { String lowered = normalizeText(instruction) == null ? "" : instruction.toLowerCase(Locale.ROOT); if (containsAny(lowered, "student", "学生", "学号")) return RoleConstants.STUDENT; if (containsAny(lowered, "teacher", "counselor", "老师", "咨询师", "工号")) return RoleConstants.COUNSELOR; if (containsAny(lowered, "admin", "管理员")) return RoleConstants.ADMIN; return null; }
    private String generateStudentAccount(String studentNo) { String sanitized = normalizeText(studentNo); if (!StringUtils.hasText(sanitized)) throw new BusinessException("学生学号不能为空"); return "s_" + sanitized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ""); }
    private String generateCounselorAccount(String counselorNo) { String sanitized = normalizeText(counselorNo); if (!StringUtils.hasText(sanitized)) throw new BusinessException("老师工号不能为空"); return "c_" + sanitized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ""); }
    private String getUserFieldValue(SysUser user, String fieldName) { return switch (normalizeFieldName(fieldName)) { case FIELD_ACCOUNT -> user.getAccount(); case FIELD_DISPLAY_NAME -> user.getDisplayName(); case FIELD_REAL_NAME -> user.getRealName(); case FIELD_STUDENT_NO -> user.getStudentNo(); case FIELD_COUNSELOR_NO -> user.getCounselorNo(); case FIELD_ROLE_CODE -> user.getRoleCode(); case FIELD_STATUS -> user.getStatus(); default -> null; }; }
    private boolean isMutableUserField(String fieldName) { String f = normalizeFieldName(fieldName); return FIELD_ACCOUNT.equals(f) || FIELD_DISPLAY_NAME.equals(f) || FIELD_REAL_NAME.equals(f) || FIELD_STUDENT_NO.equals(f) || FIELD_COUNSELOR_NO.equals(f) || FIELD_STATUS.equals(f); }
    private String normalizeNewFieldValue(String fieldName, String value) { return FIELD_STATUS.equals(normalizeFieldName(fieldName)) ? normalizeUserStatus(value) : normalizeText(value); }
    private void validateFieldChange(String fieldName, String newValue, Long currentUserId, String roleCode) { String f = normalizeFieldName(fieldName); String v = normalizeNewFieldValue(f, newValue); if (!StringUtils.hasText(v)) throw new BusinessException("字段 " + fieldName + " 的新值不能为空"); if (FIELD_ACCOUNT.equals(f)) { SysUser existing = findUserByAccount(v); if (existing != null && !existing.getId().equals(currentUserId)) throw new BusinessException("账号已存在: " + v); } if (FIELD_STUDENT_NO.equals(f)) { if (!RoleConstants.STUDENT.equals(roleCode)) throw new BusinessException("只有学生账号支持修改学号"); SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentNo, v).ne(SysUser::getId, currentUserId).last("limit 1")); if (existing != null) throw new BusinessException("学号已存在: " + v); } if (FIELD_COUNSELOR_NO.equals(f)) { if (!RoleConstants.COUNSELOR.equals(roleCode)) throw new BusinessException("只有老师账号支持修改工号"); SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getCounselorNo, v).ne(SysUser::getId, currentUserId).last("limit 1")); if (existing != null) throw new BusinessException("工号已存在: " + v); } if (FIELD_STATUS.equals(f) && !StringUtils.hasText(normalizeUserStatus(v))) throw new BusinessException("账号状态仅支持 ACTIVE 或 DISABLED"); }
    private MentalResource findResource(Long resourceId, String resourceTitle, String instruction) { if (resourceId != null) { MentalResource resource = mentalResourceMapper.selectById(resourceId); if (resource != null) return resource; } if (StringUtils.hasText(resourceTitle)) { MentalResource resource = mentalResourceMapper.selectOne(new LambdaQueryWrapper<MentalResource>().eq(MentalResource::getTitle, resourceTitle.trim()).last("limit 1")); if (resource != null) return resource; } Matcher idMatcher = RESOURCE_ID_PATTERN.matcher(instruction); if (idMatcher.find()) return mentalResourceMapper.selectById(Long.parseLong(idMatcher.group(1))); String quotedValue = extractQuotedValue(instruction); if (StringUtils.hasText(quotedValue)) return mentalResourceMapper.selectOne(new LambdaQueryWrapper<MentalResource>().eq(MentalResource::getTitle, quotedValue).last("limit 1")); return null; }
    private void mergeFieldUpdate(Map<String, String> updates, String fieldName, String newValue) { String f = normalizeFieldName(fieldName); if (StringUtils.hasText(f) && StringUtils.hasText(newValue)) updates.put(f, newValue.trim()); }
    private void inferFieldUpdatesFromInstruction(String instruction, Map<String, String> updates) { String lowered = instruction.toLowerCase(Locale.ROOT); if (containsAny(lowered, "禁用", "停用", "disable", "suspend", "ban")) updates.put(FIELD_STATUS, UserStatusConstants.DISABLED); if (containsAny(lowered, "启用", "恢复", "enable", "activate", "restore")) updates.put(FIELD_STATUS, UserStatusConstants.ACTIVE); if (containsAny(lowered, "改学号", "studentno", "student no")) updates.put(FIELD_STUDENT_NO, extractStudentNo(instruction)); if (containsAny(lowered, "改工号", "counselorno", "counselor no")) updates.put(FIELD_COUNSELOR_NO, extractCounselorNo(instruction)); if (containsAny(lowered, "改显示名", "改名字", "displayname", "display name")) updates.put(FIELD_DISPLAY_NAME, extractDisplayName(instruction)); if (containsAny(lowered, "改真实姓名", "改姓名", "realname", "real name")) updates.put(FIELD_REAL_NAME, extractRealName(instruction)); if (containsAny(lowered, "改账号", "账号改成", "change account", "set account")) updates.put(FIELD_ACCOUNT, extractAccount(instruction)); }

    private record ParsedTask(String taskType, String parseStatus, String summaryText, String failureReason, List<AdminAiTaskItem> items) { private static ParsedTask ready(String taskType, String summaryText, List<AdminAiTaskItem> items) { return new ParsedTask(taskType, AdminAiTaskConstants.PARSE_READY, summaryText, null, items); } private static ParsedTask needMoreInfo(String failureReason) { return new ParsedTask(null, AdminAiTaskConstants.PARSE_NEED_MORE_INFO, null, failureReason, List.of()); } }

    private class UserFilter {
        protected String account; protected String displayName; protected String realName; protected String studentNo; protected String counselorNo; protected String status; protected String roleCode;
        private void merge(AdminOpsAiAction action) { account = firstText(account, action.account()); displayName = firstText(displayName, action.displayName()); realName = firstText(realName, action.realName()); studentNo = firstText(studentNo, action.studentNo()); counselorNo = firstText(counselorNo, action.counselorNo()); status = firstText(status, action.status()); roleCode = firstText(roleCode, action.roleCode()); }
        private void fillMissingFromInstruction(String instruction) { account = firstText(account, extractAccount(instruction)); displayName = firstText(displayName, extractDisplayName(instruction)); realName = firstText(realName, extractRealName(instruction)); studentNo = firstText(studentNo, extractStudentNo(instruction)); counselorNo = firstText(counselorNo, extractCounselorNo(instruction)); status = firstText(status, extractStatus(instruction)); roleCode = firstText(roleCode, resolveRoleFromInstruction(instruction)); }
        private AdminOpsAiAction toAction(String operationType, String fieldName, String newValue) { return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, operationType, null, fieldName, newValue, account, displayName, realName, studentNo, counselorNo, status, null, null, null, roleCode); }
    }

    private final class UserMutationDraft extends UserFilter {
        private void fillMissingFromInstruction(String instruction) { super.fillMissingFromInstruction(instruction); }
        private void merge(AdminOpsAiAction action) { super.merge(action); if (StringUtils.hasText(action.fieldName()) && StringUtils.hasText(action.newValue())) { switch (normalizeFieldName(action.fieldName())) { case FIELD_ACCOUNT -> account = action.newValue().trim(); case FIELD_DISPLAY_NAME -> displayName = action.newValue().trim(); case FIELD_REAL_NAME -> realName = action.newValue().trim(); case FIELD_STUDENT_NO -> studentNo = action.newValue().trim(); case FIELD_COUNSELOR_NO -> counselorNo = action.newValue().trim(); case FIELD_STATUS -> status = action.newValue().trim(); case FIELD_ROLE_CODE -> roleCode = action.newValue().trim(); default -> { } } } }
        private AdminOpsAiAction toCreateAction() { return new AdminOpsAiAction(AdminAiTaskConstants.TARGET_USER, AdminAiTaskConstants.OP_CREATE, null, null, null, account, displayName, realName, studentNo, counselorNo, status, null, null, null, roleCode); }
    }
}
