package me.sathish.my_github_cleaner.base.github;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GitHubDeleterServiceTest {

    @Autowired
    private GitHubDeleter gitHubDeleterService;

    @Test
    void testDeleteRepository() {
        // You can call the method here but
        // avoid hitting real GitHub API in integration tests
        // or mock dependencies if refactored to inject HttpCliendcct
    }
}