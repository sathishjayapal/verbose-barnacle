package me.sathish.my_github_cleaner.base;

import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import me.sathish.my_github_cleaner.base.github.GitHubService;
import me.sathish.my_github_cleaner.base.github.GitHubServiceConstants;
import me.sathish.my_github_cleaner.base.repositories.GitHubRepository;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GitHubDataRunnerEventServiceTest implements GitHubServiceConstants {

    @Mock
    private GitHubService gitHubService;

    @Mock
    private RepositoriesService repositoriesService;

    @Mock
    private EventTrackerService eventTrackerService;

    @Mock
    private Environment environment;

    private GitHubDataRunner gitHubDataRunner;

    @BeforeEach
    void setUp() {
        lenient().when(environment.getProperty(eq(GITHUB_USERNAME_KEY))).thenReturn("testuser");
        lenient()
                .when(environment.getProperty(eq("sathishlogger.enabled"), eq("true")))
                .thenReturn("false");
        lenient()
                .when(environment.getProperty(eq("sathishlogger.url"), anyString()))
                .thenReturn("http://localhost:8080");
        lenient()
                .when(environment.getProperty(eq("sathishlogger.application-name"), anyString()))
                .thenReturn("test-app");
        gitHubDataRunner = new GitHubDataRunner(gitHubService, repositoriesService, eventTrackerService, environment);
    }

    @Test
    void scheduledInterval_isTwentyFourHours() throws NoSuchMethodException {
        Method method = GitHubDataRunner.class.getDeclaredMethod("findMissingRepositories");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertTrue(scheduled != null, "findMissingRepositories should be annotated with @Scheduled");
        assertEquals(86400000L, scheduled.fixedDelay(), "Scheduled interval should be 24 hours in milliseconds");
    }

    @Test
    void run_existingDbNoNewRepos_invokesEventTracker() {
        List<GitHubRepository> githubRepos = List.of(createRepository("repo1"));
        when(gitHubService.fetchAllPublicRepositoriesForUser("testuser")).thenReturn(githubRepos);
        when(repositoriesService.findAllRepoNames()).thenReturn(List.of("repo1"));
        when(repositoriesService.countByRecords()).thenReturn(Boolean.TRUE);

        gitHubDataRunner.run();

        verify(eventTrackerService).sendGitHubEventToEventstracker(anyString());
    }

    @Test
    void run_emptyDbWithNewRepos_invokesEventTracker() {
        GitHubRepository repo = createRepository("repo1");
        when(gitHubService.fetchAllPublicRepositoriesForUser("testuser")).thenReturn(List.of(repo));
        when(repositoriesService.findAllRepoNames()).thenReturn(Collections.emptyList());
        when(gitHubService.getAuthenticatedUserRepository("repo1")).thenReturn(Optional.of(repo));
        when(repositoriesService.createReactive(any(GitHubRepository.class))).thenReturn(Mono.just(1L));

        gitHubDataRunner.run();

        verify(repositoriesService).createReactive(any(GitHubRepository.class));
        verify(eventTrackerService).sendGitHubEventToEventstracker(anyString());
    }

    @Test
    void run_noReposAnywhere_invokesEventTracker() {
        when(gitHubService.fetchAllPublicRepositoriesForUser("testuser")).thenReturn(Collections.emptyList());
        when(repositoriesService.findAllRepoNames()).thenReturn(Collections.emptyList());
        when(repositoriesService.countByRecords()).thenReturn(Boolean.FALSE);

        gitHubDataRunner.run();

        verify(eventTrackerService).sendGitHubEventToEventstracker(anyString());
    }

    private GitHubRepository createRepository(String name) {
        GitHubRepository repository = new GitHubRepository();
        repository.setName(name);
        repository.setCloneUrl("https://github.com/testuser/" + name + ".git");
        return repository;
    }
}
