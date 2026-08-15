# URL Shortener Backend

This is the backend service for a URL shortener and click-count analytics application. It provides REST APIs for creating short links, redirecting visitors, listing active links, and reading click totals.

The project is also a practical learning project for Spring Boot, PostgreSQL, Redis, Kafka, Docker, database migrations, asynchronous processing, concurrency, testing, and Maven build automation.

## Highlights

- **Simple link creation** — Generates eight-character Base62 links while validating destination URLs.
- **Fast and safe redirects** — Uses Redis for cache-first lookups, falls back to PostgreSQL, and enforces link status and expiration.
- **Event-driven analytics** — Publishes click events to Kafka and processes them transactionally with duplicate-event protection.
- **Production-minded engineering** — Includes Flyway migrations, Docker Compose infrastructure, Testcontainers tests, JaCoCo coverage gates, Spotless formatting, OpenAPI documentation, and k6 performance checks.

## Technology Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA and Hibernate
- PostgreSQL 16
- Redis 7.4
- Apache Kafka 4.3 for local development
- Flyway
- Spring Boot Actuator
- Springdoc OpenAPI
- JUnit, Mockito, and Spring MVC Test
- Testcontainers for PostgreSQL and Kafka integration tests
- Spotless with Google Java Format
- JaCoCo for code coverage
- Maven Wrapper
- Docker Compose
- k6 for performance testing

## Architecture

```text
Client
  |
  | REST
  v
UrlController
  |
  v
UrlServiceImpl
  |\
  | \-- Redis redirect cache
  |
  \---- PostgreSQL through UrlRepository
          |
          \-- redirect click event -> Kafka topic
                                      |
                                      v
                              ShortUrlClickedConsumer
                                      |
                                      v
                              PostgreSQL click update
```

### Redirect flow

1. `UrlServiceImpl` checks Redis for the short code.
2. On a cache hit, it returns the cached destination without querying PostgreSQL.
3. On a cache miss, it loads the link from PostgreSQL and verifies its status and expiration.
4. A valid database result is written to Redis with a bounded TTL.
5. A `ShortUrlClickedEvent` is published to Kafka.
6. `ShortUrlClickedConsumer` records the event, increments `number_of_clicks`, and publishes a `ClickUpdatedEvent` after the transaction.

The redirect path and analytics path are intentionally separated: redirecting a visitor does not wait for the click-count database update to complete.

## Main Components

| Component | Responsibility |
| --- | --- |
| `UrlController` | Exposes create, redirect, list, and analytics endpoints. |
| `UrlServiceImpl` | Validates URLs, creates short codes, checks link state, uses the redirect cache, and publishes click events. |
| `RedirectCacheServiceImpl` | Reads and writes redirect mappings in Redis and handles cache infrastructure failures gracefully. |
| `UrlRepository` | Persists short URLs, lists valid links, and atomically increments click counts. |
| `ShortUrlClickedConsumer` | Consumes Kafka events, prevents duplicate processing, updates click counts, and emits application events. |
| `ProcessedClickEventRepository` | Stores event IDs that have already been processed. |
| `KafkaClickEventPublisher` | Publishes `ShortUrlClickedEvent` records to the configured Kafka topic. |
| `ShortUrlMapper` | Maps entities to API responses and click-update events. |
| `FlywayConfig` | Runs database migrations before the JPA `EntityManagerFactory` is initialized. |
| `GlobalExceptionHandler` | Maps validation and application exceptions to HTTP responses. |

## Project Structure

```text
Backend/
├── src/
│   ├── main/
│   │   ├── java/com/appsdeveloperblog/ws/urlshorten/
│   │   │   ├── common/entity/       # Shared JPA base entities
│   │   │   ├── config/              # Flyway and web configuration
│   │   │   └── url/
│   │   │       ├── controller/      # REST controllers
│   │   │       ├── entity/          # ShortUrl and processed-event entities
│   │   │       ├── event/            # Application and Kafka event records
│   │   │       ├── exception/        # Custom exceptions and handlers
│   │   │       ├── mapper/           # Entity-to-response mapping
│   │   │       ├── messaging/        # Kafka publisher and consumer
│   │   │       ├── model/             # API requests, responses, and enums
│   │   │       ├── publisher/         # Post-transaction event publishers
│   │   │       ├── repository/        # Spring Data repositories
│   │   │       ├── service/           # Service interfaces and implementations
│   │   │       └── util/              # Small shared utilities
│   │   └── resources/
│   │       ├── application.yml       # Runtime configuration
│   │       └── db/migration/          # Versioned Flyway SQL migrations
│   └── test/java/                    # Unit and integration tests
├── performance/                      # k6 redirect performance scenario
├── docs/                             # Runbooks, design notes, and learning docs
├── .github/workflows/verify.yml      # GitHub Actions verification workflow
├── docker-compose.yml                # PostgreSQL, Redis, and Kafka
├── pom.xml                           # Maven dependencies and build lifecycle
└── mvnw / mvnw.cmd                   # Maven Wrapper scripts
```

## API Endpoints

The API supports both the original routes and `/api/v1/urls` aliases for create/list operations.

### Create a short URL

```http
POST /urls
POST /api/v1/urls
Content-Type: application/json
```

Request:

```json
{
  "longUrl": "https://www.example.com/articles/spring-boot"
}
```

Response: `201 Created`

```json
{
  "shortCode": "aB12xYz9",
  "shortUrl": "http://localhost:11000/aB12xYz9",
  "status": "ACTIVE",
  "validFrom": "2026-08-15T10:00:00Z",
  "expiresAt": "2026-09-14T10:00:00Z"
}
```

### Redirect to the original URL

```http
GET /{shortCode}
```

Returns `302 Found` with the original URL in the `Location` header when the link is active, valid, and not expired. Invalid, inactive, expired, or missing links return `404 Not Found`.

### List active short URLs

```http
GET /urls?pageNumber=0&pageSize=10
GET /api/v1/urls?pageNumber=0&pageSize=10
```

The response contains active links whose `validFrom` is in the past and whose `expiresAt` is in the future. Results are ordered by creation time in descending order.

### Get click-count analytics

```http
GET /urls/{urlId}/analytics
```

Response: `200 OK`

```json
{
  "short_url_id": "7a4e8c1d-4eb0-4b1a-8ee8-6df0e76415f4",
  "number_of_clicks": 42
}
```

### Operational and API documentation endpoints

```text
GET /actuator/health
GET /actuator/metrics
GET /swagger-ui/index.html
GET /v3/api-docs
```

## Prerequisites

- Java 21 or a compatible JDK
- Docker Desktop with Docker Compose
- k6, only if you want to run the performance scenario

## Run the Local Dependencies

From the `Backend` directory:

```powershell
docker compose up -d
```

The Compose file starts:

| Service | Container port | Host port | Purpose |
| --- | ---: | ---: | --- |
| PostgreSQL | 5432 | 5433 | Durable short-link and analytics storage |
| Redis | 6379 | 6779 | Redirect cache |
| Kafka | 9092 | 9092 | Click-event transport |

Kafka topics are not explicitly created by Docker Compose. Create the click-event topic before running the application:

```powershell
docker exec kafka-broker /opt/kafka/bin/kafka-topics.sh --create --if-not-exists --topic short-url-clicked --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

## Run the Application

The default application configuration uses:

```text
Application: http://localhost:11000
PostgreSQL: localhost:5433
Redis: localhost:6379
Kafka: localhost:9092
```

There is currently a local port mismatch: Docker Compose publishes Redis on host port `6779`, while `application.yml` points to host port `6379`. When running the application directly on the host with the current Compose file, override the Redis port:

```powershell
$env:SPRING_DATA_REDIS_PORT = "6779"
.\mvnw.cmd spring-boot:run
```

Alternatively, change the Compose mapping to `6379:6379` so it matches `application.yml`.

Stop the local dependencies with:

```powershell
docker compose down
```

## Database Migrations

Flyway loads migrations from:

```text
src/main/resources/db/migration
```

Current migrations:

1. `V1__create_short_url_table.sql` creates the `short_url` table.
2. `V2__add_click_count_to_short_url.sql` adds `number_of_clicks` with a default of zero.
3. `V3__create_processed_click_event_table.sql` creates the event-id deduplication table.

The application uses `ddl-auto: validate`, so Hibernate validates the schema while Flyway owns schema creation and changes.

## Testing

### Unit tests

Run unit and slice tests without the Failsafe integration-test phase:

```powershell
.\mvnw.cmd test
```

The test suite covers URL creation and validation, redirect behavior, cache behavior, controller responses, mapping, exception handling, and click-event handling.

### Integration tests

Run the complete Maven verification lifecycle:

```powershell
.\mvnw.cmd verify
```

Integration tests use Testcontainers and therefore require a working Docker engine.

- `UrlApplicationIT` starts PostgreSQL and Kafka, configures the Kafka bootstrap address dynamically, and creates the `short-url-clicked` topic before exercising the create-and-redirect flow.
- `ShortUrlClickedConsumerIT` starts PostgreSQL, disables automatic Kafka listener startup, invokes the consumer directly, and verifies that 200 concurrent click events are not lost.

### Coverage and formatting

JaCoCo combines unit and integration-test execution data during the Maven lifecycle. The current coverage gates are:

- Line coverage: at least 80%.
- Branch coverage: at least 70%.

After `verify`, open the report at:

```text
target/site/jacoco/index.html
```

Spotless checks Google Java Format during the `verify` phase:

```powershell
.\mvnw.cmd spotless:check
.\mvnw.cmd spotless:apply
```

## Performance Test

Start PostgreSQL and the backend first. Create a short URL and copy its `shortCode`, then run:

```powershell
k6 run -e BASE_URL=http://localhost:11000 -e SHORT_CODE=<shortCode> performance/redirect-url.k6.js
```

The scenario uses 10 virtual users for 30 seconds, does not follow redirects, and checks:

- 95th-percentile request duration below 200 ms.
- HTTP failure rate below 1%.
- Every response returns `302` and includes a `Location` header.

## Continuous Integration

GitHub Actions runs the following workflow on pushes and pull requests targeting `master`:

1. Set up Java 21.
2. Run `./mvnw --batch-mode verify`.
3. Upload the JaCoCo HTML report as a workflow artifact, even when verification fails.

Because `verify` runs Testcontainers-based integration tests, the CI runner must provide Docker support.

## Configuration Reference

Important settings are defined in `src/main/resources/application.yml`:

| Property | Current value | Purpose |
| --- | --- | --- |
| `server.port` | `11000` | HTTP server port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/db_url_shortener` | PostgreSQL connection |
| `spring.data.redis.port` | `6379` | Redis port expected by the application |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `app.base-url` | `http://localhost:11000` | Base URL returned in create responses |
| `app.cache.redirect-ttl` | `30m` | Maximum redirect-cache lifetime |
| `app.kafka.topics.short-url-clicked` | `short-url-clicked` | Click-event topic |
| `logging.file.name` | `./logs/url-shortener.log` | Application log file |

For a shared or production environment, move credentials and environment-specific values out of source-controlled configuration and inject them through environment variables or a secret-management system.

## Current Limitations and Next Steps

- Redis is used for redirect caching, but the `RedisClickUpdatePublisher` currently contains a placeholder rather than a completed Redis Pub/Sub implementation.
- The analytics API currently returns an aggregate click count; daily, geographic, device, and referrer analytics are not implemented.
- There is no authentication, authorization, ownership model, or rate limiting yet.
- The local Compose file does not create the Kafka topic automatically.
- Redis has unit tests with mocked `StringRedisTemplate`, but no Redis Testcontainers integration test yet.
- The URL private-network validation covers selected IPv4 patterns and should be expanded if the service is exposed to untrusted users.
- Local database credentials are currently stored in `application.yml` and should be externalized before deployment.

## Learning Goals

This backend provides practice with:

- Designing a layered Spring Boot service.
- Building REST APIs and handling HTTP status codes.
- Validating and normalizing untrusted input.
- Persisting data with JPA and PostgreSQL.
- Managing schema evolution with Flyway.
- Applying cache-aside patterns and bounded TTLs.
- Separating synchronous redirect traffic from asynchronous analytics processing.
- Handling duplicate events and concurrent database updates.
- Writing unit, MVC, integration, concurrency, coverage, and performance tests.
- Connecting Maven phases, quality gates, Testcontainers, and CI execution.
