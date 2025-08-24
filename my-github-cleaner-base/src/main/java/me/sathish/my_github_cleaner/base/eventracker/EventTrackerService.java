package me.sathish.my_github_cleaner.base.eventracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.UUID;

@Service
public class EventTrackerService {

    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventTrackerService(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    /**
     * Send an event to the Eventstracker service to log the deletion of the GitHub repository.
     *
     * @param repositoryName The name of the repository that was deleted.
     * @param payLoad        The payload associated with the event.
     */
    public void sendEventToEventstracker(String repositoryName, String payLoad) {
        try {
            // Create event payload
            DomainEventDTO eventDTO = new DomainEventDTO();
            eventDTO.setEventId(UUID.randomUUID().toString());
            eventDTO.setEventType("GITHUB_REPOSITORY_DELETED");
            eventDTO.setPayload(payLoad);
            eventDTO.setCreatedBy(environment.getProperty("githubusername", "system"));
            eventDTO.setUpdatedBy(environment.getProperty("githubusername", "system"));
            eventDTO.setDomain(10024L); // Use GARMIN domain ID (valid existing domain)

            String jsonPayload = objectMapper.writeValueAsString(eventDTO);
            System.out.println("Event Payload: " + jsonPayload);
            // Create HTTP client and request
            HttpClient client = HttpClient.newHttpClient();
            String eventstrackerUrl = environment.getProperty("eventstracker.url", "http://localhost:9081");
            System.out.println("Eventstracker URL: " + eventstrackerUrl);
            HttpRequest eventRequest = HttpRequest.newBuilder()
                    .uri(URI.create(eventstrackerUrl + "/api/domainEvents"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send the request and log the response
            HttpResponse<String> eventResponse = client.send(eventRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (eventResponse.statusCode() == 201) {
                System.out.println("Event sent to Eventstracker successfully for repository: " + repositoryName);
            } else {
                System.out.println("Failed to send event to Eventstracker. Status: " + eventResponse.statusCode());
                throw new RuntimeException("Failed to send event to Eventstracker. Status: " + eventResponse.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error sending event to Eventstracker: " + e.getMessage());
            throw new RuntimeException("Error sending event to Eventstracker: " + e.getMessage());
        }
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
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

        public Long getDomain() { return domain; }
        public void setDomain(Long domain) { this.domain = domain; }
    }
}