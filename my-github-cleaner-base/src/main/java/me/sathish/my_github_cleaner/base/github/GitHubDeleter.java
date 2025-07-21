package me.sathish.my_github_cleaner.base.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

@Service
public class GitHubDeleter {
    @Autowired
    Environment environment;

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
            } else {
                System.out.println("Failed to delete repository. Status: " +
                        response.statusCode());
                System.out.println("Response: " + response.body());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting repository: " + e.getMessage());
        }

    }

}
