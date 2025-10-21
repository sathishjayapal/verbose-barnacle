package me.sathish.my_github_cleaner.base;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import me.sathish.my_github_cleaner.base.github.GitHubService;
import me.sathish.my_github_cleaner.base.github.GitHubServiceConstants;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class GitHubDataRunner implements CommandLineRunner, GitHubServiceConstants {
    private final GitHubService gitHubService;
    private final RepositoriesService repositoriesService;
    private final EventTrackerService eventTrackerService;
    private final Environment environment;
    SathishLoggerClient logger = new SathishLoggerClient("http://localhost:8080", "my-app");

    public GitHubDataRunner(
            GitHubService gitHubService,
            RepositoriesService repositoriesService,
            EventTrackerService eventTrackerService,
            Environment environment) {
        String correlationId = UUID.randomUUID().toString();
        logger.info("Processing request", correlationId);
        this.gitHubService = gitHubService;
        this.repositoriesService = repositoriesService;
        this.eventTrackerService = eventTrackerService;
        this.environment = environment;
    }

    /**
     * Find repositories that are on GitHub but not in the database and vice versa. This method is scheduled to run every
     * 24 hours.
     *
     * @return a set of repository names that are on GitHub but not in the database
     */
    @Scheduled(fixedDelay = 86400000L)
    private Set<String> findMissingRepositories() {
        logger.info("Application started");
        Set<String> githubRepoNames =
                gitHubService.fetchAllPublicRepositoriesForUser(environment.getProperty(GITHUB_USERNAME_KEY)).stream()
                        .map(GitHubRepository::getName)
                        .collect(Collectors.toSet());
        // Get all repository names from the database
        List<String> dbRepoNamesList = repositoriesService.findAllRepoNames();
        log.error("dbRepoNamesList size: " + dbRepoNamesList.size());
        Set<String> dbRepoNames = dbRepoNamesList.stream().collect(Collectors.toSet());

        // Find repositories that are on GitHub but not in the database
        Set<String> missingInRepo = dbRepoNames.stream()
                .filter(repoName -> !githubRepoNames.contains(repoName))
                .collect(Collectors.toSet());
        if (missingInRepo.isEmpty()) {

            log.error("No missing repositories found. All GitHub repositories are present in the database.");
        } else {
            log.error("The following repositories are on Database but missing in Github:");
            missingInRepo.forEach(System.out::println);
        }

        Set<String> missingInDb = githubRepoNames.stream()
                .filter(repoName -> !dbRepoNames.contains(repoName))
                .collect(Collectors.toSet());
        if (missingInDb.isEmpty()) {
            log.error("No missing repositories found. All GitHub repositories are present in the database.");
            return Collections.emptySet();
        } else {
            log.error("The following repositories are on GitHub but missing in the database:");
            missingInDb.forEach(System.out::println);
            return missingInDb;
        }
    }

    /**
     * Called by Spring Boot at startup. Finds any repositories that are present in
     * GitHub but not in the database and saves them to the database. If there are no
     * new repositories, it will send an event to event tracker with the payload
     * indicating that no new repositories were added to the database.
     * <p>
     * This method will also send an event to event tracker if there are no
     * repositories present in the database.
     * <p>
     * This method runs once when the application starts and then runs once a day
     * after that.
     * <p>
     * If there are no new missing repositories to fetch, it will fetch all repositories
     * from GitHub and save them to the database and send an event to event tracker.
     * <p>
     * If there are no repositories present in the database, it will fetch all
     * repositories from GitHub and save them to the database and send an event to
     * event tracker.
     * <p>
     * If there are new missing repositories to fetch and save to the database, it will
     * send an event to event tracker with the payload indicating that new
     * repositories were added to the database.
     * <p>
     * If there are no new missing repositories to fetch and there are repositories
     * present in the database, it will not do anything.
     *
     * @param args ignored
     */
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
                GitHubRepository firstRepo = reposToSave.get(0); // Get the first successfully fetched repo
                saveAndEvent(reposToSave, firstRepo);
            } else {
                log.error("No new missing repositories were successfully fetched for saving to DB.");
                String payLoad = "No new repositories to DB repository"
                        + String.format("{\"addedAt\":\"%s\",\"addedBy\":\"%s\"}", LocalDateTime.now(), SYSTEM_USER);
                log.error("Sending event to Eventstracker: " + payLoad);
                eventTrackerService.sendGitHubEventToEventstracker(payLoad);
            }
        } else {
            if (repositoriesService.countByRecords()) {
                log.error("Repositories already exist in the database. No new repositories to fetch.");
                String payLoad = "No new repositories to DB repository"
                        + String.format("{\"addedAt\":\"%s\",\"addedBy\":\"%s\"}", LocalDateTime.now(), SYSTEM_USER);
                eventTrackerService.sendGitHubEventToEventstracker(payLoad);
            } else {
                List<GitHubRepository> reposToSave =
                        gitHubService.fetchAllPublicRepositoriesForUser(environment.getProperty(GITHUB_USERNAME_KEY));
                if (reposToSave != null && !reposToSave.isEmpty()) {
                    GitHubRepository firstRepo = reposToSave.get(0); // Get the first fetched repo
                    saveAndEvent(reposToSave, firstRepo);
                } else {
                    log.error("No user repositories found or fetched for saving to DB.");
                    String payLoad = "No new repositories to DB repository"
                            + String.format(
                                    "{\"addedAt\":\"%s\",\"addedBy\":\"%s\"}", LocalDateTime.now(), SYSTEM_USER);
                    eventTrackerService.sendGitHubEventToEventstracker(payLoad);
                }
            }
        }
    }

    /**
     * Save the given list of GitHub repositories to the database using a reactive approach
     * and then send an event to the Eventstracker service with the name of the first
     * repository in the list.
     *
     * @param reposToSave the list of GitHub repositories to save to the database
     * @param firstRepo   the first repository in the list, used for sending the event
     */
    private void saveAndEvent(List<GitHubRepository> reposToSave, GitHubRepository firstRepo) {
        Flux<GitHubRepository> fluxFromList = Flux.fromIterable(reposToSave); // Create new Flux for saving
        saveRepositoriesReactive(fluxFromList).blockLast();
        String repositoryName = firstRepo.getName();
        String payLoad = "Saved to DB repository"
                + String.format(
                        "{\"repositoryName\":\"%s\",\"addedAt\":\"%s\",\"addedBy\":\"%s\"}",
                        repositoryName, LocalDateTime.now(), SYSTEM_USER);
        eventTrackerService.sendGitHubEventToEventstracker(payLoad);
    }

    /**
     * Save the given Flux of GitHubRepository objects to the database using the reactive RepositoriesService.
     * <p>
     * This method will save each repository, and if any of them fail to save (e.g., due to a duplicate repository name),
     * it will log the error and continue with the other repositories.
     * <p>
     * The returned Flux will contain the IDs of the saved repositories, or an empty Flux if no repositories were saved.
     * @param repositoriesFlux a Flux of GitHubRepository objects to save
     * @return a Flux of the IDs of the saved repositories
     */
    private Flux<Long> saveRepositoriesReactive(Flux<GitHubRepository> repositoriesFlux) {
        return repositoriesFlux.flatMap(gitHubRepository -> repositoriesService
                .createReactive(gitHubRepository)
                .onErrorResume(e -> {
                    // Ideally use a logger here, e.g., log.error(...)
                    log.error("Error saving repo: " + e.getMessage());
                    return reactor.core.publisher.Mono.empty();
                }));
    }
}
