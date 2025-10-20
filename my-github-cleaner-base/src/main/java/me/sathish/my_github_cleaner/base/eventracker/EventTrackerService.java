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
import me.sathish.my_github_cleaner.base.config.RabbitMQConfiguration;
import me.sathish.my_github_cleaner.base.util.RabbitConfigProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventTrackerService {

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final RabbitConfigProperties rabbitConfigProperties;
    private List<DomainDTO> domainsData;
    private final RabbitMQConfiguration rabbitMQConfiguration;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    public EventTrackerService(Environment environment, ObjectMapper objectMapper, RabbitMQConfiguration rabbitMQConfiguration, RabbitTemplate rabbitTemplate, RabbitConfigProperties rabbitConfigProperties) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.rabbitMQConfiguration = rabbitMQConfiguration;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitConfigProperties = rabbitConfigProperties;
    }

    @PostConstruct
    public void fetchDomainsOnStartup() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String eventsTrackerUrl = environment.getProperty("eventstracker_url", "http://localhost:9081");
            String EventServiceUserName = environment.getProperty("eventstracker_username", "system");
            String EventServicePassword = environment.getProperty("eventstracker_password", "system");
            
            // Validate required configuration
            if (eventsTrackerUrl == null || eventsTrackerUrl.trim().isEmpty()) {
                throw new IllegalStateException("EventTracker URL is not configured");
            }
            
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
                log.info("Successfully fetched {} domains on startup", domainsData.size());
            } else {
                String errorMsg = String.format("Failed to fetch domains. HTTP Status: %d, Response: %s", 
                    domainsResponse.statusCode(), domainsResponse.body());
                log.error(errorMsg);
                throw new EventTrackerException(errorMsg);
            }
        } catch (IOException e) {
            String errorMsg = "Network error while fetching domains: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventTrackerException(errorMsg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Request interrupted while fetching domains: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventTrackerException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error while fetching domains: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventTrackerException(errorMsg, e);
        }
    }
    private void publishDomainEventMessage(final DomainEventDTO domainEventDTO) throws Exception {

    }
    /**
     * Send an event to the Eventstracker service to log the deletion of the GitHub repository.
     *
     * @param payLoad The payload associated with the event.
     * @throws EventTrackerException if the event cannot be sent
     * @throws IllegalArgumentException if payLoad is null or empty
     */
    public void sendGitHubEventToEventstracker(String payLoad) {
        // Input validation
        if (payLoad == null || payLoad.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        
        // Check if domains data is available
        if (domainsData == null || domainsData.isEmpty()) {
            String errorMsg = "Domains data is not available. Service may not be properly initialized.";
            log.error(errorMsg);
            throw new EventTrackerException(errorMsg);
        }
        
        try {
            DomainEventDTO eventDTO = createEventDTO(payLoad);
            String jsonPayload = objectMapper.writeValueAsString(eventDTO);
            
            log.info("Sending GitHub event to EventTracker. Event ID: {}", eventDTO.getEventId());
            log.debug("Event Payload: {}", jsonPayload);
            
            sendToRabbitMQ(jsonPayload);
            
            log.info("Successfully sent GitHub event to EventTracker. Event ID: {}", eventDTO.getEventId());
            
        } catch (IOException e) {
            String errorMsg = "Failed to serialize event payload: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventTrackerException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error while sending event to EventTracker: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventTrackerException(errorMsg, e);
        }
    }
    
    /**
     * Creates a DomainEventDTO from the given payload.
     */
    private DomainEventDTO createEventDTO(String payLoad) {
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
            String errorMsg = "Domain 'GITHUB_REPO' not found in available domains. Available domains: " + 
                domainsData.stream().map(DomainDTO::getDomainName).toList();
            log.error(errorMsg);
            throw new EventTrackerException(errorMsg);
        }
        
        return eventDTO;
    }
    
    /**
     * Sends the JSON payload to RabbitMQ with proper error handling.
     */
    private void sendToRabbitMQ(String jsonPayload) {
        try {
            String exchange = rabbitConfigProperties.sathishProjectEventsExchange();
            String routingKey = rabbitConfigProperties.githubRoutingKey();
            
            if (exchange == null || exchange.trim().isEmpty()) {
                throw new EventTrackerException("RabbitMQ exchange is not configured");
            }
            if (routingKey == null || routingKey.trim().isEmpty()) {
                throw new EventTrackerException("RabbitMQ routing key is not configured");
            }
            
            log.debug("Sending message to RabbitMQ exchange: {}, routing key: {}", exchange, routingKey);
            
            // Send multiple messages as per original logic, but with better error handling
            for (int i = 0; i < 10; i++) {
                try {
                    rabbitTemplate.convertAndSend(exchange, routingKey, jsonPayload);
                    log.debug("Successfully sent message {} of 10 to RabbitMQ", i + 1);
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to send message %d of 10 to RabbitMQ: %s", i + 1, e.getMessage());
                    log.error(errorMsg, e);
                    // Continue with other messages, but log the failure
                }
            }
        } catch (Exception e) {
            String errorMsg = "Failed to send messages to RabbitMQ: " + e.getMessage();
            log.error(errorMsg, e);
            throw new EventTrackerException(errorMsg, e);
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
