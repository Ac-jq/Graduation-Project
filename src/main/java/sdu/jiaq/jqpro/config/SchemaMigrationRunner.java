package sdu.jiaq.jqpro.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lightweight schema guard for local delivery environments where the database
 * may already exist before fresh columns are added to schema.sql.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureSysUserAvatarUrlColumn();
        ensureStudentProfileAvatarUrlColumn();
        ensureMentalScaleReportRecommendedResourceIdsColumn();
        ensureAiChatSessionArchivedAtColumn();
        ensureAiPersonaSettingTable();
        ensureAdminAiTaskAgentColumns();
    }

    private void ensureSysUserAvatarUrlColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'sys_user'
                  AND column_name = 'avatar_url'
                """, Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT '头像地址' AFTER counselor_no");
    }

    private void ensureStudentProfileAvatarUrlColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'student_profile'
                  AND column_name = 'avatar_url'
                """, Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE student_profile ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT '头像地址' AFTER user_id");
    }

    private void ensureMentalScaleReportRecommendedResourceIdsColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'mental_scale_report'
                  AND column_name = 'recommended_resource_ids'
                """, Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE mental_scale_report ADD COLUMN recommended_resource_ids VARCHAR(255) NULL COMMENT '推荐资源ID快照，逗号分隔' AFTER ai_interpretation");
    }

    private void ensureAiChatSessionArchivedAtColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'ai_chat_session'
                  AND column_name = 'archived_at'
                """, Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE ai_chat_session ADD COLUMN archived_at DATETIME NULL COMMENT '归档时间' AFTER status");
    }

    private void ensureAiPersonaSettingTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_persona_setting (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
                    mentor_name VARCHAR(64) NOT NULL DEFAULT '青禾导师' COMMENT 'AI导师昵称',
                    avatar_text VARCHAR(32) NOT NULL DEFAULT '青' COMMENT 'AI导师头像文本',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    UNIQUE KEY uk_ai_persona_student (student_user_id),
                    CONSTRAINT fk_ai_persona_student FOREIGN KEY (student_user_id) REFERENCES sys_user (id)
                ) COMMENT='学生AI导师设定表'
                """);
    }
    private void ensureAdminAiTaskAgentColumns() {
        ensureColumnExists("admin_ai_task", "workflow_status",
                "ALTER TABLE admin_ai_task ADD COLUMN workflow_status VARCHAR(32) NOT NULL DEFAULT 'NEED_CLARIFICATION' COMMENT '工作流状态' AFTER parse_status");
        ensureColumnExists("admin_ai_task", "agent_status",
                "ALTER TABLE admin_ai_task ADD COLUMN agent_status VARCHAR(32) NOT NULL DEFAULT 'CLARIFYING' COMMENT '智能体会话状态' AFTER execute_status");
        ensureColumnExists("admin_ai_task", "pending_prompt",
                "ALTER TABLE admin_ai_task ADD COLUMN pending_prompt VARCHAR(500) NULL COMMENT '待补充追问' AFTER failure_reason");
        ensureColumnExists("admin_ai_task", "conversation_log",
                "ALTER TABLE admin_ai_task ADD COLUMN conversation_log TEXT NULL COMMENT '多轮对话日志' AFTER pending_prompt");
    }

    private void ensureColumnExists(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }
}
