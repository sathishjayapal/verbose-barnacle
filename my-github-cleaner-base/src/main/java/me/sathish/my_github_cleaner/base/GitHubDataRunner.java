package me.sathish.my_github_cleaner.base;

import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import me.sathish.my_github_cleaner.base.github.GitHubService;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GitHubDataRunner implements CommandLineRunner {
    private final GitHubService gitHubService;
    private final RepositoriesService repositoriesService;
    private final Environment environment;
    Logger logger = LoggerFactory.getLogger(GitHubDataRunner.class);

    public GitHubDataRunner(GitHubService gitHubService,
                            RepositoriesService repositoriesService,
                            Environment environment) {
        this.gitHubService = gitHubService;
        this.repositoriesService = repositoriesService;
        this.environment = environment;
    }
    @Scheduled(fixedDelay= 86400000L)
    private Boolean findMissingRepositories() {
        System.out.println("Comparing GitHub repositories with database records...");

        // Get all repository names from GitHub
        List<GitHubRepository> githubRepos = gitHubService.getAllUserRepositoriesPaginated(environment.getProperty("githubusername")).collectList().block();
        Set<String> githubRepoNames = githubRepos.stream()
                .map(GitHubRepository::getName)
                .collect(Collectors.toSet());

        // Get all repository names from the database
        List<String> dbRepoNamesList = repositoriesService.findAllRepoNames();
        Set<String> dbRepoNames = dbRepoNamesList.stream().collect(Collectors.toSet());

        // Find repositories that are on GitHub but not in the database
        Set<String> missingInRepo = dbRepoNames.stream()
                .filter(repoName -> !githubRepoNames.contains(repoName))
                .collect(Collectors.toSet());

        Set<String> missingInDb = githubRepoNames.stream()
                .filter(repoName -> !dbRepoNames.contains(repoName))
                .collect(Collectors.toSet());
        if (missingInDb.isEmpty()) {
            System.out.println("No missing repositories found. All GitHub repositories are present in the database.");
            return Boolean.TRUE;
        } else {
            System.out.println("The following repositories are on GitHub but missing in the database:");
            missingInDb.forEach(System.out::println);
            return Boolean.FALSE;
        }
    }

    public void run(String... args) {
        if (findMissingRepositories()) {
            Flux<GitHubRepository> repositoriesFlux = gitHubService.
                    getAllUserRepositoriesPaginated(environment.getProperty("githubusername"));
            saveRepositoriesReactive(repositoriesFlux).blockLast();
        }
    }

    private Flux<Long> saveRepositoriesReactive(Flux<GitHubRepository> repositoriesFlux) {
        return repositoriesFlux.flatMap(gitHubRepository -> repositoriesService
                .createReactive(gitHubRepository)
                .onErrorResume(e -> {
                    // Ideally use a logger here, e.g., log.error(...)
                    logger.error("Error saving repo: " + e.getMessage());
                    return reactor.core.publisher.Mono.empty();
                }));
    }
}
