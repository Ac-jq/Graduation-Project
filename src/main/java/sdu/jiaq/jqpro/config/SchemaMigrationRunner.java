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
}
