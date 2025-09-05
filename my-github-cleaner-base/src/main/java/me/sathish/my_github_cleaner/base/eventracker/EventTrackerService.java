package me.sathish.my_github_cleaner.base.eventracker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventTrackerService {

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private List<DomainDTO> domainsData;

    @Autowired
    public EventTrackerService(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void fetchDomainsOnStartup() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String eventsTrackerUrl = environment.getProperty("eventstracker.url", "http://localhost:9081");
        String EventServiceUserName = environment.getProperty("eventstracker.username", "system");
        String EventServicePassword = environment.getProperty("eventstracker.password", "system");
        HttpRequest getDomainsRequest = HttpRequest.newBuilder()
                .uri(URI.create(eventsTrackerUrl + "/api/domains"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(
                        "Authorization",
                        "Basic "
                                + Base64.getEncoder()
                                        .encodeToString((EventServiceUserName + ":" + EventServicePassword).getBytes()))
                .GET()
                .build();
        HttpResponse<String> domainsResponse = client.send(getDomainsRequest, HttpResponse.BodyHandlers.ofString());
        if (domainsResponse.statusCode() == 200) {
            domainsData = objectMapper.readValue(domainsResponse.body(), new TypeReference<List<DomainDTO>>() {});
            log.debug("Fetched domains on startup. List length: " + domainsData.size());
        } else {
            System.out.println("Failed to fetch domains on startup. Status: " + domainsResponse.statusCode());
            throw new RuntimeException("Failed to fetch domains on startup. Status: " + domainsResponse.statusCode());
        }
    }

    /**
     * Send an event to the Eventstracker service to log the deletion of the GitHub repository.
     *
     * @param repositoryName The name of the repository that was deleted.
     * @param payLoad        The payload associated with the event.
     */
    public void sendEventToEventstracker(String repositoryName, String payLoad) {
        try {
            DomainEventDTO eventDTO = new DomainEventDTO();
            eventDTO.setEventId(UUID.randomUUID().toString());
            eventDTO.setEventType("GITHUB_REPOSITORY_PROJECT");
            eventDTO.setPayload(payLoad);
            eventDTO.setCreatedBy(environment.getProperty("githubusername", "system"));
            eventDTO.setUpdatedBy(environment.getProperty("githubusername", "system"));

            Optional<DomainDTO> domainOpt = domainsData.stream()
                    .filter(d -> "GITHUB_REPO".equals(d.getDomainName()))
                    .findFirst();
            if (domainOpt.isPresent()) {
                eventDTO.setDomain(domainOpt.get().getId());
            } else {
                log.error("Domain 'GITHUB_REPO' not found in domainsData");
                throw new RuntimeException("Domain 'GITHUB_REPO' not found in domainsData");
            }

            String jsonPayload = objectMapper.writeValueAsString(eventDTO);
            System.out.println("Event Payload: " + jsonPayload);

            HttpClient client = HttpClient.newHttpClient();
            String eventsTrackerUrl = environment.getProperty("eventstracker.url", "http://localhost:9081");
            String EventServiceUserName = environment.getProperty("eventstracker.username", "system");
            String EventServicePassword = environment.getProperty("eventstracker.password", "system");

            HttpRequest eventRequest = HttpRequest.newBuilder()
                    .uri(URI.create(eventsTrackerUrl + "/api/domainEvents"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(
                            "Authorization",
                            "Basic "
                                    + Base64.getEncoder()
                                            .encodeToString(
                                                    (EventServiceUserName + ":" + EventServicePassword).getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send the request and log the response
            HttpResponse<String> eventResponse = client.send(eventRequest, HttpResponse.BodyHandlers.ofString());

            if (eventResponse.statusCode() == 201) {
                log.debug("Event sent to Eventstracker successfully for repository: " + repositoryName);
            } else {
                log.error("Failed to send event to Eventstracker. Status: " + eventResponse.statusCode());
                throw new RuntimeException(
                        "Failed to send event to Eventstracker. Status: " + eventResponse.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error sending event to Eventstracker: " + e.getMessage());
            throw new RuntimeException("Error sending event to Eventstracker: " + e.getMessage());
        }
    }

    @Getter
    @Setter
    public static class DomainDTO {

        private Long id;

        @NotNull private String domainName;

        @NotNull private String status;

        private String comments;
    }

    // Inner class to represent the DomainEventDTO for JSON serialization
    public static class DomainEventDTO {
        private String eventId;
        private String eventType;
        private String payload;
        private String createdBy;
        private String updatedBy;
        private Long domain;

        // Getters and setters
        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }

        public Long getDomain() {
            return domain;
        }

        public void setDomain(Long domain) {
            this.domain = domain;
        }
    }
}
