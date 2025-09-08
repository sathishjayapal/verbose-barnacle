package me.sathish.my_github_cleaner.base.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class DBCleanupMessageConsumer {
    private ObjectMapper objectMapper;
    public DBCleanupMessageConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    //TODO 982025 change here.
    @RabbitListener(queues = "${garminrun-event.garmin-newrun-queue}")
    public void consumeMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
