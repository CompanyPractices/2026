# Review Validation — SMP Project

**Date:** 2026-06-08
**Validated against:** source code in `services/`
**Total findings:** ~90
**Inconsistencies:** 19

---

## Gateway

### 1.2.1 — INACCURATE

Claim: HttpClient "not Spring-managed".
Actual: `BeanConfiguration.java:11` defines `@Bean public HttpClient httpClient()`. `HealthService` receives via constructor injection. Fully Spring-managed. Valid critique: JDK type lacks Spring features. "Not Spring-managed" false.

### 1.5.1 — INACCURATE

Claim: `InMemoryRateLimiter.java:21` — `synchronized(window)` on computeIfAbsent value.
Actual: Line 21 = `this.clock = clock`. `computeIfAbsent` at line 24, `synchronized(window)` at line 27. Correct ref: `:24,27`.

---

## Switch

### 2.3.3 — INACCURATE

Claim: Logger down → "only logs warning".
Actual: `RouteService.java:49` uses `LOG.error(...)`. Error level, not warning.

### 2.5.1 — INACCURATE

Claim: Test creates `AuthorizationClient(null)`.
Actual: `RouteServiceTest.java:22,61` = `new AuthorizationClient(SwitchTestData.defaultProperties(), null)`. First arg non-null `SwitchProperties` with `authorizationStubEnabled=true`. Only `RestClient` param is null. Shorthand misleading.

---

## Authorization

### 3.3.2 — INACCURATE

Claim: `LimitUsage.java:27` — `@Index(columnList = "card_id")`.
Actual: `@Index` at line 15, not 27. Line 27 = `@Column(name = "usage_date")`. Substantive claim (column `card_id` absent) correct.

### 3.5.2 — INACCURATE

Claim: PostgreSQL dep active "without `scope`".
Actual: `authorization/pom.xml:92` has `<scope>runtime</scope>`. Scope present. JPA dep missing scope = normal (compile scope intentional).

---

## Card Management

### 4.1.4 — INACCURATE

Claim: Property prefix `generator.card-service.*` in properties vs `app.card-generator.*` expected. `minBalance`/`maxBalance` default to 0.
Actual: Only `min-balance` (line 22) uses wrong prefix `generator.card-service.min-balance`. Remaining 4 props (`max-balance`, `min-daily-limit`, `max-daily-limit`, `currency-code`) use correct `app.card-generator.*` prefix. Only `minBalance` defaults to 0. `maxBalance` binds correctly to `50000000`.

### 4.2.2 — INACCURATE

Claim: `expiryDate` "always null (new entity instances)" + "reparses every call".
Actual: All-args constructor (`CardEntity.java:88`) calls `setExpiryDate(LocalDate.now().plusYears(3))` — new instances have `expiryDate` populated. Only null after JPA load (no-arg constructor). Getter at lines 91-96 is lazy-init: parses once, caches. Not reparse-every-call.

### 4.4.1 — INACCURATE

Claim: Serialization mismatch — wire format ambiguous.
Actual: `CardService.cardModelFromEntity()` line 143 uses `entity.getStrExpiryDate()` (returns `String`). Never calls `getExpiryDate()` (returns `LocalDate`). Wire format consistently `String` MMyy. No ambiguity.

---

## Terminal Simulator

### 5.3.2 — WRONG

Claim: `TerminalSimulatorService.java:34` catches only `HttpClientErrorException` (4xx). 5xx → raw propagate.
Actual: `TerminalSimulatorService.java` has **zero** try-catch blocks. Line 34 = `int hour;` (local var in `randomDateTime()`). `HttpClientErrorException` caught in `GlobalExceptionHandler.java:33`. Catch-all `Exception` handler at line 39 handles 5xx. Both file ref and behavioral claim wrong.

### 5.3.6 — INACCURATE

Claim: `(int)(card.availableBalance() + randomValue)` — cast truncates sum.
Actual: `TerminalSimulatorService.java:108` = `card.availableBalance() + (int) randomValue`. Cast applies to `randomValue` only, not sum. Truncation/overflow concern doesn't follow from actual code.

---

## Merchant Acquirer

### 6.2.2 — UNCLEAR

Claim: `acquirerId` never set to `issuerId`.
Actual: `AuthorizationRequestFactory.java:32` = `.acquirerId(merchant.getAcquirerId())` — correctly sets `acquirerId`. Merchant `AuthorizationRequest` record has no `issuerId` field at all (12 fields, none named `issuerId`). Finding should state: "DTO missing `issuerId`" — not ambiguous claim about setter.

---

## Transaction Logger

### 7.3.1 — INACCURATE

Claim: `matches()` compares 18 fields.
Actual: `TransactionMapper.java:70-90` has 20 `Objects.equals()` comparisons (lines 71-90). Count = 20, not 18.

### 7.3.2 — WRONG

Claim: WebSocket broadcast inside `@Transactional`. Rollback → ghost tx.
Actual: `store()` (`TransactionService.java:42`) has **no `@Transactional`**. Only `@Transactional` in file = `@Transactional(readOnly = true)` on `getStats()` (line 85). `store()` calls `saveAndFlush()` (line 50) — commits before broadcast (lines 58-59). No surrounding transaction. Ghost tx scenario impossible.

---

## Dashboard

### 8.1.5 — INACCURATE

Claim: `LineChart.tsx` — "Empty stub."
Actual: Returns `<div>LineChart Component</div>`. Placeholder component, not empty. Same pattern as `Filters.tsx` (8.1.4 correctly described as `<div>Filters Component</div> only`).

### 8.1.6 — INACCURATE

Claim: `PieChart.tsx` — "Empty stub."
Actual: Returns `<div>PieChart Component</div>`. Same as 8.1.5 — placeholder, not empty.

---

## Common

### 9.1.2 — WRONG

Claim: `@Nullable` + `@NotNegative` → validation fail.
Actual: `@NotNegative` = `@Min(0)`. Per Jakarta Bean Validation spec, `@Min` treats `null` as valid. `null` → skip → valid. `5` → `5 >= 0` → valid. `-1` → fail. No conflict.

### 9.4.2 — INACCURATE

Claim: Card-management DTOs = "only successful common sharing case."
Actual: `ErrorResponse` from `com.processing.common.dto` shared by card-management, merchant-acquirer, transaction-logger. `@Bin`, `@Pan`, `@NotNegative` also shared. "Only" false.

### 9.5.1 — INACCURATE

Claim: `retryAfterMs` at `ErrorResponse.java:5`.
Actual: Line 5 = `String message`. `retryAfterMs` at line 8. Type claim (`String`) correct.

### 9.5.2 — INACCURATE

Claim: `@Pattern("\\d{16}")` — exact 16 digits.
Actual: `Pan.java:9` = `@Pattern(regexp = "^\\d{16}$")`. Has anchors `^` and `$`. Review omits anchors — materially different regex. Anchored version = exact match. Unanchored (review's version) = substring match.

---

## Summary

| # | Finding | Verdict | Issue |
|---|---------|---------|-------|
| 1 | 1.2.1 | INACCURATE | HttpClient IS Spring-managed (`@Bean`) |
| 2 | 1.5.1 | INACCURATE | Wrong line: synchronized at :27, not :21 |
| 3 | 2.3.3 | INACCURATE | `LOG.error`, not warning |
| 4 | 2.5.1 | INACCURATE | First arg non-null; shorthand misleading |
| 5 | 3.3.2 | INACCURATE | `@Index` at :15, not :27 |
| 6 | 3.5.2 | INACCURATE | PostgreSQL has `<scope>runtime</scope>` |
| 7 | 4.1.4 | INACCURATE | Only `min-balance` wrong prefix; `maxBalance` binds OK |
| 8 | 4.2.2 | INACCURATE | New instances populated; getter caches, not reparses |
| 9 | 4.4.1 | INACCURATE | No serialization mismatch — uses `getStrExpiryDate()` |
| 10 | 5.3.2 | WRONG | Zero catch blocks in file; handler in GlobalExceptionHandler |
| 11 | 5.3.6 | INACCURATE | Cast on `randomValue` only, not sum |
| 12 | 6.2.2 | UNCLEAR | DTO has no `issuerId` field — say that |
| 13 | 7.3.1 | INACCURATE | 20 fields, not 18 |
| 14 | 7.3.2 | WRONG | `store()` not `@Transactional`; no ghost tx risk |
| 15 | 8.1.5 | INACCURATE | Placeholder component, not empty |
| 16 | 8.1.6 | INACCURATE | Placeholder component, not empty |
| 17 | 9.1.2 | WRONG | `@Min(0)` treats null as valid — no conflict |
| 18 | 9.4.2 | INACCURATE | `ErrorResponse` shared across 3 services |
| 19 | 9.5.1 | INACCURATE | Line 8, not line 5 |
