package uk.gov.justice.laa.crime.dces.integration.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
public abstract class ApplicationTestConfig {


    @Container
    @ServiceConnection
    public static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("db_name")
            .withUsername("test")
            .withPassword("test");

    static {
        setUpDatabase();
    }

    private static void setUpDatabase() {
        postgresContainer.start();
        System.setProperty("DB_PORT", postgresContainer.getFirstMappedPort().toString());
        System.out.println("DB_PORT: " + System.getProperty("DB_PORT"));
    }
}