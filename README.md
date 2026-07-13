# MyGithubCleaner

A Spring Boot multi-module application that syncs, manages, and deletes GitHub repositories. It integrates with an
EventTracker service via RabbitMQ and uses Spring Cloud Config for externalized configuration.

## Architecture

| Module                   | Purpose                                                                    |
|--------------------------|----------------------------------------------------------------------------|
| `my-github-cleaner-base` | Core logic: GitHub API client, repository sync, event tracking, scheduling |
| `my-github-cleaner-web`  | Web layer: REST endpoints, security, React/TypeScript frontend             |

**Key integrations:**

- **GitHub API** — fetches repos, deletes repos on demand
- **PostgreSQL** — persists repository metadata
- **RabbitMQ** — publishes domain events to EventTracker
- **Spring Cloud Config** — centralized configuration (via config server)
- **Spring Cloud Kubernetes Discovery** — service discovery in K8s

## Prerequisites

- Java 24
- [Docker](https://www.docker.com/get-started/) (for local PostgreSQL)
- [Node.js](https://nodejs.org/) 22+ (for frontend dev server)
- Access to a Spring Cloud Config Server

## Getting Started

### 1. Environment

Copy the `.env.example` template at the repo root and fill in your values:

```bash
cp .env.example .env
ln -s ../.env my-github-cleaner-base/.env
```

The symlink matters because Spring resolves `.env` relative to whatever
directory the app is actually run from: the CLI command below runs from the
repo root, but IntelliJ run configurations that target `MyGithubCleanerApplication`
directly default to `my-github-cleaner-base/` as the working directory. Both
need to see the same file, or the two setups will silently use different
config (that's how this drifted before) — don't recreate
`my-github-cleaner-base/.env` as a separate copy.

Key variables: `GITHUB_TOKEN`, `GITHUB_CLEANER_DB_URL`, `RABBITMQ_HOST`,
`EVENTSTRACKER_URL`/`eventstracker_username`/`eventstracker_password`,
`SATHISHLOGGER_URL`, `CONFIG_SERVER_URL`. See `.env.example` for the full list
and important notes on each.

**`CONFIG_SERVER_URL`, `SPRING_CLOUD_CONFIG_USERNAME`, and
`SPRING_CLOUD_CONFIG_PASSWORD` must be real shell/IDE environment variables**,
not just present in `.env` — Spring Boot resolves the config-server import
location before this file's properties are available, so a value that only
lives in `.env` won't resolve in time. Export them in your shell profile or
your IDE run configuration.

### 2. Start Infrastructure

```bash
docker compose up -d
```

This starts a local PostgreSQL instance on port **5439**.

### 3. Run the Backend

Use the `local` profile. In IntelliJ add `-Dspring.profiles.active=local` to VM options, selecting module
`my-github-cleaner-web` as the classpath.

```bash
./mvnw spring-boot:run -pl my-github-cleaner-web -am -Dspring-boot.run.profiles=local
```

`-am` ("also make") builds `my-github-cleaner-base` first — without it Maven
fails with a dependency-resolution error since the base module's jar isn't
in the local repo yet.

### 4. Run the Frontend Dev Server

```bash
npm install
npm run devserver
```

The frontend is served at `localhost:3000` and proxies API calls to the backend at `localhost:7080`.

## Testing

- **Integration tests** use Testcontainers (container reuse is enabled).
- **Frontend tests:** `npm run test`

## Build

```bash
./mvnw clean package
```

Node.js is auto-downloaded via `frontend-maven-plugin`; final JS/CSS is bundled into the jar.

### Run the Jar

```bash
java -Dspring.profiles.active=production -jar ./my-github-cleaner-web/target/my-github-cleaner-web-0.0.1-SNAPSHOT.jar
```

### Docker Image

```bash
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=me.sathish/my-github-cleaner
```

Set `SPRING_PROFILES_ACTIVE=production` when running the container.

## Kubernetes

A discovery server manifest is provided in `k8s/kubernetes-discoveryserver.yaml` for Spring Cloud Kubernetes service
discovery.

## Security

Authentication uses HTTP Basic with bcrypt-encoded passwords. Credentials are configured via environment variables
(`app.security.admin.*`, `app.security.viewer.*`). **Never commit real credentials to version control.**

## Further Reading

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Cloud Config](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa.html)
- [React](https://react.dev/learn)
- [Tailwind CSS](https://tailwindcss.com/)
