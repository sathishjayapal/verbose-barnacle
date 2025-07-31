package me.sathish.my_github_cleaner.base.github;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private final WebClient webClient;
    private final String githubToken;

    public GitHubService(Environment environment) {
        this.environment = environment;
        this.githubToken = environment.getProperty(GITHUB_TOKEN_KEY);
        this.webClient = createWebClient();
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();
    }

    public Mono<List<GitHubRepository>> getUserRepositories(String username) {
        return webClient.get()
                .uri(buildUri(USER_REPOS_PATH, username))
                .headers(this::setAuthorizationHeader)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GitHubRepository>>() {});
    }

    public Mono<List<GitHubRepository>> getAuthenticatedUserRepositories() {
        return validateToken()
                .then(webClient.get()
                        .uri(buildUri(AUTH_USER_REPOS_PATH))
                        .headers(this::setAuthorizationHeader)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<GitHubRepository>>() {}));
    }

    public Mono<GitHubRepository> getAuthenticatedUserRepository(String repoName) {
        return validateToken()
                .then(Mono.just(environment.getProperty(GITHUB_USERNAME_KEY)))
                .flatMap(username -> webClient.get()
                        .uri(USER_REPO_PATH, username, repoName)
                        .headers(this::setAuthorizationHeader)
                        .retrieve()
                        .bodyToMono(GitHubRepository.class));
    }

    public Flux<GitHubRepository> getAllUserRepositoriesPaginated(String username) {
        return getAllRepositoriesRecursive(1, username, USER_REPOS_PATH);
    }

    public Flux<GitHubRepository> getAllAuthenticatedUserRepositoriesPaginated() {
        return validateToken()
                .thenMany(getAllRepositoriesRecursive(1, null, AUTH_USER_REPOS_PATH));
    }

    private Flux<GitHubRepository> getAllRepositoriesRecursive(int page, String username, String endpoint) {
        return webClient.get()
                .uri(buildUri(endpoint, username, page, PER_PAGE, SORT_PARAM))
                .headers(this::setAuthorizationHeader)
                .retrieve()
                .bodyToFlux(GitHubRepository.class)
                .timeout(Duration.ofSeconds(30))  // Add timeout
                .onErrorResume(error -> {
                    log.error("Error fetching repositories: {}", error.getMessage());
                    return error instanceof Exception
                            ? Flux.error(error)  // Propagate rate limit errors
                            : Flux.empty();      // Handle other errors
                })
                .expand(repo ->
                        fetchNextPage(page + 1, username, endpoint)
                                .collectList()
                                .flatMapMany(list -> list.isEmpty()
                                        ? Flux.empty()   // Stop when no more results
                                        : Flux.fromIterable(list))
                )
                .limitRate(20);  // More conservative rate limiting
    }

    private Mono<Void> validateToken() {
        return isValidToken()
                ? Mono.empty()
                : Mono.error(new RuntimeException(TOKEN_REQUIRED_ERROR));
    }

    private boolean isValidToken() {
        return githubToken != null && !githubToken.isEmpty();
    }

    private void setAuthorizationHeader(HttpHeaders headers) {
        if (isValidToken()) {
            headers.setBearerAuth(githubToken);
        }
    }

    private String buildUri(String path, Object... params) {
        String uri;
        if (params != null && params.length > 0) {
            String baseUri = String.format("%s?per_page=%d&sort=%s", path, PER_PAGE, SORT_PARAM);
            uri = baseUri.replace("{username}", params[0].toString());
        } else {
            uri = String.format("%s?per_page=%d&sort=%s", path, PER_PAGE, SORT_PARAM);
        }
        log.debug("Built URI: {}", uri);
        return uri;
    }

    private String buildPaginatedUri(UriBuilder uriBuilder, String endpoint,
                                     String username, int page) {
        return uriBuilder
                .path(endpoint)
                .queryParam("per_page", PER_PAGE)
                .queryParam("page", page)
                .queryParam("sort", SORT_PARAM)
                .build(username != null ? username : new Object[0])
                .toString();
    }

    private Flux<GitHubRepository> fetchNextPage(int nextPage, String username, String endpoint) {
        return webClient.get()
                .uri(buildUri(endpoint, username, nextPage, PER_PAGE, SORT_PARAM))
                .headers(this::setAuthorizationHeader)
                .retrieve()
                .bodyToFlux(GitHubRepository.class)
                .take(PER_PAGE)
                .onErrorResume(error -> Flux.empty()); // Add error handling
    }
}
