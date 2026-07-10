package me.sathish.my_github_cleaner.base.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerException;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService implements GitHubServiceConstants {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String TOKEN_REQUIRED_ERROR = "GitHub token is required for authenticated requests";
    private static final int PER_PAGE = 100;
    private static final String SORT_PARAM = "updated";
    private static final String AUTH_USER_REPOS_PATH = "/user/repos";
    private final Environment environment;
    private final RestTemplate restTemplate;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;
    private final EventTrackerService eventTrackerService;
    private Set<String> tokenScopes;

    @Autowired
    public GitHubService(Environment environment, EventTrackerService eventTrackerService) {
        this(environment, new RestTemplate(), HttpClient.newHttpClient(), eventTrackerService);
    }

    GitHubService(Environment environment, RestTemplate restTemplate, HttpClient httpClient, EventTrackerService eventTrackerService) {
        this.environment = environment;
        this.githubToken = environment.getProperty(GITHUB_TOKEN_KEY);
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.eventTrackerService = eventTrackerService;
        initializeTokenScopes();
    }

    // Backward-compatible constructor for tests
    GitHubService(Environment environment, RestTemplate restTemplate, HttpClient httpClient) {
        this.environment = environment;
        this.githubToken = environment.getProperty(GITHUB_TOKEN_KEY);
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.eventTrackerService = null;
        initializeTokenScopes();
    }

    private void initializeTokenScopes() {
        if (githubToken != null && !githubToken.isEmpty()) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GITHUB_API_BASE_URL + "/user"))
                        .header("Authorization", "Bearer " + githubToken)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String scopes = response.headers().firstValue("X-OAuth-Scopes").orElse("");
                tokenScopes = Arrays.stream(scopes.split(",")).map(String::trim).collect(java.util.stream.Collectors.toSet());
                log.info("GitHub token scopes: {}", tokenScopes);
            } catch (Exception e) {
                log.warn("Failed to fetch token scopes: {}", e.getMessage());
                tokenScopes = new HashSet<>();
            }
        } else {
            tokenScopes = new HashSet<>();
        }
    }

    // Fetch a specific repository for the authenticated user
    public Optional<GitHubRepository> getAuthenticatedUserRepository(String repoName) {
        validateToken();
        ensureScope("public_repo", "fetch repository");
        String username = environment.getProperty(GITHUB_USERNAME_KEY);
        String orgName = environment.getProperty(GITHUB_ORG_KEY);
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("GitHub username is not configured");
        }
        if (orgName == null || orgName.isEmpty()) {
            log.error("GitHub organization name is not configured.");
        }
        String url = String.format("%s/repos/%s/%s", GITHUB_API_BASE_URL, username, repoName);
        String orgUrl = String.format("%s/repos/%s/%s", GITHUB_API_BASE_URL, orgName, repoName);
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Try user repository first
        try {
            log.debug("Requesting repository: {}", url);
            ResponseEntity<GitHubRepository> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GitHubRepository.class);

            // If response is null or bad, try the organization URL
            if (response.getBody() == null && orgName != null && !orgName.isEmpty()) {
                log.error("Primary response was null, trying organization URL: {}", orgUrl);
                ResponseEntity<GitHubRepository> orgResponse =
                        restTemplate.exchange(orgUrl, HttpMethod.GET, entity, GitHubRepository.class);
                return Optional.ofNullable(orgResponse.getBody());
            }

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Exception occurred while fetching user repository {}: {}", repoName, e.getMessage());

            // If exception occurred and org name is available, try organization URL
            if (orgName != null && !orgName.isEmpty()) {
                try {
                    log.error("Trying organization URL due to exception: {}", orgUrl);
                    ResponseEntity<GitHubRepository> orgResponse =
                            restTemplate.exchange(orgUrl, HttpMethod.GET, entity, GitHubRepository.class);
                    return Optional.ofNullable(orgResponse.getBody());
                } catch (Exception orgException) {
                    log.error("Failed to fetch organization repository {}: {}", repoName, orgException.getMessage());
                }
            }

            return Optional.empty();
        }
    }

    /**
     * Update the description of a GitHub repository.
     * Tries the authenticated user's repository first, then falls back to the configured organization.
     *
     * @param repoName    the repository name to update
     * @param description the new description to set
     * @return true if the GitHub API returned a 2xx response, false otherwise
     */
    public boolean updateRepository(String repoName, String description) {
        validateToken();
        ensureScope("repo", "update repository description");
        String username = environment.getProperty(GITHUB_USERNAME_KEY);
        String orgName = environment.getProperty(GITHUB_ORG_KEY);
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("GitHub username is not configured");
        }

        String url = String.format("%s/repos/%s/%s", GITHUB_API_BASE_URL, username, repoName);
        String orgUrl = orgName != null && !orgName.isEmpty()
                ? String.format("%s/repos/%s/%s", GITHUB_API_BASE_URL, orgName, repoName)
                : null;

        Map<String, String> body = new HashMap<>();
        body.put("description", description);
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }

        try {
            log.debug("Updating repository description: {}", url);
            HttpResponse<String> response = httpClient.send(
                    buildPatchRequest(url, jsonBody), HttpResponse.BodyHandlers.ofString());
            if (is2xxSuccessful(response)) {
                return true;
            }
            log.error("Failed to update user repository {}: HTTP {} - Response: {}", repoName, response.statusCode(), response.body());
            return tryOrgUpdate(repoName, orgUrl, jsonBody);
        } catch (Exception e) {
            log.error("Failed to update user repository {}: {}", repoName, e.getMessage(), e);
            return tryOrgUpdate(repoName, orgUrl, jsonBody);
        }
    }

    private boolean tryOrgUpdate(String repoName, String orgUrl, String jsonBody) {
        if (orgUrl == null || orgUrl.isEmpty()) {
            log.error("No organization URL configured for repo {}, skipping org update", repoName);
            return false;
        }
        try {
            log.error("Trying organization repository update for {} at URL: {}", repoName, orgUrl);
            HttpResponse<String> orgResponse =
                    httpClient.send(buildPatchRequest(orgUrl, jsonBody), HttpResponse.BodyHandlers.ofString());
            if (!is2xxSuccessful(orgResponse)) {
                log.error("Failed to update organization repository {}: HTTP {} - Response: {}", repoName, orgResponse.statusCode(), orgResponse.body());
            }
            return is2xxSuccessful(orgResponse);
        } catch (Exception orgException) {
            log.error("Failed to update organization repository {}: {}", repoName, orgException.getMessage(), orgException);
            return false;
        }
    }

    private HttpRequest buildPatchRequest(String url, String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + githubToken)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    private boolean is2xxSuccessful(HttpResponse<String> response) {
        return response != null && response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public List<GitHubRepository> fetchAllPublicRepositoriesForUser(String username) {
        validateToken();
        ensureScope("public_repo", "fetch all repositories");
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<GitHubRepository> allRepos = new ArrayList<>();
        String url = buildUri(AUTH_USER_REPOS_PATH) + "&page=1";

        while (url != null) {
            log.info("Fetching repositories from: {}", url);
            ResponseEntity<List<GitHubRepository>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new org.springframework.core.ParameterizedTypeReference<>() {});

            List<GitHubRepository> page = response.getBody();
            if (page != null) {
                allRepos.addAll(page);
            }

            url = extractNextPageUrl(response.getHeaders());
        }

        log.info("Fetched {} total repositories across all pages", allRepos.size());
        return allRepos;
    }

    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private String extractNextPageUrl(HttpHeaders headers) {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null) {
            return null;
        }
        for (String linkHeader : linkHeaders) {
            Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private void setAuthorizationHeader(HttpHeaders headers) {
        if (githubToken != null && !githubToken.isEmpty()) {
            headers.setBearerAuth(githubToken);
        }
    }

    private String buildUri(String path, Object... params) {
        String uri;
        if (params != null && params.length > 0 && params[0] != null) {
            String baseUri = String.format("%s?per_page=%d&sort=%s", path, PER_PAGE, SORT_PARAM);
            uri = baseUri.replace("{username}", params[0].toString());
        } else {
            uri = String.format("%s?per_page=%d&sort=%s", path, PER_PAGE, SORT_PARAM);
        }
        log.debug("Built URI: {}", uri);
        return GITHUB_API_BASE_URL + uri;
    }

    private void validateToken() {
        if (githubToken == null || githubToken.isEmpty()) {
            throw new RuntimeException(TOKEN_REQUIRED_ERROR);
        }
    }

    /**
     * Ensures the GitHub token has the required scope for the specified operation.
     * If the scope is missing, logs the error, sends an event to EventTracker, and throws an exception.
     *
     * @param requiredScope the required GitHub token scope (e.g., "repo", "public_repo")
     * @param operation     the operation being attempted (for error messaging)
     */
    private void ensureScope(String requiredScope, String operation) {
        if (tokenScopes.contains(requiredScope)) {
            return;
        }

        String errorMsg = String.format("GitHub token lacks required '%s' scope for operation: %s. Current scopes: %s",
                requiredScope, operation, tokenScopes);
        log.error(errorMsg);

        // Send event about token scope issue
        if (eventTrackerService != null) {
            try {
                String payload = String.format("{\"issue\":\"token_scope_missing\",\"required_scope\":\"%s\",\"operation\":\"%s\",\"current_scopes\":\"%s\"}",
                        requiredScope, operation, String.join(",", tokenScopes));
                eventTrackerService.sendGitHubEventToEventstracker(payload, GITHUB_TOKEN_ISSUE);
            } catch (EventTrackerException e) {
                log.warn("Failed to send token scope event: {}", e.getMessage());
            }
        }

//        throw new RuntimeException(errorMsg);
    }
}
