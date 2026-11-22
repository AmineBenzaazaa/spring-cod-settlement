package com.msetransport.codsettlement.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One real PostgreSQL 16 container for the whole test run (singleton container pattern).
 * {@code @ServiceConnection} makes Spring Boot point the DataSource at it, no properties needed.
 * Flyway migrates it exactly as in production, and Hibernate validates the mapping against it.
 *
 * <p>Why not H2: H2 does not behave like PostgreSQL for {@code SELECT ... FOR UPDATE},
 * {@code numeric} rounding, check constraints, {@code bytea} or {@code timestamptz}.
 */
@ActiveProfiles("test")
public abstract class PostgresTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start(); // stopped by Testcontainers' Ryuk sidecar when the JVM exits
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE dispute, reconciliation_result, remittance_line, remittance_statement_file,
                         remittance_statement, delivery_attempt, orders, courier, app_user, merchant
                RESTART IDENTITY CASCADE""");
    }
}
