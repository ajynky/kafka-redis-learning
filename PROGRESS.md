# Kafka + Spring Boot + Redis Learning Project

## Status
- Current step: pushed to GitHub -> next MS0 (clear span names) or root README
- Last worked on: 8 Nov 2026 (2 instances, clientIdPrefix, own postgresDataSource bean, ShedLock verified)
- Next session: `docker compose up -d` (kafka, redis, postgres), run app with profile local

## Context (resume here)
- Stack: Spring Boot 4.1.1, Java 25, Kafka 4.1.1 (KRaft), Redis 7, Postgres 18.6 (orders_db, app/app), Hibernate 7.4.5
- App runs in UTC (TimeZone.setDefault in main); timestamps use Instant
- Current flow: POST /orders -> KafkaPublisher (sync) -> orders -> OrderConsumer (Redis order + CREATED SETNX) / PaymentConsumer -> payments -> NotificationConsumer (PAID/FAILED) -> GET /orders/{id}/status
- Step 15 target: POST /orders -> @Transactional save orders row + outbox row -> OutboxRelay @Scheduled -> Kafka (eventId header) -> idempotent consumers
- Postgres = source of truth for orders; Redis stays as read model/status
- Test profile has no DB -> mvn verify fails; use -DskipTests (test DB in Good to have)
- 15.3 spec: package `entity`; OrderEntity (table orders: orderId String @Id, orderData, createdAt Instant); OutboxEvent (table outbox: id Long IDENTITY, topic, messageKey, payload, createdAt, publishedAt nullable); repos OrderEntityRepository, OutboxRepository (+ derived query: first 100 with publishedAt null, order by id asc); Lombok @Getter @Setter @NoArgsConstructor (no @Data)

## Project flow
POST /orders -> Producer -> Kafka topic "orders" -> Consumer -> Redis -> GET /orders/{id}

## Day 1 Steps
- [x] Step 0: Setup check - Java 25.0.4.1, Docker 29.6.2 (IDE: IntelliJ; Postman to confirm)
- [x] Step 1: Learn concepts (quick check passed 3/3)
- [x] Step 2: Kafka (apache/kafka:4.1.1) + Redis (redis:7) running in Docker
- [x] Step 3: Kafka CLI practice (create, describe, produce, consume, --from-beginning)
- [x] Step 4: Redis CLI practice (SET, GET, DEL, KEYS, EX/TTL)
- [x] Step 5: Spring Boot 4.1.1 project created and running on port 8080

## Next Steps (unlocked after Day 1)
- [x] Step 6: Producer - POST /orders -> KafkaTemplate -> topic orders + bonus partition/offset log, key->partition confirmed
- [x] Step 7: Consumer @KafkaListener with ConsumerRecord; consumer groups, committed offsets, LAG, offset reset
- [x] Step 8: Consumer saves to Redis (order:<key>, fallback order:noKey<partition>-<offset>); Redis = latest state
- [x] Step 9: GET /orders/{id} via RedisService, Order with @Builder, 404 when missing
- [-] Step 10: JSON messages - SKIPPED (user choice, keep plain String messages)
- [x] Step 11: payments topic (2 partitions) via @Bean NewTopic + TopicBuilder, yaml topic.<name>.*
- [x] Step 12: Error handling: consumer retry + orders-dlt, producer sync send (503), Redis down (503)
- [x] Step 13: Embedded Kafka flow test (OrderFlowTest). Rest moved to Good to have
- [x] Step 14: Final project - Order -> Payment -> Notification, 3 consumer groups, status in Redis (setIfAbsent for CREATED), GET /orders/{id}/status
- [x] Step 15: Transactional Outbox (PostgreSQL in Docker + Spring Data JPA, outbox table, @Scheduled publisher, idempotent consumer)
  - [x] 15.1 Postgres 18 in docker-compose (postgres:18.6-alpine3.24, db orders_db, user app, volume pgdata)
  - [x] 15.2 pom: data-jpa + postgresql driver; datasource in application-local.yaml (HikariPool started, dialect auto = PostgreSQLDialect)
  - [x] 15.3 Entities + JpaRepository: OrderEntity (orders), OutboxEvent (outbox), findTop100ByPublishedAtIsNullOrderByIdAsc
  - [x] 15.4 OrderService @Transactional: save order + outbox row (no Kafka call in request)
  - [x] 15.5 OutboxRelay @Scheduled: unpublished rows -> Kafka (eventId header) -> mark published
  - [x] 15.6 Idempotent consumers: skip already-processed eventId
  - [x] 15.7a Verify: Kafka down -> POST OK -> Kafka up -> relayed (done in 15.5); replay (published_at=null) -> "Skip duplicate eventId=4", no 2nd payment/notification
  - [-] 15.7b Test DB - moved to Good to have (build with -DskipTests)

## Stuck / Questions
- (none yet)

## Resources (verified)
- Kafka Quickstart: https://kafka.apache.org/quickstart
- Confluent Spring course: https://developer.confluent.io/courses/spring/apache-kafka-intro/
- Spring Kafka reference: https://docs.spring.io/spring-kafka/reference/reference.html
- Spring Data Redis reference: https://docs.spring.io/spring-data/redis/reference/
- Baeldung Spring Kafka articles: https://www.baeldung.com/tag/spring-kafka

## Pending cleanups
- [x] Outbox cleanup: switch test values to cleanup-cron "0 0 3 * * *" and retention 7d
- [x] Scheduler error handler: ShedLock lock call fails when Postgres down -> full stack trace every run (TaskUtils$LoggingErrorHandler); log one line instead
- [x] Silence LoggingProducerListener (logs payload = customer data at ERROR on send failure)
- [x] spring.jpa.show-sql: false (relay poll query every second)
- [x] Delete OrderProducer.java (unused; OrderFlowTest uses KafkaPublisher)
- [x] Timezone: -Duser.timezone=UTC in IntelliJ VM options, remove TimeZone.setDefault from main
- [x] spring.datasource.hikari.connection-timeout: 2000 (503 took 30s)
- [x] ApiErrorHandler: fixed messages for DataIntegrityViolation (409) / CannotCreateTransaction (503), rename handlers, log.error for DB down
- [x] spring.jpa.open-in-view: false (removes startup WARN)
- [ ] (optional) -Duser.timezone=UTC in surefire argLine for tests
- [x] Reusable KafkaPublisher.publish(topic, key, value) (removed duplicated send code)
- [x] Move spring.kafka.consumers.* to app.kafka.consumers.*
- [x] OrderConsumer skips keyless records (consistent with PaymentConsumer)
- [x] Redis key prefixes as constants in OrderRepository
- [x] AppKafkaProperties instead of @Value in OrderProducer/PaymentConsumer
- [x] Record logs at DEBUG in all consumers
- [x] KafkaConfig: helper newTopic(Topic t) for the 3 NewTopic beans
- [x] OrderStatusResponse record for GET /status
- [x] Remove unused (Consumers record, @Slf4j in OrderProducer)
- [x] Repository debug logging (key only)
- [x] Request logging filter (OncePerRequestFilter: method, URI, param names, status, duration)

## Next plan (multi-instance)
- [x] spring.jpa.hibernate.ddl-auto: validate (tables already exist; schema changes now = manual SQL until Flyway)
- [x] Run 2 local instances (IntelliJ run config copy, -Dserver.port=8081, Allow multiple instances) -> observe partition split + duplicate relay
- [x] ShedLock on OutboxRelay (only one pod runs it; lock row in DB)
- [x] Outbox cleanup job (OutboxCleanupService: cron + retention, @Modifying bulk delete, ShedLock "outboxCleanup") - verified: deleted 8 rows
- [x] Central logging local: grafana/otel-lgtm (Grafana + Loki + Tempo + Prometheus + OTel collector in one container) + spring-boot-starter-opentelemetry (trace ID in logs) + OTel logback appender -> logs via OTLP
  - [x] CL1 otel-lgtm in docker-compose (3000 UI, 4317 gRPC, 4318 HTTP)
  - [x] CL2 starter-opentelemetry -> traceId/spanId in log lines
  - [x] CL3 OTel logback appender + logging export endpoint
  - [x] CL4 Grafana Explore -> Loki: logs visible, search by text/traceId
  - [x] CL5 Link POST (trace A) to Kafka flow (trace B): option 1 log orderId+eventId in OrderService
  - [x] CL6 option 2: trace_parent column in outbox -> relay continues trace A (one trace POST -> notification)
- [ ] (last) Flyway versioned migrations (V1__...sql, flyway_schema_history, ddl-auto validate)

## Secrets + GitHub (done 8 Nov 2026)
- [x] .env (root, NOT committed) read by docker-compose (${POSTGRES_*}) and Spring (spring.config.import optional:file:../../.env[.properties]); yaml has placeholders only
- [x] Root .gitignore (.env, **/target/, .idea/, *.iml, _to_delete/, *.zip) + .env.example (committed, placeholder values)
- [x] First commit 4daa41f pushed to GitHub (origin/main)
- [x] Root README: how to run (setup, schema SQL, run, try, observability, commands)

## Next plan (production-style microservices)
- [ ] MS0 Clear span names per listener (payment.process, notification.send) - quick win before split
- [ ] MS1 Maven multi-module: parent pom + modules order-service, payment-service, notification-service (+ shared "contracts" module for topic names, header names like eventId)
- [ ] MS2 order-service: POST/GET APIs, Postgres (orders + outbox), OutboxRelay, OrderConsumer (Redis read model) - own spring.application.name
- [ ] MS3 payment-service: PaymentConsumer + idempotency (own Redis keys), publishes payments
- [ ] MS4 notification-service: NotificationConsumer, status updates
- [ ] MS5 Data ownership: each service owns its data (no shared tables); decide who owns order-status (order-service consumes payments vs notification writes)
- [ ] MS6 Each service: own port, profile, service.name -> separate nodes in Tempo node graph / Service Graph
- [ ] MS7 Containerize each service (Dockerfile, UTC) + docker-compose with all services + infra (service names instead of localhost; Kafka listener config for in-network access)
- [ ] MS8 End-to-end check: POST -> one trace across 3 services in Tempo; scale payment-service to 2 replicas -> partition split
- [ ] MS9 Production concerns: Actuator health/readiness, config via env/secrets, Flyway per service DB

## Good to have (later)
- [ ] Test DB for test profile (Testcontainers Postgres + @ServiceConnection recommended; or H2). Until then: mvn clean verify -DskipTests. OrderFlowTest also needs Redis/ProcessedEventRepository mocked
- [ ] Containerize app: Dockerfile (eclipse-temurin:25-jre, ENTRYPOINT java -Duser.timezone=UTC -jar), app service in docker-compose, profile with service names (kafka:9092 needs KRaft listener config for in-network access, redis, postgres) instead of localhost
- [ ] Logback DuplicateMessageFilter to drop repeated log lines (Kafka reconnect flood)
- [ ] Spring Boot Actuator health checks + consumer lag metrics/alerts (detect outages without reading logs)
- [x] Configure Mockito as -javaagent in maven-surefire-plugin (removes dynamic agent warning, needed for future JDKs)
- [ ] Test: DLT test with embedded Kafka (mock save throws -> record in orders-dlt, KafkaTestUtils.getSingleRecord)
- [ ] Test: MockMvc tests for API errors (404, 400, 405, 503)
