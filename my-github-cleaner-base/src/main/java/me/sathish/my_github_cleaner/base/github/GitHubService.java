package me.sathish.my_github_cleaner.base.github;

import com.fasterxml.jackson.databind.JsonNode;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriBuilder;
import java.util.ArrayList;
import java.util.List;

import static reactor.core.publisher.Flux.empty;

@Service
public class GitHubService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String GITHUB_TOKEN_KEY = "GITHUB_TOKEN";
    private static final String GITHUB_USERNAME_KEY = "githubusername";
    private static final String TOKEN_REQUIRED_ERROR = "GitHub token is required for authenticated requests";
    private static final int PER_PAGE = 100;
    private static final String SORT_PARAM = "updated";

    private static final String USER_REPOS_PATH = "/users/{username}/repos";
    private static final String AUTH_USER_REPOS_PATH = "/user/repos";
    private static final String USER_REPO_PATH = "/{username}/repos/{repo}";

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
        String url = "https://api.github.com/user";
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        JsonNode json = response.getBody();
        return json.get("public_repos").asInt() + json.get("total_private_repos").asInt();
    }

    public List<GitHubRepository> getUserRepositories(String username) {
        String url = "https://api.github.com/user/repos";
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<List<GitHubRepository>> response = restTemplate.exchange(
            url, HttpMethod.GET, entity,
            new org.springframework.core.ParameterizedTypeReference<List<GitHubRepository>>() {}
        );
        return response.getBody();
    }

    public List<GitHubRepository> getAuthenticatedUserRepositories() {
        validateToken();
        String url = buildUri(AUTH_USER_REPOS_PATH);
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<List<GitHubRepository>> response = restTemplate.exchange(
            url, HttpMethod.GET, entity,
            new org.springframework.core.ParameterizedTypeReference<List<GitHubRepository>>() {}
        );
        return response.getBody();
    }

    public GitHubRepository getAuthenticatedUserRepository(String repoName) {
        validateToken();
        String username = environment.getProperty(GITHUB_USERNAME_KEY);
        String url = GITHUB_API_BASE_URL + USER_REPO_PATH.replace("{username}", username).replace("{repo}", repoName);
        HttpHeaders headers = new HttpHeaders();
        setAuthorizationHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<GitHubRepository> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, GitHubRepository.class
        );
        return response.getBody();
    }

    public List<GitHubRepository> getAllUserRepositoriesPaginated(String username) {
        return getAllRepositoriesRecursive(1, username, USER_REPOS_PATH);
    }

    public List<GitHubRepository> getAllAuthenticatedUserRepositoriesPaginated() {
        validateToken();
        return getAllRepositoriesRecursive(1, null, AUTH_USER_REPOS_PATH);
    }

    private List<GitHubRepository> getAllRepositoriesRecursive(int page, String username, String endpoint) {
        List<GitHubRepository> allRepos = new ArrayList<>();
        String url = buildUri(endpoint, username, page, PER_PAGE, SORT_PARAM);
        while (true) {
            HttpHeaders headers = new HttpHeaders();
            setAuthorizationHeader(headers);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<GitHubRepository>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new org.springframework.core.ParameterizedTypeReference<List<GitHubRepository>>() {}
            );
            List<GitHubRepository> repos = response.getBody();
            if (repos == null || repos.isEmpty()) break;
            allRepos.addAll(repos);
            if (repos.size() < PER_PAGE) break;
            page++;
        }
        return allRepos;
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
