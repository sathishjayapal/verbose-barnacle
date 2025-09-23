package me.sathish.my_github_cleaner.base.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
import me.sathish.my_github_cleaner.base.repositories.RepositoriesRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DBCleanupMessageConsumer {
    private final EventTrackerService eventTrackerService;
    private final ObjectMapper objectMapper;
    private final RepositoriesRepository repositoriesRepository;

    public DBCleanupMessageConsumer(
            EventTrackerService eventTrackerService,
            ObjectMapper objectMapper,
            RepositoriesRepository repositoriesRepository) {
        this.eventTrackerService = eventTrackerService;
        this.objectMapper = objectMapper;
        this.repositoriesRepository = repositoriesRepository;
    }
    /*
    {"id":null,"eventId":"a07968f2-4bbf-42eb-8853-0b09b18ee5e8","eventType":"GITHUB_REPOSITORY_PROJECT","payload":"Failed to delete repository
    {\"Repo Record ID\":\"10007\",\"repositoryName\":\"gjhj\",\"deletedAt\":\"2025-09-14T08:52:25.167003\",\"deletedBy\":\"sathishjayapal\"}","createdBy":"sathishjayapal","updatedBy":"sathishjayapal","domain":10093}
     */
    @RabbitListener(queues = "${sathishprojects.github_operations_queue}")
    @Transactional
    public void consumeMessage(String message) {
        try {
            EventMessage eventMessage = objectMapper.readValue(message, EventMessage.class);
            log.info(
                    "Received message - EventId: {}, Type: {}, Domain: {}",
                    eventMessage.getEventId(),
                    eventMessage.getEventType(),
                    eventMessage.getDomain());

            if (eventMessage.getEventType().equals("GITHUB_REPOSITORY_PROJECT")) {
                RepositoryPayload payload = objectMapper.readValue(
                        extractJsonFromPayload(eventMessage.getPayload()), RepositoryPayload.class);
                log.info(
                        "Repository deletion details - Name: {}, Deleted by: {}, at: {}",
                        payload.getRepositoryName(),
                        payload.getDeletedBy(),
                        payload.getDeletedAt());
                repositoriesRepository.deleteById(Long.parseLong(payload.getRepoRecordId()));
                String eventPayload = createEventPayload(payload);
                eventTrackerService.sendGitHubEventToEventstracker(eventPayload);
            }
        } catch (Exception e) {
            log.error("Error deserializing message: {}", message, e);
        }
    }

    private String createEventPayload(RepositoryPayload payload) {
        String repoRecordID = payload.getRepoRecordId();
        String status = "Repository deleted successfully from DB!";
        return String.format(
                "%s{\"repoRecordId\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                status, repoRecordID, LocalDateTime.now(), payload.getDeletedBy());
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

@Data
class RepositoryPayload {
    private String repoRecordId;
    private String repositoryName;
    private String deletedAt;
    private String deletedBy;
}
