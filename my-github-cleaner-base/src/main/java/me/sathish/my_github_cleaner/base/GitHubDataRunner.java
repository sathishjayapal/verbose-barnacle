package me.sathish.my_github_cleaner.base;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import me.sathish.my_github_cleaner.base.github.GitHubService;
import me.sathish.my_github_cleaner.base.github.GitHubServiceConstants;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class GitHubDataRunner implements CommandLineRunner, GitHubServiceConstants {
    private final GitHubService gitHubService;
    private final RepositoriesService repositoriesService;
    private final EventTrackerService eventTrackerService;
    private final Environment environment;
    Logger logger = LoggerFactory.getLogger(GitHubDataRunner.class);

    public GitHubDataRunner(
            GitHubService gitHubService,
            RepositoriesService repositoriesService,
            EventTrackerService eventTrackerService,
            Environment environment) {
        this.gitHubService = gitHubService;
        this.repositoriesService = repositoriesService;
        this.eventTrackerService = eventTrackerService;
        this.environment = environment;
    }

    @Scheduled(fixedDelay = 86400000L)
    private Set<String> findMissingRepositories() {
        Set<String> githubRepoNames =
                gitHubService.fetchAllPublicRepositoriesForUser(environment.getProperty(GITHUB_USERNAME_KEY)).stream()
                        .map(GitHubRepository::getName)
                        .collect(Collectors.toSet());
        // Get all repository names from the database
        List<String> dbRepoNamesList = repositoriesService.findAllRepoNames();
        System.out.println("dbRepoNamesList size: " + dbRepoNamesList.size());
        Set<String> dbRepoNames = dbRepoNamesList.stream().collect(Collectors.toSet());

        // Find repositories that are on GitHub but not in the database
        Set<String> missingInRepo = dbRepoNames.stream()
                .filter(repoName -> !githubRepoNames.contains(repoName))
                .collect(Collectors.toSet());
        if (missingInRepo.isEmpty()) {
            System.out.println("No missing repositories found. All GitHub repositories are present in the database.");
        } else {
            System.out.println("The following repositories are on Database but missing in Github:");
            missingInRepo.forEach(System.out::println);
        }

        Set<String> missingInDb = githubRepoNames.stream()
                .filter(repoName -> !dbRepoNames.contains(repoName))
                .collect(Collectors.toSet());
        if (missingInDb.isEmpty()) {
            System.out.println("No missing repositories found. All GitHub repositories are present in the database.");
            return Collections.emptySet();
        } else {
            System.out.println("The following repositories are on GitHub but missing in the database:");
            missingInDb.forEach(System.out::println);
            return missingInDb;
        }
    }

    public void run(String... args) {
        Set<String> missingInDb = findMissingRepositories();
        // Create a Flux of repositories by getting each missing repository individually
        if (missingInDb.size() > 0) {
            List<GitHubRepository> reposToSave = missingInDb.stream()
                    .map(repoName -> gitHubService
                            .getAuthenticatedUserRepository(repoName)
                            .orElse(null)) // Convert Optional to value or null
                    .filter(repo -> repo != null) // Filter out nulls (failed fetches)  )
                    .collect(Collectors.toList());

            if (reposToSave != null && !reposToSave.isEmpty()) {
                GitHubRepository firstRepo = reposToSave.getFirst(); // Get the first successfully fetched repo
                Flux<GitHubRepository> fluxFromList = Flux.fromIterable(reposToSave); // Create new Flux for saving
                saveRepositoriesReactive(fluxFromList).blockLast();
                String repositoryName = firstRepo.getName();
                String payLoad = new StringBuffer("Saved to DB repository")
                        .append(String.format(
                                "{\"repositoryName\":\"%s\",\"addedAt\":\"%s\",\"addedBy\":\"%s\"}",
                                repositoryName, LocalDateTime.now(), SYSTEM_USER))
                        .toString();
                eventTrackerService.sendEventToEventstracker(repositoryName, payLoad);
            } else {
                logger.info("No new missing repositories were successfully fetched for saving to DB.");
                String payLoad = new StringBuffer("No new repositories to DB repository")
                        .append(String.format(
                                "{\"addedAt\":\"%s\",\"addedBy\":\"%s\"}", LocalDateTime.now(), SYSTEM_USER))
                        .toString(); // Added missing argument
                eventTrackerService.sendEventToEventstracker("NA", payLoad);
                throw new RuntimeException("No new missing repositories were successfully fetched for saving to DB.");
            }
        } else {
            if (repositoriesService.countByRecords()) {
                System.out.println("Repositories already exist in the database. No new repositories to fetch.");
                String payLoad = new StringBuffer("No new repositories to DB repository")
                        .append(String.format(
                                "{\"addedAt\":\"%s\",\"addedBy\":\"%s\"}", LocalDateTime.now(), SYSTEM_USER))
                        .toString();
                eventTrackerService.sendEventToEventstracker("NA", payLoad);
            } else {
                List<GitHubRepository> reposToSave =
                        gitHubService.fetchAllPublicRepositoriesForUser(environment.getProperty(GITHUB_USERNAME_KEY));
                if (reposToSave != null && !reposToSave.isEmpty()) {
                    GitHubRepository firstRepo = reposToSave.get(0); // Get the first fetched repo
                    Flux<GitHubRepository> fluxFromList = Flux.fromIterable(reposToSave); // Create new Flux for saving
                    saveRepositoriesReactive(fluxFromList).blockLast();
                    String repositoryName = firstRepo.getName();
                    String payLoad = new StringBuffer("Saved to DB repository")
                            .append(String.format(
                                    "{\"repositoryName\":\"%s\",\"addedAt\":\"%s\",\"addedBy\":\"%s\"}",
                                    repositoryName, LocalDateTime.now(), SYSTEM_USER))
                            .toString();
                    eventTrackerService.sendEventToEventstracker(repositoryName, payLoad);
                } else {
                    logger.info("No user repositories found or fetched for saving to DB.");
                    String payLoad = new StringBuffer("No new repositories to DB repository")
                            .append(String.format(
                                    "{\"addedAt\":\"%s\",\"addedBy\":\"%s\"}", LocalDateTime.now(), SYSTEM_USER))
                            .toString();
                    eventTrackerService.sendEventToEventstracker("NA", payLoad);
                }
            }
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
