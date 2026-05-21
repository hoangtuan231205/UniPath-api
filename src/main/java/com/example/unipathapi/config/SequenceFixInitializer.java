//package com.example.unipathapi.config;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//import org.jspecify.annotations.NonNull;
//
//@Component
//public class SequenceFixInitializer implements ApplicationRunner {
//
//    private static final Logger log = LoggerFactory.getLogger(SequenceFixInitializer.class);
//
//    private final JdbcTemplate jdbcTemplate;
//
//    public SequenceFixInitializer(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    @Override
//    public void run(@NonNull ApplicationArguments args) {
//        try {
//            String sequenceName = jdbcTemplate.queryForObject(
//                    "SELECT pg_get_serial_sequence('users', 'id')",
//                    String.class
//            );
//
//            if (sequenceName == null || sequenceName.isBlank()) {
//                log.warn("Khong tim thay sequence cho users.id, bo qua dong bo sequence.");
//                return;
//            }
//
//            jdbcTemplate.execute(String.format(
//                    "SELECT setval('%s', COALESCE((SELECT MAX(id) FROM users), 0) + 1, false)",
//                    sequenceName
//            ));
//
//            log.info("Da dong bo sequence cho users.id: {}", sequenceName);
//        } catch (Exception ex) {
//            log.warn("Khong the dong bo sequence users.id, bo qua de tranh fail startup.", ex);
//        }
//    }
//}
//
//
//
