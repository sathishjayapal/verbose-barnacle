package me.sathish.my_github_cleaner.base.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GitHubDeleterTest {
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
    }

    @Test
    void deleteRepository_success() throws IOException, InterruptedException {
        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(204);
            assertDoesNotThrow(() -> GitHubDeleter.deleteRepository("testToken", "testOwner", "testRepo"));
        }
    }

    @Test
    void deleteRepository_failure() throws IOException, InterruptedException {
        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(404);
            when(httpResponse.body()).thenReturn("Not Found");
            assertDoesNotThrow(() -> GitHubDeleter.deleteRepository("testToken", "testOwner", "testRepo"));
        }
    }

    @Test
    void deleteRepository_throwsException() throws IOException, InterruptedException {
        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("Test Exception"));
            assertDoesNotThrow(() -> GitHubDeleter.deleteRepository("testToken", "testOwner", "testRepo"));
        }
    }
}
