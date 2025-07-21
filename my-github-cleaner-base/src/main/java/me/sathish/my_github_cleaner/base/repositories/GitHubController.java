package me.sathish.my_github_cleaner.base.repositories;

import java.util.List;

import me.sathish.my_github_cleaner.base.github.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/github")
@PreAuthorize("hasAuthority('VIEWER')")
public class GitHubController {
    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/repos/{username}")
    public Mono<List<GitHubRepository>> getUserRepositories(@PathVariable String username) {
        System.out.println("Not making any request to GitHub API if username is empty");
        return gitHubService.getUserRepositories(username);
    }

    @GetMapping("/repos")
    public Mono<List<GitHubRepository>> getMyRepositories() {
        return gitHubService.getAuthenticatedUserRepositories();
    }

    @GetMapping("/repos/{username}/all")
    public Flux<GitHubRepository> getAllUserRepositories(@PathVariable String username) {
        return gitHubService.getAllUserRepositoriesPaginated(username);
    }

//    @GetMapping("/repos/all")
//    public Flux<GitHubRepository> getAllMyRepositories() {
//        return gitHubService.getAllAuthenticatedUserRepositoriesPaginated();
//    }
}
