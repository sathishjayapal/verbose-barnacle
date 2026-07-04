package me.sathish.my_github_cleaner.base.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class GitHubService implements GitHubServiceConstants {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
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
        this.restTemplate = new RestTemplate();
    }

    // Fetch a specific repository for the authenticated user
    public Optional<GitHubRepository> getAuthenticatedUserRepository(String repoName) {
        validateToken();
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

    public List<GitHubRepository> fetchAllPublicRepositoriesForUser(String username) {
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
}
