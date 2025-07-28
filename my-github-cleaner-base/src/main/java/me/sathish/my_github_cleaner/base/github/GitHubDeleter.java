package me.sathish.my_github_cleaner.base.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class GitHubDeleter {
    @Autowired
    Environment environment;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpResponse deleteRepository(String repositoryName) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + environment.getProperty("githubusername") + "/" + repositoryName))
                    .header("Authorization", "token " + environment.getProperty("GITHUB_TOKEN"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                System.out.println("Repository deleted successfully!");
                String payLoad = new StringBuffer("Repository deleted successfully!").append(String.format("{\"repositoryName\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                        repositoryName, LocalDateTime.now(), environment.getProperty("githubusername"))).toString();
                // Send event to Eventstracker after successful deletion
                sendDeletionEventToEventstracker(repositoryName,payLoad);
                
            } else {
                System.out.println("Failed to delete repository. Status: " +
                        response.statusCode());
                String payLoad = new StringBuffer("Failed to delete repository").append(String.format("{\"repositoryName\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                        repositoryName, LocalDateTime.now(), environment.getProperty("githubusername"))).toString();
                sendDeletionEventToEventstracker(repositoryName,payLoad);

                System.out.println("Response: " + response.body());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting repository: " + e.getMessage());
        }
    }

    private void sendDeletionEventToEventstracker(String repositoryName,String payLoad) {
        try {
            // Create event payload
            DomainEventDTO eventDTO = new DomainEventDTO();
            eventDTO.setEventId(UUID.randomUUID().toString());
            eventDTO.setEventType("ACTIVE");
            eventDTO.setPayload(payLoad);
            eventDTO.setCreatedBy(environment.getProperty("githubusername", "system"));
            eventDTO.setUpdatedBy(environment.getProperty("githubusername", "system"));
            eventDTO.setDomain(10024L); // Use GARMIN domain ID (valid existing domain)

            String jsonPayload = objectMapper.writeValueAsString(eventDTO);

            HttpClient client = HttpClient.newHttpClient();
            String eventstrackerUrl = environment.getProperty("eventstracker.url", "http://localhost:9081");

            HttpRequest eventRequest = HttpRequest.newBuilder()
                    .uri(URI.create(eventstrackerUrl + "/api/domainEvents"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes()))
                    // Add authentication if needed
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> eventResponse = client.send(eventRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (eventResponse.statusCode() == 201) {
                System.out.println("Event sent to Eventstracker successfully for repository: " + repositoryName);
            } else {
                System.out.println("Failed to send event to Eventstracker. Status: " + eventResponse.statusCode());
                throw new RuntimeException("Failed to send event to Eventstracker. Status: " + eventResponse.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Error sending event to Eventstracker: " + e.getMessage());
            throw new RuntimeException("Error sending event to Eventstracker: " + e.getMessage());
            // Don't throw exception here to avoid breaking the main deletion flow
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
