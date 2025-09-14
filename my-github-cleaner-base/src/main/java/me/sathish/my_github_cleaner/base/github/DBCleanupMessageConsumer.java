package me.sathish.my_github_cleaner.base.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DBCleanupMessageConsumer {
    private final ObjectMapper objectMapper;
    private final RepositoriesRepository repositoriesRepository;

    public DBCleanupMessageConsumer(ObjectMapper objectMapper, RepositoriesRepository repositoriesRepository) {
        this.objectMapper = objectMapper;
        this.repositoriesRepository = repositoriesRepository;
    }

    @RabbitListener(queues = "${garminrun-event.garmin-newrun-queue}")
    @Transactional
    public void consumeMessage(String message) {
        try {
            EventMessage eventMessage = objectMapper.readValue(message, EventMessage.class);
            log.info("Received message - EventId: {}, Type: {}, Domain: {}", 
                    eventMessage.getEventId(), 
                    eventMessage.getEventType(),
                    eventMessage.getDomain());
            repositoriesRepository.deleteById(eventMessage.getDomain());
        } catch (Exception e) {
            log.error("Error deserializing message: {}", message, e);
        }
    }

    private String extractJsonFromPayload(String payload) {
        // Extract the JSON part after "Failed to delete repository" or other prefix
        int jsonStart = payload.indexOf("{");
        return payload.substring(jsonStart);
    }
}

@Data
class EventMessage {
    private Long id;
    private String eventId;
    private String eventType;
    private String payload;
    private String createdBy;
    private String updatedBy;
    private Long domain;
}
