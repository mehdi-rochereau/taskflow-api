package com.mehdi.taskflow;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for every test requiring a real database.
 *
 * <p>Starts a disposable MySQL 8.4 container matching the production engine version declared in
 * {@code docker-compose.yml}, and lets Flyway replay every migration against it. Tests extending
 * this class run against the exact schema that will be deployed, including foreign key {@code ON
 * DELETE} clauses and {@code CHECK} constraints, which neither a Mockito mock nor an in-memory
 * database can reproduce.
 *
 * <p>This class declares no test slice on purpose: subclasses pick their own ({@code @DataJpaTest},
 * {@code @SpringBootTest}, and so on) and inherit only the container.
 *
 * @see MySQLContainer
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    /**
     * Disposable MySQL instance backing the Spring context.
     *
     * <p>{@code @ServiceConnection} wires the container's JDBC URL, username and password into the
     * Spring context automatically. Without it, the datasource properties would have to be injected
     * manually through {@code @DynamicPropertySource}, because the container's port is assigned at
     * runtime and cannot be known when {@code application.yml} is written.
     *
     * <p>Pinned to 8.4 to match the production engine. This tag must be kept in sync with the
     * {@code taskflow-db} image in {@code docker-compose.yml}: a test suite running on a different
     * engine version proves nothing about what is actually deployed. It was pinned to 8.0 until 18
     * Aug 2026, after production had already moved to 8.4.
     */
    @ServiceConnection static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        // Started explicitly rather than through @Container: @Container ties the
        // lifecycle to a single test class and would restart MySQL for each one.
        // A static block starts it once per JVM, keeping the suite fast as more
        // integration test classes are added later.
        MYSQL.start();
    }
}
