# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /app

# Copy root pom and module poms first for dependency caching
COPY pom.xml .
COPY my-github-cleaner-base/pom.xml my-github-cleaner-base/pom.xml
COPY my-github-cleaner-web/pom.xml my-github-cleaner-web/pom.xml

# Download dependencies (cache mount persists across builds)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B || true

# Copy frontend configuration files
COPY package.json .
COPY package-lock.json .
COPY webpack.config.js .
COPY tsconfig.json .

# Copy source code
COPY my-github-cleaner-base/src my-github-cleaner-base/src
COPY my-github-cleaner-web/src my-github-cleaner-web/src

# Build the application, skipping tests
# (npm install/build happens inside this goal via frontend-maven-plugin;
#  -Dmaven.compiler.proc=full is required because JDK 24 no longer runs
#   Lombok's annotation processor implicitly)
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    mvn package -DskipTests -B -Dmaven.compiler.proc=full -Dspotless.check.skip=true

# Runtime stage
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Patch OS-level packages — this stage ships, so Trivy-flagged CVEs get resolved here
RUN apk update && apk upgrade --no-cache

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the executable jar (built by the my-github-cleaner-web module)
COPY --from=build /app/my-github-cleaner-web/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 7080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:7080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
