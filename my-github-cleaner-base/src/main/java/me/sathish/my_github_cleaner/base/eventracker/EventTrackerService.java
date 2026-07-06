package me.sathish.my_github_cleaner.base.eventracker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.sathish.my_github_cleaner.base.config.RabbitMQConfiguration;
import me.sathish.my_github_cleaner.base.util.RabbitConfigProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

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

import static me.sathish.my_github_cleaner.base.github.GitHubServiceConstants.GITHUB_REPOSITORY_PROJECT;

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
        String eventsTrackerUrl = environment.getProperty("EVENTSTRACKER_URL");
        String eventServiceUserName = environment.getProperty("eventstracker_username");
        String eventServicePassword = environment.getProperty("eventstracker_password");

        validateRequiredProperty("EVENTSTRACKER_URL", eventsTrackerUrl);
        validateRequiredProperty("eventstracker_username", eventServiceUserName);
        validateRequiredProperty("eventstracker_password", eventServicePassword);

        log.info("Connecting to EventTracker at {} as user '{}'", eventsTrackerUrl, eventServiceUserName);

        try {
            HttpClient client =
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

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

            HttpResponse<String> domainsResponse = client.send(getDomainsRequest, HttpResponse.BodyHandlers.ofString());
            int status = domainsResponse.statusCode();

            if (status == 401 || status == 403) {
                throw new IllegalStateException(String.format(
                        "EventTracker authentication failed (HTTP %d). "
                                + "The password for user '%s' is wrong. "
                                + "Fix 'eventstracker_password' in your environment and restart.",
                        status, eventServiceUserName));
            }

            if (status >= 300 && status < 400) {
                String location =
                        domainsResponse.headers().firstValue("Location").orElse("(none)");
                throw new IllegalStateException(String.format(
                        "EventTracker returned HTTP %d (redirect to %s). "
                                + "This means the credentials for user '%s' were rejected. "
                                + "Fix 'eventstracker_password' in your environment and restart.",
                        status, location, eventServiceUserName));
            }

            if (status != 200) {
                throw new IllegalStateException(String.format(
                        "EventTracker returned HTTP %d. Expected 200. Response: %s", status, domainsResponse.body()));
            }

            String contentType =
                    domainsResponse.headers().firstValue("Content-Type").orElse("");
            if (!contentType.contains("application/json")) {
                throw new IllegalStateException(String.format(
                        "EventTracker returned Content-Type '%s' instead of application/json. "
                                + "Response starts with: %s",
                        contentType,
                        domainsResponse
                                .body()
                                .substring(
                                        0, Math.min(200, domainsResponse.body().length()))));
            }

            domainsData = objectMapper.readValue(domainsResponse.body(), new TypeReference<List<DomainDTO>>() {});
            log.info("Successfully fetched {} domains from EventTracker", domainsData.size());

        } catch (IllegalStateException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot reach EventTracker at " + eventsTrackerUrl + ". Is the service running? Error: "
                            + e.getMessage(),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while connecting to EventTracker", e);
        }
    }

    private void validateRequiredProperty(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Required property '%s' is missing or empty. "
                            + "Set it in your environment variables and restart.",
                    name));
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
        sendGitHubEventToEventstracker(payLoad, GITHUB_REPOSITORY_PROJECT);
    }

    /**
     * Send an event to the Eventstracker service with a specific event type.
     *
     * @param payLoad   The payload associated with the event.
     * @param eventType The event type to use.
     * @throws EventTrackerException    if the event cannot be sent
     * @throws IllegalArgumentException if payLoad is null or empty
     */
    public void sendGitHubEventToEventstracker(String payLoad, String eventType) {
        // Input validation
        if (payLoad == null || payLoad.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }

        if (domainsData == null || domainsData.isEmpty()) {
            throw new EventTrackerException(
                    "Domains data is not available. This should not happen — startup validation should have caught this.");
        }

        try {
            DomainEventDTO eventDTO = createEventDTO(payLoad, eventType);
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
     * Creates a DomainEventDTO from the given payload and event type.
     */
    private DomainEventDTO createEventDTO(String payLoad, String eventType) {
        DomainEventDTO eventDTO = new DomainEventDTO();
        eventDTO.setEventId(UUID.randomUUID().toString());
        eventDTO.setEventType(eventType);
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
