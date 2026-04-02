# Penny Pilot — Personal Finance Tracker

## Context

**Problem**: "I don't always know where my money is going. I want to look at one screen and be able to determine where my money is going with ease."

**Solution**: A self-hosted, privacy-first personal finance app (think Rocket Money but containerized for homelab users). Syncs with banks via SimpleFIN, auto-categorizes transactions, and shows spending breakdowns through an intuitive dashboard.

**Deployment model**: Jellyfin/Vaultwarden-style — Docker container accessed via browser. Multi-tenant by design (user registration, isolated data). Primary target: Linux homelab users.

---

## Tech Stack

| Layer     | Technology                              |
|-----------|----------------------------------------|
| Database  | SQLite (file-based, volume-mounted)    |
| API       | Java 21, Spring Boot 3.x, Gradle      |
| Frontend  | TypeScript, React 18, Vite             |
| UI        | Shadcn/ui + Tailwind CSS               |
| Charts    | Recharts                               |
| Auth      | Username + password (bcrypt, JWT)      |
| Data Sync | SimpleFIN (behind provider abstraction)|
| Infra     | Docker, docker-compose                 |
| Testing   | JUnit 5 (API), Vitest (frontend)      |

---

## Data Model

### USERS
| Column        | Type    | Notes                    |
|---------------|---------|--------------------------|
| id            | INTEGER | PK, autoincrement        |
| username      | TEXT    | unique, not null         |
| password_hash | TEXT    | bcrypt                   |
| email         | TEXT    | unique, nullable         |
| created_at    | TEXT    | ISO 8601 timestamp       |

### ACCOUNTS (linked bank accounts)
| Column          | Type    | Notes                                    |
|-----------------|---------|------------------------------------------|
| id              | INTEGER | PK                                       |
| user_id         | INTEGER | FK -> users                              |
| name            | TEXT    | e.g., "Chase Checking"                   |
| institution     | TEXT    | e.g., "Chase"                            |
| account_type    | TEXT    | checking, savings, credit                |
| provider_type   | TEXT    | simplefin, csv, manual                   |
| provider_config | TEXT    | JSON blob (tokens, connection details)   |
| balance_cents   | INTEGER | last known balance                       |
| last_synced_at  | TEXT    | ISO 8601 timestamp                       |

### CATEGORIES
| Column             | Type    | Notes                                  |
|--------------------|---------|----------------------------------------|
| id                 | INTEGER | PK                                     |
| user_id            | INTEGER | FK -> users                            |
| name               | TEXT    | e.g., "Coffee"                         |
| icon               | TEXT    | emoji or icon name                     |
| color              | TEXT    | hex color for charts                   |
| is_subscription    | BOOLEAN | flags subscription-type categories     |
| parent_category_id | INTEGER | nullable FK -> categories (for nesting)|

### TRANSACTIONS
| Column        | Type    | Notes                                     |
|---------------|---------|-------------------------------------------|
| id            | INTEGER | PK                                        |
| account_id    | INTEGER | FK -> accounts                            |
| category_id   | INTEGER | nullable FK -> categories (null = uncategorized) |
| amount_cents  | INTEGER | positive = income, negative = expense     |
| description   | TEXT    | transaction description                   |
| merchant_name | TEXT    | normalized merchant name                  |
| date          | TEXT    | ISO 8601 date                             |
| is_recurring  | BOOLEAN | detected or manually marked               |
| external_id   | TEXT    | from SimpleFIN, for deduplication         |

### CATEGORY_RULES
| Column        | Type    | Notes                                    |
|---------------|---------|------------------------------------------|
| id            | INTEGER | PK                                       |
| user_id       | INTEGER | FK -> users                              |
| match_pattern | TEXT    | glob pattern, e.g., "STARBUCKS*"         |
| category_id   | INTEGER | FK -> categories                         |
| priority      | INTEGER | higher priority rules win on conflicts   |

### BUDGETS
| Column       | Type    | Notes                     |
|--------------|---------|---------------------------|
| id           | INTEGER | PK                        |
| user_id      | INTEGER | FK -> users               |
| category_id  | INTEGER | FK -> categories          |
| amount_cents | INTEGER | monthly budget limit      |
| month        | TEXT    | "YYYY-MM" format          |

---

## Screens & User Flows

### 1. Registration & Login
- Registration: username, password, optional email
- Login: username + password -> JWT token
- JWT stored in httpOnly cookie or localStorage

### 2. Onboarding (first login)
- Prompt to link a bank account via SimpleFIN
- User enters their SimpleFIN setup token
- App connects, pulls accounts, user selects which to sync
- Prompt to create initial categories (or use defaults: Groceries, Dining, Transportation, Entertainment, Subscriptions, Shopping, Bills, Income, Other)

### 3. Dashboard (the "one screen")
Main view after login. Configurable time range (default: current month).

Components:
- **Spending by Category** — horizontal bar chart or pie/donut chart showing how much was spent per category
- **Income vs. Expenses** — summary cards showing total income, total expenses, net cash flow
- **Subscription Tracker** — list of recurring charges with monthly total (e.g., "Netflix $15.99, Spotify $9.99 — Total: $45.97/mo")
- **Recent Transactions** — last 10-20 transactions with category, amount, date
- **Budget Progress** — bars showing spent vs. budgeted per category (only for categories with budgets set)
- **Time range selector** — dropdown or date picker: This Month, Last 30 Days, Last 3 Months, Custom Range

### 4. Transactions Page
- Full transaction list with pagination
- Filters: date range, category, account, amount range, search by description/merchant
- Sort by: date, amount, category
- Inline category assignment (click to change category)
- Bulk categorization (select multiple, assign category)

### 5. Categories Page
- List of categories with icon, color, subcategories
- Create / edit / delete categories
- Category rules management: add patterns (e.g., "STARBUCKS*" -> Coffee)
- View spending per category with mini chart

### 6. Accounts Page
- List of linked bank accounts with last sync time and balance
- Add new account (SimpleFIN link flow)
- Manual sync trigger
- Remove account

### 7. Budgets Page
- Set monthly budget limits per category
- Copy previous month's budgets to current month
- Visual progress bars (green -> yellow -> red as spending approaches/exceeds limit)

### 8. Settings Page
- Change password
- SimpleFIN token management
- Default categories setup
- Export data (CSV)

---

## API Endpoints

### Auth
- `POST /api/auth/register` — create account
- `POST /api/auth/login` — get JWT
- `POST /api/auth/logout` — invalidate token

### Accounts
- `GET /api/accounts` — list user's linked accounts
- `POST /api/accounts` — link new account
- `POST /api/accounts/{id}/sync` — trigger sync
- `DELETE /api/accounts/{id}` — unlink account

### Transactions
- `GET /api/transactions` — list with filters (date range, category, account, search, pagination)
- `PUT /api/transactions/{id}` — update (mainly category assignment)
- `PUT /api/transactions/bulk-categorize` — assign category to multiple transactions
- `GET /api/transactions/summary` — aggregated spending by category for a time range

### Categories
- `GET /api/categories` — list user's categories (with subcategories nested)
- `POST /api/categories` — create category
- `PUT /api/categories/{id}` — update
- `DELETE /api/categories/{id}` — delete (reassign transactions to "Other")

### Category Rules
- `GET /api/category-rules` — list rules
- `POST /api/category-rules` — create rule
- `PUT /api/category-rules/{id}` — update
- `DELETE /api/category-rules/{id}` — delete

### Budgets
- `GET /api/budgets?month=YYYY-MM` — get budgets for a month
- `POST /api/budgets` — set budget
- `PUT /api/budgets/{id}` — update
- `POST /api/budgets/copy?from=YYYY-MM&to=YYYY-MM` — copy budgets

### Dashboard
- `GET /api/dashboard/summary?from=DATE&to=DATE` — income, expenses, net
- `GET /api/dashboard/by-category?from=DATE&to=DATE` — spending per category
- `GET /api/dashboard/subscriptions` — detected recurring charges
- `GET /api/dashboard/budget-progress?month=YYYY-MM` — spent vs budget per category

---

## SimpleFIN Integration

### Architecture
```
TransactionProvider (interface)
├── SimpleFINProvider (implements)
├── CSVProvider (future fallback)
└── PlaidProvider (future option)
```

### SimpleFIN Flow
1. User obtains a SimpleFIN setup token from simplefin.org
2. App exchanges setup token for an access URL (one-time operation)
3. App stores access URL in `accounts.provider_config`
4. On sync: app calls SimpleFIN access URL -> gets account list and transactions
5. App deduplicates via `external_id` and applies category rules to new transactions

### Auto-Categorization Pipeline (on sync)
1. Pull new transactions from SimpleFIN
2. For each uncategorized transaction:
   a. Check merchant name against built-in merchant map (e.g., "NETFLIX" -> Subscriptions)
   b. Check merchant name / description against user's category rules (pattern matching)
   c. If no match, leave as uncategorized
3. Detect recurring patterns (same merchant, similar amount, monthly interval) -> flag `is_recurring`

---

## Docker Setup

### docker-compose.yml structure
```yaml
services:
  api:
    build: ./api
    ports: ["8080:8080"]
    volumes: ["./data:/app/data"]
    environment:
      DB_PATH: /app/data/pennypilot.db
      JWT_SECRET: ${JWT_SECRET}

  frontend:
    build: ./frontend
    ports: ["3000:3000"]
    depends_on: [api]
```

### DB Portability
- SQLite file lives at `./data/pennypilot.db`
- If file exists on startup, Hibernate uses it; if not, creates with schema
- Users can back up by copying the `.db` file
- Works both containerized (volume mount) and bare metal (local path)

---

## Testing Strategy

### Philosophy
- **Target 90%+ code coverage on the backend.** Tests should catch regressions when behavior changes.
- **Test behavior, not implementation.** Tests should not care how a method achieves its result — only that the result is correct. If you refactor internals without changing behavior, zero tests should break.
- **Mock only at the edges.** The only things that get mocked are the boundaries of the application: DAOs (database access) and external providers (SimpleFIN). Everything between the controller and the DB is real.
- **Private methods are never tested directly.** They are exercised through the public methods that call them.
- **Frontend testing is light-touch.** Manual QA is the primary frontend testing strategy.

### Backend (JUnit 5)

#### Service Layer Tests (plain JUnit, NO Spring context)
- These are the majority of backend tests
- Services are instantiated directly with `new`, passing in mocked DAOs
- All downstream services, utilities, and helpers are **real implementations** — not mocks
- DAOs are mocked because we don't spin up the database for unit tests
- Example: `TransactionService` test uses a real `CategorizationService` and real `MerchantMapper`, but a mocked `TransactionRepository`

#### Controller Tests (`@WebMvcTest`, Spring web layer only)
- Controllers are thin — they wire endpoints to services and handle request/response mapping. There is minimal logic to test.
- Use `@WebMvcTest` to verify the wiring: correct HTTP methods/paths, request validation, response status codes, JSON shape, auth enforcement
- Services are stubbed to return known responses. Service behavior is NOT tested here — that's what service tests are for. Testing it again at the controller level would be redundant.
- Example: `POST /api/transactions` with missing fields returns 400, with valid data returns 201 with correct JSON shape

#### What to test:
- Every API endpoint: happy path + error cases
- Auto-categorization pipeline: exact match, pattern match, no match, conflicting rules (priority), merchant name normalization
- Transaction deduplication: duplicate external_id handling, same transaction from different syncs
- Budget calculations: spending vs. limit, cross-category, month boundary edge cases
- Auth: registration, login, invalid credentials, cross-user data isolation (user A cannot see user B's data)
- Sync pipeline: pull → deduplicate → categorize → persist (using MockProvider, mocked DAO)

#### What NOT to test:
- That a service calls `repository.save()` — testing implementation, not behavior
- Private method internals — exercised through public API
- Framework behavior (Spring, Hibernate) — trust the framework

### Frontend (Vitest)

#### What to test:
- **Pure logic functions**: date formatting, currency math (cents ↔ display), filter/sort logic, category rule pattern matching, time range calculations
- **Component existence**: key UI elements exist on the page and are visible (smoke tests)
- **API client functions**: correct request shape, error handling

#### What NOT to test:
- Visual appearance, styling, colors, text content
- Complex interaction flows (click this → that opens → fill form → submit)
- Anything that is better validated by manually using the app
- Tests should never break because of a visual/layout change

### TransactionProvider Mock (for sandbox & E2E)

The `MockProvider` implements the same `TransactionProvider` interface as `SimpleFINProvider`:
- Ships with the app as a selectable provider type
- Returns configurable fake transaction data from JSON/in-memory
- Includes preset scenarios: normal spending, refunds, duplicates, zero-amount entries, large transactions, missing merchant names
- Enables running the full app in sandbox mode (real DB, real API, real frontend, fake bank data)
- Used in the sync pipeline tests and for local development/demos

---

## Development Workflow

### Sprint Lifecycle

Each sprint targets ~1 hour of agent work. An epic can span multiple sprints if needed.

#### 1. Grooming
- Stories for the epic are defined collaboratively (user + agent in conversation)
- Agent asks implementation questions and flags risks or ambiguities
- **API contract**: For any epic that introduces or modifies API endpoints, define the endpoints during grooming — HTTP methods, paths, query/path params, request bodies, response shapes, and status codes. The agent implements controllers with `springdoc-openapi` annotations (`@Operation`, `@ApiResponse`, `@Schema`) so the OpenAPI spec and Swagger UI are auto-generated from code. Swagger UI is served at `/swagger-ui.html` for browsing and testing.
- Any decisions that deviate from or aren't covered by this spec are captured as **dev notes** on the story in `BACKLOG.md`
- **Confidence level**: Agent provides a High/Medium/Low rating on whether it can complete the epic in a single sprint (~1hr), with reasoning. If Medium or Low, the agent should explain what's driving the risk (story complexity, unknowns, number of stories, etc.) so the epic can be scoped down if needed.

#### 2. Execute
- Agent works through stories in order within the epic
- After each story: run all tests, commit, push to feature branch
- If context is running low, stop cleanly between stories (do not push through and produce sloppy work)
- Resume in the next sprint session — `BACKLOG.md` dev notes and commit history bridge the gap

#### 3. Pull Request
- Once all stories in the epic are complete, create a PR to `main`
- Agent stops and waits for review

#### 4. Review & Feedback Loop
- User reviews the PR and leaves comments on GitHub
- Agent reads PR comments via `gh pr view` and `gh api`, makes fixes, pushes
- Repeat until the PR is approved and merged

#### 5. Retro
- User provides the retro prompt (format varies intentionally by sprint)
- Agent participates in the retro conversation
- Learnings are encoded into `CLAUDE.md` so future sprints benefit

### Story Format

Stories are minimal. The spec and `CLAUDE.md` provide the detail — stories don't duplicate it.

```markdown
### Story: Category CRUD API
Create REST endpoints for managing budget categories (create, list, update, delete).
Support one level of nesting. Seed defaults on user registration.
```

If grooming produces implementation decisions not covered by the spec, they're captured as dev notes:

```markdown
### Story: Category CRUD API
Create REST endpoints for managing budget categories (create, list, update, delete).
Support one level of nesting. Seed defaults on user registration.

> **Dev notes**: Deletion is soft-delete (archived flag), not hard-delete.
> Reassign transactions to "Uncategorized" on archive.
```

### Context Handoff Between Sessions

When a sprint spans multiple sessions, the new agent session picks up context from:
1. **`CLAUDE.md`** — project rules, conventions, and learnings from past retros
2. **`BACKLOG.md`** — which stories are done, which are next, dev notes from grooming
3. **Git history** — `git log` and the feature branch show what's been built
4. **PR comments** — if in the review phase, `gh` CLI surfaces feedback to address

### Backlog Structure (`BACKLOG.md`)

```markdown
# Backlog

## Epic: Auth & Data Foundation
Status: In Progress

### Story: User Registration & Login (JWT)
Implement registration and login endpoints with bcrypt password hashing and JWT tokens.
- [x] Complete

### Story: Category CRUD API
Create REST endpoints for managing budget categories. Support one level of nesting.
Seed defaults on user registration.
- [ ] Complete

> **Dev notes**: Soft-delete categories instead of hard-delete.

### Story: Transaction CRUD API
REST endpoints for transactions. List endpoint supports filtering by date range,
category, account, and search. Pagination required.
- [ ] Complete

---

## Epic: SimpleFIN Integration
Status: Not Started

### Story: TransactionProvider Interface
...
```

### CLAUDE.md Role

`CLAUDE.md` is the agent's persistent instruction set. It contains:
- Tech stack and conventions
- Test commands (`cd api && ./gradlew test`, `cd frontend && npm test`)
- Testing philosophy (mock only at edges, test behavior not implementation)
- Architecture notes (ports, API prefix, money-as-cents, etc.)
- Sprint workflow rules (commit per story, push to feature branch, stop between stories if context is low)
- Learnings from past retros (updated after each retro)

`CLAUDE.md` grows over time as retros surface new patterns and corrections.

---

## Stretch Goals (NOT part of MVP)

These are future features to consider after the MVP is complete:
- Mortgage tracking
- Student loan accounts
- Retirement / 401k accounts
- Investment portfolio tracking
- OAuth / SSO support (Authelia, Keycloak integration)
- Plaid integration as alternative to SimpleFIN
- Mobile-responsive PWA
