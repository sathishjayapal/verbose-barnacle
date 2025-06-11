package me.sathish.my_github_cleaner.base.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EntityScan("me.sathish.my_github_cleaner.base")
@EnableJpaRepositories("me.sathish.my_github_cleaner.base")
@EnableTransactionManagement
public class DomainConfig {
}
