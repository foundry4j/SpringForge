# SpringForge

<img src="docs/forge-logo.svg" alt="SpringForge logo" align="right" width="128" height="128" />

[![Maven Central](https://img.shields.io/maven-central/v/io.github.foundry4j/quarkforge-core?logo=apachemaven&color=blue)](https://central.sonatype.com/artifact/io.github.foundry4j/quarkforge-core)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=foundry4j_SpringForge&metric=coverage)](https://sonarcloud.io/summary/new_code?id=foundry4j_SpringForge)
![Java 27+](https://img.shields.io/badge/Java-27%2B-ED8B00?logo=openjdk&logoColor=white)
[![CI](https://github.com/foundry4j/SpringForge/actions/workflows/ci.yml/badge.svg)](https://github.com/foundry4j/SpringForge/actions/workflows/ci.yml)

A modular rapid microservice development framework based on Spring Boot, with built-in everyday development fundamentals such as unified responses, exception handling, data access, security authentication, rate limiting, Redis, and observability.

## Modules

| Module | Responsibility |
| --- | --- |
| `forge-core` | Error codes, domain exceptions, `ApiResult` / `PageResult`, `ScopedValue` based `ForgeContext`, SpEL evaluator, ID and duration helpers |
| `forge-cache` | Caffeine local cache, Redis cache, two-level cache with graceful Redis degradation, `KeyValueStore` abstraction |
| `forge-ratelimit` | Standalone `@RateLimit` aspect, `KeyStrategy` dimensions, pluggable `RateLimiter` backend (in-memory or Redis) |
| `forge-idempotency` | Standalone `@Idempotent` aspect, cached result replay, pluggable `IdempotentStore` (in-memory or Redis) and key resolver |
| `forge-lock` | Local and Redis lock managers, safe Lua based release, `@DistributedLock` aspect |
| `forge-web` | Bean validation extensions, RFC 9457 global error handling, response wrapping, request context filter, API versioning, access log, local file storage, HTTP resolvers for the guard modules |
| `forge-validation` | Reusable constraints: ID card, bank card (Luhn), password strength, enum name |
| `forge-data` | `BaseEntity` / `TenantEntity`, JPA auditing, field level change audit, multi-tenancy, soft delete, snowflake ids, cached hierarchical data dictionary |
| `forge-crypto` | AES-256-GCM `CryptoService`, JPA `@Encrypted` converter, `@Sensitive` masking serializers |
| `forge-security` | Login user context, stateless HS256 JWT service, servlet authentication filter, `@RequiresRole` / `@RequiresPermission` |
| `forge-audit` | Entity change audit log and `@OperationLog` aspect backed by JDBC writes |
| `forge-resilience` | `@FaultTolerant`: circuit breaker, retry and fallback |
| `forge-messaging` | Broker neutral `ForgeMessage`, local / RabbitMQ / Kafka transports, `@MessageConsumer` registration |
| `forge-outbox` | Transactional outbox, pessimistic claiming, exponential backoff, stuck event recovery, background relay |
| `forge-notification` | Persistent notification log, `NotificationSender` SPI for system, e-mail and SMS delivery, inbox API, WebSocket push, exponential retry |
| `forge-scheduling` | JobRunr facade, `@ForgeScheduled`, annotation driven registration, persistent and distributed jobs |
| `forge-excel` | Streaming `.xlsx` / CSV export with POI SXSSF, streaming import with row level errors, Spring MVC helpers |
| `forge-rpc` | gRPC status mapping of business errors, trace id propagation interceptors |
| `forge-observability` | Actuator health indicator and optional OpenAPI `bearerAuth` scheme |
| `forge-migration` | Flyway defaults for `classpath:db/migration` with baseline enabled |
| `forge-test` | `@CleanDatabase` JUnit extension and `TestAuth` helpers |
| `forge-bom` | Gradle `java-platform` constraining every Forge module |
| `forge-app` | Runnable sample application: orders, dictionary, inventory fallback, expiry job, Flyway migration |

## Features

### Validation and errors

Request bodies are validated with Jakarta Bean Validation. Custom validators cover mobile
numbers and enum names, and every failure is rendered as an RFC 9457 problem detail with a
stable business code, the trace ID and a `violations` array.

```java
public record CreateOrderRequest(
        @NotBlank @Size(max = 128) String customerName,
        @Phone @Size(max = 32) String phone,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
```

### Rate limiting and idempotency

Two independent modules provide the protections. `forge-ratelimit` only depends on
`forge-core` and AspectJ; `forge-idempotency` adds Jackson to serialize replayed results.
Neither module requires a servlet container.

```java
@PostMapping
@RateLimit(permits = 30, windowSeconds = 60)   // per authenticated user by default
@Idempotent(ttlSeconds = 600)                  // key from Idempotency-Key or arguments
public OrderView create(@Valid @RequestBody CreateOrderRequest request) {
    return service.create(request);
}
```

`@RateLimit` exposes `KeyStrategy.USER`, `KeyStrategy.IP` and `KeyStrategy.GLOBAL`; the quota
is enforced by a `RateLimiter` bean. The default is an in-memory fixed window, which is only
correct for a single instance: every node would keep its own counter.

`@Idempotent` caches the successful result for `ttlSeconds`. A duplicate received while the
first call is still running fails with a conflict; a duplicate received afterwards gets the
cached result without invoking the method again. Keys come from expressions such as
`@Idempotent(key = "order:#request.orderNo")`, from the `Idempotency-Key` header in web
applications, or from the serialized arguments when neither is present. State is stored by
an `IdempotentStore` bean, in memory by default; a cluster needs a shared store as well.

Non-web workloads drive both modules with expressions:

```java
@RateLimit(permits = 500, windowSeconds = 60, key = KeyStrategy.GLOBAL)
@Idempotent(key = "event:#event.id", ttlSeconds = 3600)
public void onEvent(OrderEvent event) {
    // message consumer: no HTTP request involved
}
```

#### Distributed deployments

The in-memory backends keep their state inside the JVM, so with more than one instance a
quota would be multiplied by the number of nodes and two nodes could execute the same
idempotent request. Point both modules at Redis to share the state:

```yaml
forge:
  ratelimit:
    provider: redis          # shared fixed window
    key-prefix: "forge:ratelimit:"
  idempotency:
    provider: redis          # shared PROCESSING / DONE state
    key-prefix: "forge:idempotency:"
```

The Redis backend is activated only when Spring Data Redis is on the classpath and the
property is set to `redis`; otherwise the in-memory default stays in place, so modules
without Redis keep working unchanged. Both backends are written as Lua scripts, which makes
the counter increment atomic and lets exactly one node reserve an idempotency key, while
every other node receives the cached result or a conflict. Any `RateLimiter` or
`IdempotentStore` bean declared by the application still takes precedence.

### Distributed locks

```java
@DistributedLock(key = "#orderId", waitTime = 100, leaseTime = 5000)
public void settle(long orderId) {
    // only one instance at a time
}
```

The default provider is local; set `forge.lock.provider=redis` to use Redis with
`SET NX PX` and compare-and-delete release.

### Persistence

`BaseEntity` contributes the primary key, audit columns, optimistic locking and `@SoftDelete`.
`TenantEntity` adds a `@TenantId` discriminator that Hibernate applies to every query.

```java
@Entity
@Table(name = "forge_order")
public class Order extends TenantEntity {
    // tenant isolation, auditing and soft delete come from the superclass
}
```

### Dictionaries

Dictionaries are cached and support hierarchical items through `parentCode` plus `level`.

```java
List<DictItemView> items = dictService.items("order_status");
String label = dictService.translate("order_status", "PAID");
```

`/api/dicts/{typeCode}` serves the cached items; `/api/admin/dicts` offers CRUD for types
and items. Every mutation evicts the affected cache entries, and the enabled dictionaries
are preloaded at startup.

### File storage and API versions

`FileStorage` stores uploads under `forge.web.file.location` with size and content-type
validation and path traversal protection. Downloads stream the file with an RFC 5987
`Content-Disposition`. `@ApiVersion` resolves the version from the `X-Api-Version` header or
a `vN` path segment and renders `Deprecation` / `Sunset` hints for deprecated versions.

### Excel import and export

```java
ExcelExporter.write(out, ExcelFormat.XLSX,
        ExcelExportSpec.<Order>builder("Orders")
                .column("Order no", Order::orderNo)
                .column("Amount", Order::amount)
                .build(),
        orderStream);

ImportResult result = ExcelHttp.importUpload(file,
        ExcelImportSpec.<Order>builder()
                .mapper(row -> new Order(row.cell("Order no"), new BigDecimal(row.cell("Amount"))))
                .validator((order, ctx) -> {
                    if (order.amount().signum() <= 0) {
                        ctx.reject("amount", order.amount().toString(), "Amount must be positive");
                    }
                })
                .rowSink(orders::add)
                .build());
```

Export streams through POI `SXSSFWorkbook` (constant memory) or CSV with an optional UTF-8
BOM. Import streams too, maps each row, runs the validators and reports failed rows as
`RowError` instead of aborting, with an optional error cap.

### Security and audit

```java
@RequiresPermission("order:close")
public void close(long orderId) {
    LoginUser user = AuthContextHolder.require();   // id, username, roles, permissions
}
```

`JwtTokenService` issues and verifies HS256 tokens when `forge.jwt.secret` is configured.
`@OperationLog` records method arguments, result, error, duration and the trace id;
entity changes are captured by the JPA audit listener into `forge_audit_log`.

### Observability and testing

`forge-observability` registers a `forge` health component and, when springdoc is present,
adds the `bearerAuth` security scheme. `forge-test` contributes a JUnit extension and
identity helpers:

```java
@SpringBootTest
@CleanDatabase
class UserRepositoryTest {
    @Test
    void adminCanCloseOrder() {
        TestAuth.authenticate(Set.of("ADMIN"));
        // ...
    }
}
```

### Fault tolerance

```java
@FaultTolerant(name = "inventory", fallback = "fallbackStock")
public int stock(long sku) {
    return remoteInventory.stock(sku);
}

private int fallbackStock(long sku, Throwable cause) {
    return 0;
}
```

Circuit breaker and retry are both applied; the fallback may accept the original arguments,
the `Throwable`, or both.

### Messaging and outbox

Business code writes an event into the outbox inside the same transaction:

```java
@Transactional
public OrderView create(CreateOrderRequest request) {
    Order order = repository.save(newOrder(request));
    outbox.record("order.created", "order.created", OrderView.from(order), Map.of());
    return OrderView.from(order);
}
```

A background relay claims pending rows with `SELECT ... FOR UPDATE`, delivers them to the
configured publisher and retries failed deliveries with exponential backoff. Without a broker
the events are written to the log, so local development needs no infrastructure.

### Notifications

Every notification is persisted in `forge_notification` with its channel, recipient, status
and delivery attempts. Three channels are supported out of the box through the
`NotificationSender` SPI: `SYSTEM`, `EMAIL` and `SMS`.

```java
notificationService.send(NotificationRequest.system("user-1", "Order paid", "Thanks Ada"));
notificationService.send(NotificationRequest.email("ada@example.com", "Order paid", "Thanks Ada"));
notificationService.send(NotificationRequest.sms("13800138000", "Order paid", "Thanks Ada"));
```

The built-in senders write to the log, so the module works without an SMTP server or an SMS
gateway. Declare a `NotificationSender` bean for a channel to plug a real provider; it
replaces the built-in sender of that channel:

```java
@Bean
NotificationSender smtpNotificationSender(JavaMailSender mailSender) {
    return new SmtpNotificationSender(mailSender);
}
```

A failed delivery stays `PENDING` and is retried by a background dispatcher with an
exponential backoff. Once the attempt budget is exhausted the notification becomes `FAILED`
and can be replayed with `notificationService.retry(id)`. A delivery failure never rolls back
the calling business transaction.

### Scheduling

```java
@ForgeScheduled(cron = "0 */10 * * * *", name = "order-expiry")
public void expireOverdueOrders() {
    orderService.expireOverdue(Duration.ofMinutes(30));
}
```

Annotated methods are registered as recurring JobRunr jobs after the context is refreshed.
JobRunr stores the schedule in the application database, so it survives restarts, is shared by
every node and is processed by the background job servers. Programmatic jobs use the JobRunr
lambda API through `ForgeScheduler`:

```java
forgeScheduler.enqueue(() -> reportService.buildDailyReport());
forgeScheduler.scheduleCron("weekly", "0 0 3 * * MON", () -> reportService.archive());
forgeScheduler.cancel("weekly", "forge");
```

Cron expressions use the JobRunr format, either five fields or six fields with seconds first.
The background job server and the dashboard are controlled by JobRunr's own properties:

```yaml
jobrunr:
  background-job-server:
    enabled: true
  dashboard:
    enabled: false
```

## Requirements

- JDK 27
- Gradle wrapper (included)
- Docker only if you want to run the Redis, RabbitMQ, Kafka or PostgreSQL variants

## Quick start

```bash
./gradlew build
./gradlew :forge-app:bootRun
```

The sample application starts on port 8080 with an in-memory H2 database, Flyway migrations,
local cache, local locks, the outbox relay and the JobRunr background job server.

```bash
curl -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -H 'X-Tenant-Id: acme' \
  -d '{"customerName":"Ada","phone":"13800138000","amount":99.90}'

curl localhost:8080/api/dicts/order_status
curl localhost:8080/api/inventory/42
```

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `forge.context.default-tenant` | `default` | Tenant used when no `X-Tenant-Id` header is present |
| `forge.web.response-wrapping` | `true` | Wraps successful responses into `ApiResult` |
| `forge.web.access-log.enabled` | `true` | Writes one access log line per request |
| `forge.web.api-version.enabled` | `true` | Enables `@ApiVersion` negotiation |
| `forge.web.api-version.default-version` | `v1` | Version assumed when the caller sends none |
| `forge.web.file.base-dir` | `${java.io.tmpdir}/forge-files` | Root directory of uploaded files |
| `forge.web.file.max-size` | `10MB` | Maximum accepted upload size |
| `forge.data.worker-id` | `0` | Snowflake worker identifier, must be unique per node |
| `forge.data.dict.warmup` | `true` | Preloads enabled dictionaries at startup |
| `forge.crypto.key` | – | AES key material, required to enable `CryptoService` |
| `forge.jwt.secret` | – | HS256 secret, required to enable JWT authentication |
| `forge.jwt.issuer` | `forge` | Expected `iss` claim |
| `forge.jwt.ttl` | `1h` | Lifetime of issued tokens |
| `forge.audit.enabled` | `true` | Captures entity changes into `forge_audit_log` |
| `forge.rpc.trace.enabled` | `true` | Propagates the trace id through gRPC metadata |
| `forge.observability.enabled` | `true` | Registers the framework health indicator |
| `forge.cache.provider` | `local` | `local`, `redis` or `tiered` |
| `forge.cache.local-ttl` | `30s` | Local cache entry lifetime |
| `forge.ratelimit.provider` | `local` | `local` or `redis` |
| `forge.ratelimit.key-prefix` | `forge:ratelimit:` | Prefix of the Redis quota keys |
| `forge.idempotency.provider` | `local` | `local` or `redis` |
| `forge.idempotency.key-prefix` | `forge:idempotency:` | Prefix of the Redis idempotency keys |
| `forge.lock.provider` | `local` | `local` or `redis` |
| `forge.lock.wait-time` | `2s` | Maximum wait for a lock |
| `forge.messaging.enabled` | `false` | Enables the RabbitMQ / Kafka publisher setup |
| `forge.messaging.broker` | `none` | `none`, `rabbitmq` or `kafka` |
| `forge.outbox.enabled` | `true` | Enables outbox storage and relay |
| `forge.outbox.batch-size` | `50` | Events claimed per relay pass |
| `forge.outbox.max-attempts` | `8` | Delivery attempts before an event is marked failed |
| `forge.notification.enabled` | `true` | Enables the notification log and the sender SPI |
| `forge.notification.retry-enabled` | `true` | Retries pending notifications in the background |
| `forge.notification.max-attempts` | `3` | Delivery attempts before a notification is marked failed |
| `forge.notification.retry-delay` | `1m` | Base delay of the exponential backoff |
| `forge.notification.poll-interval` | `30s` | Pause between two retry passes |
| `forge.notification.email-enabled` | `false` | Fans user notifications out to e-mail |
| `forge.notification.sms-enabled` | `false` | Fans user notifications out to SMS |
| `forge.notification.web-socket-path` | `/ws/notifications` | Endpoint of the real-time notification socket |
| `forge.scheduling.enabled` | `true` | Enables the JobRunr facade and the `@ForgeScheduled` registrar |
| `forge.scheduling.group` | `forge` | Prefix of the recurring job identifiers |
