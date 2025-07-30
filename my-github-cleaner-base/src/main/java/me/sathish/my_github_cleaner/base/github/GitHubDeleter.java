package me.sathish.my_github_cleaner.base.github;


import me.sathish.my_github_cleaner.base.eventracker.EventTrackerService;
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

@Service
public class GitHubDeleter {
    @Autowired
    Environment environment;

    @Autowired
    private  EventTrackerService eventTrackerService; // Inject the new service
    
    private final ObjectMapper objectMapper = new ObjectMapper(); // Keep ObjectMapper for other potential uses or remove if not needed elsewhere

    public HttpResponse deleteRepository(String repositoryName) {

        try {
            HttpClient client = HttpClient.newHttpClient();
            String githubusername = environment.getProperty("githubusername");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + githubusername + "/" + repositoryName))
                    .header("Authorization", "token " + environment.getProperty("GITHUB_TOKEN"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                System.out.println("Repository deleted successfully!");
                String payLoad = new StringBuffer("Repository deleted successfully!").append(String.format("{\"repositoryName\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                        repositoryName, LocalDateTime.now(), githubusername)).toString();
                // Send event to Eventstracker after successful deletion
                eventTrackerService.sendEventToEventstracker(repositoryName,payLoad);
            } else {
                System.out.println("Failed to delete repository. Status: " +
                        response.statusCode());
                String payLoad = new StringBuffer("Failed to delete repository").append(String.format("{\"repositoryName\":\"%s\",\"deletedAt\":\"%s\",\"deletedBy\":\"%s\"}",
                        repositoryName, LocalDateTime.now(), githubusername)).toString();
                eventTrackerService.sendEventToEventstracker(repositoryName,payLoad);
                System.out.println("Response: " + response.body());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting repository: " + e.getMessage());
        }
    }
}