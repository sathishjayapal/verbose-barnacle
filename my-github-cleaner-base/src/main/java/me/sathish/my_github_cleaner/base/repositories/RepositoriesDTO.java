package me.sathish.my_github_cleaner.base.repositories;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepositoriesDTO {

    private Long id;

    @NotNull @RepositoriesRepoNameUnique
    private String repoName;

    @NotNull private LocalDateTime repoCreatedDate;

    @NotNull private LocalDateTime repoUpdatedDate;

    @NotNull private String cloneUrl;

    @NotNull private String description;
}
