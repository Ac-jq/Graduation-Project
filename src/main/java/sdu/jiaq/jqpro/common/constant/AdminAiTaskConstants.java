package sdu.jiaq.jqpro.common.constant;

/**
 * Admin AI task state constants.
 */
public final class AdminAiTaskConstants {

    public static final String TASK_TYPE_ACCOUNT_STATUS = "ACCOUNT_STATUS";
    public static final String TASK_TYPE_COUNSELOR_CREATE = "COUNSELOR_CREATE";
    public static final String TASK_TYPE_RESOURCE_STATUS = "RESOURCE_STATUS";

    public static final String PARSE_READY = "READY";
    public static final String PARSE_NEED_MORE_INFO = "NEED_MORE_INFO";

    public static final String CONFIRM_PENDING = "PENDING";
    public static final String CONFIRM_CONFIRMED = "CONFIRMED";
    public static final String CONFIRM_CANCELED = "CANCELED";

    public static final String EXECUTE_WAITING = "WAITING";
    public static final String EXECUTE_EXECUTED = "EXECUTED";
    public static final String EXECUTE_CANCELED = "CANCELED";
    public static final String EXECUTE_FAILED = "FAILED";

    public static final String TARGET_USER = "USER";
    public static final String TARGET_RESOURCE = "RESOURCE";

    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_PUBLISH = "PUBLISH";
    public static final String OP_OFFLINE = "OFFLINE";

    private AdminAiTaskConstants() {
    }
}
