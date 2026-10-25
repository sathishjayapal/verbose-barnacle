package me.sathish.my_github_cleaner.base.repositories;

import static me.sathish.my_github_cleaner.base.github.GitHubServiceConstants.GITHUB_REPOSITORY_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import me.sathish.my_github_cleaner.base.github.GitHubDeleter;
import me.sathish.my_github_cleaner.base.github.GitHubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
public class RepositoriesServiceTest {

    @Mock
    private RepositoriesRepository repositoriesRepository;

    @Mock
    private GitHubDeleter gitHubDeleter;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private EventTrackerService eventTrackerService;

    @InjectMocks
    private RepositoriesService repositoriesService;

    @Test
    void update_pushesDescriptionToGitHub() {
        Long id = 1L;
        Repositories existing = createRepository(id, "myrepo", "old description");
        RepositoriesDTO dto = createRepositoryDTO("myrepo", "new description");

        when(repositoriesRepository.findById(id)).thenReturn(Optional.of(existing));
        when(repositoriesRepository.save(any(Repositories.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gitHubService.updateRepository("myrepo", "new description")).thenReturn(true);

        repositoriesService.update(id, dto);

        assertEquals("new description", existing.getDescription());
        verify(gitHubService).updateRepository("myrepo", "new description");
        verify(eventTrackerService).sendGitHubEventToEventstracker(any(String.class), eq(GITHUB_REPOSITORY_UPDATED));
    }

    @Test
    void update_gitHubFailure_stillSavesAndSendsEvent() {
        Long id = 1L;
        Repositories existing = createRepository(id, "myrepo", "old description");
        RepositoriesDTO dto = createRepositoryDTO("myrepo", "new description");

        when(repositoriesRepository.findById(id)).thenReturn(Optional.of(existing));
        when(repositoriesRepository.save(any(Repositories.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gitHubService.updateRepository("myrepo", "new description")).thenReturn(false);

        repositoriesService.update(id, dto);

        assertEquals("new description", existing.getDescription());
        verify(gitHubService).updateRepository("myrepo", "new description");
        verify(eventTrackerService).sendGitHubEventToEventstracker(any(String.class), eq(GITHUB_REPOSITORY_UPDATED));
    }

    @Test
    void update_gitHubException_stillSavesAndSendsEvent() {
        Long id = 1L;
        Repositories existing = createRepository(id, "myrepo", "old description");
        RepositoriesDTO dto = createRepositoryDTO("myrepo", "new description");

        when(repositoriesRepository.findById(id)).thenReturn(Optional.of(existing));
        when(repositoriesRepository.save(any(Repositories.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gitHubService.updateRepository("myrepo", "new description"))
                .thenThrow(new RuntimeException("GitHub token is missing"));

        repositoriesService.update(id, dto);

        assertEquals("new description", existing.getDescription());
        verify(gitHubService).updateRepository("myrepo", "new description");
        verify(eventTrackerService).sendGitHubEventToEventstracker(any(String.class), eq(GITHUB_REPOSITORY_UPDATED));
    }

    @Test
    void update_allEditFormFields_persistedCorrectly() {
        Long id = 10000L;
        LocalDateTime createdDate = LocalDateTime.of(2022, 7, 5, 2, 12, 55);
        LocalDateTime updatedDate = LocalDateTime.of(2022, 7, 5, 2, 13, 28);
        String cloneUrl = "https://github.com/sathishjayapal/azuredeploy.git";

        Repositories existing = createRepository(id, "azuredeploy", "old description");
        existing.setRepoCreatedDate(createdDate);
        existing.setRepoUpdatedDate(updatedDate);
        existing.setCloneUrl(cloneUrl);

        RepositoriesDTO dto = new RepositoriesDTO();
        dto.setRepoName("azuredeploy");
        dto.setRepoCreatedDate(createdDate);
        dto.setRepoUpdatedDate(updatedDate);
        dto.setCloneUrl(cloneUrl);
        dto.setDescription("Cleanup for desc v2");

        when(repositoriesRepository.findById(id)).thenReturn(Optional.of(existing));
        ArgumentCaptor<Repositories> savedCaptor = ArgumentCaptor.forClass(Repositories.class);
        when(repositoriesRepository.save(savedCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gitHubService.updateRepository("azuredeploy", "Cleanup for desc v2"))
                .thenReturn(true);

        repositoriesService.update(id, dto);

        Repositories saved = savedCaptor.getValue();
        assertEquals("azuredeploy", saved.getRepoName());
        assertEquals(createdDate, saved.getRepoCreatedDate());
        assertEquals(updatedDate, saved.getRepoUpdatedDate());
        assertEquals(cloneUrl, saved.getCloneUrl());
        assertEquals("Cleanup for desc v2", saved.getDescription());
        verify(gitHubService).updateRepository("azuredeploy", "Cleanup for desc v2");
        verify(eventTrackerService).sendGitHubEventToEventstracker(any(String.class), eq(GITHUB_REPOSITORY_UPDATED));
    }

    @Test
    void findAll_noFilter_usesFindAllWithPageable() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("repoName").ascending());
        Page<Repositories> repoPage = new PageImpl<>(List.of(createRepository(1L, "myrepo", "desc")));
        when(repositoriesRepository.findAll(pageable)).thenReturn(repoPage);

        Page<RepositoriesDTO> result = repositoriesService.findAll(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(repositoriesRepository).findAll(pageable);
    }

    @Test
    void findAll_textFilter_usesNameOrIdQuery() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("repoName").ascending());
        Page<Repositories> repoPage = new PageImpl<>(List.of(createRepository(1L, "verbose-parakeet", "desc")));
        when(repositoriesRepository.findAllByIdOrRepoNameContainingIgnoreCase(null, "verbose", pageable))
                .thenReturn(repoPage);

        Page<RepositoriesDTO> result = repositoriesService.findAll("verbose", pageable);

        assertEquals(1, result.getTotalElements());
        verify(repositoriesRepository).findAllByIdOrRepoNameContainingIgnoreCase(null, "verbose", pageable);
    }

    @Test
    void findAll_numericFilter_matchesId() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<Repositories> repoPage = new PageImpl<>(List.of(createRepository(10000L, "myrepo", "desc")));
        when(repositoriesRepository.findAllByIdOrRepoNameContainingIgnoreCase(10000L, "10000", pageable))
                .thenReturn(repoPage);

        Page<RepositoriesDTO> result = repositoriesService.findAll("10000", pageable);

        assertEquals(1, result.getTotalElements());
        verify(repositoriesRepository).findAllByIdOrRepoNameContainingIgnoreCase(10000L, "10000", pageable);
    }

    private Repositories createRepository(Long id, String repoName, String description) {
        Repositories repositories = new Repositories();
        repositories.setId(id);
        repositories.setRepoName(repoName);
        repositories.setDescription(description);
        repositories.setRepoCreatedDate(LocalDateTime.now());
        repositories.setRepoUpdatedDate(LocalDateTime.now());
        repositories.setCloneUrl("https://github.com/test/" + repoName + ".git");
        return repositories;
    }

    private RepositoriesDTO createRepositoryDTO(String repoName, String description) {
        RepositoriesDTO dto = new RepositoriesDTO();
        dto.setRepoName(repoName);
        dto.setDescription(description);
        dto.setRepoCreatedDate(LocalDateTime.now());
        dto.setRepoUpdatedDate(LocalDateTime.now());
        dto.setCloneUrl("https://github.com/test/" + repoName + ".git");
        return dto;
    }
}
