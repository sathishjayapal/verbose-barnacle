package me.sathish.my_github_cleaner.base;

import me.sathish.my_github_cleaner.base.util.RabbitConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(RabbitConfigProperties.class)
@ComponentScan("me.sathish.my_github_cleaner")
public class MyGithubCleanerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(MyGithubCleanerApplication.class, args);
    }
}
