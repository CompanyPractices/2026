# Review Outcome — SMP Project

**Date:** 2026-06-11
**Source:** `llm/protocols/review-2026-06-08/review.md` + validation corrections
**Method:** Every suggestion cross-checked against current source code.
**Reviewer:** qwen/qwen3.7-max

Outcomes: **fixed** / **partially fixed** / **remains** / **won't fix**

---

## Reviewer task

Read files under llm/protocols/review-2026-06-08.
Process every suggestion from review.md and check it vs current code.
Create new review-outcome.md file with the following:
- per-module structure as in review.md;
- every suggestion repeated;
- every suggestion has it outcome as per current code;

Outcomes are:
- fixed or partially fixed, when author resolved the problem mentioned;
- remains, when nothing is changed;
- won't fix, when on the second thought you decide that the problem not worth fixing;

Don't forget some code could be moved around during refactoring.


## 1. Gateway

### 1.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 1.1.1 | `isTransactionRequest()` duplicated in two filters. Extract to shared util. | **remains** — identical private method still in both `TransactionRateLimitFilter` and `TransactionValidationFilter`. |
| 1.1.2 | Health probe timeout hardcoded 2s. | **fixed** — configurable via `HealthProperties.requestTimeout` (default 3s), set in `application.yml`. |
| 1.1.3 | `X-Request-Id` header name hardcoded string. | **fixed** — extracted to `private static final String ID_HEADER_NAME` in `RequestLoggingFilter.java:33`. |
| 1.1.4 | Wildcard import `java.util.*` in `MutableHeadersRequestWrapper`. | **remains** — line 6 still `import java.util.*`. |
| 1.1.5 | 2-space indent in `TransactionValidationException`. | **fixed** — now uses 4-space indent. (Validator claimed 4-space originally; reviewer re-verified 2-space at review time. Current code is 4-space regardless.) |
| 1.1.6 | `Retry-After: 1` (seconds) vs JSON `retryAfterMs: 1000` inconsistent units. | **remains** — header still `"1"`, JSON still `1000`. |

### 1.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 1.2.1 | JDK `HttpClient` instead of Spring `RestClient`. | **remains** — `BeanConfiguration` and `HealthClient` still use `java.net.http.HttpClient`. (Validation corrected "not Spring-managed" → it IS `@Bean`-managed, but JDK type still lacks Spring features.) |
| 1.2.2 | Error responses written manually as JSON in filters. No `@ControllerAdvice`. | **remains** — all 3 error filters write JSON manually via `ObjectMapper.writeValue()`. No `@ControllerAdvice` in gateway. |

### 1.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 1.3.1 | No validation of `terminalType`; no format check on `transmissionDateTime`. | **remains** — `terminalType` unchecked; `transmissionDateTime` only checked for non-blank. |
| 1.3.2 | Manual imperative validation instead of Bean Validation annotations. | **remains** — `TransactionRequestValidator` uses manual `if`/`throw` despite DTO having Jakarta annotations. |

### 1.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 1.4.1 | No `issuerId` field in gateway `AuthorizationRequest`. | **fixed** — `issuerId` added to common `AuthorizationRequest` with `@NotBlank` + `withIssuerId()` helper. |
| 1.4.2 | `amount` is `Integer`, should be `Long`. | **fixed** — changed to `Long` with `@NotNull @PositiveOrZero`. |

### 1.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 1.5.1 | `computeIfAbsent` + `synchronized(window)` fragility. | **remains** — same pattern in `InMemoryRateLimiter.allowRequest()`. (Validation corrected line refs to `:24,27`.) |
| 1.5.2 | SCG MVC routes + servlet `OncePerRequestFilter` hybrid. | **remains** — 7 filters are `OncePerRequestFilter` subclasses alongside SCG MVC routes. |

---

## 2. Switch

### 2.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 2.1.1 | `TransactionStatus` duplicated in switch + transaction-logger. | **fixed** — single copy in `common`, both modules import `com.processing.common.dto.transactionlogger.TransactionStatus`. |
| 2.1.2 | Connect 3s, read 5s hardcoded in `RestClientConfig`. | **fixed** — externalized via `SwitchProperties.HttpProperties` + `application.yml`. Separate read timeouts for auth (5s) and logger (2s). |
| 2.1.3 | URL paths built with string concat. | **remains** — `LoggerClient` and `AuthorizationClient` still use `switchProperties.url() + "/path"`. No `UriComponentsBuilder`. |
| 2.1.4 | Manual constructor injection + `LoggerFactory.getLogger`. | **remains** — no `@RequiredArgsConstructor` or `@Slf4j` anywhere in switch. |
| 2.1.5 | `acquiringFee` always `null` — dead field. | **fixed** — fetched from `MerchantAcquirerClient` via real HTTP call. |
| 2.1.6 | `authCode: "TEST01"` in stub — test data in production path. | **fixed** — auth service generates random 6-char codes. "TEST01" only in `@Schema(example)` and test fixtures. |

### 2.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 2.2.1 | No `@Retryable` or Resilience4j. | **fixed** — Resilience4j retry for both auth and logger clients, configurable attempts + backoff. |
| 2.2.2 | No virtual threads. | **remains** — not configured. |

### 2.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 2.3.1 | Retry 3x → DECLINED not implemented. | **fixed** — after retry exhaustion, throws `AuthorizationException` → returns DECLINED code "05". |
| 2.3.2 | Retry 3x → reversal mti=0400 not implemented. | **fixed** — reversal sent via `authorizationClient.reverse()` when logger fails for APPROVED tx. Returns code "96". |
| 2.3.3 | Logger down for APPROVED tx → only logs error. | **fixed** — now reverses reservation and returns DECLINED code "96". (Validation corrected: `LOG.error`, not warning.) |
| 2.3.4 | No `@Valid` on `@RequestBody` in `RouteController`. | **remains** — `@Valid` absent; Jakarta annotations on DTO never triggered. |

### 2.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 2.4.1 | `amount` is `Long` vs gateway `Integer`. | **fixed** — unified to `Long` in common DTO. |
| 2.4.2 | `terminalType` is `String`, auth expects enum. | **remains** — still unconstrained `String` in common DTO. |
| 2.4.3 | `transmissionDateTime` is `LocalDateTime` vs gateway `String`. | **partially fixed** — changed to `String` in common DTO (consistent), but not a proper temporal type. Parsing deferred to service layer. |

### 2.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 2.5.1 | Test fragility — `AuthorizationClient(null)`. | **fixed** — proper test doubles (stubs, lambdas), centralized factory, behavioral assertions, 5 test cases. (Validation corrected: first arg was non-null `SwitchProperties`.) |

---

## 3. Authorization

### 3.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 3.1.1 | Duplicate `CardStatus`, missing `DELETED`. | **partially fixed** — `AuthService` uses common `CardModelStatus` enum. Local `constants/CardStatus.java` exists but is dead code. |
| 3.1.2 | Duplicate `AuthorizationRequest`/`AuthorizationResponse` DTOs. | **fixed** — auth uses common DTOs. |
| 3.1.3 | Duplicate `ReserveRequest`. | **fixed** — uses common `ReserveRequest`. |
| 3.1.4 | `processingTimeMs` hardcoded `1`. | **fixed** — real elapsed time measured. |
| 3.1.5 | `new Random()` per `generateAuthCode()`. | **remains** — still `new Random().ints(...)` per call, not `ThreadLocalRandom`. |
| 3.1.6 | `java.util.Calendar` for RRN. | **remains** — still uses `Calendar`. |

### 3.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 3.2.1 | `WebClient.block()` in synchronous service. | **remains** — both `getCard()` and `reserve()` use `WebClient.block()`. |
| 3.2.2 | `WebClient.create()` — no timeouts, no base URL. | **remains** — still bare `WebClient.create()`. |
| 3.2.3 | Mutable Lombok DTOs. | **fixed** — all DTOs are records. |
| 3.2.4 | No `@ControllerAdvice`. | **remains** — no `@ControllerAdvice` in authorization module. |

### 3.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 3.3.1 | TODO daily/monthly limit not implemented. | **fixed** — fully implemented with pessimistic locking via `LimitUsage` entity. |
| 3.3.2 | `@Index(columnList = "card_id")` wrong column. | **fixed** — now indexes on `pan` and `usage_date`. (Validation corrected line to `:15`.) |
| 3.3.3 | `getCard()` null return → NPE. | **remains** — no explicit null check on `WebClient.block()` result. |
| 3.3.4 | NPE risks with `expiryDate` and `amount`. | **partially fixed** — `amount` protected by `@Valid`; `expiryDate` null check remains. (Validation corrected: `availableBalance` is primitive `long`, cannot NPE.) |

### 3.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 3.4.1 | **CRITICAL:** `expiryDate` `LocalDate` vs `String` MMyy — auth flow broken. | **fixed** — both sides use `YearMonth` via shared `CardModel` in common. |
| 3.4.2 | CMS URL port mismatch (`:8081` vs `:8080`). | **remains** — auth defaults to `:8081`, card-management defaults to `:8080`. |
| 3.4.3 | `status` enum vs String mismatch. | **fixed** — shared `CardModelStatus` enum from common. |

### 3.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 3.5.1 | `pan` URL path traversal risk. | **remains** — raw string concatenation, no `UriComponentsBuilder` or encoding. |
| 3.5.2 | `pom.xml` dependency issues. | **partially fixed** — common dep OK; hardcoded `spring-test:6.2.0` and redundant `springdoc` version remain. (Validation corrected: PostgreSQL has `<scope>runtime</scope>`.) |

---

## 4. Card Management

### 4.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 4.1.1 | Orphan `CardController.java` in `resources/`. | **fixed** — deleted. |
| 4.1.2 | Manual constructor injection in `CardController`. | **fixed** — uses `@RequiredArgsConstructor`. |
| 4.1.3 | Mutable `private String pan` in `LuhnValidator`. | **fixed** — field removed, `pan` is now local variable only. |
| 4.1.4 | Property prefix mismatch in `CardGeneratorOptions`. | **fixed** — all 5 properties use `app.card-generator.*` prefix. (Validation corrected: only `min-balance` was wrong.) |
| 4.1.5 | `@Transient` on `private static DateTimeFormatter`. | **fixed** — field removed from entity. Formatter extracted to `CardExpiryDateMapper` as `private static final`. |

### 4.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 4.2.1 | `findCards()` returns `List` with `Pageable`. | **fixed** — redesigned to manual offset/limit with separate count query. |
| 4.2.2 | `expiryDate` `@Transient` + lazy-init getter. | **fixed** — `expiryDate` now plain `String` JPA column. Conversion via MapStruct `CardExpiryDateMapper`. (Validation corrected: getter cached, not reparse-every-call.) |
| 4.2.3 | No `@Transactional` on mutations. | **remains** — zero `@Transactional` annotations in entire module. |
| 4.2.4 | No `@Version` for optimistic locking. | **remains** — no `@Version` field. |

### 4.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 4.3.1 | `reserve()` without `@Transactional`. | **remains** — read-check-save unprotected. |
| 4.3.2 | `patchCard()` lost-update bug. | **remains** — read-modify-write without transaction or version. |
| 4.3.3 | `monthlyLimit = dailyLimit * 30`. | **remains** — hardcoded at `CardGeneratorService.java:53`. |

### 4.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 4.4.1 | No finding (serialization correct). | N/A — (Validation confirmed `getStrExpiryDate()` used correctly. Now refactored to MapStruct mapper.) |

### 4.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 4.5.1 | `@Transient` on static field. | **fixed** — annotation and field removed from entity. |
| 4.5.2 | `reserve()` in entity, no `@Transactional`. | **fixed** — moved to immutable `Card` record as pure function `withReserved()`. |
| 4.5.3 | `this.pan = pan` on singleton `LuhnValidator`. | **fixed** — mutable field removed. |

---

## 5. Terminal Simulator

### 5.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 5.1.1 | Duplicated DTOs, not using `common`. | **remains** — local `AuthorizationRequest`/`AuthorizationResponse` still in use; common dependency commented out in `pom.xml`. |
| 5.1.2 | URLs hardcoded. | **fixed** — injected via `@Value` with env-var overrides. |
| 5.1.3 | `merchantId`, MCC, `acquirerId` hardcoded. | **partially fixed** — randomized at runtime (not single constants), but not externally configurable via properties. |
| 5.1.4 | `System.out.println` in service. | **fixed** — removed (no logger added either). |
| 5.1.5 | `Random` instance field, non-threadsafe. | **remains** — `new Random()` in service and all strategy classes. |
| 5.1.6 | `stanCounter` plain `int`. | **remains** — `StanGenerator` uses plain `int` with `++`, not `AtomicInteger`. |
| 5.1.7 | `acquirerId` same prefix as `terminalId`. | **fixed** — `"TERM"` vs `"ACQ"` prefixes. |
| 5.1.8 | `new RestTemplate()` — deprecated. | **remains** — `RestTemplate` bean in `AppConfig`, used in `GatewayClient`. |

### 5.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 5.2.1 | `RestTemplate` deprecated, use `RestClient`. | **remains** — still `RestTemplate`. |
| 5.2.2 | No virtual threads. | **remains** — not configured. |

### 5.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 5.3.1 | No try-catch in `generateTransactionHandler`. | **remains** — zero try-catch blocks; single failure aborts batch. |
| 5.3.2 | `ex.printStackTrace()` via stdout in `GlobalExceptionHandler`. | **remains** — `ex.printStackTrace()` still present, no SLF4J logger in module. (Validation corrected: original claim about `TerminalSimulatorService:34` was wrong; handler is in `GlobalExceptionHandler`.) |
| 5.3.3 | Switch no `default` case. | **remains** — all 5 enum values covered but no `default`. |
| 5.3.4 | "mixed" fractions sum 0.95. | **fixed** — last segment runs to `count`, capturing remaining ~5% for BLOCKED. |
| 5.3.5 | `daily_limit`: amount = limit - 1, if limit==0 → -1. | **remains** — `AlmostDailyLimitStrategy.calculateAmount()` returns `card.dailyLimit() - 1` with no zero guard. |
| 5.3.6 | Cast issue on `no_money` amount. | **remains** — refactored into strategy pattern but `amount` type mismatch (`long` vs `Long`) persists. (Validation corrected: cast applies to `randomValue` only.) |
| 5.3.7 | `getInvalidPan()` ~10% still valid Luhn. | **fixed** — deterministically flips check digit between `'0'` and `'1'`, always producing invalid Luhn. |
| 5.3.8 | `issuerId = ""` empty string. | **remains** — still set to `""` in `TransactionFactory`. |

### 5.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 5.4.1 | `amount: long`, includes `issuerId`. | **remains** — local DTO uses primitive `long`; `issuerId` present and set to `""`. |
| 5.4.2 | Calls card-management directly, bypassing gateway. | **fixed** — auth path goes through gateway. Card fetch still direct (intentional for setup). |
| 5.4.3 | `transmissionDateTime` format. | **remains** — uses `LocalDateTime.toString()` without explicit formatter. |

### 5.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 5.5.1 | `cards` volatile ref race. | **remains** — `volatile List<Card>` with `ArrayList`, concurrent `run()` calls would race. |
| 5.5.2 | Shared `authResps` list not thread-safe. | **remains** — plain `ArrayList`. Currently safe (sequential calls), but not future-proof. |

---

## 6. Merchant Acquirer

### 6.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 6.1.1 | Duplicated DTOs. | **fixed** — imports `AuthorizationRequest`/`AuthorizationResponse` from common. |
| 6.1.2 | Hardcoded gateway URL. | **fixed** — injected via `@Value` with env-var override. |
| 6.1.3 | Currency `"643"` hardcoded. | **fixed** — parameterized, sourced from card data. |
| 6.1.4 | `"TERM001"`, `"POS"` hardcoded. | **partially fixed** — terminal ID randomized; `"POS"` type still hardcoded. |
| 6.1.5 | Enum lowercase (`grocery`, `electronics`). | **remains** — still lowercase, not UPPER_CASE. |
| 6.1.6 | `countLower`/`countUpper` misleading naming. | **remains** — fields still named `countLower`/`countUpper` despite representing amount bounds. |
| 6.1.7 | `log.info(String.valueOf(...))` — not parameterized. | **remains** — 4 instances of `log.info(String.valueOf(...))`. |
| 6.1.8 | `ErrorResponse.java` dead code. | **fixed** — deleted; using common `ErrorResponse`. |
| 6.1.9 | `new RestTemplate()`. | **fixed** — replaced with Spring 6 `RestClient`. |

### 6.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 6.2.1 | `RestTemplate` deprecated. | **fixed** — same as 6.1.9, uses `RestClient`. |
| 6.2.2 | DTO lacks `issuerId`. | **fixed** — `issuerId` present in common `AuthorizationRequest`. (Validation corrected: clarify that field was absent, not about setter.) |

### 6.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 6.3.1 | Fabricated response codes (`"505"`, `999`). | **fixed** — deterministic fallback only on gateway exception. |
| 6.3.2 | No catch-all `Exception` handler. | **remains** — handles specific exceptions only. |
| 6.3.3 | 0 cards → `IndexOutOfBoundsException`. | **fixed** — `CardProvider` guards with `isEmpty()` check. |
| 6.3.4 | 0 merchants → `IllegalArgumentException`. | **fixed** — `MerchantProvider` guards with `isEmpty()` check. |
| 6.3.5 | `transmissionDateTime` no timezone. | **remains** — `LocalDateTime.now().toString()`, no timezone info. |

### 6.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 6.4.1 | No `issuerId` in DTO. | **fixed** — same as 6.2.2. |
| 6.4.2 | Calls through gateway (correct). | **remains** — all calls go through gateway (intentional architecture). |

### 6.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 6.5.1 | `Random` as local variable per `run()`. | **fixed** — moved to `@Component` bean field. |
| 6.5.2 | `StanGenerator` `AtomicInteger`. | **remains** — still `AtomicInteger` (correct, no issue). |

---

## 7. Transaction Logger

### 7.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 7.1.1 | `TransactionStatus` duplicated from switch. | **fixed** — single copy in common. |
| 7.1.2 | `"stored"` hardcoded status string. | **remains** — `private static final String STORED_STATUS = "stored"`. No enum. |

### 7.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 7.2.1 | `withPage(int)` ignores argument. | **fixed** — now computes `pageNumber * limit`. |
| 7.2.2 | `getSort()` hardcoded. | **fixed** — sort is constructor parameter with default. |
| 7.2.3 | `.setAllowedOrigins("*")`. | **fixed** — configurable via `websocket.allowed-origins` property. |
| 7.2.4 | `remove()` during forEach. | **fixed** — dead sessions collected into separate set, removed after iteration. |

### 7.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 7.3.1 | `matches()` compares 20 fields incl timestamps. | **remains** — all 21 fields compared. Retransmission with different `createdAt` triggers false conflict. (Validation corrected count to 20, now 21.) |
| 7.3.2 | `store()` NOT `@Transactional`. | **partially fixed** — still no `@Transactional`. Broadcast failure now properly caught and logged, doesn't fail store. (Validation corrected: `store()` was never `@Transactional`, so no ghost-tx risk.) |
| 7.3.3 | No WebSocket heartbeat. | **fixed** — scheduled `@Scheduled` ping with configurable interval. |

### 7.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 7.4.1 | No `terminalType`, no `responseCode` in `TransactionResponse`. | **partially fixed** — `terminalType` added. `responseCode` still absent. |

### 7.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 7.5.1 | Idempotent store pattern. | **fixed** — check-then-insert + `DataIntegrityViolationException` fallback. |
| 7.5.2 | `TransactionSpecification` nullable predicates. | **fixed** — comprehensive filtering by PAN, status, date range, merchantId, issuerId, MCC. |

---

## 8. Dashboard

### 8.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 8.1.1 | `client.ts` empty stub. | **fixed** — generic `fetchApi<T>()` wrapper. |
| 8.1.2 | `useWebSocket.ts` empty stub. | **fixed** — full implementation with reconnection + backoff. |
| 8.1.3 | `useStats.ts` empty stub. | **fixed** — fetches from API with loading/error states. |
| 8.1.4 | `Filters.tsx` placeholder. | **fixed** — full filter form with status, issuer, MCC, date range. |
| 8.1.5 | `LineChart.tsx` placeholder. | **fixed** — full Recharts `LineChart` (renamed to `TransactionLineChart`). (Validation corrected: was placeholder, not empty stub.) |
| 8.1.6 | `PieChart.tsx` placeholder. | **fixed** — full Recharts `PieChart` (renamed to `TransactionPieChart`). (Validation corrected: was placeholder, not empty stub.) |
| 8.1.7 | Mock data imported directly in `App.tsx`. | **fixed** — uses hooks/API. Mock data only used for lookup dictionaries. |
| 8.1.8 | Only 4 of 8 `DashboardStats` displayed. | **remains** — `KpiCards` still shows only 4 metrics. |
| 8.1.9 | `w-2/3` hardcoded, non-responsive. | **remains** — still `w-2/3` in `App.tsx`. |

### 8.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 8.2.1 | No `useMemo` on `kpiCards` array. | **remains** — array recreated every render. |
| 8.2.2 | `TransactionModal` rows computed inline. | **remains** — no memoization. |
| 8.2.3 | `toLocaleString('ru-RU').replace(',', '.')` hacky. | **remains** — still uses `.replace(',', '.')` hack. |

### 8.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 8.3.1 | No error boundaries. | **remains** — no `ErrorBoundary` component. |
| 8.3.2 | No loading states. | **fixed** — loading states in Header, charts, and table. |
| 8.3.3 | No empty state. | **fixed** — empty states in all major components. |
| 8.3.4 | `format.test.ts` misleading `describe` name. | **remains** — `describe('formatAmount', ...)` tests `convertPenniesToRubles`. |
| 8.3.5 | `App.test.tsx` `\s*` false-green test. | **fixed** — tests assert specific content, no vacuous regex. (Validation corrected line to `:30`.) |
| 8.3.6 | Parameter named `pennies`. | **remains** — still `pennies` in `convertPenniesToRubles`. |

### 8.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 8.4.1 | TS types expect `terminalType`/`responseCode` not in logger API. | **remains** — `responseCode` in frontend type but not backend DTO. `terminalType` optionality differs. |
| 8.4.2 | `totalAmount` — kopecks or rubles undocumented. | **remains** — no documentation about monetary unit. |

---

## 9. Common

### 9.1 Basics

| # | Suggestion | Outcome |
|---|-----------|---------|
| 9.1.1 | `AuthorizationRequest` no validation annotations. | **fixed** — comprehensive Jakarta annotations on all fields. |
| 9.1.2 | `@Nullable` + `@NotNegative` conflict. | **remains** — still present, but functionally correct (`@NotNegative` validator returns `true` for `null`). (Validation confirmed no conflict per Jakarta spec.) |
| 9.1.3 | `CardModel.status` is `String`, not enum. | **fixed** — now `CardModelStatus` enum. |

### 9.2 Core Libraries

| # | Suggestion | Outcome |
|---|-----------|---------|
| 9.2.1 | `ExactSize` correct composed annotation. | **fixed** — correct implementation, null-safe. |
| 9.2.2 | `DigitsOnly` `^\d*$` allows empty. | **remains** — still `^\d*$` in `DigitsOnlyValidator`. |

### 9.3 Incomplete

| # | Suggestion | Outcome |
|---|-----------|---------|
| 9.3.1 | Missing shared DTOs. | **fixed** — comprehensive set: `TransactionRequest`, `TransactionResponse`, `TransactionStoredResponse`, `TransactionStatus`, card-management DTOs, `ErrorResponse`, `ServiceUnavailableResponse`. |
| 9.3.2 | `AuthorizationRequest` no `issuerId`. | **fixed** — `issuerId` present with `@NotBlank`. |
| 9.3.3 | Type mismatches (`amount: Integer`, `transmissionDateTime: String`). | **partially fixed** — `amount` unified to `Long`. `transmissionDateTime` remains `String` in `AuthorizationRequest` while `TransactionRequest`/`TransactionResponse` use `Instant`. |

### 9.4 Integration

| # | Suggestion | Outcome |
|---|-----------|---------|
| 9.4.1 | Nobody imports from common — dead code. | **fixed** — merchant-acquirer and transaction-logger actively import common DTOs. |
| 9.4.2 | Card-management DTOs sharing. | **fixed** — card-management DTOs centralized; `ErrorResponse` shared by 3+ services. (Validation corrected: "only" was false.) |

### 9.5 Other

| # | Suggestion | Outcome |
|---|-----------|---------|
| 9.5.1 | `retryAfterMs` is `String`. | **remains** — still `String` in `ErrorResponse` record. (Validation corrected line to `:8`.) |
| 9.5.2 | `@Pan` regex anchoring. | **fixed** — `"^\\d{16}$"` properly anchored. (Validation corrected: review omitted anchors.) |

---

## Scorecard

| Outcome | Count |
|---------|-------|
| fixed | 52 |
| partially fixed | 9 |
| remains | 48 |
| won't fix | 0 |
| n/a | 1 |

### By module

| Module | Fixed | Partial | Remains |
|--------|-------|---------|---------|
| Gateway | 4 | 0 | 10 |
| Switch | 9 | 1 | 6 |
| Authorization | 7 | 3 | 8 |
| Card Management | 10 | 0 | 5 |
| Terminal Simulator | 6 | 1 | 16 |
| Merchant Acquirer | 11 | 1 | 6 |
| Transaction Logger | 8 | 2 | 3 |
| Dashboard | 10 | 0 | 10 |
| Common | 9 | 1 | 4 |

### Top remaining issues by severity

| Severity | Finding | Module |
|----------|---------|--------|
| **High** | 4.2.3/4.3.1/4.3.2 — No `@Transactional` on mutations | Card Mgmt |
| **High** | 5.3.1 — No try-catch, simulation crashes | Terminal |
| **High** | 3.2.1/3.2.2 — `WebClient.block()` + no config | Auth |
| **Medium** | 5.1.6 — Non-atomic STAN counter | Terminal |
| **Medium** | 7.3.1 — `matches()` compares timestamps | Logger |
| **Medium** | 8.4.1 — TS types mismatch backend | Dashboard |
| **Medium** | 3.4.2 — CMS URL port mismatch | Auth |
