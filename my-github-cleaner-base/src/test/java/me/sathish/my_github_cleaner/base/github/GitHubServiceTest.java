package me.sathish.my_github_cleaner.base.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GitHubServiceTest {

    @Mock
    private Environment environment;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        lenient()
                .when(environment.getProperty(GitHubServiceConstants.GITHUB_TOKEN_KEY))
                .thenReturn("test-token");
        lenient()
                .when(environment.getProperty(GitHubServiceConstants.GITHUB_USERNAME_KEY))
                .thenReturn("testuser");
        gitHubService = new GitHubService(environment, new RestTemplate(), httpClient);
    }

    @Test
    void updateRepository_success() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        boolean result = gitHubService.updateRepository("myrepo", "new description");

        assertTrue(result);
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("https://api.github.com/repos/testuser/myrepo", captor.getValue().uri().toString());
        assertEquals("PATCH", captor.getValue().method());
    }

    @Test
    void updateRepository_orgFallback_success() throws Exception {
        lenient().when(environment.getProperty(GitHubServiceConstants.GITHUB_ORG_KEY)).thenReturn("testorg");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse)
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(404).thenReturn(200);

        boolean result = gitHubService.updateRepository("myrepo", "new description");

        assertTrue(result);
    }

    @Test
    void updateRepository_failure() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);

        boolean result = gitHubService.updateRepository("myrepo", "new description");

        assertFalse(result);
    }

    @Test
    void updateRepository_missingUsername_throws() {
        when(environment.getProperty(GitHubServiceConstants.GITHUB_USERNAME_KEY)).thenReturn("");
        gitHubService = new GitHubService(environment, new RestTemplate(), httpClient);

        assertThrows(RuntimeException.class, () -> gitHubService.updateRepository("myrepo", "desc"));
    }

    @Test
    void updateRepository_missingToken_throws() {
        when(environment.getProperty(GitHubServiceConstants.GITHUB_TOKEN_KEY)).thenReturn(null);
        gitHubService = new GitHubService(environment, new RestTemplate(), httpClient);

        assertThrows(RuntimeException.class, () -> gitHubService.updateRepository("myrepo", "desc"));
    }
}
