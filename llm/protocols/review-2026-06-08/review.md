# Code Review — SMP Project

**Date:** 2026-06-08
**Project:** SMP — educational intern project
**Modules:** gateway, switch, authorization, card-management, terminal-simulator, merchant-acquirer, transaction-logger, dashboard, common

**Reviewer:** deepseek/deepseek-v4-pro
**Opponent 1:** xiaomi/mimo-v2.5-pro
**Opponent 2:** z-ai/glm-5.1

---

## Intro

### Goals

Review across 5 topics:
1. **Basics** — duplication, hardcode, layer mixing, style
2. **Core Libraries** — Spring best practices, Java 21 features
3. **Incomplete** — missing error handling, wrong business logic
4. **Integration** — cross-check API contracts between services
5. **Other** — suspicious, not fitting above

### Context

Educational intern project. Not real fintech. Findings focus on teachable improvements.

## Reviewer task (review)

Review the code and write it into llm/protocols/review-2026-06-08/review.md.

Don't forget it is an educational project, not the real fintech processing app.

Review structure:
- Introduction with review goals.
- Section per module.
- Review topics in subsections.

Review topics, which also define areas of review, are:

1. Basics of Software Design and coding.
   E.g., code duplication, hardcode, mixed responsibilities, mixed layers, improper style, etc.

2. Core libraries usage.
   E.g., anything that could be improved from Spring best practices point of view.
   E.g., Java 21 neat features usage.

3. Incomplete secondary branches of code.
   E.g., improper or missing errors handling, wrong business logic failures handling, etc.

4. Integration errors.
   E.g., cross check expectations on both sides of API calls between modules.

5. Other.
   Here you put everything you find suspicious that doesn't fall into topics above.

Review rules:
- Use caveman skill and AX when writing review file.
- Reference files by `filename:linenumber`.
- Russian in comments is fine.

## Reviewer task (opponent feedback)

Under llm/protocols/review-2026-06-08/ folder, review.md was validated by opponents and their feedback
placed into review-validation-*.md files. Process these files and improve review.md:
- You may (partially) agree or disagree with every feedback statement.
- When agreed, correct review.md accordingly and double check nothing else is wrong with corrected statement.
- When disagreed, add visually separated note into the feedback file near the corresponding statement.

## Opponent task

Validate code review in llm/protocols/review-2026-06-08/review.md:
- Cross-check every statement against source code.
- Check every statement for clarity and correctness.
- Report inconsistencies only, don't respond for statements which are ok.
- Use caveman skill and AX when writing output file.
- Output into llm/protocols/review-2026-06-08/review-validation.md.

---

## 1. Gateway

Entry point: validation, rate limit, logging, routing.

### 1.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 1.1.1 | `TransactionRateLimitFilter.java:60`, `TransactionValidationFilter.java:70` | `isTransactionRequest()` duplicated in two filters. Same logic, same `TRANSACTIONS_PATH` constant. Extract to shared util. |
| 1.1.2 | `HealthService.java:19` | Health probe timeout hardcoded 2s. Not configurable. |
| 1.1.3 | `RequestLoggingFilter.java:27` | `X-Request-Id` header name hardcoded string. Extract to shared constant. |
| 1.1.4 | `MutableHeadersRequestWrapper.java:6` | Wildcard import `java.util.*`. Checkstyle should catch. |
| 1.1.5 | `TransactionValidationException.java` | 2-space indent vs 4-space project convention. |
| 1.1.6 | `TransactionRateLimitFilter.java:51,56` | `Retry-After: 1` (seconds) vs JSON `retryAfterMs: 1000` — inconsistent units. Client must parse both. |

### 1.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 1.2.1 | `HealthService.java`, `BeanConfiguration.java:12` | `java.net.http.HttpClient` instead of Spring `RestClient`. Bean IS Spring-managed (`@Bean` + constructor injection), but JDK type lacks Spring features (interceptors, metrics, timeouts). |
| 1.2.2 | `TransactionValidationFilter.java:75`, `TransactionRateLimitFilter.java:49` | Error responses written manually as JSON in filters. No `@ControllerAdvice`. |

### 1.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 1.3.1 | `TransactionRequestValidator.java` | No validation of `terminalType`. No format check on `transmissionDateTime`. |
| 1.3.2 | `TransactionRequestValidator.java` | Manual imperative validation. Should use Bean Validation annotations on DTO. |

### 1.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 1.4.1 | `dto/AuthorizationRequest.java` | No `issuerId` field. Switch enriches after BIN routing. Works in practice but DTO differs from switch — fragile if serialized differently. |
| 1.4.2 | `dto/AuthorizationRequest.java` | `amount` is `Integer`. Switch + logger use `Long`. Overflow at ~2.1B (21M RUB in kopecks — edge case). |

### 1.5 Other

| # | Location | Issue |
|---|----------|-------|
| 1.5.1 | `InMemoryRateLimiter.java:24,27` | `computeIfAbsent` + `synchronized(window)` on map value. Correct only if window ref never changes — fragile. |
| 1.5.2 | — | Spring Cloud Gateway MVC routes + servlet `OncePerRequestFilter` hybrid. Filters duplicate `isTransactionRequest()` route config already handles. Unconventional. |

---

## 2. Switch

BIN routing, orchestration (route → authorize → log), error recovery.

### 2.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 2.1.1 | `enums/TransactionStatus.java` | Duplicated in switch + transaction-logger. Same package, same values. Extract to `common`. |
| 2.1.2 | `RestClientConfig.java:14` | Connect 3s, read 5s hardcoded. Not configurable. |
| 2.1.3 | `LoggerClient.java:28`, `AuthorizationClient.java:32` | URL paths built with string concat. Use `UriComponentsBuilder`. |
| 2.1.4 | — | Manual constructor injection + `LoggerFactory.getLogger`. No `@RequiredArgsConstructor` / `@Slf4j`. Inconsistent with other services. |
| 2.1.5 | `RouteService.java:76` | `acquiringFee` always `null` in `buildTransaction()`. Dead field. |
| 2.1.6 | `AuthorizationResponse.java:27` | `authCode: "TEST01"` in stub — test data in production code path. |

### 2.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 2.2.1 | — | No `@Retryable` or Resilience4j. Retry only in comments. |
| 2.2.2 | — | No virtual threads. Switch is I/O-bound (auth + logger sequential calls). Good educational addition. |

### 2.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 2.3.1 | `AuthorizationClient.java:42` | Retry 3x → DECLINED responseCode=05 — **not implemented**. Returns `authUnavailable()` on any exception. |
| 2.3.2 | `LoggerClient.java:37` | Retry 3x → reversal mti=0400, code=96 — **not implemented**. Returns `false`. |
| 2.3.3 | `RouteService.java:49` | Logger down for APPROVED tx → only logs error (`LOG.error`). Audit trail silently lost. |
| 2.3.4 | `RouteController.java:28` | No `@Valid` on `@RequestBody`. Called directly → no validation. |

### 2.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 2.4.1 | `model/AuthorizationRequest.java:10` | `amount` is `Long`. Gateway → `Integer`. Auth → `Integer`. Type mismatch across chain. |
| 2.4.2 | `model/AuthorizationRequest.java:14` | `terminalType` is `String`. Auth expects `TerminalType` enum. Works via Jackson name match — fragile. |
| 2.4.3 | `model/AuthorizationRequest.java:12` | `transmissionDateTime` is `LocalDateTime`. Gateway sends `String`. ISO 8601 must match exactly. |

### 2.5 Other

| # | Location | Issue |
|---|----------|-------|
| 2.5.1 | `RouteServiceTest.java:22,61` | Test creates `AuthorizationClient(SwitchTestData.defaultProperties(), null)` — first arg non-null `SwitchProperties`, only `RestClient` param null. Works only with `authorizationStubEnabled=true`. Fragile. |

---

## 3. Authorization

Card lookup, status/balance check, fund reserve, RRN gen.

### 3.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 3.1.1 | `enums/CardStatus.java` | Duplicates common `CardStatus`. Missing `DELETED` value. |
| 3.1.2 | `dto/AuthorizationRequest.java`, `dto/AuthorizationResponse.java` | Duplicate common DTOs. Auth uses mutable Lombok classes; common uses records. |
| 3.1.3 | `dto/ReserveRequest.java` | Duplicates common. Auth: `amount: Integer`; common: `amount: long`. Incompatible. |
| 3.1.4 | `AuthorizationResponse.java:37,50` | `processingTimeMs` hardcoded `1` with TODO. Never measured. |
| 3.1.5 | `AuthService.java:154` | `new Random()` per `generateAuthCode()` call. Use `ThreadLocalRandom`. |
| 3.1.6 | `AuthService.java:22` | `java.util.Calendar` (legacy pre-Java 8) for RRN. Use `java.time`. |

### 3.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 3.2.1 | `AuthService.java:98,119` | `WebClient.block()` in synchronous `@Service`. Defeats WebFlux. Replace with `RestClient`. |
| 3.2.2 | `AppConfig.java:10` | `WebClient.create()` — no timeouts, no base URL, no error config. |
| 3.2.3 | `dto/*.java` | Mutable Lombok DTOs. Common uses records — should follow pattern. |
| 3.2.4 | — | No `@ControllerAdvice`. `ConstraintViolationException` → Spring Boot defaults. |

### 3.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 3.3.1 | `AuthService.java:65` | `// TODO check daily limit`, `// TODO check month limit`. `LimitUsage` entity + repo exist — never wired into `AuthService`. |
| 3.3.2 | `LimitUsage.java:15` | `@Index(columnList = "card_id")` — column `card_id` doesn't exist. Should be `pan`. Schema gen fails. |
| 3.3.3 | `AuthService.java:99` | `getCard()` can return `null` (empty Mono → `.block()` null). No null check → NPE at `cardResponse.getStatus()`. |
| 3.3.4 | `AuthService.java:61,69` | `expiryDate.isBefore(...)` → NPE if card-management returns null `expiryDate`. `availableBalance` is primitive `long` — cannot NPE. `request.getAmount()` (Integer) auto-unbox NPE possible. |

### 3.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 3.4.1 | `dto/CardResponse.java` | **Critical:** `expiryDate` is `LocalDate`. Card-management returns `String` (MMyy, e.g. `"0629"`). Jackson can't deserialize `"0629"` → `LocalDate`. **Auth flow always fails.** Falls to `SERVICE_UNAVAILABLE`. |
| 3.4.2 | `application.yaml:9` | CMS URL defaults to `localhost:8081`. Card-management defaults to `8080`. Port mismatch — won't communicate out of box. |
| 3.4.3 | `dto/CardResponse.java` | `status` is `CardStatus` enum. Card-management returns `String`. Jackson case-insensitive match may work — not guaranteed. |

### 3.5 Other

| # | Location | Issue |
|---|----------|-------|
| 3.5.1 | `AuthService.java:86` | `pan` interpolated directly in URL — path traversal risk if pan contains `/`. |
| 3.5.2 | `pom.xml:49,87,92` | JPA/PostgreSQL deps commented out in primary section, active later. PostgreSQL has `<scope>runtime</scope>` — correct. JPA scope missing is normal (compile scope). DB dependency exists despite LimitUsage not wired. |

---

## 4. Card Management

Card CRUD, Luhn PAN gen, bulk generate, fund reserve.

### 4.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 4.1.1 | `src/main/resources/CardController.java` | Orphan duplicate of real `CardController`. Stale code in `resources/`. Delete. |
| 4.1.2 | `CardController.java:36` | Manual constructor injection vs `@RequiredArgsConstructor`. Inconsistent with rest of project. |
| 4.1.3 | `LuhnValidator.java:11` | `private String pan` mutated on every `isValid()`. Never read. Remove — false coupling + thread-safety confusion. |
| 4.1.4 | `CardGeneratorOptions.java` | Only `min-balance` uses wrong prefix `generator.card-service.min-balance`. Remaining 4 props use correct `app.card-generator.*`. Only `minBalance` silently defaults to `0`; `maxBalance` binds correctly. |
| 4.1.5 | `CardEntity.java:67` | `@Transient` on `private static DateTimeFormatter`. Static fields never persisted — annotation meaningless. Use `private static final`. |

### 4.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 4.2.1 | `CardRepository.java:23` | `findCards()` accepts `Pageable`, returns `List`. Forces 2 queries (list + count). Return `Page<T>`. |
| 4.2.2 | `CardEntity.java:91` | `expiryDate` `@Transient` + lazy-init getter. All-args constructor sets expiry (not always null for new instances). Getter caches result — not reparse-every-call. JPA-loaded entities via no-arg constructor have null `expiryDate` — use `@PostLoad`. |
| 4.2.3 | `CardService.java` | No `@Transactional` on mutations (`patchCard`, `deleteCard`, `reserve`). |
| 4.2.4 | `CardEntity.java` | No `@Version` for optimistic locking. |

### 4.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 4.3.1 | `CardService.java:125` | `reserve()`: read → check → deduct → save without `@Transactional`. Concurrent → lost update. |
| 4.3.2 | `CardService.java:96` | `patchCard()`: same lost-update bug. |
| 4.3.3 | `CardGeneratorService.java:36` | `monthlyLimit = dailyLimit * 30` — arbitrary 30-day assumption. |

### 4.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 4.4.1 | — | No finding; serialization uses `getStrExpiryDate()` (returns String MMyy), matching `CardModel.expiryDate: String`. No mismatch. |

### 4.5 Other

| # | Location | Issue |
|---|----------|-------|
| 4.5.1 | `CardEntity.java:67` | `private static` with `@Transient` — annotation ignored by JPA. |
| 4.5.2 | `CardEntity.java:108` | `reserve()` mutates `availableBalance` in entity (rich domain) — good. Contra: no `@Transactional`. |
| 4.5.3 | `LuhnValidator.java:15` | `this.pan = pan` on singleton bean → stale shared field. Validation result correct (uses local var), but field misleading. |

---

## 5. Terminal Simulator

POS transaction generator, 5 scenarios.

### 5.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 5.1.1 | `dto/AuthorizationRequest.java`, `dto/AuthorizationResponse.java` | Duplicated DTOs. Not using `common`. |
| 5.1.2 | `GatewayClient.java:18` | URLs hardcoded: `"http://gateway:8080/api/transactions"`, `"http://card-management:8080/api/cards"`. |
| 5.1.3 | `TerminalSimulatorService.java:83` | `merchantId`, MCC array, `acquirerId` prefix all hardcoded. |
| 5.1.4 | `TerminalSimulatorService.java:132` | `System.out.println(tx)` in service. Use logger. |
| 5.1.5 | `TerminalSimulatorService.java:27` | `Random random = new Random()` as instance field. Non-threadsafe, shared across concurrent `run()`. |
| 5.1.6 | `TerminalSimulatorService.java:28` | `stanCounter` plain `int` with `++`. Not atomic, not volatile. Concurrent → duplicate STANs. |
| 5.1.7 | `TerminalSimulatorService.java:86` | `acquirerId` = `"TERM%03d"` — same prefix as `terminalId`. Should differ. |
| 5.1.8 | `GatewayClient.java:17` | `new RestTemplate()` — deprecated. No pool, no timeouts. |

### 5.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 5.2.1 | `GatewayClient.java:17` | `RestTemplate` deprecated. Use `RestClient`. |
| 5.2.2 | — | No virtual threads. Ideal candidate — tx per virtual thread. |

### 5.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 5.3.1 | `TerminalSimulatorService.java:126` | `generateTransactionHandler` — no try-catch. Gateway unreachable → simulation crashes, loses results. |
| 5.3.2 | `TerminalSimulatorService.java:126`, `GlobalExceptionHandler.java:33,39,41` | `generateTransactionHandler` has no try-catch — first error crashes simulation. `GlobalExceptionHandler` catches `HttpClientErrorException` + catch-all `Exception` (returns 500). Real issue: `ex.printStackTrace()` via stdout, not logger. |
| 5.3.3 | `TerminalSimulatorService.java:162` | `run()` switch — no `default`. Unknown scenario → silent zero results. |
| 5.3.4 | `TerminalSimulatorService.java:163` | `"mixed"` fractions sum 0.95. Small `count` → `(int)` truncation → all in blocked bucket. |
| 5.3.5 | `TerminalSimulatorService.java:101` | `daily_limit`: `amount = card.dailyLimit() - 1`. If `limit == 0` → amount = -1. |
| 5.3.6 | `TerminalSimulatorService.java:108` | `no_money`: `card.availableBalance() + (int) randomValue`. Cast on `randomValue` only, not sum. `(int)` truncation on `randomValue` (double → int). |
| 5.3.7 | `TerminalSimulatorService.java:59` | `getInvalidPan()` flips last digit. ~10% chance still valid Luhn. |
| 5.3.8 | `TerminalSimulatorService.java:87` | `String issuerId = ""` — empty string, not `null`. |

### 5.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 5.4.1 | `dto/AuthorizationRequest.java` | `amount: long`, includes `issuerId`. Gateway DTO: `Integer`, no `issuerId`. Strict deserialization → fail. |
| 5.4.2 | `GatewayClient.java:19` | Calls `card-management:8080` directly, bypassing gateway. Inconsistent with merchant-acquirer. |
| 5.4.3 | `dto/AuthorizationRequest.java` | `transmissionDateTime` → `toInstant(ZoneOffset.UTC).toString()` (ISO-8601 with `Z`). Correct for `Instant` downstream. |

### 5.5 Other

| # | Location | Issue |
|---|----------|-------|
| 5.5.1 | `TerminalSimulatorService.java:30` | `cards` volatile ref but list contents read concurrently. `run()` assigns new list — race with `getRandomCard()`. |
| 5.5.2 | `TerminalSimulatorService.java:126` | `generateTransactionHandler` receives shared `authResps` list. 4 segments append sequentially — breaks if parallelized. |

---

## 6. Merchant Acquirer

Merchant-scenario tx generator, DDD-lite domain model.

### 6.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 6.1.1 | `AuthorizationRequest.java`, `AuthorizationResponse.java` | Duplicated DTOs. Same pattern as terminal-simulator. |
| 6.1.2 | `GatewayClient.java:18` | Hardcoded `"http://gateway:8080"`. |
| 6.1.3 | `AuthorizationRequestFactory.java:26` | Currency `"643"` hardcoded. Ignores card data. |
| 6.1.4 | `SimulationService.java:72` | `"TERM001"`, `"POS"` hardcoded. |
| 6.1.5 | `ScenarioType.java:3` | Enum lowercase: `grocery`, `electronics`, etc. Convention: `UPPER_CASE`. |
| 6.1.6 | `Scenario.java` | `countLower`/`countUpper` → these are price amounts, not counts. Misleading naming. |
| 6.1.7 | `SimulationService.java:44` | `log.info(String.valueOf(...))` — 10+ occurrences. Use parameterized: `log.info("{}", val)`. |
| 6.1.8 | `ErrorResponse.java` | Entire file commented-out dead code. Delete. |
| 6.1.9 | `GatewayClient.java:17` | `new RestTemplate()` — deprecated. |

### 6.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 6.2.1 | `GatewayClient.java:17` | `RestTemplate` deprecated. Use `RestClient`. |
| 6.2.2 | `AuthorizationRequest.java` (merchant-acquirer) | DTO lacks `issuerId` field (12 fields, none named `issuerId`). Switch must enrich via BIN routing — not set by merchant simulator. |

### 6.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 6.3.1 | `SimulationService.java:118` | Catch-all creates fabricated response: `responseCode="505"`, `processingTimeMs=999`. Not real gateway codes — misleading. |
| 6.3.2 | `GlobalExceptionHandler.java` | No catch-all `Exception` handler. Unhandled → Spring default error page. |
| 6.3.3 | `SimulationService.java:83` | `cardsResponse.cards().get(iterableCard - 1)` — 0 cards → `get(-1)` → **IndexOutOfBoundsException**. |
| 6.3.4 | `SimulationService.java:84` | `random.nextInt(0, countMerchants - 1)` — 0 merchants → `nextInt(0, -1)` → **IllegalArgumentException**. |
| 6.3.5 | `AuthorizationRequest.java` | `transmissionDateTime` → `LocalDateTime.now().toString()` — no timezone. Deserialization to `Instant` fails without default TZ. |

### 6.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 6.4.1 | `AuthorizationRequest.java` | 12 fields, no `issuerId`. Consistent with gateway DTO but switch enriches later. |
| 6.4.2 | `GatewayClient.java` | Calls `gateway:8080/api/cards` through gateway (correct). Terminal-simulator bypasses — routing inconsistency. |

### 6.5 Other

| # | Location | Issue |
|---|----------|-------|
| 6.5.1 | `SimulationService.java:81` | `Random random = new Random()` as local variable per `run()`. Correct. |
| 6.5.2 | `StanGenerator.java:9` | `AtomicInteger`. Correct. |

---

## 7. Transaction Logger

Storage, search (Specification), dashboard aggregation, WebSocket broadcast.

### 7.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 7.1.1 | `enums/TransactionStatus.java` | Duplicated from switch. Same package, values. Move to `common`. |
| 7.1.2 | `TransactionMapper.java:14` | `"stored"` — hardcoded status string. |

### 7.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 7.2.1 | `OffsetBasedPageRequest.java:48` | `withPage(int)` ignores argument, always returns offset 0. Pagination bug. |
| 7.2.2 | `OffsetBasedPageRequest.java:29` | `getSort()` hardcodes `createdAt DESC`. Not customizable. |
| 7.2.3 | `WebSocketConfig.java:18` | `.setAllowedOrigins("*")` — overly permissive CORS. |
| 7.2.4 | `DefaultWebSocketManager.java:29` | `remove()` during `ConcurrentHashMap.newKeySet()` forEach — technically supported, unusual pattern. |

### 7.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 7.3.1 | `TransactionMapper.java:70` | `matches()` compares 20 fields incl timestamps for idempotency. Slightly different timestamp → `TransactionConflictException`. Compare business keys only. |
| 7.3.2 | `TransactionService.java:42` | `store()` NOT `@Transactional`. `saveAndFlush()` at :50 commits immediately within its own tx. Broadcast at :59 is post-commit — no ghost tx risk. Real issue: broadcast failure only logged, connected clients miss update silently. |
| 7.3.3 | `DefaultWebSocketManager.java` | No heartbeat/ping-pong. Stale sessions accumulate until broadcast fails. |

### 7.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 7.4.1 | `TransactionResponse.java` | No `terminalType`, no `responseCode`. Dashboard types expect both → `undefined`. |

### 7.5 Other

| # | Location | Issue |
|---|----------|-------|
| 7.5.1 | `TransactionService.java:42` | Idempotent store: `findById` + `DataIntegrityViolationException` fallback. Good pattern. |
| 7.5.2 | `TransactionSpecification.java` | `Specification.where()` chain with nullable predicates. Correct. |

---

## 8. Dashboard

React + TypeScript SPA. KPIs, transaction table, charts (stubs).

### 8.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 8.1.1 | `src/api/client.ts` | Empty stub — `export {}`. No HTTP client. |
| 8.1.2 | `src/hooks/useWebSocket.ts` | Empty stub — `export {}`. |
| 8.1.3 | `src/hooks/useStats.ts` | Empty stub — `export {}`. |
| 8.1.4 | `src/components/Filters.tsx` | `<div>Filters Component</div>` only. |
| 8.1.5 | `src/components/LineChart.tsx` | `<div>LineChart Component</div>` placeholder only. |
| 8.1.6 | `src/components/PieChart.tsx` | `<div>PieChart Component</div>` placeholder only. |
| 8.1.7 | `src/App.tsx:5,10,19` | `MOCK_DASHBOARD_STATS`, `MOCK_TRANSACTIONS` imported directly. Hooks never used. |
| 8.1.8 | `src/components/KpiCards.tsx:14` | Only 4 of 8 `DashboardStats` displayed. `approvedCount`, `declinedCount`, `averageAmount`, `transactionsPerMinute` missing. |
| 8.1.9 | `src/App.tsx:11` | `w-2/3` hardcoded — non-responsive. |

### 8.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 8.2.1 | `KpiCards.tsx:9` | No `useMemo` on `kpiCards` array. |
| 8.2.2 | `TransactionModal.tsx:14` | Rows computed inline every render. |
| 8.2.3 | `format.ts:10` | `toLocaleString('ru-RU').replace(',', '.')` — hacky. |

### 8.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 8.3.1 | — | No error boundaries. Render error → whole SPA crash. |
| 8.3.2 | — | No loading states. |
| 8.3.3 | — | No empty state. |
| 8.3.4 | `format.test.ts:4` | `describe('formatAmount', ...)` tests `convertPenniesToRubles` — misleading name. |
| 8.3.5 | `App.test.tsx:30` | `getByText(/88\s*%/)` — `\s*` allows zero whitespace. False-green test. |
| 8.3.6 | `format.ts:8` | `convertPenniesToRubles` — param named `pennies` (misspelled for "kopeck"). |

### 8.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 8.4.1 | `types/index.ts:7` | `Transaction` has `terminalType`, `responseCode`. Logger `TransactionResponse` has neither. → `undefined`. |
| 8.4.2 | `types/index.ts:32` | `DashboardStats.totalAmount` — kopecks or rubles? No doc. |

---

## 9. Common

Shared DTO records + validation annotations.

### 9.1 Basics

| # | Location | Issue |
|---|----------|-------|
| 9.1.1 | `AuthorizationRequest.java:7` | No validation annotations. Other DTOs in module have them. |
| 9.1.2 | `PatchCardRequest.java:22,27` | `@Nullable` + `@NotNegative` (= `@Min(0)`). Per Jakarta spec, `@Min` skips `null` — no conflict. `@Nullable` documentative; `@NotNegative` doesn't reject null. |
| 9.1.3 | `CardModel.java:29` | `status` is `String`, not `CardStatus` enum — loses type safety. |

### 9.2 Core Libraries

| # | Location | Issue |
|---|----------|-------|
| 9.2.1 | `ExactSize.java` | Correct composed annotation — `@Size` with equal min/max + `@OverridesAttribute`. Good. |
| 9.2.2 | `DigitsOnly.java` | `^\d*$` allows empty. Should be `^\d+$`. Safe in practice (callers use `@NotBlank`) but annotation semantics wrong. |

### 9.3 Incomplete

| # | Location | Issue |
|---|----------|-------|
| 9.3.1 | — | Missing: `TransactionRequest`, `TransactionResponse`, `TransactionSearchResponse`, `HealthResponse`. Defined locally per service. |
| 9.3.2 | `AuthorizationRequest.java` | No `issuerId`. Switch needs it — common DTO incompatible with actual inter-service contract. |
| 9.3.3 | `AuthorizationRequest.java` | `amount: Integer` vs switch `Long`. `transmissionDateTime: String` vs switch `LocalDateTime`. Common DTO unusable for switch. |

### 9.4 Integration

| # | Location | Issue |
|---|----------|-------|
| 9.4.1 | — | **Nobody imports `AuthorizationRequest`/`AuthorizationResponse` from common.** Every service defines own copy. Dead code. |
| 9.4.2 | `cardmanagement/*` | Card-management DTOs used by card-management. `ErrorResponse` also shared by merchant-acquirer + transaction-logger. `@Bin`, `@Pan`, `@NotNegative` also shared. |

### 9.5 Other

| # | Location | Issue |
|---|----------|-------|
| 9.5.1 | `ErrorResponse.java:8` | `retryAfterMs` is `String` — should be `Long`. |
| 9.5.2 | `Pan.java:9` | `@Pattern(regexp = "^\\d{16}$")` — exact 16 digits, anchored. Correct for path variable validation. |

---

## Critical Summary

| # | Severity | Module | Finding |
|---|:--------:|--------|---------|
| 3.4.1 | **Critical** | Auth | `expiryDate` `LocalDate` vs `String` MMyy — auth **never** deserializes card. Flow broken. |
| 4.2.3 | **High** | Card Mgmt | No `@Transactional` on mutations — lost-update on concurrent reserve/patch. |
| 3.3.1 | **High** | Auth | Daily/monthly limits not implemented despite existing entity+repo. |
| 5.3.1 | **High** | Terminal | No try-catch in handler — simulation crashes on first error. |
| 6.3.3 | **High** | Merchant | Empty cards → `IndexOutOfBoundsException`. |
| 8.1.1 | **High** | Dashboard | All hooks empty stubs. Entire frontend mock-only. |
| 9.4.1 | **High** | Common | `AuthorizationRequest`/`Response` — dead code, nobody imports. |
| 2.3.1 | **Medium** | Switch | Retry commented, not implemented. |
| 4.1.4 | **Medium** | Card Mgmt | Property prefix mismatch — config silently ignored. |
| 5.1.6 | **Medium** | Terminal | Non-atomic STAN — duplicates under concurrency. |
| 7.2.1 | **Medium** | Logger | `withPage()` ignores arg — pagination bug. |
| 8.4.1 | **Medium** | Dashboard | TS types expect fields not in logger API. |
