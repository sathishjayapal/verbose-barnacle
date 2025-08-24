package me.sathish.my_github_cleaner.base.github;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GitHubService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String GITHUB_TOKEN_KEY = "GITHUB_TOKEN";
    private static final String GITHUB_USERNAME_KEY = "githubusername";
    private static final String TOKEN_REQUIRED_ERROR = "GitHub token is required for authenticated requests";
    private static final int PER_PAGE = 100;
    private static final String SORT_PARAM = "updated";
    private static final String AUTH_USER_REPOS_PATH = "/user/repos";
    private final Environment environment;
    private final RestTemplate restTemplate;
    private final String githubToken;

    public GitHubService(Environment environment) {
        this.environment = environment;
        this.githubToken = environment.getProperty(GITHUB_TOKEN_KEY);
        System.out.println("GITHUB_TOKEN: " + githubToken);
        this.restTemplate = new RestTemplate();
    }

    public Integer getAuthenticatedUserTotalRepoCount() {
        String url = "https://api.github.com/user/repos";
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        JsonNode json = response.getBody();
        if (json.get("public_repos") != null && json.get("total_private_repos") != null) {
            return json.get("public_repos").asInt()
                    + json.get("total_private_repos").asInt();
        } else if (json.get("public_repos") != null) {
            return json.get("public_repos").asInt();
        } else if (json.get("total_private_repos") != null) {
            return json.get("total_private_repos").asInt();
        }
        return -1;
    }

    // Fetch a specific repository for the authenticated user
    public Optional<GitHubRepository> getAuthenticatedUserRepository(String repoName) {
        validateToken();
        String username = environment.getProperty(GITHUB_USERNAME_KEY);
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("GitHub username is not configured");
        }

        try {
            String url = String.format("%s/repos/%s/%s", GITHUB_API_BASE_URL, username, repoName);
            HttpHeaders headers = new HttpHeaders();
            setAuthorizationHeader(headers);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            log.debug("Requesting repository: {}", url);
            ResponseEntity<GitHubRepository> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, GitHubRepository.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch repository {}: {}", repoName, e.getMessage());
            return Optional.empty();
        }
    }

    public List<GitHubRepository> fetchAllPublicRepositoriesForUser(String username) {
        validateToken();
        String url = buildUri(AUTH_USER_REPOS_PATH);
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<List<GitHubRepository>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new org.springframework.core.ParameterizedTypeReference<>() {});
        return response.getBody();
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
}
