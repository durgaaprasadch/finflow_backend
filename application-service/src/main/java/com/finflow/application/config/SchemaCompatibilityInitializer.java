package com.finflow.application.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaCompatibilityInitializer {

    @Bean
    ApplicationRunner applicationSchemaCompatibilityRunner(JdbcTemplate jdbcTemplate) {
        return args -> normalizeStatusColumns(jdbcTemplate);
    }

    private void normalizeStatusColumns(JdbcTemplate jdbcTemplate) {
        alterIfTableExists(jdbcTemplate,
                "loan_applications",
                "ALTER TABLE loan_applications MODIFY COLUMN status VARCHAR(50)");
        alterIfTableExists(jdbcTemplate,
                "loan_status_history",
                "ALTER TABLE loan_status_history MODIFY COLUMN from_status VARCHAR(50)");
        alterIfTableExists(jdbcTemplate,
                "loan_status_history",
                "ALTER TABLE loan_status_history MODIFY COLUMN to_status VARCHAR(50)");
    }

    @SuppressWarnings("null")
    private void alterIfTableExists(JdbcTemplate jdbcTemplate, String tableName, String sql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        if (count != null && count > 0) {
            jdbcTemplate.execute(sql);
        }
    }
}
