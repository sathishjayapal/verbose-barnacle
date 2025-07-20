package me.sathish.my_github_cleaner.base.github;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
public class GitHubDeleterServiceTest {

    @Autowired
    private GitHubDeleter gitHubDeleter;
    @Autowired
    Environment environment;

    @Test
    void testDeleteRepository() {
        gitHubDeleter.deleteRepository("sathishjee");
    }
}