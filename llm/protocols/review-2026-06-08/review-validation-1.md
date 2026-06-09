# Review Validation

**Date:** 2026-06-08
**Source:** `llm/protocols/review-2026-06-08/review.md`
**Method:** Cross-check every statement against source code. Report inconsistencies only.

---

## Inconsistencies Found

### 1.1.5 — WRONG

**Claim:** `TransactionValidationException.java` — 2-space indent vs 4-space convention.
**Reality:** File uses 4-space indent. Consistent with project.

---

> **REVIEWER RESPONSE: Disagree.** Verified source: file uses 2-space indent (line 4 = 2-space class body, line 5 = 4-space method body). Project convention is 4-space. Review finding correct — kept unchanged.

---

### 3.3.2 — INACCURATE

**Claim:** `@Index(columnList = "card_id")` — column `card_id` doesn't exist. Should be `pan`.
**Reality:** Column name IS wrong. But finding understates severity — `card_id` is completely non-existent column on entity. Schema generation fails outright. Not "should be pan" — it's "phantom column, index broken".

---

> **REVIEWER RESPONSE: Partial agree.** Line number fixed (15, not 27). Severity claim overstates — review already says "column `card_id` doesn't exist. Should be `pan`. Schema gen fails." This communicates the severity clearly. No wording change needed beyond line number.

---

### 3.3.4 — INACCURATE

**Claim:** `expiryDate.isBefore(...)`, `amount > availableBalance` → NPE if card-management returns null fields.
**Reality:** `availableBalance` is primitive `long` in `CardResponse` — cannot be null. Only `expiryDate` (LocalDate) has real NPE risk. `request.getAmount()` is Integer — autoboxing NPE possible but that's request-side, not card response.

---

### 4.1.5 — INACCURATE

**Claim:** `@Transient` on `private static DateTimeFormatter` — annotation meaningless.
**Reality:** True — `@Transient` on static is redundant. But JPA ignores static fields anyway. Harmless noise, not a bug. Finding implies JPA-specific issue when it's just dead annotation.

---

> **REVIEWER RESPONSE: Disagree.** `@Transient` on static field is misleading to readers — implies JPA significance where none exists. Finding is valid (redundant annotation noise), just low severity. Kept in review with adjusted wording.

---

### 5.3.2 — WRONG (two errors)

**Claim:** `TerminalSimulatorService.java:34` — catches only `HttpClientErrorException` (4xx). 5xx → raw propagate.
**Reality:**
1. `TerminalSimulatorService.java:34` is `private int stanCounter = 1;`. No try-catch in this file.
2. Exception handling is in `GlobalExceptionHandler.java`. It has catch-all `Exception` handler at line 39 → returns 500. 5xx does NOT propagate raw.
3. Real issue: `ex.printStackTrace()` at line 41 — stdout instead of logger. Different finding.

---

### 6.3.3 — CORRECT (validator error)

**Claim:** Merchant-acquirer `SimulationService.java:83` — 0 cards → `get(-1)` → IndexOutOfBoundsException.
**Validator said:** INACCURATE — cited `TerminalSimulatorService.java:71` guard.
**Reality:** Validator checked wrong service. Merchant-acquirer has NO guard. Line 78: `iterableCard = cardsResponse.cards().size()`. If empty → 0. Line 83: `get(iterableCard - 1)` → `get(-1)` → IndexOutOfBoundsException. Confirmed by reading source. Finding is CORRECT.

---

### 7.3.1 — INACCURATE

**Claim:** `matches()` compares 18 fields incl timestamps.
**Reality:** Compares 20 fields. List: id, mti, stan, rrn, pan, processingCode, amount, currencyCode, terminalId, merchantId, mcc, acquirerId, issuerId, acquiringFee, status, declineReason, authCode, processingTimeMs, transmissionDateTime, createdAt.

---

### 7.3.2 — WRONG

**Claim:** WebSocket broadcast inside `@Transactional`. Rollback after broadcast → ghost tx.
**Reality:** `store()` method is NOT `@Transactional`. Broadcast at line 59 is in non-transactional method. `@Transactional(readOnly=true)` is on `getStats()` at line 85. Claim about broadcast-inside-transaction scope is incorrect.

---

### 8.1.5 — INACCURATE

**Claim:** `LineChart.tsx` — empty stub.
**Reality:** Returns `<div>LineChart Component</div>`. Placeholder component, not empty stub. Compare with `client.ts` which is truly `export {}`.

---

### 8.1.6 — INACCURATE

**Claim:** `PieChart.tsx` — empty stub.
**Reality:** Same as 8.1.5. Returns `<div>PieChart Component</div>`. Placeholder, not empty.

---

### 8.3.5 — INACCURATE

**Claim:** `App.test.tsx:31` — `getByText(/88\s*%/)`.
**Reality:** Line 30, not 31. Line 31 is `Одобрено` assertion.

---

### 9.5.1 — INACCURATE

**Claim:** `ErrorResponse.java:5` — `retryAfterMs` is String.
**Reality:** Line 8, not 5. Line 5 is `String message`.

---

## Summary

| # | Verdict | Module | Issue |
|---|---------|--------|-------|
| 1.1.5 | WRONG | Gateway | 4-space indent, not 2-space |
| 3.3.2 | INACCURATE | Auth | Understated severity — phantom column, not "wrong column" |
| 3.3.4 | INACCURATE | Auth | `availableBalance` is primitive `long`, cannot NPE |
| 4.1.5 | INACCURATE | Card Mgmt | Harmless dead annotation, not a bug |
| 5.3.2 | WRONG | Terminal | Wrong file:line + 5xx IS caught by catch-all handler |
| 6.3.3 | CORRECT | Merchant | Validator checked wrong service. Finding valid |
| 7.3.1 | INACCURATE | Logger | 20 fields, not 18 |
| 7.3.2 | WRONG | Logger | `store()` not `@Transactional` — broadcast not in tx scope |
| 8.1.5 | INACCURATE | Dashboard | Placeholder component, not empty stub |
| 8.1.6 | INACCURATE | Dashboard | Placeholder component, not empty stub |
| 8.3.5 | INACCURATE | Dashboard | Line 30, not 31 |
| 9.5.1 | INACCURATE | Common | Line 8, not 5 |

**12 inconsistencies found. 78 findings verified correct.**
