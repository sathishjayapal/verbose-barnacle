package me.sathish.my_github_cleaner.base.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sathishprojects")
public record RabbitConfigProperties(String sathishProjectEventsExchange, String githubRoutingKey) {

}
