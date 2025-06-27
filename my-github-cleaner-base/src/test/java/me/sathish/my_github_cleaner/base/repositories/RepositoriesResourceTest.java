package me.sathish.my_github_cleaner.base.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.my_github_cleaner.base.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;


public class RepositoriesResourceTest extends BaseIT {

    @Test
    @Sql("/data/repositoriesData.sql")
    void getAllRepositoriess_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/repositoriess")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(2))
                    .body("content.get(0).id", Matchers.equalTo(1000));
    }

    @Test
    @Sql("/data/repositoriesData.sql")
    void getAllRepositoriess_filtered() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/repositoriess?filter=1001")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).id", Matchers.equalTo(1001));
    }

    @Test
    void getAllRepositoriess_unauthorized() {
        RestAssured
                .given()
                    .redirects().follow(false)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/repositoriess")
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .body("code", Matchers.equalTo("AUTHORIZATION_DENIED"));
    }

    @Test
    @Sql("/data/repositoriesData.sql")
    void getRepositories_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/repositoriess/1000")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("repoName", Matchers.equalTo("Elementum tempus egestas sed sed risus pretium."));
    }

    @Test
    void getRepositories_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/repositoriess/1666")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createRepositories_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/repositoriesDTORequest.json"))
                .when()
                    .post("/api/repositoriess")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, repositoriesRepository.count());
    }

    @Test
    void createRepositories_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/repositoriesDTORequest_missingField.json"))
                .when()
                    .post("/api/repositoriess")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("repoName"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    @Sql("/data/repositoriesData.sql")
    void updateRepositories_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/repositoriesDTORequest.json"))
                .when()
                    .put("/api/repositoriess/1000")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals("Eget est lorem ipsum dolor sit amet. Phasellus vestibulum lorem sed risus ultricies tristique.", repositoriesRepository.findById(((long)1000)).orElseThrow().getRepoName());
        assertEquals(2, repositoriesRepository.count());
    }

    @Test
    @Sql("/data/repositoriesData.sql")
    void deleteRepositories_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/repositoriess/1000")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, repositoriesRepository.count());
    }

}
