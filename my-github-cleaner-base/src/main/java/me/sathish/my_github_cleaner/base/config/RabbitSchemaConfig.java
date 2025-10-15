package me.sathish.my_github_cleaner.base.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.Map;

@Configuration
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
    TopicExchange gitHubEventsExchange() {
        return new TopicExchange("x.sathishprojects.github.events.exchange");
    }
    @Bean
    TopicExchange gitHubEventsDLXExchange() { return new TopicExchange("x.sathishprojects.github.events.dlx.exchange");}

    @Bean
    Queue getDlqSathishProjectsEventsQueue() {
        return new Queue("dlq.sathishprojects.events");
    }
    @Bean
    Binding getDlqSathishProjectsEventsBinding(TopicExchange gitHubEventsDLXExchange, Queue getDlqSathishProjectsEventsQueue) {
        return BindingBuilder
                .bind(getDlqSathishProjectsEventsQueue)
                .to(gitHubEventsDLXExchange)
                .with("#");
    }
    @Bean
    Queue getEventsSathishProjectsQueue() {
        return new Queue("q.sathishprojects Events");
    }

    @Bean
    Binding getDlqSathishProjectsEventsBinding(FanoutExchange sathishProjectsFanoutExchange, Queue getEventsSathishProjectsQueue) {
        return BindingBuilder
                .bind(getEventsSathishProjectsQueue)
                .to(sathishProjectsFanoutExchange);
    }

    @Bean
    Queue gitHubDeadLetterQueue() {
        return new Queue("sathishprojects.github.dlq.queue");
    }
    @Bean
    Binding gitHubDeadLetterQueueBinder(TopicExchange dlxTopicExchange, Queue gitHubDeadLetterQueue) {
        return BindingBuilder
                .bind(gitHubDeadLetterQueue)
                .to(dlxTopicExchange)
                .with("sathishprojects.github.#"); // Add routing key pattern for all GitHub-related dead letters
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("sathishprojects.exchange");
    }


    /**
     * GitHub-related queues.
     * @return
     */
    @Bean
    Queue gitHubOperatationsQueue() {
        Map<String, Object> args = Map.of(
                "x-dead-letter-exchange", "sathishprojects.dlx.exchange"
        );
        return new Queue("sathishprojects.github.operations.queue", true, false,
                true, args);
    }
    @Bean
    Queue gitHubCommunicationsQueue() {
        Map<String, Object> args = Map.of(
                "x-dead-letter-exchange", "sathishprojects.dlx.exchange"
        );
        return new Queue("sathishprojects.github.communications.queue");
    }



    @Bean
    Binding gitHubOperationsBinding(TopicExchange topicExchange, Queue gitHubOperatationsQueue) {
        return BindingBuilder.bind(gitHubOperatationsQueue).to(topicExchange).with("sathishprojects.github.operations");
    }

    @Bean
    Binding gitHubCommunicationsBinding(TopicExchange topicExchange, Queue gitHubCommunicationsQueue) {
        return BindingBuilder.bind(gitHubCommunicationsQueue).to(topicExchange).with("sathishprojects.github.communications");
    }




}
