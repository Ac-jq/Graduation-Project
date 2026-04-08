package sdu.jiaq.jqpro.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lightweight schema guard for local delivery environments where the database
 * may already exist before fresh columns are added to schema.sql.
 */
@Component
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureStudentProfileAvatarUrlColumn();
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
}
