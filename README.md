<table>
  <tr>
    <td width="110" align="center">
      <img src="docs/forge-logo.svg" width="100" alt="SpringForge logo"/>
    </td>
    <td>
      <h1>SpringForge</h1>
      <p>A modular starter kit for Spring Boot 4 microservices on Java 27.</p>
    </td>
  </tr>
</table>

[![Java](https://img.shields.io/badge/Java-27-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.8.2-02303A.svg)](https://gradle.org/)
[![JUnit](https://img.shields.io/badge/JUnit-6.0.3-25A162.svg)](https://junit.org/)
[![JaCoCo](https://img.shields.io/badge/JaCoCo-report-yellowgreen.svg)](https://www.jacoco.org/jacoco/)

## Overview

SpringForge packages the cross-cutting concerns of a microservice into small, independently
usable Gradle modules. Every module ships an auto-configuration, sensible defaults and tests,
so an application only has to depend on the modules it needs and keep its own code focused on
the domain.

- Spring Boot 4.1 with Spring Framework 7, Jackson 3 and Hibernate 7
- Java 27 language level, Markdown `///` documentation comments, unnamed variables
- Local-first defaults: the sample application runs without Redis, RabbitMQ, Kafka or an
  external database
- Production-ready primitives: RFC 9457 errors, idempotency, rate limiting, distributed
  locks, multi-tenant persistence, transactional outbox and Quartz scheduling

## Modules

| Module | Responsibility |
| --- | --- |
| `forge-core` | Error codes, domain exceptions, `ApiResult` / `PageResult`, `ScopedValue` based `ForgeContext`, SpEL evaluator, ID and duration helpers |
| `forge-cache` | Caffeine local cache, Redis cache, two-level cache with graceful Redis degradation, `KeyValueStore` abstraction |
| `forge-lock` | Local and Redis lock managers, safe Lua based release, `@DistributedLock` aspect |
| `forge-web` | Bean validation extensions, RFC 9457 global error handling, response wrapping, `@Idempotent`, `@RateLimit`, request context filter |
| `forge-data` | `BaseEntity` / `TenantEntity`, JPA auditing, multi-tenancy, soft delete, cached data dictionary |
| `forge-resilience` | `@FaultTolerant`: circuit breaker, retry and fallback |
| `forge-messaging` | Broker neutral `ForgeMessage`, RabbitMQ and Kafka publishers, type based routing |
| `forge-outbox` | Transactional outbox, pessimistic claiming, exponential backoff, stuck event recovery, background relay |
| `forge-scheduling` | Quartz facade, `@ForgeScheduled`, `AbstractForgeJob`, injection aware job factory |
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

### Idempotency and rate limiting

```java
@PostMapping
@Idempotent(header = "Idempotency-Key", ttlSeconds = 600)
@RateLimit(name = "order-create", permits = 30, windowSeconds = 60)
public OrderView create(@Valid @RequestBody CreateOrderRequest request) {
    return service.create(request);
}
```

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

### Scheduling

```java
@ForgeScheduled(cron = "0 */10 * * * ?", name = "order-expiry")
public void expireOverdueOrders() {
    orderService.expireOverdue(Duration.ofMinutes(30));
}
```

Jobs are registered after context refresh. Programmatic registration, one-shot jobs, pause,
resume and cancel are available through `ForgeScheduler`.

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
local cache, local locks, the outbox relay and Quartz enabled.

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
| `forge.cache.provider` | `local` | `local`, `redis` or `tiered` |
| `forge.cache.local-ttl` | `30s` | Local cache entry lifetime |
| `forge.lock.provider` | `local` | `local` or `redis` |
| `forge.lock.wait-time` | `2s` | Maximum wait for a lock |
| `forge.messaging.enabled` | `false` | Enables the RabbitMQ / Kafka publisher setup |
| `forge.messaging.broker` | `none` | `none`, `rabbitmq` or `kafka` |
| `forge.outbox.enabled` | `true` | Enables outbox storage and relay |
| `forge.outbox.batch-size` | `50` | Events claimed per relay pass |
| `forge.outbox.max-attempts` | `8` | Delivery attempts before an event is marked failed |
| `forge.scheduling.enabled` | `true` | Enables the Quartz facade and job registrar |