package com.finflow.document.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaCompatibilityInitializer {

    @Bean
    ApplicationRunner documentSchemaCompatibilityRunner(JdbcTemplate jdbcTemplate) {
        return args -> normalizeDocumentColumns(jdbcTemplate);
    }

    private void normalizeDocumentColumns(JdbcTemplate jdbcTemplate) {
        alterIfTableExists(jdbcTemplate,
                "loan_documents",
                "ALTER TABLE loan_documents MODIFY COLUMN document_type VARCHAR(50)");
        alterIfTableExists(jdbcTemplate,
                "loan_documents",
                "ALTER TABLE loan_documents MODIFY COLUMN status VARCHAR(30)");
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
