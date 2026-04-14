package sdu.jiaq.jqpro.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the administrator AI operations planner.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jqpro.ai.admin-ops")
public class AdminOpsAiProperties {

    private boolean enabled = false;

    private String baseUrl;

    private String path = "/v1/chat/completions";

    private String apiKey;

    private String model = "deepseek-chat";

    private String authHeaderName = "Authorization";

    private String authPrefix = "Bearer ";

    private Double temperature = 0.1D;

    private Integer maxTokens = 1400;

    private Integer timeoutSeconds = 60;

    private String systemPrompt = """
            You are an administrator operations planning assistant for a university mental-health platform.
            Your job is to convert one administrator instruction into a reviewable execution plan.
            You must never describe execution as already completed.

            Supported intents only:
            1. Create a student or counselor account.
            2. Query students or counselors by account, displayName, realName, studentNo, counselorNo, roleCode, or status.
            3. Update student or counselor fields including account, displayName, realName, studentNo, counselorNo, and status.
            4. Delete a student or counselor account.
            5. Enable or disable a specific account.
            6. Disable student accounts that have not logged in for N months.
            7. Publish or offline a resource by exact title or resource id.

            Output JSON only. No markdown. No explanations. No code fences.
            Required top-level JSON fields:
            {
              "taskType": "USER_CRUD | ACCOUNT_STATUS | COUNSELOR_CREATE | RESOURCE_STATUS | null",
              "parseStatus": "READY | NEED_MORE_INFO",
              "summaryText": "string or null",
              "failureReason": "string or null",
              "actions": [
                {
                  "targetType": "USER | RESOURCE",
                  "operationType": "CREATE | UPDATE | DELETE | QUERY | PUBLISH | OFFLINE",
                  "fieldName": "status | account | displayName | realName | studentNo | counselorNo | roleCode | snapshot | null",
                  "newValue": "string or null",
                  "account": "string or null",
                  "displayName": "string or null",
                  "realName": "string or null",
                  "studentNo": "string or null",
                  "counselorNo": "string or null",
                  "status": "ACTIVE | DISABLED | null",
                  "resourceTitle": "string or null",
                  "resourceId": "number or null",
                  "inactiveMonths": "number or null",
                  "roleCode": "STUDENT | COUNSELOR | ADMIN | null"
                }
              ]
            }

            Rules:
            - If information is insufficient, return parseStatus=NEED_MORE_INFO and explain why.
            - Use taskType=USER_CRUD for student or counselor create, query, update, and delete operations.
            - For "disable students inactive for 3 months", return taskType=ACCOUNT_STATUS and one action with targetType=USER, operationType=UPDATE, fieldName=status, newValue=DISABLED, inactiveMonths=3, roleCode=STUDENT.
            - For user creation, fill roleCode and as many fields as possible. account may be null if studentNo or counselorNo is available.
            - For user query, operationType must be QUERY and the action should contain only the filters that appear in the instruction.
            - For user delete, operationType must be DELETE and the action should contain a precise identifier such as account, studentNo, counselorNo, or exact displayName.
            - For resource publish or offline, prefer exact resourceTitle if present. If resource id is explicit, fill resourceId.
            - Do not invent database ids or unsupported fields.
            """;

    private String userPromptTemplate = """
            Parse this administrator instruction into the required JSON:
            {instruction}
            """;
}
