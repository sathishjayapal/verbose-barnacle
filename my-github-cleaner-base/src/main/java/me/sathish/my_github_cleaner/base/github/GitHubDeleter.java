package me.sathish.my_github_cleaner.base.github;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class GitHubDeleter {
    public static void deleteRepository(String token, String owner, String repoName) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repoName))
                    .header("Authorization", "token " + token)
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

        } catch (IOException | InterruptedException e) {
            System.err.println("Error deleting repository: " + e.getMessage());
        }
    }

}
