package me.sathish.my_github_cleaner.base.github;

import java.net.http.HttpResponse;
import me.sathish.my_github_cleaner.base.config.BaseIT;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class GitHubDeleterServiceTest extends BaseIT {

    @Autowired
    private GitHubDeleter gitHubDeleter;

    @Autowired
    Environment environment;

    @Test
    void testDeleteRepository() {
        HttpResponse response = gitHubDeleter.deleteRepository("sathishjee");
        Assert.assertEquals(204, response.statusCode());
    }
}
