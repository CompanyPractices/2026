# Code Review — SMP Project

**Date:** 2026-06-11
**Project:** SMP — educational intern project
**Modules:** gateway, switch, authorization, card-management, terminal-simulator, merchant-acquirer, transaction-logger, dashboard, common

**Reviewer:** qwen/qwen3.7-max

---

## Introduction

### Goals

Review across 5 topics:
1. **Basics** — code duplication, hardcode, mixed responsibilities, mixed layers, improper style
2. **Database integrity** — duplicate/improper unique handling, table inconsistencies, unhandled transaction failures, huge resultsets, missing indexes
3. **Concurrency** — API handler safety under concurrent requests
4. **Unit tests quality** — inefficient structure, undercoverage or overcoverage
5. **Other** — suspicious findings not fitting above

### Context

Educational intern project. Not real fintech. Findings focus on teachable improvements.

## Reviewer task (review)

Review the code and write it into llm/protocols/review-2026-06-11/review.md.

Don't forget it is an educational project, not the real fintech processing app.

Review structure:
- Introduction with review goals.
- Section per module.
- Review topics in subsections.

Review topics, which also define areas of review, are:

1. Basics of Software Design and coding.
   E.g., code duplication, hardcode, mixed responsibilities, mixed layers, improper style, etc.

2. Database integrity.
   E.g. possible duplication or improper uniqs handling, inconsistencies among related tables, not handled transaction failures, not handled huge resultsets, not indexed selects to possibly large tables.

3. Concurrency issues.
   Check API handlers execution paths safety in case of several concurrent requests.

4. Unit tests quality.
   E.g. inefficient test structure, undercoverage or overcoverage.

5. Other.
   Here you put everything you find suspicious that doesn't fall into topics above.

Review rules:
- Use caveman skill and AX when writing review file.
- Reference files by `filename:linenumber`.
- Russian in comments is fine.

## Reviewer task (opponent feedback)

Under llm/protocols/review-2026-06-11/ folder, review.md was validated by opponents and their feedback
placed into review-validation-*.md files. Process these files and improve review.md:
- You may (partially) agree or disagree with every feedback statement.
- When agreed, correct review.md accordingly and double check nothing else is wrong with corrected statement.
- When disagreed, add visually separated note into the feedback file near the corresponding statement.

## Opponent task

Validate code review in llm/protocols/review-2026-06-11/review.md:
- Cross-check every statement against source code.
- Check every statement for clarity and correctness.
- Report inconsistencies only, don't respond for statements which are ok.
- Use caveman skill and AX when writing output file.
- Output into llm/protocols/review-2026-06-11/review-validation.md.

---

## Common Module

### 1. Basics

- **Mixed responsibilities in DTO**: `AuthorizationRequest` (`services/common/src/main/java/com/processing/common/dto/authorization/AuthorizationRequest.java:72-83`) contains business methods `withIssuerId()` and `forReversal()`. DTO should be pure data carrier. Move factory logic to service layer or separate builder. Note: `forReversal(String rrn)` accepts `rrn` parameter but never uses it — dead parameter.

- **ErrorResponse uses String timestamp**: `ErrorResponse` (`services/common/src/main/java/com/processing/common/dto/ErrorResponse.java:6`) stores timestamp as `String`. Callers must format manually. Use `Instant` or `LocalDateTime` for type safety.

- **No tests**: Common module has zero test files. Validators (BinValidator, PanValidator, etc.) are simple but should still have unit tests — they are shared across all services.

### 2. Database Integrity

N/A — no DB in common module.

### 3. Concurrency

N/A — pure DTOs and validators.

### 4. Unit Tests

- **Zero coverage**: No `src/test/` directory exists. All 6 custom validators and 17 DTOs untested. Since every service depends on common, bugs here propagate everywhere.

### 5. Other

- **CardModelStatus vs CardStatus duplication**: `CardModelStatus` (`services/common/src/main/java/com/processing/common/dto/cardmanagement/CardModelStatus.java`) has 4 values (ACTIVE, INACTIVE, BLOCKED, EXPIRED). Card-management defines its own `CardStatus` with 5 values (adds DELETED). Authorization service uses `CardModelStatus` enum directly. `CardStatus.java` (`services/authorization/src/main/java/com/processing/authorization/constants/CardStatus.java`) — utility class with String constants — is dead code (never imported or referenced anywhere). Confusing for interns.

---

## Gateway

### 1. Basics

- **Duplicated `isDownstreamUnavailable`**: `DownstreamErrorFilter:59-75` and `CircuitBreakerFilter:108-124` contain identical exception-chain traversal logic. Extract to shared utility.

- **Duplicated `rethrow` method**: `DownstreamErrorFilter:96-108` and `CircuitBreakerFilter:138-150` have identical `rethrow(Exception)` methods. Same fix — extract.

- **Hardcoded path strings**: `TransactionValidationFilter:46`, `TransactionRateLimitFilter:31` both define `TRANSACTIONS_PATH = "/api/transactions"`. Single constant in shared location preferred.

- **`ErrorResponse.serviceName` always null**: `TransactionValidationFilter:86-92` passes `null` for `serviceName`. Should pass `"gateway"`.

### 2. Database Integrity

N/A — gateway has no DB.

### 3. Concurrency

- **Rate limiter memory leak**: `InMemoryRateLimiter:34` — `ConcurrentHashMap` grows unbounded. Old keys never evicted. In current usage (single key `POST /api/transactions`) not a real problem, but if keys become per-client or per-IP, memory leak.

- **Rate limiter per-instance only**: `InMemoryRateLimiter` — single-instance in-memory. If gateway scales to multiple replicas, rate limit not shared. Acceptable for educational project, but worth noting.

### 4. Unit Tests

- **Good coverage**: 11 test files covering filters, rate limiter, circuit breaker, validator, integration. Well-structured with MockMvc and standalone tests.
- **Missing**: No test for `DownstreamServiceResolver` path matching. No test for `OpenApiFilter`.

### 5. Other

- **ResponseCachingFilter never invalidates cache**: `ResponseCachingFilter:70-73` caches GET `/api/cards/**` responses. But POST/PATCH/DELETE to `/api/cards/**` pass through without cache eviction. Stale data served after card updates. Add cache eviction on non-GET methods.

- **ResponseCachingFilter catches all exceptions → 503**: `ResponseCachingFilter:77-85` catches any `Exception` during filter chain and returns 503. This hides real errors (e.g., validation errors, auth errors) behind "Card Management Service is temporarily unavailable". Should only catch downstream connectivity exceptions.

---

## Switch

### 1. Basics

- **RouteService mixes orchestration and mapping**: `RouteService:95-122` — `buildTransaction()` and `toInstant()` are private mapping methods in an orchestration service. Acceptable for educational scope, but could be extracted.

- **`toInstant` fallback for null/blank but not for malformed**: `RouteService:128-137` — returns `Instant.now()` for null/blank input (line 130). But if `transmissionDateTime` is non-blank and unparseable by both `Instant.parse()` and `LocalDateTime.parse()`, exception propagates uncaught.

- **HealthResponse records share name but differ in shape**: `switch/dto/HealthResponse.java` has `status, service, version, dependencies`. `authorization/dto/HealthResponse.java` has `status, service, dependencies` (no version). `card-management/models/HealthResponse.java` has `status, service, cardsInDatabase` (no dependencies). Overlap on `status`+`service` but not identical. Could move common base to common module.

### 2. Database Integrity

N/A — switch has no DB.

### 3. Concurrency

- **Stateless service**: Switch is fully stateless. No concurrency issues in handler paths. RestClient and Retry instances are thread-safe.

### 4. Unit Tests

- **Good structure**: 6 test classes (+ 3 support files: test data helper and hand-written test doubles). Uses hand-written test doubles (`CapturingAuthorizationClient`, `TrackingLoggerClient`) instead of mocks — good practice for educational project.
- **Missing**: No test for `MerchantAcquirerClient`. No test for `AcquiringFeeClient` interface implementation. No test for `RetryFactory` backoff logic.

### 5. Other

- **GET with body in MerchantAcquirerClient**: `MerchantAcquirerClient:27-31` sends GET request with body (`AcquirerFeeRequest`). HTTP GET with body is technically allowed but widely discouraged — many proxies and servers ignore/drop GET bodies. Switch should use query params or POST.

- **Authorization retry uses 1ms interval**: `RetryFactory:58` — `IntervalFunction.of(MIN_INTERVAL_MS)` where `MIN_INTERVAL_MS = 1`. Authorization retries happen with essentially no delay. Defeats purpose of retry for transient failures.

- **Reversal fire-and-forget**: `AuthorizationClient:62-73` — reversal sends request but swallows all exceptions. If reversal fails, money stays reserved on card. Acceptable for educational project. Already logs at ERROR level (line 72), which is good.

---

## Authorization

### 1. Basics

- **Dead code — CardStatus constants**: `services/authorization/src/main/java/com/processing/authorization/constants/CardStatus.java` — utility class with String constants. Never referenced. Code uses `CardModelStatus` enum from common instead. Remove.

- **Dead code — unused HttpHeaders**: `AuthService:270-271` creates `HttpHeaders` object, sets content type, but never passes it to WebClient. WebClient sets content type directly on line 276. Remove lines 270-271.

- **Inconsistent URL construction**: `AuthService:213` — `getCard()` prepends `http://` if missing. `AuthService:272` — `reserve()` does not. If `cmsUrl` lacks scheme, `reserve()` fails.

- **Mixed indentation in AuthController**: `AuthController:129-147` — method body mixes 8-space and 4-space indentation.

- **Copy-paste in test config**: `authorization/src/test/resources/application.yaml` contains `websocket` config (lines 6-8) irrelevant to authorization. Copied from another service.

- **`@Value` + `@RequiredArgsConstructor` conflict**: `AuthService:70-71` uses `@Value` field injection alongside `@RequiredArgsConstructor` constructor injection. Mixed injection styles. Prefer constructor injection for all dependencies.

### 2. Database Integrity

- **`LimitUsage.updatedAt` never updated**: `LimitUsage:38-40` — `@ColumnDefault("CURRENT_TIMESTAMP")` only sets value on INSERT. On subsequent `save()` calls (update), `updatedAt` stays at original value. Need `@UpdateTimestamp` or manual set.

- **`@Transactional` effectively no-op**: `AuthService:93` — `@Transactional(rollbackFor = Exception.class)` but method catches all exceptions internally (lines 96-107, 172-175) and returns decline responses. Transaction never rolls back. Either remove annotation or restructure to let exceptions propagate for rollback.

- **Monthly limit query sums wrong field and not locked**: `LimitUsageRepository:25-34` — `sumMonthlyAmountByPanAndMonth` JPQL sums `lu.dailyAmount` instead of `lu.monthlyAmount`. Method name says "monthly" but aggregates daily field — likely bug. Additionally, no lock on this query. Concurrent transactions for same PAN could both read same sum and both approve, exceeding monthly limit. Daily check uses `PESSIMISTIC_WRITE` but monthly doesn't.

### 3. Concurrency

- **RRN generation — CAS loop correct but fragile**: `AuthService:320-344` — uses `AtomicReference` CAS loop. Correct for single-instance. But RRN format produces 12 chars (Javadoc says 10). Sequence counter wraps at 100 — if >100 RRN/sec, collisions occur.

- **`new Random()` per call in generateAuthCode**: `AuthService:369` — creates new `Random` instance each call. Not a concurrency bug (each instance independent) but wasteful. Use `ThreadLocalRandom`.

### 4. Unit Tests

- **Missing `@Test` annotation**: `DBIntegrationTest.java:146` — `authorizeShouldReturnDeclinedWhenExceededMonthlyLimit()` has no `@Test`. Test never executes. Monthly limit path untested in integration.

- **Good coverage otherwise**: `AuthControllerTest` (6 tests), `AuthServiceTest` (18 tests), `DBIntegrationTest` (9 `@Test`-annotated methods, including `whatDatabase()` diagnostic). Controller tests use MockMvc, service tests use Mockito spy, integration tests use TestContainers.

- **`whatDatabase()` diagnostic test**: `DBIntegrationTest` — `whatDatabase()` prints DB URL. Not a real test. Should be removed or converted to `@BeforeEach` diagnostic logging.

### 5. Other

- **HealthService log message wrong**: `HealthService:95` — log says "Failed to get card" but method is health check. Copy-paste from AuthService. Should say "Health check failed for {url}".

- **WebClient blocking calls in reactive context**: `AuthService:238` and `AuthService:285` — `.block()` on WebClient Mono. Blocks servlet thread. Acceptable for synchronous Spring MVC, but defeats purpose of using WebFlux WebClient. Could use RestTemplate instead for consistency.

---

## Card Management

### 1. Basics

- **HealthController depends on implementation, not interface**: `HealthController:18` — injects `CardServiceImpl` instead of `CardService` interface. Breaks DIP. Should inject `CardService`.

- **`countCardsFiltered()` overload confusion**: `CardService:82` (no args) and `CardService:94` (5 args) — two methods with same name, very different signatures. The no-arg version calls `cardRepository.countCards()`, the 5-arg calls `cardRepository.countCards(status, bin, ...)`. Rename no-arg to `countAllCards()`.

- **`Card.fromDraft` sets expiry relative to `now()`**: `Card:181` — `YearMonth.now().plusYears(cardYtl)`. If cards generated in batch, all get same expiry. Fine for educational project.

- **`faker.name().fullName().toUpperCase()` may produce invalid names**: `CardGeneratorService:50` — Faker can produce names with characters outside `^[A-Z\\s\\-\\.']+$` pattern (e.g., digits, special chars from non-Latin names). Generated cards may fail validation if re-submitted through `CreateCardRequest`.

### 2. Database Integrity

- **PAN unique constraint — properly enforced**: `CardEntity` has both `@Column(unique = true)` on `pan` field (line 30) and `@Index(name = "uk_cards_pan", columnList = "pan", unique = true)` (line 17). Redundant but safe — duplicate PANs rejected at DB level.

- **`saveAll` batch atomicity**: `JavaPersistenceAdapter:80-89` — `saveAll()` delegates to JPA `saveAll()`. Spring Data JPA provides implicit transaction. Mapping (`.stream().map().toList()`) runs to completion before `saveAll()` is called, so mapping exceptions prevent any DB writes. No partial save risk.

- **Soft delete properly filtered**: `CardEntity:23` has `@SQLRestriction("status <> 'DELETED'")` — Hibernate applies this filter to all JPA queries including `findByPan()`. DELETED cards excluded everywhere automatically.

### 3. Concurrency

- **`reserve()` race condition**: `CardServiceImpl:145-149` — reads card via `getCard()`, then calls `withReserved()` which checks balance, then `save()`. Two concurrent reserve requests for same card could both read same balance, both pass check, both save — resulting in negative balance. Need pessimistic lock or `@Version` optimistic lock on CardEntity.

### 4. Unit Tests

- **Good test suite**: Tests use TestContainers, RestAssured, and DataFaker. Integration tests cover card CRUD, generation, reservation.
- **Missing**: No concurrent reservation test. No explicit test verifying `@SQLRestriction` excludes DELETED cards.

### 5. Other

- **`CardGeneratorService` uses `new Random()`**: `CardGeneratorService:32` — instance field `new Random()`. `java.util.Random` is thread-safe (uses CAS internally) but shared instance causes contention under concurrent access. Use `ThreadLocalRandom` for better performance.

---

## Terminal Simulator

### 1. Basics

- **Duplicated AuthorizationRequest DTO**: `terminal-simulator/dto/AuthorizationRequest.java` duplicates `common/dto/authorization/AuthorizationRequest.java`. Terminal-simulator doesn't depend on common module for DTOs. Should reuse common DTO.

- **Duplicated ErrorResponse**: `terminal-simulator/ErrorResponse.java` duplicates `common/dto/ErrorResponse.java`. Same issue.

- **Hardcoded merchant/acquirer format**: `TransactionFactory:42-43` — `String.format("MERCH%10d", ...)` and `String.format("ACQ%03d", ...)`. Hardcoded format strings. If authorization service expects different format (e.g., `MERCH00000000029` from common DTO example), mismatch possible.

### 2. Database Integrity

N/A — terminal-simulator has no DB.

### 3. Concurrency

- **StanGenerator not thread-safe**: `StanGenerator:9-16` — `stanCounter++` is not atomic. Concurrent calls produce duplicate STANs. Use `AtomicInteger`.

- **`cards` field race condition**: `TerminalSimulatorService:28` — `volatile List<Card> cards`. If two `/run` requests hit concurrently, second overwrites `cards` while first still iterating. `getRandomCard()` reads stale or wrong list. Make `cards` method-local or synchronize.

- **`new Random()` shared across threads**: `TerminalSimulatorService:27` — `Random` instance field used by `getRandomCard()` from potentially concurrent requests. `java.util.Random` is thread-safe (CAS-based) but shared instance causes contention under load. Use `ThreadLocalRandom` for better performance.

### 4. Unit Tests

- **4 test files**: Service tests, controller tests, client tests, StanGenerator test.
- **StanGenerator test exists but doesn't test concurrency**: `StanGeneratorTest` likely tests sequential generation only.
- **Missing**: No test for concurrent `/run` calls.

### 5. Other

- **`GatewayClient.sendToGateway` no error handling**: `GatewayClient:30-34` — `postForEntity` can throw `RestClientException`. No try-catch. Exception propagates to service, which doesn't catch it either — results in 500 to caller.

---

## Merchant Acquirer

### 1. Basics

- **`String.valueOf()` for logging**: `SimulationService:32,35,39,43` — `log.info(String.valueOf(request))`. Not structured logging. Use `log.info("request: {}", request)` pattern.

- **Non-standard response code**: `TransactionSender:32` — creates DECLINED response with `responseCode = "505"`. Not in standard ISO 8583 codes defined in `AuthorizationResponse`. Should use `"96"` (system error) or `"05"` (do not honor).

- **Fee saved before transaction sent**: `TransactionBuilder:42-44` — `acquirerProvider.calculateFee()` saves fee to DB inside build loop. If `transactionSender.sendAll()` fails, fees remain in DB without corresponding transactions. Build and send should be atomic or fee save should happen after send.

### 2. Database Integrity

- **AcquirerFee PK is `transmissionDateTime`**: `AcquirerFee:15-16` — `@Id private String transmissionDateTime`. Multiple transactions can share same timestamp → PK collision → `DataIntegrityViolationException`. Need proper PK (auto-generated UUID or composite key).

- **Repository generic type mismatch**: `AcquirerFeeRepository:8` — `JpaRepository<AcquirerFee, Long>` but entity `@Id` is `String`. Should be `JpaRepository<AcquirerFee, String>`. May cause runtime errors on `findById()`.

- **NPE in getAcquirerFee**: `AcquirerProvider:30-32` — `findBy...()` can return `null` if fee not found. `.getAcquirerFee()` on null → NPE. Need null check or `Optional`.

- **`spring.jpa.show-sql: true` in production config**: `application.yaml:17` — SQL logging enabled. Performance impact. Should be `false` or profile-gated.

### 3. Concurrency

- **StanGenerator race condition**: `StanGenerator:10-16` — `counter.addAndGet(1)` then check `> 999999` then `counter.set(1)`. Two threads can both read `> 999999` concurrently and both `set(1)` → duplicate STANs. Also `value = counter.get()` on line 14 is redundant since `addAndGet` already returned the value. Use `compareAndSet` loop or `synchronized`.

### 4. Unit Tests

- **3 test files**: `AcquirerProviderTest` (3 tests), `MerchantProviderTest` (5 tests), `SimulationServiceTest` (6 tests). All use Mockito.
- **Missing**: No integration tests. No test for `TransactionSender`. No test for `TransactionBuilder`. No test for fee calculation edge cases (zero amount, overflow).

### 5. Other

- **Flyway + ddl-auto=update conflict**: `application.yaml:8,10-12` — `spring.jpa.hibernate.ddl-auto: update` alongside Flyway migrations. Hibernate auto-update can conflict with Flyway schema management. Use `ddl-auto: validate` or `none` when Flyway is active.

---

## Transaction Logger

### 1. Basics

- **`sumAmount()` returns `long`**: `TransactionRepository:15` — `SELECT SUM(t.amount)` returns `long`. If total amount exceeds `Long.MAX_VALUE` (~9.2 × 10^18 kopecks = 9.2 × 10^16 rubles), overflow. Unrealistic for educational project but bad pattern.

- **`averageProcessingTimeMs()` returns `double`**: `TransactionRepository:17-18` — returns `double` but service casts to `double` anyway. Fine.

- **Health endpoint does `count(*)`**: `HealthController:27` — calls `transactionRepository.count()` on every health check. On large tables (millions of rows), `count(*)` is slow. Cache or use approximate count.

### 2. Database Integrity

- **Idempotent store — race condition**: `TransactionService:44-56` — `findById()` then `saveAndFlush()`. Two concurrent requests with same UUID both pass `findById` check (both see empty), both try to insert. Second gets `DataIntegrityViolationException` — which is caught (line 52) and retried. This is correct defensive pattern. But relies on UUID PK constraint existing.

- **`createdAt` not set by DB**: `Transaction:61` — `private Instant createdAt` has no `@ColumnDefault` or `@CreationTimestamp`. Value comes from request body. If caller doesn't set it, column is null (no `nullable=false`). Should add `@CreationTimestamp` or `nullable=false`.

- **Dashboard stats queries not cached**: `TransactionService:97-106` — `getStats()` runs 6 separate queries (count, countByStatus×2, sumAmount, countByCreatedAtAfter, averageProcessingTimeMs). Each hits DB. For dashboard polling every few seconds, this is expensive. Consider materialized view or caching.

### 3. Concurrency

- **WebSocket broadcast during iteration**: `DefaultWebSocketManager:32-47` — iterates `sessions` (ConcurrentHashMap.newKeySet — safe for concurrent modification). `sendMessage()` can throw if session closed concurrently — caught and added to `deadSessions`. Pattern is correct.

- **`store()` not `@Transactional`**: `TransactionService:44` — `store()` method lacks `@Transactional`. `findById` and `saveAndFlush` are separate DB operations. If save fails after WebSocket broadcast, client gets error but WebSocket already sent. Acceptable trade-off for this use case.

### 4. Unit Tests

- **Excellent coverage**: 15 test classes + 1 config helper. Integration tests with TestContainers. Controller tests, service tests, mapper tests, specification tests, WebSocket tests, exception handler tests. Best test coverage in project.

- **Missing**: No load test for `getStats()`. No test for WebSocket reconnection scenarios.

### 5. Other

- **`TransactionFilter.status` uses regex validation**: `TransactionFilter:15` — `@Pattern(regexp = "APPROVED|DECLINED")`. Fragile — if new status added to enum, regex must be updated manually. Consider custom validator or enum-based param.

- **OpenApiConfig typo**: `OpenApiConfig:12` — "статисти**а**" should be "статисти**к**а".

---

## Dashboard (TypeScript/React)

### 1. Basics

- **O(n²) deduplication**: `App.tsx:19-21` — `allTransactions.filter((tx, index, self) => index === self.findIndex(t => t.id === tx.id))`. For each element, `findIndex` scans from start. O(n²). Use `Map` or `Set` by id.

- **Hardcoded WebSocket URL**: `useWebSocket.ts:17` — default `ws://localhost:8088/ws/transactions`. Not configurable via env. In Docker, URL differs. Should read from `import.meta.env.VITE_WS_URL`.

- **`useStats` not used in App**: `App.tsx` uses `useLiveStats` which internally calls `useStats`. But `App.test.tsx` mocks `useStats` directly. Coupling between test mock structure and internal hook composition is fragile.

- **`fetchApi` no timeout**: `api/client.ts:1-9` — `fetch()` with no `AbortController` timeout. If API hangs, UI hangs forever.

### 2. Database Integrity

N/A — dashboard has no DB.

### 3. Concurrency

- **`useLiveStats` stats drift over time**: `useLiveStats.ts:40-56` — `useEffect` depends on `liveTransactions` array. Every new WebSocket message creates new array reference → effect fires → stats incremented via `statsRef`. But `liveTransactions` is also sliced to 20 items (`useWebSocket.ts:56`). When old tx drops off the list, stats don't decrement. Stats drift upward over time.

- **`useWebSocket` effect re-runs on every option change**: `useWebSocket.ts:100` — `useEffect` depends on `[url, maxRetries, retryDelayMs, maxRetryDelayMs]`. If parent re-renders with same values but new object reference, WebSocket reconnects unnecessarily. Use `useMemo` for options or destructure.

### 4. Unit Tests

- **5 test files**: App.test.tsx, useWebSocket.test.ts, Filters.test.tsx, TransactionChart.test.tsx, format.test.ts. Reasonable coverage for educational frontend.

- **Missing**: No test for `useLiveStats`. No test for `useTransactions`. No test for `TransactionTable` component. No test for CSV export.

### 5. Other

- **`mockData.ts` in production bundle**: `mockData.ts` imported by `TransactionTable.tsx` for issuer/MCC names. Hardcoded mapping. Should come from API or config.

- **No error boundary**: App has no React error boundary. Unhandled render errors crash entire page.

---

## Cross-Cutting Findings

### 1. Basics

- **HealthResponse records in 7 services**: Each service defines own `HealthResponse` record with different fields (see Switch section for details). Could move common base to `common` module.

- **`maskPan` duplicated in 3+ places**: `switch/RouteController:41-46`, `card-management/CardServiceImpl:151`, `authorization/AuthService:391-398`. Each uses different masking pattern (4+****+4, 6+******+4, 4+********+4). Inconsistent masking across project.

- **STAN generator duplicated**: Terminal-simulator and merchant-acquirer each have own `StanGenerator` with different implementations (plain int vs AtomicInteger). Both have concurrency bugs.

### 2. Database Integrity

- **Shared PostgreSQL, no schema isolation**: Authorization, card-management, merchant-acquirer, and transaction-logger all share same PostgreSQL instance. Table name collisions possible if entities share names. No schema-per-service separation.

### 3. Concurrency

- **No service uses optimistic locking**: None of the JPA entities use `@Version`. All read-modify-write patterns (reserve, limit update) are vulnerable to lost updates under concurrency.

### 4. Unit Tests

- **Test quality varies widely**: Transaction-logger has excellent coverage (15 test classes + 1 config). Terminal-simulator has decent coverage (4 test files including 168-line service test). Merchant-acquirer has minimal coverage (3 test files). Common module has zero tests. Overall acceptable for educational project.

### 5. Other

- **`ddl-auto: update` in all DB services**: Authorization, card-management, merchant-acquirer, transaction-logger all use `hibernate.ddl-auto: update`. In production this is dangerous — can silently alter schema. For educational project acceptable, but should be noted.

- **No distributed tracing**: Services log independently. No correlation ID passed between services. Hard to trace a transaction through full pipeline. Gateway `RequestLoggingFilter:50` does set `MDC.put("requestId", requestId)` and passes `X-Request-Id` header downstream (line 46), but downstream services don't propagate it further.

---

## Critical Summary

| # | Severity | Module | Finding |
|---|:--------:|--------|---------|
| 1 | **Critical** | Authorization | `sumMonthlyAmountByPanAndMonth` sums `dailyAmount` instead of `monthlyAmount` — monthly limit check uses wrong data. `LimitUsageRepository:25-34`. |
| 2 | **High** | Card Mgmt | `reserve()` race condition — no lock on read-modify-write. Concurrent requests → negative balance. `CardServiceImpl:145-149`. |
| 3 | **High** | Merchant Acq | `AcquirerFee` PK is `transmissionDateTime` — multiple tx with same timestamp → PK collision. `AcquirerFee:15-16`. |
| 4 | **High** | Merchant Acq | `AcquirerFeeRepository` generic type `Long` but entity `@Id` is `String` — runtime errors on `findById()`. `AcquirerFeeRepository:8`. |
| 5 | **High** | Merchant Acq | `getAcquirerFee()` — `findBy...()` can return `null` → NPE on `.getAcquirerFee()`. `AcquirerProvider:30-32`. |
| 6 | **High** | Terminal Sim | `StanGenerator` — `stanCounter++` not atomic → duplicate STANs under concurrency. `StanGenerator:9-16`. |
| 7 | **High** | Terminal Sim | `cards` field — concurrent `/run` requests overwrite each other's card list. `TerminalSimulatorService:28`. |
| 8 | **High** | Gateway | `ResponseCachingFilter` catches all exceptions → 503, hiding real errors behind "service unavailable". `ResponseCachingFilter:77-85`. |
| 9 | **Medium** | Authorization | Missing `@Test` on `authorizeShouldReturnDeclinedWhenExceededMonthlyLimit()` — monthly limit path untested in integration. `DBIntegrationTest:146`. |
| 10 | **Medium** | Authorization | `@Transactional` effectively no-op — all exceptions caught internally, transaction never rolls back. `AuthService:93`. |
| 11 | **Medium** | Authorization | Monthly limit query has no pessimistic lock — concurrent approvals can exceed limit. `LimitUsageRepository:25-34`. |
| 12 | **Medium** | Authorization | `LimitUsage.updatedAt` never updated on subsequent saves — stays at INSERT timestamp. `LimitUsage:38-40`. |
| 13 | **Medium** | Switch | Authorization retry uses 1ms interval — no meaningful backoff for transient failures. `RetryFactory:58`. |
| 14 | **Medium** | Switch | GET with body in `MerchantAcquirerClient` — widely discouraged, proxies may drop body. `MerchantAcquirerClient:27-31`. |
| 15 | **Medium** | Merchant Acq | Fee saved to DB before transaction sent — orphaned fees on send failure. `TransactionBuilder:42-44`. |
| 16 | **Medium** | Merchant Acq | Flyway + `ddl-auto: update` conflict — Hibernate auto-update can clash with migrations. `application.yaml:8,10-12`. |
| 17 | **Medium** | Merchant Acq | `StanGenerator` race — `addAndGet` then `set(1)` not atomic → duplicate STANs. `StanGenerator:10-16`. |
| 18 | **Medium** | Terminal Sim | `GatewayClient.sendToGateway` — no error handling, `RestClientException` → 500. `GatewayClient:30-34`. |
| 19 | **Medium** | Gateway | `ResponseCachingFilter` never invalidates cache — stale data after POST/PATCH/DELETE. `ResponseCachingFilter:70-73`. |
| 20 | **Medium** | Dashboard | `useLiveStats` stats drift upward — WebSocket slice drops old items but stats never decrement. `useLiveStats.ts:40-56`. |
| 21 | **Low** | Common | Zero test coverage — 6 validators + 17 DTOs untested, shared by all services. |
| 22 | **Low** | Dashboard | O(n²) deduplication in `App.tsx:19-21` — `findIndex` per element. Use `Map` by id. |
| 23 | **Low** | Cross-cutting | `maskPan` duplicated in 3 places with different masking patterns (4+8+4, 6+6+4, 4+4+4). |
| 24 | **Low** | Cross-cutting | No `@Version` optimistic locking on any JPA entity — all read-modify-write patterns vulnerable. |
| 25 | **Low** | Cross-cutting | `ddl-auto: update` in all 4 DB services — dangerous in production, acceptable for educational project. |
