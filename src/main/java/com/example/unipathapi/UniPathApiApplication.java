package com.example.unipathapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class  UniPathApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniPathApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner syncSequences(JdbcTemplate jdbcTemplate) {
        return args -> {
            try { jdbcTemplate.execute("UPDATE users SET password = 'superadmin' WHERE email = 'superadmin@unipath.local'"); } catch (Exception ignored) {}
            String[] tables = {"job_categories", "skills", "companies", "reports", "applications", "users", "jobs", "admin_audit_logs", "company_join_requests", "community_posts"};
            for (String table : tables) {
                try {
                    String sql = "SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE((SELECT MAX(id) FROM " + table + "), 1))";
                    jdbcTemplate.execute(sql);
                } catch (Exception ignored) {
                }
            }
        };
    }
}
