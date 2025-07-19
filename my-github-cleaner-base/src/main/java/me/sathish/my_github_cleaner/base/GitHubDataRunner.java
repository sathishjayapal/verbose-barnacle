package me.sathish.my_github_cleaner.base;

import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import me.sathish.my_github_cleaner.base.github.GitHubService;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    public void run(String... args) {
        Flux<GitHubRepository> repositoriesFlux = gitHubService.
                getAllUserRepositoriesPaginated(environment.getProperty("githubusername"));
        if (repositoriesService.countByRecords()) {
            logger.info("Repositories already exist in the database. Skipping data fetch.");
            saveRepositoriesReactive(repositoriesFlux).blockLast();
        } else
            saveRepositoriesReactive(repositoriesFlux).blockLast();
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
