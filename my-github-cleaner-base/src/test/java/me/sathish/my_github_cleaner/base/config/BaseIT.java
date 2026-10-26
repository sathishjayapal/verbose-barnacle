package me.sathish.my_github_cleaner.base.config;

import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import me.sathish.my_github_cleaner.base.MyGithubCleanerApplication;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Abstract base class to be extended by every IT test. Starts the Spring Boot context with a
 * Datasource connected to the Testcontainers Docker instance. The instance is reused for all tests,
 * with all data wiped out before each test.
 */
@SpringBootTest(classes = MyGithubCleanerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
// Test property sources are bound before spring.config.import resolution, so
// disabling the config client here reliably prevents the `configserver:` import
// in application.yml from contacting a (nonexistent on CI) config server. The
// datasource comes from the Testcontainers PostgreSQL @ServiceConnection below.
@TestPropertySource(
        properties = {
            "spring.cloud.config.enabled=false",
            "spring.rabbitmq.listener.simple.auto-startup=false",
            "spring.rabbitmq.listener.direct.auto-startup=false",
            "eventstracker.startup-fetch.enabled=false",
            "github.data-runner.enabled=false",
            "error.handling.http-status-in-json-response=true",
            "error.handling.exception-logging=NO_LOGGING",
            "error.handling.full-stacktrace-http-statuses=5xx",
            "error.handling.log-levels.5xx=ERROR",
            "error.handling.http-statuses.org.springframework.security.authorization.AuthorizationDeniedException=unauthorized",
            "EVENTSTRACKER_URL=http://localhost:9081",
            "eventstracker_username=test",
            "eventstracker_password=test",
            "GITHUB_TOKEN=test-token",
            "githubusername=test-user",
            "githuborgkey=test-org",
            "app.security.admin.username=" + BaseIT.ADMIN,
            "app.security.admin.password={noop}" + BaseIT.PASSWORD,
            "app.security.viewer.username=" + BaseIT.VIEWER,
            "app.security.viewer.password={noop}" + BaseIT.PASSWORD
        })
@Sql("/data/clearAll.sql")
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
public abstract class BaseIT {

    @ServiceConnection
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17.5");

    public static final String ADMIN = "admin";
    public static final String VIEWER = "viewer";
    public static final String PASSWORD = "Bootify!";

    static {
        postgreSQLContainer.withReuse(true).start();
    }

    @LocalServerPort
    public int serverPort;

    @Autowired
    public RepositoriesRepository repositoriesRepository;

    @PostConstruct
    public void initRestAssured() {
        RestAssured.port = serverPort;
        RestAssured.urlEncodingEnabled = false;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @SneakyThrows
    public String readResource(final String resourceName) {
        return StreamUtils.copyToString(getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8);
    }
}
