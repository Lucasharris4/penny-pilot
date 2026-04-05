# Penny Pilot

A self-hosted, privacy-first personal finance tracker. See `SPEC.md` for full product specification.

## Tech Stack

- **API**: Java 21, Spring Boot 3.x, Gradle
- **Frontend**: TypeScript, React 18, Vite, Shadcn/ui, Tailwind CSS, Recharts
- **Database**: SQLite (file-based, volume-mounted at `./data/pennypilot.db`)
- **Auth**: Username + password (bcrypt), JWT
- **Data Sync**: SimpleFIN (behind `TransactionProvider` interface)
- **Infra**: Docker, docker-compose

## Architecture

- API runs on port 8080, frontend on port 3000
- Frontend proxies API calls to `/api/*`
- All monetary values stored as **cents (integers)** — never floating point
- REST API follows standard CRUD patterns
- All controllers annotated with `springdoc-openapi` annotations (`@Operation`, `@ApiResponse`, `@Schema`) — Swagger UI auto-served at `/swagger-ui.html`
- `TransactionProvider` is an interface — `SimpleFINProvider` and `MockProvider` are implementations. Never couple sync logic directly to SimpleFIN.
- Multi-tenant: every query must be scoped to the authenticated user. User A must never see User B's data.

## Testing

### Philosophy
- **90%+ backend coverage.** Test behavior, not implementation.
- **Mock only at the edges.** DAOs and external providers (SimpleFIN) are mocked. Everything between the controller and the DB edge uses real implementations.
- **Private methods are never tested directly.** They are exercised through public methods.

### Backend (JUnit 5)

**Service layer tests** (the majority of tests):
- Plain JUnit — NO Spring context
- Instantiate services with `new`, inject mocked DAOs
- All downstream services, utilities, and helpers are **real implementations**
- Example: `TransactionService` test uses real `CategorizationService`, real `MerchantMapper`, mocked `TransactionRepository`

**Controller tests**:
- `@WebMvcTest` — Spring web layer only
- Services are stubbed (their logic is already tested in service tests — don't duplicate)
- Test: HTTP status codes, request validation, response JSON shape, auth enforcement

**What NOT to test:**
- That a service calls `repository.save()` — that's implementation
- Private methods directly
- Framework behavior (Spring, Hibernate)

### Frontend (Vitest)

- Test pure logic functions: date formatting, currency math, filter/sort, pattern matching
- Component smoke tests: key elements exist and are visible
- Do NOT test: visual appearance, text content, colors, complex interaction flows
- Manual QA is the primary frontend testing strategy

### MockProvider

- Implements `TransactionProvider` interface
- Returns configurable fake data for sandbox mode and testing
- Includes scenarios: normal spending, refunds, duplicates, zero-amount, missing merchant names

## Test Commands

```bash
cd api && ./gradlew test
cd frontend && npm test
```

## Sprint Workflow

1. **Groom**: Collaboratively define stories. Ask implementation questions. Define API endpoints for the epic (methods, paths, params, request/response shapes, status codes). Provide completion confidence (High/Medium/Low with reasoning). Capture decisions as dev notes on stories in `BACKLOG.md`. **Flag any external tool or dependency requirements** (CLI tools, new libraries, etc.) — these must be approved before execution begins since the agent cannot install tools autonomously.
2. **Execute**: Work through stories in order. After each story: run ALL tests, commit, push to feature branch. If context is running low, stop cleanly between stories.
3. **PR**: Create PR to `main` when the epic is complete. Stop and wait for review.
4. **Feedback**: Read PR comments via `gh`. Make fixes, push. Repeat until approved.
5. **Retro**: User provides retro prompt. Participate, then update this file with learnings.

## Backlog Conventions

- Epic statuses: `⬜ Not Started` / `🔨 In Progress` / `✅ Complete`
- Completed epics are moved to a `## Done` section in `BACKLOG.md`, above Stretch Goals but below active/upcoming epics

## Commit Rules

- One commit per completed story
- Run all tests (API + frontend) before each commit
- Descriptive commit messages
- Push to feature branch, never directly to `main`

## Database Migrations (Flyway)

- Schema changes require a new Flyway migration in `api/src/main/resources/db/migration/`
- Naming: `V{N}__{description}.sql` (e.g., `V2__add_budgets_table.sql`)
- **Never modify an existing migration file** — always create a new one
- Hibernate `ddl-auto` is set to `none` — Flyway owns all schema changes (SQLite's type system is incompatible with Hibernate's `validate` mode)
- Tests use `create-drop` with Flyway disabled — no migrations needed for tests
- The SIMPLEFIN provider is seeded in V1. The MOCK provider is seeded at startup only under the `dev` profile.

## Conventions

- Use existing patterns and utilities — don't create new abstractions for one-time operations
- Don't add features beyond what the current story asks for
- Don't add comments or docstrings to code you didn't change
- If a story is ambiguous and the spec doesn't clarify, ask — don't assume

## Retro Learnings

- **Confidence levels should reflect full session cost.** Factor in grooming discussion, review cycles, and back-and-forth — not just implementation complexity. A "High confidence, 1 story" estimate undersells a story that requires significant design discussion or multiple review rounds.
- **Build reusable test utilities when patterns emerge.** The authenticated MockMvc helper pattern (mock `jwtService.isValid()` + `jwtService.parseToken()` with a fake Bearer token) is duplicated across controller tests. Extract shared test setup when the same boilerplate appears in 2+ test classes.
- **JDK 21 is the project target (LTS).** Gradle 8.13 cannot parse JDK 25 version strings. If the system default Java differs, use `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` override.
- **Run `npm run build` (not just `tsc --noEmit`) before committing frontend.** The Docker build uses `tsc -b` which enforces `verbatimModuleSyntax` — type-only imports are required. `tsc --noEmit` doesn't catch this. Always run the full production build to match what Docker will do.
- **Never push directly to main.** Even small fixes get a branch and PR. The user reviews all changes before they hit main.
- **Only implement what was explicitly agreed to in grooming.** Don't bundle extra stories or endpoints that weren't discussed. If it's in the backlog but wasn't groomed for this sprint, it doesn't ship this sprint.
- **Avoid `cd` in Bash commands.** Use absolute paths everywhere to minimize permission prompts. The user wants to walk away during execution without babysitting approvals.
- **Prioritize frontend stories for visible progress.** Backend-only sprints produce invisible work. Bundle at least one frontend story per sprint so demos are tangible and direction can be validated.
