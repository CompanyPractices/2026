# Review Validation — 2nd Pass

**Validated:** 2026-06-11
**Source:** `review.md` (reviewer: qwen/qwen3.7-max)
**Method:** Cross-check every statement against source code. Report inconsistencies only.

---

## Common Module

### 1. Basics

- **Wrong line numbers** — `AuthorizationRequest:62-83` claimed for `withIssuerId()`/`forReversal()`. Actual: fields at 62-70, methods start at **72**. Correct range: `72-83`.

- **Wrong validator count** — Claim says "5 custom validators". Actual: **6** (`BinValidator`, `PanValidator`, `DigitsOnlyValidator`, `RegexValidator`, `ExactSizeValidator`, `NotNegativeValidator`).

- **Inaccurate CardStatus description** — Claim says authorization "checks against string constants in `CardStatus.java`". Wrong. Authorization uses `CardModelStatus` enum directly (`currCardStatus == CardModelStatus.ACTIVE`). `CardStatus.java` is dead code — never imported or referenced. Framing that authorization cross-references both is incorrect.

---

## Gateway

No inconsistencies found. All 10 claims verified accurate.

---

## Switch

### 1. Basics

- **HealthResponse "duplicates" is wrong** — `switch/dto/HealthResponse` has fields `status, service, version, dependencies`. Authorization has `status, service, dependencies` (no `version`). Card-management has `status, service, cardsInDatabase` (completely different). Not duplicates — different record shapes.

### 4. Unit Tests

- **"10 test files" wrong** — `switch/src/test/` has **9 files total**, only **6 are test classes**. Other 3 are support doubles (`CapturingAuthorizationClient`, `TrackingLoggerClient`) and test data (`SwitchTestData`).

### 5. Other

- **`toInstant` description inaccurate** — Claim says "no default fallback for truly malformed dates". Code at `RouteService:128-131`: if `dateTime` is null/blank, returns `Instant.now()` — that IS a default fallback. Unparseable non-blank string does throw, but claim conflates "missing" with "unparseable".

---

## Authorization

### 1. Basics

No inconsistencies found.

### 2. Database Integrity

- **Monthly limit query sums wrong field** — `LimitUsageRepository:26` sums `lu.dailyAmount`, **not** `lu.monthlyAmount`. Method name says "monthly" but aggregates daily field. Deeper bug than review noted.

### 3. Concurrency

No inconsistencies found.

### 4. Unit Tests

- **Active test count wrong** — Claim says "DBIntegrationTest (9 active tests)". Actual: **8** active `@Test` methods. `authorizeShouldReturnDeclinedWhenExceededMonthlyLimit()` at line 146 lacks `@Test` — the count of 9 includes the broken one.

### 5. Other

No inconsistencies found.

---

## Card Management

### 1. Basics

- **Wrong line numbers** — `countCardsFiltered()` no-arg at line **82**, not 85. Five-arg at line **94**, not 99.
- **Wrong line number** — `faker.name().fullName().toUpperCase()` at line **50**, not 56.

### 2. Database Integrity

- **"Soft delete without filtering" is FALSE** — `CardEntity:23` has `@SQLRestriction("status <> 'DELETED'")`. All JPA queries (including `findByPan`) automatically exclude DELETED cards. Review claim is wrong. Also `deleteCard()` at line **120**, not 127.

### 3. Concurrency

- **Wrong line numbers** — `reserve()` spans lines **145-148**, not 143-147.

### 5. Other

- **Wrong line number** — `new Random()` at line **32**, not 33.

---

## Terminal Simulator

### 3. Concurrency

- **`Random` thread-safety overstated** — `java.util.Random` is thread-safe per Javadoc. `nextInt()` uses CAS internally. Sharing causes contention, not correctness bugs. Claim implies duplicate/corrupt values — not true. `ThreadLocalRandom` improves perf but `Random` isn't broken.

### 5. Other

- **Null query param framing wrong** — `UriComponentsBuilder.queryParam("limit", null)` omits param from URL entirely. Does NOT produce `limit=null`. Standard Spring pattern for conditional params. Not a bug.

---

## Merchant Acquirer

### 3. Concurrency

- **Race condition description imprecise** — Claim says race is "between `set(1)` and `get()`". Actual critical race: between `addAndGet(1)` and `counter.set(1)` — two threads can both pass `> 999999` check concurrently. Also `value = counter.get()` on line 14 is redundant since `addAndGet` already returned the value.

---

## Transaction Logger

### 1. Basics

- **Wrong line numbers** — `sumAmount()` at line **15**, not 14 (14 is `@Query` annotation). `averageProcessingTimeMs()` at line **17-18**, not 16.

### 2. Database Integrity

- **Wrong line number** — `createdAt` field at `Transaction:61`, not 59.

- **Query count wrong** — Claim says "5 separate queries". Actual: **6** — `count()`, `countByStatus(APPROVED)`, `countByStatus(DECLINED)`, `sumAmount()`, `countByCreatedAtAfter()`, `averageProcessingTimeMs()`.

### 3. Concurrency

- **Wrong line numbers** — `broadcast()` at `DefaultWebSocketManager:32-47`, not 40-53.

### 4. Unit Tests

- **Test file count debatable** — 16 `.java` files in test dir, but one is `TestContainersConfig.java` (config helper, not test class). Actual test files: **15**.

### 5. Other

- **Wrong line number** — `@Pattern` annotation at `TransactionFilter:15`, not 12.

---

## Dashboard

### 1. Basics

- **Wrong line numbers** — `fetchApi` spans lines **1-9**, not 2-10.

### 3. Concurrency

- **Wrong line numbers** — `useLiveStats` effect at lines **40-56**, not 49-63.

- **"Double-counting" misleading** — Effect uses `statsRef` to accumulate, only processes `liveTransactions[0]`. Increments once per new message, not repeatedly. Label "double-counting" is inaccurate.

- **Wrong line number** — `useWebSocket` dependency array at line **100**, not 96.

### 4. Unit Tests

- **"6 test files" but lists 5** — Claim says "6 test files" then names 5. Dashboard has exactly **5** test files. `starters/` file doesn't count — different project.

---

## Cross-Cutting Findings

### 1. Basics

- **`maskPan` line numbers wrong** — `card-management/CardServiceImpl:153` → actual **151**. `authorization/AuthService:391-397` → method named `maskPAN` (uppercase), ends at **398**.

- **HealthResponse count wrong** — Claim says "5+ services". Actual: **7 services + 1 shared starter = 8 copies**. Understated.

### 4. Unit Tests

- **"Terminal-simulator minimal coverage" wrong** — Has 4 test files including 168-line service test with 8 test methods covering scenarios, error cases, proportions. Not "minimal." Merchant-acquirer (3 files) is closer to minimal.

### 5. Other

- **"Gateway never sets MDC requestId" is FALSE** — `gateway/…/filter/RequestLoggingFilter.java:50` calls `MDC.put("requestId", requestId)`. Also passes `X-Request-Id` downstream (line 46). The `%X{requestId}` in log pattern IS populated. Claim is wrong.

---

## Summary

| Section | Issues |
|---------|--------|
| Common | 3 (line nums, validator count, CardStatus framing) |
| Gateway | 0 |
| Switch | 3 (HealthResponse not duplicate, test count, toInstant) |
| Authorization | 2 (wrong field in monthly query, test count) |
| Card Management | 5 (line nums ×3, soft-delete filtering false, reserve lines) |
| Terminal Simulator | 2 (Random thread-safety, null query param) |
| Merchant Acquirer | 1 (race condition description) |
| Transaction Logger | 6 (line nums ×4, query count, test count) |
| Dashboard | 5 (line nums ×3, double-counting label, test count) |
| Cross-Cutting | 4 (maskPan lines, HealthResponse count, terminal coverage, MDC requestId) |
| **Total** | **31** |

---

## Reviewer Notes (disagreements / partial agreements)

### Authorization — "Active test count wrong" (line 59)

> **Partially disagree.** Verified: DBIntegrationTest has 9 methods annotated with `@Test` (including `whatDatabase()`). The method at line 146 lacks `@Test` — it's a 10th method that doesn't count. So "9 active tests" was technically correct as `@Test`-annotated method count. However, `whatDatabase()` is a diagnostic, not a real test. Review corrected to "9 `@Test`-annotated methods, including `whatDatabase()` diagnostic" for clarity.

### Transaction Logger — "Test file count debatable" (line 126)

> **Partially disagree.** "16 test files" counted all `.java` files in test dir, which is a valid file count. But opponent is right that `TestContainersConfig.java` is a support file, not a test class. Review corrected to "15 test classes + 1 config helper" for precision.

### Dashboard — "Double-counting misleading" (line 144)

> **Partially disagree.** The effect does process only `liveTransactions[0]` per firing, so "double-counting" (implying same tx counted twice) was imprecise. However, the underlying concern is valid: when the WebSocket slice drops old items from the array, stats accumulated via `statsRef` don't decrement. Stats monotonically increase. Rephrased to "stats drift over time" in review.
