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
import java.nio.charset.StandardCharsets;
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
    public EventTrackerService(
            Environment environment,
            ObjectMapper objectMapper,
            RabbitMQConfiguration rabbitMQConfiguration,
            RabbitTemplate rabbitTemplate,
            RabbitConfigProperties rabbitConfigProperties) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.rabbitMQConfiguration = rabbitMQConfiguration;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitConfigProperties = rabbitConfigProperties;
    }

    @PostConstruct
    public void fetchDomainsOnStartup() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            String eventsTrackerUrl = environment.getProperty("EVENTSTRACKER_URL");
            String eventServiceUserName = environment.getProperty("eventstracker_username");
            String eventServicePassword = environment.getProperty("eventstracker_password");

            log.info("Fetching domains from EventTracker URL: {} as user: {}", eventsTrackerUrl, eventServiceUserName);

            // Validate required configuration
            if (eventsTrackerUrl == null || eventsTrackerUrl.trim().isEmpty()) {
                throw new IllegalStateException("EventTracker URL is not configured");
            }

            String credentials = Base64.getEncoder()
                    .encodeToString(
                            (eventServiceUserName + ":" + eventServicePassword).getBytes(StandardCharsets.UTF_8));

            URI domainsUri = URI.create(eventsTrackerUrl.replaceAll("/+$", "") + "/api/domains");
            HttpRequest getDomainsRequest = HttpRequest.newBuilder()
                    .uri(domainsUri)
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + credentials)
                    .GET()
                    .build();

            log.info("Requesting domains from: {}", domainsUri);

            HttpResponse<String> domainsResponse = client.send(getDomainsRequest, HttpResponse.BodyHandlers.ofString());

            log.info(
                    "EventTracker response: status={}, finalURI={}",
                    domainsResponse.statusCode(),
                    domainsResponse.uri());

            if (domainsResponse.statusCode() == 200) {
                domainsData = objectMapper.readValue(domainsResponse.body(), new TypeReference<List<DomainDTO>>() {});
                log.info("Successfully fetched {} domains on startup", domainsData.size());
            } else {
                log.error(
                        "Failed to fetch domains. HTTP Status: {}, Final URI: {}, Response: {}",
                        domainsResponse.statusCode(),
                        domainsResponse.uri(),
                        domainsResponse.body());
                domainsData = List.of();
            }
        } catch (IOException e) {
            String errorMsg = "Network error while fetching domains: " + e.getMessage();
            log.error(errorMsg, e);
            domainsData = List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Request interrupted while fetching domains: " + e.getMessage();
            log.error(errorMsg, e);
            domainsData = List.of();
        } catch (Exception e) {
            String errorMsg = "Unexpected error while fetching domains: " + e.getMessage();
            log.error(errorMsg, e);
            domainsData = List.of();
        }
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
            // Retry once lazily in case EventTracker became available after app startup.
            fetchDomainsOnStartup();
            if (domainsData == null || domainsData.isEmpty()) {
                String errorMsg = "Domains data is not available. Service may not be properly initialized.";
                log.error(errorMsg);
                throw new EventTrackerException(errorMsg);
            }
        }

        try {
            DomainEventDTO eventDTO = createEventDTO(payLoad);
            String jsonPayload = objectMapper.writeValueAsString(eventDTO);

            log.info("Sending GitHub event to EventTracker. Event ID: {}", eventDTO.getEventId());
            log.debug("Event Payload: {}", jsonPayload);

            sendToRabbitMQ(eventDTO);

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
                .filter(d -> "GITHUB_DOMAIN".equals(d.getDomainName()))
                .findFirst();

        if (domainOpt.isPresent()) {
            eventDTO.setDomain(domainOpt.get().getId());
        } else {
            String errorMsg = "Domain 'GITHUB_DOMAIN' not found in available domains. Available domains: "
                    + domainsData.stream().map(DomainDTO::getDomainName).toList();
            log.error(errorMsg);
            throw new EventTrackerException(errorMsg);
        }

        return eventDTO;
    }

    /**
     * Sends the DomainEventDTO to RabbitMQ with proper error handling.
     */
    private void sendToRabbitMQ(DomainEventDTO eventDTO) {
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

            rabbitTemplate.convertAndSend(exchange, routingKey, eventDTO);
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
