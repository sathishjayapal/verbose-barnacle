package me.sathish.my_github_cleaner.base.repositories;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class Repositories {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String repoName;

    @Column(nullable = false)
    private LocalDateTime repoCreatedDate;

    @Column(nullable = false)
    private LocalDateTime repoUpdatedDate;

    @Column(nullable = false, columnDefinition = "text")
    private String cloneUrl;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

}
