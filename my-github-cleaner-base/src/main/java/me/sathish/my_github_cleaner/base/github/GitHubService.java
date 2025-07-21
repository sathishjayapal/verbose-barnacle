package me.sathish.my_github_cleaner.base.github;

import java.util.List;

import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service

public class GitHubService {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private final WebClient webClient;
    @Autowired
    Environment environment;
    String githubToken;
    public GitHubService(Environment environment
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();
        this.environment=environment;
        this.githubToken = environment.getProperty("GITHUB_TOKEN");

    }

    public Mono<List<GitHubRepository>> getUserRepositories(String username) {
        String githubToken = environment.getProperty("GITHUB_TOKEN");
        return webClient
                .get()
                .uri("/users/{username}/repos?per_page=100&sort=updated", username)
                .headers(headers -> {
                    if ( githubToken!= null && !githubToken.isEmpty()) {
                        headers.setBearerAuth(githubToken);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GitHubRepository>>() {});
    }

    public Mono<List<GitHubRepository>> getAuthenticatedUserRepositories() {
        if (githubToken == null || githubToken.isEmpty()) {
            return Mono.error(new RuntimeException("GitHub token is required for authenticated requests"));
        }

        return webClient
                .get()
                .uri("/user/repos?per_page=100&sort=updated")
                .headers(headers -> headers.setBearerAuth(githubToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GitHubRepository>>() {});
    }

    public Flux<GitHubRepository> getAllUserRepositoriesPaginated(String username) {
        return getAllRepositoriesRecursive(1, username, "/users/{username}/repos");
    }

    public Flux<GitHubRepository> getAllAuthenticatedUserRepositoriesPaginated() {
        if (githubToken == null || githubToken.isEmpty()) {
            return Flux.error(new RuntimeException("GitHub token is required for authenticated requests"));
        }
        return getAllRepositoriesRecursive(1, null, "/user/repos");
    }

    private Flux<GitHubRepository> getAllRepositoriesRecursive(int page, String username, String endpoint) {
        return webClient
                .get()
                .uri(uriBuilder -> {
                    if (username != null) {
                        return uriBuilder
                                .path(endpoint)
                                .queryParam("per_page", 100)
                                .queryParam("page", page)
                                .queryParam("sort", "updated")
                                .build(username);
                    } else {
                        return uriBuilder
                                .path(endpoint)
                                .queryParam("per_page", 100)
                                .queryParam("page", page)
                                .queryParam("sort", "updated")
                                .build();
                    }
                })
                .headers(headers -> {
                    if (githubToken != null && !githubToken.isEmpty()) {
                        headers.setBearerAuth(githubToken);
                    }
                })
                .retrieve()
                .bodyToFlux(GitHubRepository.class)
                .expand(repo -> {
                    // Continue fetching next page if current page has 100 items (max per page)
                    return webClient
                            .get()
                            .uri(uriBuilder -> {
                                if (username != null) {
                                    return uriBuilder
                                            .path(endpoint)
                                            .queryParam("per_page", 100)
                                            .queryParam("page", page + 1)
                                            .queryParam("sort", "updated")
                                            .build(username);
                                } else {
                                    return uriBuilder
                                            .path(endpoint)
                                            .queryParam("per_page", 100)
                                            .queryParam("page", page + 1)
                                            .queryParam("sort", "updated")
                                            .build();
                                }
                            })
                            .headers(headers -> {
                                if (githubToken != null && !githubToken.isEmpty()) {
                                    headers.setBearerAuth(githubToken);
                                }
                            })
                            .retrieve()
                            .bodyToFlux(GitHubRepository.class)
                            .take(100);
                });
    }
}
