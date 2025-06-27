# MyGithubCleaner
GitHubCleaner is a tool to clean up your GitHub account by removing unwanted repositories, issues, and pull requests.
## Development

When starting the application `docker compose up` is called and the app will connect to the contained services.
[Docker](https://www.docker.com/get-started/) must be available on the current system.

During development it is recommended to use the profile `local`. In IntelliJ `-Dspring.profiles.active=local` can be
added in the VM options of the Run Configuration after enabling this property in "Modify options". Create your own
`application-local.yml` file to override settings for development. For this multi-module
project you have to select the highest module `my-github-cleaner-web` as the classpath.

Lombok must be supported by your IDE. For IntelliJ install the Lombok plugin and enable annotation processing -
[learn more](https://bootify.io/next-steps/spring-boot-with-lombok.html).

In addition to the Spring Boot application, the DevServer must also be started - for this
[Node.js](https://nodejs.org/) version 22 is required. On first usage and after updates the dependencies have to be installed:

```
npm install
```

The DevServer can be started as follows:

```
npm run devserver
```

Using a proxy the whole application is now accessible under `localhost:3000`. All changes to the templates and JS/CSS
files are immediately visible in the browser.

## Testing requirements

Testcontainers is used for running the integration tests. Due
to the reuse flag, the container will not shut down after the tests. It can be stopped manually if needed.

Frontend unit tests can be executed with `npm run test`.

## Build

The application can be tested and built using the following command:

```
mvnw clean package
```

Node.js is automatically downloaded using the `frontend-maven-plugin` and the final JS/CSS files are integrated into the jar.

Start your application with the following command - here with the profile `production`:

```
java -Dspring.profiles.active=production -jar ./my-github-cleaner-web/target/my-github-cleaner-web-0.0.1-SNAPSHOT.jar
```

If required, a Docker image can be created with the Spring Boot plugin. Add `SPRING_PROFILES_ACTIVE=production` as
environment variable when running the container.

```
mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=me.sathish/my-github-cleaner
```

## Further readings

* [Maven docs](https://maven.apache.org/guides/index.html)  
* [Spring Boot reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)  
* [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/reference/jpa.html)
* [Learn React](https://react.dev/learn)
* [Webpack concepts](https://webpack.js.org/concepts/)  
* [npm docs](https://docs.npmjs.com/)  
* [Tailwind CSS](https://tailwindcss.com/)  
