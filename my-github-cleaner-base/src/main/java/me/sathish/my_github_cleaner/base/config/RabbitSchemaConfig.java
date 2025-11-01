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
        FanoutExchange fanoutExchange = new FanoutExchange("${fanout_exchange}");
        TopicExchange dlxExchange = new TopicExchange("${dlx_exchange}");
        TopicExchange gitHubEventsExchange = new TopicExchange("${github_events_exchange}");
        TopicExchange gitHubEventsDlxExchange = new TopicExchange("${github_events_dlx_exchange}");

        // Queues
        Queue satProjectsEventsQueue = QueueBuilder.durable("${sat_projects_events_queue}")
                .withArgument("x-dead-letter-exchange", "${dlx_exchange}")
                .withArgument("x-message-ttl", "${message_ttl_ms}")
                .build();

        Queue dlqSatProjectsEventsQueue =
                QueueBuilder.durable("${dlq_sat_projects_events_queue}").build();
        Queue dlqGitHubApiEventsQueue =
                QueueBuilder.durable("${dlq_github_api_events_queue}").build();
        Queue dlqGitHubOpsEventsQueue =
                QueueBuilder.durable("${dlq_github_ops_events_queue}").build();

        Queue gitHubApiEventsQueue = QueueBuilder.durable("${github_api_events_queue}")
                .withArgument("x-dead-letter-exchange", "${github_events_dlx_exchange}")
                .withArgument("x-message-ttl", "${message_ttl_ms}")
                .withArgument("x-dead-letter-routing-key", "${github_api_routing_key}")
                .build();

        Queue gitHubOpsEventsQueue = QueueBuilder.durable("${github_ops_events_queue}")
                .withArgument("x-dead-letter-exchange", "${github_events_dlx_exchange}")
                .withArgument("x-message-ttl", "${message_ttl_ms}")
                .withArgument("x-dead-letter-routing-key", "${github_ops_routing_key}")
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
                BindingBuilder.bind(gitHubApiEventsQueue)
                        .to(gitHubEventsExchange)
                        .with("${github_api_routing_key}"),
                BindingBuilder.bind(gitHubOpsEventsQueue)
                        .to(gitHubEventsExchange)
                        .with("${github_ops_routing_key}"),
                BindingBuilder.bind(dlqGitHubApiEventsQueue)
                        .to(gitHubEventsDlxExchange)
                        .with("${github_api_routing_key}"),
                BindingBuilder.bind(dlqGitHubOpsEventsQueue)
                        .to(gitHubEventsDlxExchange)
                        .with("${github_ops_routing_key}"),
                BindingBuilder.bind(gitHubEventsExchange).to(fanoutExchange));
    }
}
