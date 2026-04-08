package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import sdu.jiaq.jqpro.entity.SysUser;
import sdu.jiaq.jqpro.mapper.AdminAiTaskItemMapper;
import sdu.jiaq.jqpro.mapper.AdminAiTaskMapper;
import sdu.jiaq.jqpro.mapper.MentalResourceMapper;
import sdu.jiaq.jqpro.mapper.SysUserMapper;
import sdu.jiaq.jqpro.service.AdminAiTaskService;
import sdu.jiaq.jqpro.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin AI task service implementation.
 */
@Service
public class AdminAiTaskServiceImpl implements AdminAiTaskService {

    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("(?:resource|\\u8d44\\u6e90|ID|id)\\s*[:\\uFF1A#-]?\\s*(\\d+)");
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("(?:\"|\\u201c|\\u2018|\\u300a)(.*?)(?:\"|\\u201d|\\u2019|\\u300b)");
    private static final String FIELD_VALUE_TEMPLATE = "(?i)%s\\s*[:\\uFF1A=]\\s*([\\p{L}\\p{N}_-]+)";

    private final AdminAiTaskMapper adminAiTaskMapper;
    private final AdminAiTaskItemMapper adminAiTaskItemMapper;
    private final SysUserMapper sysUserMapper;
    private final MentalResourceMapper mentalResourceMapper;
    private final AuditLogService auditLogService;

    public AdminAiTaskServiceImpl(AdminAiTaskMapper adminAiTaskMapper,
                                  AdminAiTaskItemMapper adminAiTaskItemMapper,
                                  SysUserMapper sysUserMapper,
                                  MentalResourceMapper mentalResourceMapper,
                                  AuditLogService auditLogService) {
        this.adminAiTaskMapper = adminAiTaskMapper;
        this.adminAiTaskItemMapper = adminAiTaskItemMapper;
        this.sysUserMapper = sysUserMapper;
        this.mentalResourceMapper = mentalResourceMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseAdminAiTaskResponse parse(ParseAdminAiTaskRequest request) {
        Long adminUserId = SecurityUtil.getCurrentUserId();
        ParsedTask parsedTask = parseInstruction(request.getInstruction().trim());

        AdminAiTask task = new AdminAiTask();
        task.setAdminUserId(adminUserId);
        task.setInstructionText(request.getInstruction().trim());
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
                "Parsed task #" + task.getId(), null);
        return ParseAdminAiTaskResponse.builder()
                .ready(AdminAiTaskConstants.PARSE_READY.equals(task.getParseStatus()))
                .message(AdminAiTaskConstants.PARSE_READY.equals(task.getParseStatus())
                        ? "Ready to execute"
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
            throw new BusinessException("Current task is not executable");
        }
        if (!AdminAiTaskConstants.CONFIRM_PENDING.equals(task.getConfirmStatus())) {
            throw new BusinessException("Current task has already been processed");
        }

        List<AdminAiTaskItem> items = adminAiTaskItemMapper.selectList(new LambdaQueryWrapper<AdminAiTaskItem>()
                .eq(AdminAiTaskItem::getTaskId, taskId)
                .orderByAsc(AdminAiTaskItem::getSortNo, AdminAiTaskItem::getId));
        validateItemsBeforeExecution(items);
        for (AdminAiTaskItem item : items) {
            executeItem(item, adminUserId);
            item.setExecuteStatus(AdminAiTaskConstants.EXECUTE_EXECUTED);
            adminAiTaskItemMapper.updateById(item);
        }

        task.setConfirmStatus(AdminAiTaskConstants.CONFIRM_CONFIRMED);
        task.setExecuteStatus(AdminAiTaskConstants.EXECUTE_EXECUTED);
        task.setConfirmedAt(LocalDateTime.now());
        task.setExecutedAt(LocalDateTime.now());
        adminAiTaskMapper.updateById(task);
        auditLogService.record(adminUserId, "ADMIN_AI_CONFIRM", "Admin AI confirm task",
                "Confirmed task #" + taskId, null);
        return getTask(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminAiTaskResponse cancel(Long taskId) {
        Long adminUserId = SecurityUtil.getCurrentUserId();
        AdminAiTask task = getRequiredTask(taskId);
        if (!AdminAiTaskConstants.CONFIRM_PENDING.equals(task.getConfirmStatus())) {
            throw new BusinessException("Current task cannot be canceled");
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
        String normalized = instruction.trim();
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (containsAny(lowered, "\u7981\u7528", "\u505c\u7528", "\u5c01\u7981", "disable account", "suspend account", "ban account")) {
            return parseAccountStatusTask(normalized, UserStatusConstants.DISABLED);
        }
        if (containsAny(lowered, "\u542f\u7528", "\u6062\u590d", "enable account", "activate account", "restore account")) {
            return parseAccountStatusTask(normalized, UserStatusConstants.ACTIVE);
        }
        if (containsAny(lowered, "\u521b\u5efa\u54a8\u8be2\u5e08", "\u65b0\u589e\u54a8\u8be2\u5e08", "create counselor", "add counselor")) {
            return parseCounselorCreateTask(normalized);
        }
        if (containsAny(lowered, "\u4e0a\u67b6\u8d44\u6e90", "\u53d1\u5e03\u8d44\u6e90", "publish resource", "publish article", "online resource")) {
            return parseResourceStatusTask(normalized, ResourceConstants.RESOURCE_PUBLISHED, AdminAiTaskConstants.OP_PUBLISH);
        }
        if (containsAny(lowered, "\u4e0b\u67b6\u8d44\u6e90", "\u505c\u6b62\u8d44\u6e90", "offline resource", "unpublish resource", "take resource offline")) {
            return parseResourceStatusTask(normalized, ResourceConstants.RESOURCE_OFFLINE, AdminAiTaskConstants.OP_OFFLINE);
        }
        return ParsedTask.needMoreInfo("Unsupported instruction. Use account enable/disable, counselor creation, or resource publish/offline.");
    }

    private ParsedTask parseAccountStatusTask(String instruction, String targetStatus) {
        String account = extractFieldValue(instruction, "\u8d26\u53f7", "\u8d26\u6237", "account");
        if (account == null) {
            SysUser mentionedUser = findMentionedUser(instruction);
            account = mentionedUser == null ? null : mentionedUser.getAccount();
        }
        if (account == null) {
            return ParsedTask.needMoreInfo("Please provide the account to update.");
        }
        SysUser user = findUserByAccount(account);
        if (user == null) {
            return ParsedTask.needMoreInfo("Target account does not exist.");
        }
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_USER);
        item.setTargetId(user.getId());
        item.setTargetLabel(user.getAccount());
        item.setOperationType(AdminAiTaskConstants.OP_UPDATE);
        item.setFieldName("status");
        item.setOldValue(user.getStatus());
        item.setNewValue(targetStatus);
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_ACCOUNT_STATUS,
                "Ready to change account " + user.getAccount() + " status to " + targetStatus,
                List.of(item));
    }

    private ParsedTask parseCounselorCreateTask(String instruction) {
        String account = extractFieldValue(instruction, "\u8d26\u53f7", "\u8d26\u6237", "account");
        String displayName = extractFieldValue(instruction, "\u663e\u793a\u540d", "\u59d3\u540d", "\u540d\u5b57", "displayName", "name");
        String counselorNo = extractFieldValue(instruction, "\u5de5\u53f7", "counselorNo", "staffNo");
        if (account == null || displayName == null || counselorNo == null) {
            return ParsedTask.needMoreInfo("Creating counselor requires account, displayName, and counselorNo.");
        }
        if (findUserByAccount(account) != null) {
            return ParsedTask.needMoreInfo("The target account already exists.");
        }

        List<AdminAiTaskItem> items = new ArrayList<>();
        items.add(buildCreateItem(account, "account", account));
        items.add(buildCreateItem(account, "displayName", displayName));
        items.add(buildCreateItem(account, "counselorNo", counselorNo));
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_COUNSELOR_CREATE,
                "Ready to create counselor account " + account + " with displayName " + displayName,
                items);
    }

    private ParsedTask parseResourceStatusTask(String instruction, String targetStatus, String operationType) {
        MentalResource resource = findResourceByInstruction(instruction);
        if (resource == null) {
            return ParsedTask.needMoreInfo("Please provide resource ID or exact title.");
        }
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_RESOURCE);
        item.setTargetId(resource.getId());
        item.setTargetLabel(resource.getTitle());
        item.setOperationType(operationType);
        item.setFieldName("status");
        item.setOldValue(resource.getStatus());
        item.setNewValue(targetStatus);
        return ParsedTask.ready(AdminAiTaskConstants.TASK_TYPE_RESOURCE_STATUS,
                "Ready to change resource " + resource.getTitle() + " status to " + targetStatus,
                List.of(item));
    }

    private void validateItemsBeforeExecution(List<AdminAiTaskItem> items) {
        for (AdminAiTaskItem item : items) {
            if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                    && AdminAiTaskConstants.OP_UPDATE.equals(item.getOperationType())
                    && "status".equals(item.getFieldName())) {
                SysUser user = getRequiredUser(item.getTargetId());
                if (!Objects.equals(user.getStatus(), item.getOldValue())) {
                    throw new BusinessException("Account status changed. Please parse the task again.");
                }
                continue;
            }
            if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                    && AdminAiTaskConstants.OP_CREATE.equals(item.getOperationType())
                    && "account".equals(item.getFieldName())) {
                if (findUserByAccount(item.getNewValue()) != null) {
                    throw new BusinessException("Target counselor account already exists.");
                }
                continue;
            }
            if (AdminAiTaskConstants.TARGET_RESOURCE.equals(item.getTargetType())) {
                MentalResource resource = getRequiredResource(item.getTargetId());
                if (!Objects.equals(resource.getStatus(), item.getOldValue())) {
                    throw new BusinessException("Resource status changed. Please parse the task again.");
                }
            }
        }
    }

    private void executeItem(AdminAiTaskItem item, Long adminUserId) {
        if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                && AdminAiTaskConstants.OP_UPDATE.equals(item.getOperationType())
                && "status".equals(item.getFieldName())) {
            SysUser user = getRequiredUser(item.getTargetId());
            user.setStatus(item.getNewValue());
            sysUserMapper.updateById(user);
            auditLogService.record(adminUserId, "ADMIN_AI_USER_STATUS", "Admin AI update user status",
                    "Account " + user.getAccount() + " status changed to " + item.getNewValue(), null);
            return;
        }

        if (AdminAiTaskConstants.TARGET_USER.equals(item.getTargetType())
                && AdminAiTaskConstants.OP_CREATE.equals(item.getOperationType())) {
            executeCreateCounselor(item, adminUserId);
            return;
        }

        if (AdminAiTaskConstants.TARGET_RESOURCE.equals(item.getTargetType())) {
            MentalResource resource = getRequiredResource(item.getTargetId());
            resource.setStatus(item.getNewValue());
            if (ResourceConstants.RESOURCE_PUBLISHED.equals(item.getNewValue()) && resource.getPublishedAt() == null) {
                resource.setPublishedAt(LocalDateTime.now());
            }
            mentalResourceMapper.updateById(resource);
            auditLogService.record(adminUserId, "ADMIN_AI_RESOURCE_STATUS", "Admin AI update resource status",
                    "Resource #" + resource.getId() + " status changed to " + item.getNewValue(), null);
        }
    }

    private void executeCreateCounselor(AdminAiTaskItem currentItem, Long adminUserId) {
        if (!"account".equals(currentItem.getFieldName())) {
            return;
        }
        List<AdminAiTaskItem> peerItems = adminAiTaskItemMapper.selectList(new LambdaQueryWrapper<AdminAiTaskItem>()
                .eq(AdminAiTaskItem::getTaskId, currentItem.getTaskId())
                .orderByAsc(AdminAiTaskItem::getSortNo, AdminAiTaskItem::getId));
        String account = currentItem.getNewValue();
        String displayName = getItemValue(peerItems, "displayName");
        String counselorNo = getItemValue(peerItems, "counselorNo");
        String salt = PasswordCryptoUtil.generateSalt();

        SysUser counselor = new SysUser();
        counselor.setAccount(account);
        counselor.setPasswordSalt(salt);
        counselor.setPasswordHash(PasswordCryptoUtil.hashPassword("Jqpro@123", salt));
        counselor.setRoleCode(RoleConstants.COUNSELOR);
        counselor.setRealName(displayName);
        counselor.setDisplayName(displayName);
        counselor.setCounselorNo(counselorNo);
        counselor.setStatus(UserStatusConstants.ACTIVE);
        sysUserMapper.insert(counselor);
        auditLogService.record(adminUserId, "ADMIN_AI_COUNSELOR_CREATE", "Admin AI create counselor",
                "Created counselor account " + account, null);
    }

    private String getItemValue(List<AdminAiTaskItem> items, String fieldName) {
        return items.stream()
                .filter(item -> fieldName.equals(item.getFieldName()))
                .map(AdminAiTaskItem::getNewValue)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Task item is incomplete"));
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

    private AdminAiTaskItem buildCreateItem(String targetLabel, String fieldName, String newValue) {
        AdminAiTaskItem item = new AdminAiTaskItem();
        item.setTargetType(AdminAiTaskConstants.TARGET_USER);
        item.setTargetLabel(targetLabel);
        item.setOperationType(AdminAiTaskConstants.OP_CREATE);
        item.setFieldName(fieldName);
        item.setNewValue(newValue);
        return item;
    }

    private AdminAiTask getRequiredTask(Long taskId) {
        AdminAiTask task = adminAiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Task does not exist");
        }
        return task;
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("Target user does not exist");
        }
        return user;
    }

    private MentalResource getRequiredResource(Long resourceId) {
        MentalResource resource = mentalResourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("Target resource does not exist");
        }
        return resource;
    }

    private static final class ParsedTask {

        private final String taskType;
        private final String parseStatus;
        private final String summaryText;
        private final String failureReason;
        private final List<AdminAiTaskItem> items;

        private ParsedTask(String taskType, String parseStatus, String summaryText, String failureReason, List<AdminAiTaskItem> items) {
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