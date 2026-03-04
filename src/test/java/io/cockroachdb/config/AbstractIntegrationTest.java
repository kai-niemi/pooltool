package io.cockroachdb.config;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;

import io.cockroachdb.pool.configurer.Application;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        DataJdbcRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Tag("integration-test")
public abstract class AbstractIntegrationTest {
}
