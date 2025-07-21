package me.sathish.my_github_cleaner.base.repositories;

import me.sathish.my_github_cleaner.base.util.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

@Service
public class RepositoriesService {

    private final RepositoriesRepository repositoriesRepository;

    public RepositoriesService(final RepositoriesRepository repositoriesRepository) {
        this.repositoriesRepository = repositoriesRepository;
    }
    public Boolean countByRecords() {
        return repositoriesRepository.count() > 0;
    }
    public Long count() {
        return repositoriesRepository.count();
    }
        public Mono<Long> createReactive(GitHubRepository gitHubRepository) {
        RepositoriesDTO repositoriesDTO = new RepositoriesDTO();
        repositoriesDTO.setRepoName(gitHubRepository.getName());
        repositoriesDTO.setRepoCreatedDate(gitHubRepository.getCreatedAt());
        repositoriesDTO.setRepoUpdatedDate(gitHubRepository.getUpdatedAt());
        repositoriesDTO.setCloneUrl(gitHubRepository.getCloneUrl());
        repositoriesDTO.setDescription(Optional.ofNullable(gitHubRepository.getDescription()).orElse("No Description Available"));

        return Mono.fromCallable(() -> {
                    final var repositories = new Repositories();
                    mapToEntity(repositoriesDTO, repositories);
                    return repositoriesRepository.save(repositories).getId();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Page<RepositoriesDTO> findAll(final String filter, final Pageable pageable) {

        Page<Repositories> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = repositoriesRepository.findAllById(longFilter, pageable);
        } else {
            page = repositoriesRepository.findAll(pageable);
        }
        return new PageImpl<>(
                page.getContent().stream()
                        .map(repositories -> mapToDTO(repositories, new RepositoriesDTO()))
                        .toList(),
                pageable,
                page.getTotalElements());
    }

    public List<String> findAllRepoNames() { // New method
        return repositoriesRepository.findAllRepoNames();
    }

    public RepositoriesDTO get(final Long id) {
        return repositoriesRepository
                .findById(id)
                .map(repositories -> mapToDTO(repositories, new RepositoriesDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final RepositoriesDTO repositoriesDTO) {
        final Repositories repositories = new Repositories();
        mapToEntity(repositoriesDTO, repositories);
        return repositoriesRepository.save(repositories).getId();
    }

    public void update(final Long id, final RepositoriesDTO repositoriesDTO) {
        final Repositories repositories = repositoriesRepository.findById(id).orElseThrow(NotFoundException::new);
        mapToEntity(repositoriesDTO, repositories);
        repositoriesRepository.save(repositories);
    }

    public void delete(final Long id) {
        repositoriesRepository.deleteById(id);
    }

    private RepositoriesDTO mapToDTO(final Repositories repositories, final RepositoriesDTO repositoriesDTO) {
        repositoriesDTO.setId(repositories.getId());
        repositoriesDTO.setRepoName(repositories.getRepoName());
        repositoriesDTO.setRepoCreatedDate(repositories.getRepoCreatedDate());
        repositoriesDTO.setRepoUpdatedDate(repositories.getRepoUpdatedDate());

        repositoriesDTO.setCloneUrl(repositories.getCloneUrl());
        repositoriesDTO.setDescription(repositories.getDescription());
        return repositoriesDTO;
    }

    private Repositories mapToEntity(final RepositoriesDTO repositoriesDTO, final Repositories repositories) {
        repositories.setRepoName(repositoriesDTO.getRepoName());
        repositories.setRepoCreatedDate(repositoriesDTO.getRepoCreatedDate());
        repositories.setRepoUpdatedDate(repositoriesDTO.getRepoUpdatedDate());
        repositories.setCloneUrl(repositoriesDTO.getCloneUrl());
        repositories.setDescription(Optional.ofNullable(repositoriesDTO.getDescription()).orElse("No Description Available"));
        return repositories;
    }

    public boolean repoNameExists(final String repoName) {
        return repositoriesRepository.existsByRepoName(repoName);
    }
}
