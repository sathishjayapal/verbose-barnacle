package me.sathish.my_github_cleaner.base.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import lombok.extern.slf4j.Slf4j;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitHubDeleter implements GitHubServiceConstants {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s";
    private static final int SUCCESS_STATUS_CODE = 204;

    private final Environment environment;
    private final EventTrackerService eventTrackerService;
    private final HttpClient httpClient;

    @Autowired
    public GitHubDeleter(Environment environment, EventTrackerService eventTrackerService) {
        this.environment = environment;
        this.eventTrackerService = eventTrackerService;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Deletes a repository from GitHub.
     * @param repositoryName The name of the repository to delete.
     * @return The HttpResponse from the GitHub API.
     */
    public HttpResponse<String> deleteRepository(String repositoryName, Long repoRecordID) {
        log.info("Starting repository deletion process for: {}", repositoryName);
        String githubUsername = environment.getProperty(GITHUB_USERNAME_KEY);
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(GITHUB_API_URL, githubUsername, repositoryName)))
                    .header("Authorization", "token " + environment.getProperty("GITHUB_TOKEN"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response, repositoryName, githubUsername, repoRecordID);
            return response;
        } catch (Exception e) {
            log.error("Error deleting repository: {}", e.getMessage(), e);
            HttpResponse<String> response = new HttpResponse<String>() {
                @Override
                public int statusCode() {
                    return 500;
                }

                @Override
                public HttpRequest request() {
                    return null;
                }

                @Override
                public Optional<HttpResponse<String>> previousResponse() {
                    return Optional.empty();
                }

                @Override
                public HttpHeaders headers() {
                    return null;
                }

                @Override
                public String body() {
                    return "";
                }

                @Override
                public Optional<SSLSession> sslSession() {
                    return Optional.empty();
                }

                @Override
                public URI uri() {
                    return null;
                }

                @Override
                public HttpClient.Version version() {
                    return null;
                }
            };
            handleResponse(response, repositoryName, githubUsername, repoRecordID);
            log.error("Response body: {}", response.body());
            return response;
        }
    }

    private void handleResponse(
            HttpResponse<String> response, String repositoryName, String username, Long repoRecordID) {
        boolean isSuccess = response.statusCode() == SUCCESS_STATUS_CODE;
        String status = isSuccess ? "successfully" : "failed to be";
        log.error(
                "Repository {} {} {} deleted. Status: {}", repoRecordID, repositoryName, status, response.statusCode());
        String eventPayload = createEventPayload(repositoryName, username, isSuccess, repoRecordID);
        eventTrackerService.sendGitHubEventToEventstracker(eventPayload);
    }

    private String createEventPayload(String repositoryName, String username, boolean isSuccess, Long repoRecordID) {
        String status = isSuccess ? "Repository deleted successfully!" : "Failed to delete repository";
        return String.format(
                "%s{\"repoRecordId\":\"%s\",\"repositoryName\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                status, repoRecordID, repositoryName, LocalDateTime.now(), username);
    }
}
