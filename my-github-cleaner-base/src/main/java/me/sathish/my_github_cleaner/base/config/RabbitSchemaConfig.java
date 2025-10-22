package me.sathish.my_github_cleaner.base.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitSchemaConfig {
    // Exchange names
    public static final String FANOUT_EXCHANGE = "x.sathishprojects.fanout";
    public static final String DLX_EXCHANGE = "x.sathishprojects.dlx.exchange";
    public static final String GITHUB_EVENTS_EXCHANGE = "x.sathishprojects.github.events.exchange";
    public static final String GITHUB_EVENTS_DLX_EXCHANGE = "x.sathishprojects.github.events.dlx.exchange";

    // Queue names
    public static final String SAT_PROJECTS_EVENTS_QUEUE = "q.sathishprojects.events";
    public static final String DLQ_SAT_PROJECTS_EVENTS_QUEUE = "dlq.sathishprojects.events";
    public static final String GITHUB_API_EVENTS_QUEUE = "q.sathishprojects.github.api.events";
    public static final String GITHUB_OPS_EVENTS_QUEUE = "q.sathishprojects.github.ops.events";
    public static final String DLQ_GITHUB_API_EVENTS_QUEUE = "dlq.sathishprojects.github.api.events";
    public static final String DLQ_GITHUB_OPS_EVENTS_QUEUE = "dlq.sathishprojects.github.ops.events";

    // Routing keys
    public static final String GITHUB_API_ROUTING_KEY = "sathishprojects.github.api.*";
    public static final String GITHUB_OPS_ROUTING_KEY = "sathishprojects.github.ops.*";
    public static final int MESSAGE_TTL_MS = 10000; // 10 seconds

    @Bean
    public Declarables declarables() {
        // Exchanges
        FanoutExchange fanoutExchange = new FanoutExchange(FANOUT_EXCHANGE);
        TopicExchange dlxExchange = new TopicExchange(DLX_EXCHANGE);
        TopicExchange gitHubEventsExchange = new TopicExchange(GITHUB_EVENTS_EXCHANGE);
        TopicExchange gitHubEventsDlxExchange = new TopicExchange(GITHUB_EVENTS_DLX_EXCHANGE);

        // Queues
        Queue satProjectsEventsQueue = QueueBuilder.durable(SAT_PROJECTS_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-message-ttl", MESSAGE_TTL_MS)
                .build();

        Queue dlqSatProjectsEventsQueue = QueueBuilder.durable(DLQ_SAT_PROJECTS_EVENTS_QUEUE).build();
        Queue dlqGitHubApiEventsQueue = QueueBuilder.durable(DLQ_GITHUB_API_EVENTS_QUEUE).build();
        Queue dlqGitHubOpsEventsQueue = QueueBuilder.durable(DLQ_GITHUB_OPS_EVENTS_QUEUE).build();

        Queue gitHubApiEventsQueue = QueueBuilder.durable(GITHUB_API_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITHUB_EVENTS_DLX_EXCHANGE)
                .withArgument("x-message-ttl", MESSAGE_TTL_MS)
                .withArgument("x-dead-letter-routing-key", GITHUB_API_ROUTING_KEY)
                .build();

        Queue gitHubOpsEventsQueue = QueueBuilder.durable(GITHUB_OPS_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITHUB_EVENTS_DLX_EXCHANGE)
                .withArgument("x-message-ttl", MESSAGE_TTL_MS)
                .withArgument("x-dead-letter-routing-key", GITHUB_OPS_ROUTING_KEY)
                .build();

        // Bindings
        return new Declarables(
                // Exchanges
                fanoutExchange,
                dlxExchange,
                gitHubEventsExchange,
                gitHubEventsDlxExchange,

                // Queues
                satProjectsEventsQueue,
                dlqSatProjectsEventsQueue,
                gitHubApiEventsQueue,
                gitHubOpsEventsQueue,
                dlqGitHubApiEventsQueue,
                dlqGitHubOpsEventsQueue,

                // Bindings
                BindingBuilder.bind(satProjectsEventsQueue).to(fanoutExchange),
                BindingBuilder.bind(dlqSatProjectsEventsQueue).to(dlxExchange).with("#"),
                BindingBuilder.bind(gitHubApiEventsQueue).to(gitHubEventsExchange).with(GITHUB_API_ROUTING_KEY),
                BindingBuilder.bind(gitHubOpsEventsQueue).to(gitHubEventsExchange).with(GITHUB_OPS_ROUTING_KEY),
                BindingBuilder.bind(dlqGitHubApiEventsQueue).to(gitHubEventsDlxExchange).with(GITHUB_API_ROUTING_KEY),
                BindingBuilder.bind(dlqGitHubOpsEventsQueue).to(gitHubEventsDlxExchange).with(GITHUB_OPS_ROUTING_KEY),
                BindingBuilder.bind(gitHubEventsExchange).to(fanoutExchange)
        );
    }
}
