package me.sathish.my_github_cleaner.base;

import me.sathish.my_github_cleaner.base.repositories.GitHubService;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesDTO;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class GitHubDataRunner implements CommandLineRunner {
    private final GitHubService gitHubService;
    private final RepositoriesService repositoriesService;
    public GitHubDataRunner(GitHubService gitHubService, RepositoriesService repositoriesService) {
        this.gitHubService = gitHubService;
        this.repositoriesService = repositoriesService;
    }

    @Override
    public void run(String... args) throws Exception {
        gitHubService.getAllUserRepositoriesPaginated("sathishjayapal").subscribe(
            repository -> {
                RepositoriesDTO repositoriesDTO = new RepositoriesDTO();
                repositoriesDTO.setRepoName(repository.getName());
                repositoriesDTO.setRepoCreatedDate(repository.getCreatedAt());
                repositoriesDTO.setRepoUpdatedDate(repository.getUpdatedAt());
                repositoriesDTO.setDescription(repository.getDescription());
                repositoriesDTO.setCloneUrl(repository.getCloneUrl());
//                repositoriesService.create(repositoriesDTO);
            }, error -> System.err.println("Error fetching repositories: " + error.getMessage()),
            () -> System.out.println("Completed fetching repositories")
        );
    }
}
