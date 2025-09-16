package me.sathish.my_github_cleaner.base.repositories;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('VIEWER')")
public class RepositoriesResource {

    private final RepositoriesService repositoriesService;

    public RepositoriesResource(final RepositoriesService repositoriesService) {
        this.repositoriesService = repositoriesService;
    }

    @GetMapping
    public ResponseEntity<Page<RepositoriesDTO>> getAllRepositoriess(
            @RequestParam(name = "filter", required = false) final String filter,
            @SortDefault(sort = "id") @PageableDefault(size = 50) final Pageable pageable) {
        return ResponseEntity.ok(repositoriesService.findAll(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoriesDTO> getRepositories(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(repositoriesService.get(id));
    }

    @PostMapping
    public ResponseEntity<Long> createRepositories(@RequestBody @Valid final RepositoriesDTO repositoriesDTO) {
        final Long createdId = repositoriesService.create(repositoriesDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateRepositories(
            @PathVariable(name = "id") final Long id, @RequestBody @Valid final RepositoriesDTO repositoriesDTO) {
        repositoriesService.update(id, repositoriesDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepositories(@PathVariable(name = "id") final Long id) {
        repositoriesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
