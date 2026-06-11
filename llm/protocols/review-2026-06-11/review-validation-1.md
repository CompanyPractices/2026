# Review Validation 1 — 2026-06-11

Cross-check review.md statements vs source. Inconsistencies only. OK statements not listed.

---

## Common Module

### #1 Validator count wrong
- **review.md:98**: "5 custom validators"
- **Source**: 6 validators in `services/common/src/main/java/com/processing/common/dto/annotations/validators/`:
  `BinValidator`, `DigitsOnlyValidator`, `ExactSizeValidator`, `NotNegativeValidator`, `PanValidator`, `RegexValidator`
- **Fix**: change 5 → 6

### #2 DTO count imprecise
- **review.md:98**: "28 DTOs untested"
- **Source**: 28 Java files under `dto/`, but 17 actual DTOs + 5 annotations + 6 validators
- **Fix**: change "28 DTOs" → "28 DTOs, annotations, and validators" or clarify 17 DTOs proper

### #3 Line range off for AuthorizationRequest business methods
- **review.md:82**: "AuthorizationRequest (`...AuthorizationRequest.java:62-83`) contains business methods `withIssuerId()` and `forReversal()`"
- **Source**: Lines 62-70 record components; methods at 72-83
- **Fix**: change 62-83 → 72-83

### #4 `forReversal` dead parameter not noted
- **review.md:82**: says `forReversal()` exists
- **Source**: `forReversal(String rrn)` accepts `rrn` but never uses it — hardcodes `"0400"` MTI, ignores param
- **Add**: note that `rrn` param is dead

---

## Gateway

### #5 Wrong path: limiter → ratelimit
- **review.md:124**: `InMemoryRateLimiter:34`
- **Source path**: `services/gateway/src/main/java/com/processing/gateway/ratelimit/InMemoryRateLimiter.java`
- **Fix**: `InMemoryRateLimiter` → ref as `gateway/ratelimit/InMemoryRateLimiter`

### #6 No `requestId` field in ErrorResponse
- **review.md:116**: passes null for `serviceName`
- **Source** `TransactionValidationFilter:86-92`: passes `null, null` (serviceName + no `requestId` field)
- **Fix**: review only mentions `serviceName` null. ErrorResponse has no `requestId` field. OK — no change needed.

---

## Switch

### #7 Wrong package references
- **review.md:145**: `com.processing.switch.service.RouteService`  
  **Actual**: `com.processing.service.RouteService` (no `switch` in package)
- **review.md:166**: `client/MerchantAcquirerClient`  
  **Actual**: `service/MerchantAcquirerClient`
- **review.md:170**: `client/AuthorizationClient`  
  **Actual**: `service/AuthorizationClient`
- **Fix**: update package/path references

### #8 HealthResponse duplication overstated
- **review.md:149**: "duplicates `authorization/dto/HealthResponse.java` and `card-management/models/HealthResponse.java`"
- **Source**: Three HealthResponse records have same name but **different fields**:
  - switch: `status`, `service`, `version`, `dependencies`
  - authorization: `status`, `service`, `dependencies` (no version)
  - card-management: `status`, `service`, `cardsInDatabase` (no dependencies)
- **Fix**: Clarify — records share same name, overlap on `status`+`service`, but not identical copies

### #9 Review says reversal logs at ERROR "currently does" — actually does
- **review.md:170**: "should at least log at ERROR level (currently does)"
- **Source `AuthorizationClient:72`**: `LOG.error("Reversal failed...")` — already logs at ERROR
- **Fix**: rephrase to "currently does, which is good" — sentence is awkward, implies review author didn't realize ERROR already used

---

## Authorization

### #10 No inconsistencies in auth module. All claims verified. OK.

---

## Card Management

### #11 PAN unique constraint speculation wrong
- **review.md:234**: "PAN unique constraint: ??? ... If no unique constraint on PAN column, duplicate PANs possible"
- **Source `CardEntity.java`**: **Double protection** — `@Column(unique = true)` at line 30 + `@Index(name = "uk_cards_pan", columnList = "pan", unique = true)` at line 17
- **Fix**: remove "???" and speculation. PAN uniqueness guaranteed.

### #12 Soft-delete filtering claim wrong
- **review.md:238**: "deleteCard() sets status to DELETED. But `findByPan()` doesn't filter out DELETED cards."
- **Source `CardEntity.java:23`**: `@SQLRestriction("status <> 'DELETED'")` — Hibernate applies this to ALL queries including `findByPan()`. DELETED cards ARE filtered everywhere.
- **Fix**: delete this claim from review.md. Wrong.

### #13 Line numbers off — countCardsFiltered
- **review.md:226**: "CardService:85 (no args) and CardService:99 (5 args)"
- **Source**: `CardService.java` — no-arg at line 82, 5-arg starts at line 94
- **Fix**: lines 85,99 → 82,94

### #14 Line number off — faker name
- **review.md:230**: "CardGeneratorService:56"
- **Source `CardGeneratorService.java`**: `faker.name().fullName().toUpperCase()` at line 50, not 56. Line 56 is `CardDraft` constructor.
- **Fix**: line 56 → 50

### #15 Line number off — deleteCard
- **review.md:238**: "CardServiceImpl:127"
- **Source**: `deleteCard()` at lines 120-123
- **Fix**: line 127 → 120-123

### #16 saveAll partial-save concern invalid
- **review.md:236**: "if mapping fails mid-batch, partial save possible"
- **Source `JavaPersistenceAdapter:80-89`**: `.stream().map().toList()` runs to completion BEFORE `jpaRepository.saveAll()`. If mapping throws, `saveAll` never called. No partial save.
- **Fix**: remove partial-save concern

---

## Terminal Simulator

### #17 All claims verified. OK.

---

## Merchant Acquirer

### #18 Path inaccuracy
- **review.md** uses `merchantacquirer/TransactionSender`, `TransactionBuilder`, `AcquirerProvider`, `entity/AcquirerFee`, `StanGenerator`
- **Source** actual paths:
  - `service/TransactionSender.java`
  - `service/TransactionBuilder.java`
  - `service/AcquirerProvider.java`
  - `domain/entity/AcquirerFee.java`
  - `domain/StanGenerator.java`
- **Fix**: update paths

### #19 ddl-auto line number slight off
- **review.md:322**: "application.yaml:8,10-12"
- **Source**: ddl-auto at line 8. Flyway config at lines 10-12. OK.

---

## Transaction Logger

### #20 getStats query count off
- **review.md:342**: "runs 5 separate queries (count, countByStatus×2, sumAmount, averageProcessingTimeMs, countByCreatedAtAfter)"
- **Source `TransactionService:93-109`**: 6 queries — `count()`, `countByStatus×2`, `sumAmount()`, `averageProcessingTimeMs()`, `countByCreatedAtAfter()`
- **Fix**: 5 → 6

### #21 Line numbers off
- **review.md:330**: "TransactionRepository:14" — `sumAmount`  
  **Actual**: @Query at line 14, method `long sumAmount()` at line 15
- **review.md:332**: "TransactionRepository:16" — `averageProcessingTimeMs`  
  **Actual**: `long countByCreatedAtAfter` at line 16, `averageProcessingTimeMs()` at lines 17-18
- **review.md:340**: "Transaction:59" — `createdAt`  
  **Actual**: `private Instant createdAt` at line 61
- **review.md:358**: "TransactionFilter:12" — `@Pattern` regex  
  **Actual**: `@Pattern` at line 15, line 12 is `@ValidDateRange`
- **Fix**: correct line numbers

### #22 Path inaccuracy
- **review.md** uses `entity/Transaction`, `dto/TransactionFilter`
- **Source**: `model/Transaction.java`, `specification/TransactionFilter.java`
- **Fix**: update paths

---

## Dashboard

### #23 Test file count off
- **review.md:388**: "6 test files"
- **Source**: 5 test files found — `App.test.tsx`, `useWebSocket.test.ts`, `Filters.test.tsx`, `TransactionChart.test.tsx`, `format.test.ts`
- **Fix**: 6 → 5

### #24 Line numbers off in useWebSocket.ts
- **review.md:382**: ".slice(0, 20) at `useWebSocket.ts:62`"
  **Actual**: `.slice(0, 20)` at line 56
- **review.md:384**: "useEffect dependencies at `useWebSocket.ts:96`"
  **Actual**: `}, [url, maxRetries, retryDelayMs, maxRetryDelayMs])` at line 100
- **Fix**: 62 → 56, 96 → 100

---

## Cross-Cutting

### #25 maskPan line slight off
- **review.md:406**: "authorization/AuthService:391-397"
- **Source**: maskPAN method spans lines 391-398
- **Fix**: 391-397 → 391-398

### #26 HealthResponse count
- **review.md:404**: "HealthResponse duplicated in 5+ services"
- **Source**: 7 service-level files + 1 starter template = 8 total. Gateway uses different `HealthStatus` type for `status` field.
- **Fix**: "5+" → "7" for accuracy

---

## Summary

| Category | Count |
|----------|-------|
| Factual errors (wrong about code behavior) | 3: #2 DTO count, #11 PAN constraint, #12 soft-delete filtering |
| Line number offsets | 12: #3, #13, #14, #15, #20, #21 (4×), #24 (2×), #25 |
| Path/package inaccuracies | 5: #5, #7 (3×), #18, #22 |
| Minor overstatements | 2: #8 HealthResponse duplication, #26 HealthResponse count |
| Dead parameter omission | 1: #4 |
| Awkward phrasing | 1: #9 |
| Invalid concern | 1: #16 saveAll partial-save |

**Top 3 to fix:**
1. **#11 + #12** — CardManagement PAN constraint and soft-delete: review claims wrong, remove
2. **#2** — DTO count misleading (28 vs 17)
3. **#21** — Multiple line offsets in Transaction Logger section
