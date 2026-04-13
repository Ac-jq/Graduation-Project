package sdu.jiaq.jqpro.service.ai;

/**
 * Structured action returned by the administrator AI planner.
 */
public record AdminOpsAiAction(
        String targetType,
        String operationType,
        String actionType,
        String fieldName,
        String newValue,
        String account,
        String displayName,
        String counselorNo,
        String resourceTitle,
        Long resourceId,
        Integer inactiveMonths,
        String roleCode
) {
}
