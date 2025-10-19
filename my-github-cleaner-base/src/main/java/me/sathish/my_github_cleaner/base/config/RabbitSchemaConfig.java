package me.sathish.my_github_cleaner.base.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.Map;

@Configuration
@Slf4j
public class RabbitSchemaConfig {
    /**
     * Dead letter exchange for all GitHub-related queues.
     * @return
     */
    @Bean
    FanoutExchange sathishProjectsFanoutExchange() { return new FanoutExchange("x.sathishprojects.fanout");}
    @Bean
    TopicExchange sathishProjectsDlxExchange() { return new TopicExchange("x.sathishprojects.dlx.exchange");}
    @Bean
    Queue getSathishProjectsEventsQueue() {
        return QueueBuilder.durable("q.sathishprojects.events")
                .withArgument("x-dead-letter-exchange", "x.sathishprojects.dlx.exchange")
                .withArgument("x-message-ttl", 10000) // 10 seconds TTL
                .build();
    }

    @Bean
    Queue getDlqSathishProjectsEventsQueue() {
        log.error("Creating DLQ Queue: dlq.sathishprojects.events");
        return new Queue("dlq.sathishprojects.events");
    }
    @Bean
    Binding getDlqSathishProjectsEventsBinding() {
        return BindingBuilder
                .bind(getDlqSathishProjectsEventsQueue())
                .to(sathishProjectsDlxExchange())
                .with("#");
    }

    @Bean
    Binding getSathishProjectsEventsBinding() {
        return BindingBuilder
                .bind(getSathishProjectsEventsQueue())
                .to(sathishProjectsFanoutExchange());
    }

    @Bean
    TopicExchange gitHubEventsDLXExchange() { return new TopicExchange("x.sathishprojects.github.events.dlx.exchange");}

    @Bean
    Queue getGitHubAPIEventDLQ() {
        return new Queue("dlq.sathishprojects.github.api.events");
    }

    @Bean
    Binding getGitHubAPIEventsDLXExchange() {
        return BindingBuilder
                .bind(getGitHubAPIEventDLQ())
                .to(gitHubEventsDLXExchange())
                .with("sathishprojects.github.api.*");
    }

    @Bean
    Queue getGitHubOPSEventDLQ() {
        return new Queue("dlq.sathishprojects.github.ops.events");
    }

    @Bean
    Binding getGitHubNonAPIEventsDLXExchange() {
        return BindingBuilder
                .bind(getGitHubOPSEventDLQ())
                .to(gitHubEventsDLXExchange())
                .with("sathishprojects.github.ops.*");
    }

    @Bean
    TopicExchange gitHubEventsExchange() {
        return new TopicExchange("x.sathishprojects.github.events.exchange");
    }
    @Bean
    Binding getSathishProjectsGitHubEventsBinding() {
        return BindingBuilder
                .bind(gitHubEventsExchange())
                .to(sathishProjectsFanoutExchange());
    }

    @Bean
    Queue getGitHubOPSEventsQueue() {
        return QueueBuilder.durable("q.sathishprojects.github.ops.events")
                .withArgument("x-dead-letter-exchange", "x.sathishprojects.github.events.dlx.exchange")
                .withArgument("x-message-ttl", 10000) // 10 seconds TTL
                .withArgument("x-dead-letter-routing-key", "sathishprojects.github.ops.*")
                .build();
    }

    @Bean
    Queue getGitHubAPIEventsQueue() {
        return QueueBuilder.durable("q.sathishprojects.github.api.events")
                .withArgument("x-dead-letter-exchange", "x.sathishprojects.github.events.dlx.exchange")
                .withArgument("x-message-ttl", 10000) // 10 seconds TTL
                .withArgument("x-dead-letter-routing-key", "sathishprojects.github.api.*")
                .build();
    }
    @Bean
    Binding BindSathishProjectsAPIEventsQ() {
        return BindingBuilder
                .bind(getGitHubAPIEventsQueue())
                .to(gitHubEventsExchange())
                .with("sathishprojects.github.api.*");
    }
    @Bean
    Binding BindSathishProjectsOPSEventsQ() {
        return BindingBuilder
                .bind(getGitHubOPSEventsQueue())
                .to(gitHubEventsExchange())
                .with("sathishprojects.github.ops.*");
    }

}
