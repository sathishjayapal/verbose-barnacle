package me.sathish.my_github_cleaner.base.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class GitHubDeleter implements GitHubServiceConstants {
    private static final Logger logger = LoggerFactory.getLogger(GitHubDeleter.class);
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
    public HttpResponse<String> deleteRepository(String repositoryName) {
        logger.info("Starting repository deletion process for: {}", repositoryName);

        try {
            String githubUsername = environment.getProperty(GITHUB_USERNAME_KEY);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(GITHUB_API_URL, githubUsername, repositoryName)))
                    .header("Authorization", "token " + environment.getProperty("GITHUB_TOKEN"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .DELETE()
                    .build();
            HttpResponse<String> response= httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response, repositoryName, githubUsername);
            return response;
        } catch (Exception e) {
            logger.error("Error deleting repository: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting repository: " + e.getMessage(), e);
        }
    }


    private void handleResponse(HttpResponse<String> response, String repositoryName, String username) {
        boolean isSuccess = response.statusCode() == SUCCESS_STATUS_CODE;
        String status = isSuccess ? "successfully" : "failed to be";
        logger.debug("Repository {} {} deleted. Status: {}", repositoryName, status, response.statusCode());
        String eventPayload = createEventPayload(repositoryName, username, isSuccess);

        eventTrackerService.sendEventToEventstracker(repositoryName, eventPayload);

        if (!isSuccess) {
            logger.debug("Response body: {}", response.body());
        }
    }

    private String createEventPayload(String repositoryName, String username, boolean isSuccess) {
        String status = isSuccess ? "Repository deleted successfully!" : "Failed to delete repository";
        return String.format(
                "%s{\"repositoryName\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                status, repositoryName, LocalDateTime.now(), username);
    }
}
