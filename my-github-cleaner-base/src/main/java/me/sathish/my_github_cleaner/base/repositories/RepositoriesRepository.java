package me.sathish.my_github_cleaner.base.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositoriesRepository extends JpaRepository<Repositories, Long> {

    Page<Repositories> findAllById(Long id, Pageable pageable);

    boolean existsByRepoName(String repoName);

    @Query("SELECT r.repoName FROM Repositories r")
    List<String> findAllRepoNames();
}
