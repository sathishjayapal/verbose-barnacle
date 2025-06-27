package me.sathish.my_github_cleaner.base.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RepositoriesRepository extends JpaRepository<Repositories, Long> {

    Page<Repositories> findAllById(Long id, Pageable pageable);

    boolean existsByRepoName(String repoName);

}
