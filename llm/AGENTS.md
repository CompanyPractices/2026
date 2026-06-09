# 1. Thesaurus

Agent Experience (AX) — file for LLM parsing. E.g., `AGENTS.md`, memory files in `llm/`.
Developer Experience (DX) — file for humans.

Module — folder with `src/`, usually Maven subproject.

# 2. Agent Boundaries

✅ ALWAYS:
- Ask instead of guess.
 - Mark uncertain parts of response with `???`.

⚠️ ASK FIRST:
- Before changing dependencies.
- Before decompiling jars (sources likely available).

🚫 NEVER:
- Remove failing tests.

# 3. Project

Educational. Intern-built. Simulates a wide-scale fintech app without overwhelming depth.

Goals: learn microservices in fintech, improve dev skills, practice multi-team workflow.

# 4. Stack

| Layer      | Tech                                           |
|------------|------------------------------------------------|
| Backend    | Java 21, Spring Boot 3.4.1, Maven              |
| Frontend   | TypeScript 5, React 18, Vite 8, Tailwind 3     |
| DB         | PostgreSQL 16                                  |
| Infra      | Docker, Docker Compose                         |
| CI/CD      | GitHub Actions                                 |
| Lint/Java  | Checkstyle (`services/checkstyle.xml`)         |
| Lint/TS    | ESLint v9                                      |
| API Spec   | OpenAPI 3.0 (`docs/api/openapi.yaml`)          |

# 5. Services

| Service            | Purpose                              |
|--------------------|--------------------------------------|
| Gateway            | Entry point, validation, rate-limit  |
| Card Management    | Card CRUD, Luhn PAN generation       |
| Switch             | BIN routing, orchestration           |
| Authorization      | Limit/balance checks, fund reserve   |
| Terminal Simulator | POS transaction stream generator     |
| Merchant Acquirer  | Merchant-scenario transaction gen    |
| Transaction Logger | Storage, search, WebSocket push      |
| Dashboard          | React SPA, real-time charts          |
| PostgreSQL         | Shared instance (3 services)         |

Sync HTTP REST between services. Shared DTOs/validation in `services/common/`.

# 6. Key Files

| File                          | Purpose                              |
|-------------------------------|--------------------------------------|
| `docs/api-spec.md`            | Full API contract                    |
| `docs/api/openapi.yaml`       | OpenAPI 3.0 spec                     |
| `docs/checklists.md`          | Self-acceptance checklists           |
| `docs/evaluation-criteria.md` | Grading rubric (100 pts + 15 bonus)  |
| `docs/architecture.md`        | Architecture diagrams, data models   |
| `scripts/smoke-test.sh`       | Acceptance test (7 checks)           |

# 7. Folders

| Folder     | Purpose                                                              |
|------------|----------------------------------------------------------------------|
| `docs`     | Dev-process docs. See checklists.md, evaluation-criteria.md.         |
| `tz`       | Requirements, one per service. 01-devops.md = DevOps tasks.          |
| `starters` | Skeleton code. IGNORE entirely. Not part of project source.          |
| `scripts`  | smoke-test.sh — main acceptance test.                                |
| `services` | Project source. Maven multi-module: Java + Dashboard.                |
| `llm`      | AX files — generated plans, notes, LLM context.                      |

7.1. Generated files (not source code) go in `llm/`.

7.2. Single-module: `llm/` at module level (sibling of `src/`). Multi-module/uncertain: root `llm/`.

7.3. Never move or delete files in `llm/`.

# 8. Plan Rules

9.1. Write in English, use caveman skill.

8.2. Proofread before showing user:
- Maximize clarity.
- Minimal words without losing detail. Target under 50 words per action.
