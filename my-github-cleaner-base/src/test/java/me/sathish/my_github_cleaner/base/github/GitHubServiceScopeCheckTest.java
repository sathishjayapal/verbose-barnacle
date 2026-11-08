package me.sathish.my_github_cleaner.base.github;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * The daily repo scan needs the public_repo scope. A token with the broader repo scope already has it, so no
 * GITHUB_TOKEN_ISSUE event may be sent for it; a token with neither must still raise the event.
 */
class GitHubServiceScopeCheckTest {

    @SuppressWarnings("unchecked")
    private GitHubService serviceWithScopes(String scopesHeader, EventTrackerService eventTracker) throws Exception {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(GitHubServiceConstants.GITHUB_TOKEN_KEY)).thenReturn("test-token");

        HttpResponse<String> scopeResponse = mock(HttpResponse.class);
        when(scopeResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("X-OAuth-Scopes", List.of(scopesHeader)), (k, v) -> true));
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(scopeResponse);

        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        return new GitHubService(environment, restTemplate, httpClient, eventTracker);
    }

    @Test
    void repoScopeSatisfiesPublicRepo_noTokenIssueEvent() throws Exception {
        EventTrackerService eventTracker = mock(EventTrackerService.class);

        serviceWithScopes("repo", eventTracker).fetchAllPublicRepositoriesForUser("testuser");

        verify(eventTracker, never()).sendGitHubEventToEventstracker(anyString(), anyString());
    }

    @Test
    void publicRepoScope_noTokenIssueEvent() throws Exception {
        EventTrackerService eventTracker = mock(EventTrackerService.class);

        serviceWithScopes("public_repo", eventTracker).fetchAllPublicRepositoriesForUser("testuser");

        verify(eventTracker, never()).sendGitHubEventToEventstracker(anyString(), anyString());
    }

    @Test
    void unrelatedScopes_stillSendTokenIssueEvent() throws Exception {
        EventTrackerService eventTracker = mock(EventTrackerService.class);

        serviceWithScopes("read:user", eventTracker).fetchAllPublicRepositoriesForUser("testuser");

        verify(eventTracker)
                .sendGitHubEventToEventstracker(
                        org.mockito.ArgumentMatchers.contains("token_scope_missing"),
                        eq(GitHubServiceConstants.GITHUB_TOKEN_ISSUE));
    }
}
