package me.sathish.my_github_cleaner.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication

@ComponentScan("me.sathish.my_github_cleaner")
public class MyGithubCleanerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(MyGithubCleanerApplication.class, args);
    }

}
