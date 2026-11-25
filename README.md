# OrderFlow

A small order-processing service that shows how **Spring Boot, PostgreSQL, Kafka and Redis** work together, fully containerized with **Docker Compose**.

A client creates an order over REST. The order is stored in PostgreSQL, an event is published to Kafka, a consumer processes that event idempotently, and reads of an order are served through a Redis cache.

## Architecture

```
POST /api/orders
      |
      v
OrderController -> OrderService --(save)--> PostgreSQL
                         |
                         | OrderCreatedEvent (sent AFTER the DB transaction commits)
                         v
                   Kafka: orders.created
                         |
                         v
                OrderEventConsumer
                  1. OrderProcessingService   (idempotent, one DB transaction)
                  2. evict the Redis cache entry of that order
                  3. manual acknowledgment
                         |
                         | 3 retries failed?
                         v
                   Kafka: orders.created.DLT   (dead-letter topic)

GET /api/orders/{id} -> Redis (hit) / PostgreSQL (miss, then cached for 5 minutes)
```

## Tech stack

Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Apache Kafka (Spring for Apache Kafka), Redis (Spring Cache), Docker and Docker Compose, JUnit 5, Mockito, GitHub Actions.

## Run it

Everything in containers:

```bash
docker compose up -d --build
```

Or run only the infrastructure in Docker and the app from your IDE:

```bash
docker compose up -d postgres redis kafka
./mvnw spring-boot:run
```

## API

```bash
# create an order
curl -X POST localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"c-1","amount":150.00,"currency":"EGP"}'

# read it (use the id from the response)
curl localhost:8080/api/orders/<id>
```

Health: `GET /actuator/health` (shows the database and Redis status).

## Things to try

**1. The cache (Cache-Aside).** Call `GET /api/orders/<id>` twice. The log line `Cache miss for order ...` appears only once. Inspect Redis:

```bash
docker compose exec redis redis-cli
KEYS *
TTL "orders::<id>"
```

**2. Invalidation.** Right after creating an order its status is `CREATED`. The consumer then sets it to `PROCESSED` and evicts the cache entry, so the next `GET` reads the fresh value instead of a stale one.

**3. Retry and dead-letter topic.** Create an order with a `customerId` that starts with `fail` (for example `fail-demo`). The consumer throws, Kafka retries 3 times, and the record ends up in `orders.created.DLT` (see the `Dead letter received` log line).

**4. Idempotency.** Stop the app, replay every event from the beginning, then start the app again:

```bash
docker compose exec kafka kafka-consumer-groups --bootstrap-server kafka:29092 \
  --group orderflow-group --topic orders.created --reset-offsets --to-earliest --execute
```

Every event is delivered again, and the log shows `Event ... already processed - skipping`: nothing is processed twice.

## Design decisions

- **Event sent after commit.** `OrderEventProducer` is a `@TransactionalEventListener` (AFTER_COMMIT), so no event is published for an order that was rolled back.
- **Idempotent consumer.** The `processed_events` table (primary key = `eventId`) is written in the same transaction as the order update. This survives restarts and multiple consumer instances, unlike an in-memory set.
- **Manual acknowledgment.** The offset is committed only after processing succeeded.
- **Cache the DTO, not the entity.** `OrderResponse` is what lives in Redis. A JPA entity carries lazy proxies and persistence-context state.
- **Evict after commit.** The consumer evicts the cache entry after the database transaction committed, so a concurrent reader cannot re-cache the old status.
- **Docker.** Multi-stage Dockerfile, dependency layer caching, Alpine images, non-root user, and health checks so the app starts only when PostgreSQL, Redis and Kafka are ready.

## Known limitations and next steps

- **Dual write.** If the app crashes after the database commit and before the Kafka send, that event is lost. The production fix is the Transactional Outbox pattern.
- **Cache penetration.** Requests for ids that do not exist always reach the database (exceptions are not cached).
- **Redis values use JDK serialization.** Switching to a JSON serializer would make cached values readable in `redis-cli`.
- **Schema.** `ddl-auto=update` is used for simplicity. A real service would use Flyway or Liquibase.
- **Integration tests.** The current tests are unit and web-slice tests. Testcontainers tests for PostgreSQL, Kafka and Redis are the natural next step.

## Tests

```bash
./mvnw verify
```

The tests need no Docker: unit tests (Mockito) for the services and the consumer, and a `@WebMvcTest` slice for the controller.
