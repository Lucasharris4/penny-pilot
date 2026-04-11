# Backlog

## Epic: Open Source Release
Status: 🔄 In progress

### Story: SimpleFIN setup guide — Part 2
Finish the in-app half of the SimpleFIN setup walkthrough in `docs/simplefin-setup.md`. Part 1 (Steps 1–3: create a SimpleFIN Bridge account, authorize banks, generate a setup token) shipped with screenshots. Part 2 needs Step 4 (link accounts in Penny Pilot), Step 5 (initial sync and verification), a populated Troubleshooting section (including the "We are upgrading this connection" resolution time once the homelab bank connection finishes reconciling), and a Re-linking / rotating credentials section. **Blocked by the SimpleFIN 502 defect below** — linking flow has to work end-to-end before we can screenshot and document it.
- [ ] Complete

### Story: In-app guided SimpleFIN setup flow
Build an in-app guided setup flow on the Accounts page that walks non-technical users through the SimpleFIN setup step-by-step (complements the docs, doesn't replace them). Originally scoped alongside the walkthrough docs; split out so the docs can ship independently.
- [ ] Complete

### Story: README.md
Comprehensive open-source README: project description, features, quick start (docker-compose pull + run), configuration (env vars), development setup for contributors, contributing guidelines (branch/PR process, issue templates), architecture overview, and "buy me a coffee" link.
- [ ] Complete

### Defect: SimpleFIN account linking fails with 502 "Failed to parse SimpleFIN accounts response"
`POST /api/accounts/link` returns 502 when a real setup token is submitted on the homelab deployment. Surface symptom was SimpleFIN moving the Bridge from `bridge.simplefin.org` to `beta-bridge.simplefin.org`, but grooming revealed the real underlying issue: `SimpleFINProvider.claimSetupToken` was never implementing the SimpleFIN protocol correctly. The "just update the hardcoded URL" fix originally scoped here would have shipped a still-broken implementation.
- [x] Complete ✅

> **Dev notes** — grooming discovery:
> - Per the SimpleFIN spec (`simplefin.org/protocol.html`), a **setup token is a base64-encoded claim URL**. The client decodes the token, POSTs to the decoded URL with an empty body, and receives a plain-text access URL in response.
> - The previous implementation hardcoded `CLAIM_URL = "https://bridge.simplefin.org/simplefin/claim"` and POSTed the raw token as the request body. This only ever worked because the old Bridge happened to tolerate that non-standard shape; it broke the moment SimpleFIN moved the host because every user's token now decodes to a unique `https://beta-bridge.simplefin.org/simplefin/claim/<opaque-id>` URL, not a shared endpoint.
> - Protocol verified end-to-end against the live Bridge via curl before any code change: decode → POST with empty body returned 200 + access URL → `<access_url>/accounts` returned real account JSON.
> - Carved into three stories on one branch (`fix/simplefin-claim-decode`):
>   - **Story A**: Rewrite `claimSetupToken` to decode the token (delete `CLAIM_URL`, update existing test, add tests for malformed token and 403 "already used" case).
>   - **Story B**: Configure Spring `RestClient` with `HttpClient.Redirect.NORMAL` via a `RestClientCustomizer` bean. Defensive hardening — the decode approach makes the claim path redirect-resistant, but the JDK `HttpClient` default of `Redirect.NEVER` also silently affects the `fetchAccounts` / `fetchTransactions` GETs, so we harden the whole client.
>   - **Story C**: Log handled `ProviderConnectionException` / `ProviderAuthException` in `AccountController` — Spring doesn't log `@ExceptionHandler`-caught exceptions, which is why this defect required manual curl reproduction to diagnose. Future provider issues should land in `docker logs`.
> - **Not added:** the `MockWebServer` regression test for redirect handling that was in the original proposal. The decode fix makes a 302 on the claim path a protocol violation on SimpleFIN's side, and adding an HTTP-level test dependency for a hypothetical edge case isn't worth the cost.

### Story: Fix intermittent Docker Hub pulls on homelab (IPv6)
`docker compose pull` against Docker Hub fails non-deterministically on the homelab NAS with `dial tcp [2600:1f18:...]:443: connect: network is unreachable`. Root cause: the NAS has IPv6 addresses assigned to its interfaces but no working IPv6 route to the internet, so when Docker's Go resolver happens to pick an AAAA record first, the connection fails immediately. Retrying eventually lands on an IPv4 address and succeeds. Proper fix needs investigation and a decision between: (a) disabling IPv6 at the host via sysctl (`net.ipv6.conf.all.disable_ipv6=1`) — clean one-time change, reflects reality that IPv6 isn't working; (b) fixing IPv6 properly at the router/ISP level if upstream actually supports it; or (c) forcing Docker daemon to prefer IPv4 via `GODEBUG=netdns=cgo` or similar. Goal of this story is to understand what's going on, pick an option, and apply it so pulls are deterministic.
- [ ] Complete

---

## Done

## Epic: Publish to Homelab ✅
Status: Complete

### Story: Flyway migration setup
Convert existing schema from Hibernate auto-DDL to versioned Flyway migrations. Baseline the current schema as V1. Disable `ddl-auto` in production. Ensure fresh installs and existing databases both work correctly after the switch.
- [x] Complete

> **Dev notes:**
> - Flyway V1 baseline in `api/src/main/resources/db/migration/V1__baseline.sql` — creates all 7 tables + seeds SIMPLEFIN provider.
> - `ddl-auto` set to `none` (not `validate`) because SQLite's type system (`INTEGER` for PKs) doesn't match what Hibernate expects (`BIGINT`). Flyway owns all schema changes.
> - `baseline-on-migrate: true` with `baseline-version: 0` handles pre-Flyway databases — Flyway baselines at version 0, then applies V1.
> - V1 uses `INSERT OR IGNORE` for provider seeding — required because existing databases already have the row, and a plain `INSERT` hits a UNIQUE constraint.
> - MOCK provider seeded via `ProviderSeeder` (`@Profile("dev")` only), not via Flyway.
> - Flyway disabled in test profile — tests use `create-drop` with in-memory SQLite.

### Story: GitHub Actions CI/CD
Create a GitHub Actions workflow that builds multi-arch Docker images (amd64 + arm64) and pushes to Docker Hub on tagged releases. Publish a minimal `docker-compose.yml` that pulls from Docker Hub instead of building from source. Include semantic versioning on image tags plus a `latest` tag.
- [x] Complete

> **Dev notes:**
> - Workflow at `.github/workflows/release.yml` — triggers on `v*` tags.
> - Runs full test suite (API + frontend) before building images.
> - Uses `docker/build-push-action` with `docker/setup-qemu-action` for multi-arch (amd64 + arm64).
> - Images: `lharris4/penny-pilot-api` and `lharris4/penny-pilot-frontend` on Docker Hub.
> - Each tag produces versioned + `latest` tags (e.g., `1.0.0` + `latest`).
> - Requires `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` as GitHub repo secrets.
> - `docker-compose.prod.yml` pulls pre-built images, uses `prod` profile, requires `.env` with `JWT_SECRET` and `CREDENTIAL_ENCRYPTION_KEY`.
> - Release flow: `git tag v1.0.1 && git push --tags` — Actions handles the rest.

### Story: Run app in homelab environment
Deploy the published Docker image to the homelab. AC: app is running, registration works, login works, pages load correctly.
- [x] Complete

> **Dev notes:**
> - Deployed to `nas-prime-933` (Ubuntu) at `/mnt/raid1/docker/compose-files/penny-pilot/`.
> - Behind Caddy reverse proxy with `tls internal` at `https://pennypilot.local:8448`.
> - Caddy container joins `penny-pilot_penny-pilot-network` (external network) to reach the frontend container by name.
> - Port 8448 added to Caddy's ports and allowed in UFW (`192.168.50.0/24`).
> - DNS resolved via Pi-hole local DNS record: `pennypilot.local → server IP`.
> - **Gotcha: X-Forwarded-Proto.** Nginx in the frontend container was overwriting the header with `$scheme` (http) instead of passing through Caddy's header (`https`). Fixed by changing to `$http_x_forwarded_proto`. This was a v1.0.1 hotfix.
> - **Gotcha: Docker bypasses UFW.** Docker manipulates iptables directly, so published ports are accessible regardless of UFW rules. Not a security issue on LAN, but worth knowing.

**Update commands:**
```
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

---

## Epic: Polish ✅
Status: Complete

### Story: Frontend polish — loading skeletons, JWT expiry, error UX, password strength
Loading skeleton placeholders on Dashboard, Transactions, and Accounts pages. JWT 401 expiry detection redirects to login. Dismissible error banners on all pages. Password strength indicator on registration and change password forms.
- [x] Complete

### Story: Responsive layout
Collapsible sidebar with hamburger toggle on mobile. Dashboard cards and chart stack on narrow screens. Transaction table scrolls horizontally. Filter bar wraps naturally.
- [x] Complete

---

## Epic: Settings & Data Management ✅
Status: Complete

### Story: Category color picker
Add a visual color picker to category creation and editing flows. Users should be able to select a color visually rather than entering hex codes. No backend changes needed — the API already accepts/returns hex strings.
- [x] Complete

### Story: Production security hardening
Reject default secrets (JWT_SECRET, CREDENTIAL_ENCRYPTION_KEY) on startup when running with the `prod` profile — app should fail to start with a clear error message. Enforce HTTPS via `X-Forwarded-Proto` header check and HSTS response header in prod profile. Non-HTTPS requests return 403 with a helpful error. Dev profile remains unrestricted for local development.
- [x] Complete

### Story: Settings page
Frontend page with: change password form, SimpleFIN token management. Default category configuration deferred.
- [x] Complete

### Story: Extract shared JWT test configuration
Extract the duplicated `@TestConfiguration` / `JwtService` bean setup from all `@WebMvcTest` classes into a shared test utility. Every controller test copies the same boilerplate — single source of truth for test JWT config.
- [x] Complete

---

## Epic: Dashboard ✅
Status: Complete

### Story: Dashboard API ✅
- [x] Complete

### Story: Dashboard UI ✅
- [x] Complete

---

## Epic: Accounts & SimpleFIN Integration ✅
Status: Complete

### Story: TransactionProvider interface and MockProvider 
- [x] Complete ✅

### Story: Account CRUD API ✅
- [x] Complete

### Story: Sync endpoint and pipeline ✅
- [x] Complete

### Story: SimpleFIN provider implementation ✅
- [x] Complete

### Story: Sidebar Navigation ✅
- [x] Complete

### Story: Accounts UI ✅
- [x] Complete

### Story: Transactions Empty State ✅
- [x] Complete

---

## Epic: Tech Debt 
Status: Complete ✅
Priority: High

### Story: Add frontend test coverage tooling
Install `@vitest/coverage-v8` and configure Vitest to generate coverage reports. Establish a baseline coverage number for the frontend. Backend is at 91% — frontend currently has no coverage tooling.
- [x] Complete

### Story: move Clock and DefaultClock
Clock and Default clock are living in the config package currently. I want them to be moved to a package that better suites their purpose. Util seems like the best fit. We can make a decision during grooming what package would make the most sense. 
- [x] Complete

---

## Epic: Tech Debt ✅
Status: Complete

### Story: Use real JwtService in controller tests
Replace `@MockitoBean JwtService` with a real `JwtService` instance in all controller tests. Construct with test `AuthProperties`, `FixedClock`, and generate real JWT tokens instead of mocking `isValid()`/`parseToken()`. Removes fragile mock setup and aligns controller tests with the "mock only at the edges" philosophy — JwtService has no external dependencies, so there's nothing to mock.
- [x] Complete

> **Dev notes**:
> - Each `@WebMvcTest` class uses a static `@TestConfiguration` inner class to provide a real `JwtService` bean with test `AuthProperties` and `FixedClock(Instant.now())`.
> - `JwtAuthFilterTest` already used a real `JwtService` via `@SpringBootTest` — no changes needed there.
> - `FixedClock` must use `Instant.now()` (not a fixed past time) because JJWT's parser uses the system clock for expiration checks.

---

## Epic: Transactions ✅
Status: Complete

### Story: Transaction CRUD API
REST endpoints for transactions. `GET /api/transactions` supports filtering by date range, category, account, amount range, and text search on description/merchant. Supports pagination and sorting by date, amount, or category. `PUT /api/transactions/{id}` for full transaction editing.
- [x] Complete

> **Dev notes**:
> - **Stub `Account` entity** created (just `id` + `user_id`) for the FK — Accounts epic will flesh it out.
> - **No POST/DELETE endpoints.** Transactions are created exclusively by the sync pipeline. No manual creation or deletion for MVP.
> - **`transaction_type` column** uses `@Enumerated(EnumType.STRING)` — stored as TEXT "CREDIT"/"DEBIT" in SQLite, maps to Java enum `TransactionType`.
> - **`amount_cents` is unsigned** (always positive). Credit vs debit determined by `transaction_type`, not sign.
> - **`external_id` nullable** — allows non-SimpleFIN sources (mock provider, CSV import).
> - **`is_recurring` dropped** — deferred; overlap with category-based tracking.
> - **Filtering** uses JPA `Specification` for dynamic query building (date range, category, amount range, text search on description/merchant).
> - **Uncategorized transactions** show `categoryName: "Other"` in responses.
> - **API contract:**
>   - `GET /api/transactions?page=0&size=20&sort=date,desc&startDate=...&endDate=...&categoryId=...&minAmount=...&maxAmount=...&search=...` → 200, `Page<Transaction>`
>   - `PUT /api/transactions/{id}` → 200, `Transaction` — body: `{ categoryId?, amountCents, transactionType, description, merchantName?, date }`
>   - Response shape: `{ id, accountId, categoryId, categoryName, amountCents, transactionType, description, merchantName, date, externalId }`

### Story: Bulk categorization endpoint
`PUT /api/transactions/bulk-categorize` accepts a list of transaction IDs and a category ID. Assigns the category to all specified transactions in one operation.
- [x] Complete

> **Dev notes**:
> - **All-or-nothing validation.** All transaction IDs must belong to the user; if any are invalid, returns 400 with the invalid IDs and no updates are made.
> - **`@Transactional`** wraps the operation — DB failure rolls back all changes.
> - **API contract:** `PUT /api/transactions/bulk-categorize` — body: `{ transactionIds: Long[], categoryId: Long }` → 200 `{ updated: int }` or 400 `{ updated: 0, invalidIds: Long[] }`

### Story: Transactions list UI
Frontend page showing a paginated, filterable, sortable list of transactions. Inline category assignment (click a transaction's category to change it). Bulk select and categorize multiple transactions at once.
- [x] Complete

> **Dev notes**:
> - **Route:** `/transactions` — added to React Router in `App.tsx`.
> - **Filters:** text search (description/merchant), category dropdown, date range pickers. Sorting by date or amount (click column headers).
> - **Inline category edit:** click category badge → dropdown with existing categories + "New Category..." option.
> - **Bulk categorize:** checkbox selection → "Set Category" button → dialog with category picker.
> - **New category dialog:** accessible from both inline edit and bulk flows. Creates category on the fly, then applies it to the pending transaction(s).
> - **Empty state:** "No transactions found — Transactions will appear here after syncing an account."
> - **Shadcn components installed:** select, checkbox, dialog, badge, table.
> - **API client extended** (`api.ts`): `getTransactions`, `updateTransaction`, `bulkCategorize`, `getCategories`, `createCategory`.

---

## Epic: Categories ✅
Status: Complete

### Story: Category CRUD API
REST endpoints for managing budget categories (create, list, update, delete). Include icon and color. All operations scoped to the authenticated user.
- [x] Complete

> **Dev notes**:
> - **No subcategories.** `parent_category_id` dropped from the data model. Flat category list only — nesting adds UI and aggregation complexity for questionable value.
> - **No account scoping.** Categories are user-level, not account-level. A category applies across all of a user's accounts. User ID comes from JWT, not the URL path.
> - **Delete is a hard delete.** No soft-delete, no transaction reassignment. Transaction reassignment logic deferred to the Transactions epic (transactions with a deleted category will have a null `category_id`).
> - **404 on unauthorized access.** If a user tries to GET/PUT/DELETE a category that doesn't belong to them, return 404 (not 403) to avoid leaking existence.
> - **409 on duplicate name.** Reject creating a category with a name that already exists for the same user.
> - **API contract:**
>   - `GET /api/categories` → 200, `Category[]`
>   - `POST /api/categories` → 201, `Category` — body: `{ name, icon?, color? }`
>   - `PUT /api/categories/{id}` → 200, `Category` — body: `{ name, icon?, color? }`
>   - `DELETE /api/categories/{id}` → 204
>   - Response shape: `{ id, name, icon, color }`

### Story: Default category seeding
On first registration, seed a set of default categories for the new user. Default list and seeding behavior are configurable via `application.yml`.
- [x] Complete

> **Dev notes**:
> - **Configurable via `application.yml`.** `app.categories.seed-on-registration: true/false` controls whether seeding happens. The default category list (names, icons, colors) is also defined in config, not hardcoded.
> - **Triggered from `AuthService.register()`.** After user creation, calls `CategoryService.seedDefaults(userId)` if seeding is enabled.
> - **Default categories:** Groceries, Dining, Transportation, Entertainment, Subscriptions, Shopping, Bills, Income, Other. Each with a sensible icon and color.
> - **Expect this list to evolve.** The config-driven approach means updating defaults is a config change, not a code change. Changes only affect new registrations — existing users keep their categories.
> - **No default category rules are seeded.** Users build their own rules over time. Merchant map / AI auto-categorization are stretch goals.

### Story: Category rules API
CRUD endpoints for category rules. Each rule has a `match_pattern` (glob-style, case-insensitive) and a `category_id`. Rules have a `priority` field to resolve conflicts. Scoped to the authenticated user.
- [x] Complete

> **Dev notes**:
> - **Glob patterns, not regex.** `*` matches any characters. Case-insensitive. e.g., `STARBUCKS*` matches "Starbucks #1234 Seattle WA".
> - **Multiple rules can point to the same category.** e.g., `STARBUCKS*` → Coffee and `DUNKIN*` → Coffee. Each rule is its own row.
> - **Priority resolves conflicts between rules pointing to different categories.** Higher priority wins. If a transaction matches both `*FOOD*` → Groceries (priority 1) and `UBER EATS*` → Dining (priority 10), Dining wins.
> - **Pattern matching logic lives in the service layer** but is not exercised until the Transactions/Sync epic. This story delivers the CRUD and the matching utility with tests.
> - **UI for rules will abstract away glob syntax.** Users won't see asterisks — they'll pick "contains" / "starts with" / "exact match" from a dropdown, and the backend constructs the glob. This UI lives in the Transactions epic.
> - **No default rules seeded.** Manual categorization is the MVP experience. AI-assisted rule creation is a stretch goal.
> - **API contract:**
>   - `GET /api/category-rules` → 200, `CategoryRule[]`
>   - `POST /api/category-rules` → 201, `CategoryRule` — body: `{ matchPattern, categoryId, priority }`
>   - `PUT /api/category-rules/{id}` → 200, `CategoryRule` — body: `{ matchPattern, categoryId, priority }`
>   - `DELETE /api/category-rules/{id}` → 204
>   - Response shape: `{ id, matchPattern, categoryId, categoryName, priority }`

### ~~Story: Categories management UI~~
~~Frontend page listing categories with icon, color, and subcategories. Create, edit, and delete categories. Manage category rules (add/edit/remove pattern-to-category mappings).~~

> **Dev notes**: **Deferred.** No standalone categories management page for MVP. Category creation and assignment will be inline on the Transactions page (dropdown with "New..." option to create on the fly). This forces us to make the inline UX solid rather than building a separate page. Revisit if needed after Transactions epic.

---

## Epic: Project Scaffolding ✅
Status: Complete 

### Story: Initialize Spring Boot API project
Set up the API project under `api/` with Gradle, Java 21, Spring Boot 3.x. Include dependencies for Spring Web, Spring Data JPA, SQLite dialect, and Spring Security. Configure `application.yml` with SQLite datasource pointing to `./data/pennypilot.db`. Add a health check endpoint at `GET /api/health`.
- [x] Complete

> **Dev notes**: Use Spring Boot Actuator for health endpoint (standard `/actuator/health` path). SQLite dialect via `hibernate-community-dialects`. Spring Security filter chain permits all requests — Auth epic will lock it down. Include springdoc-openapi for Swagger UI. Controller test for the health endpoint.

### Story: Initialize React frontend project
Set up the frontend under `frontend/` with Vite, React 18, TypeScript. Install Shadcn/ui, Tailwind CSS, and Recharts. Add a placeholder landing page that confirms the app loads.
- [x] Complete

> **Dev notes**: Tailwind CSS v4. React 19 (current stable from Vite template). Shadcn/ui initialized but only install components as needed in future epics. Vite proxy `/api/*` to `http://localhost:8080`. Vitest configured with a smoke test.

### Story: Docker setup
Create Dockerfiles for both API and frontend. Create `docker-compose.yml` that builds and runs both services with a volume mount for `./data/`. Verify `docker-compose up --build` starts both services and the health check responds.
- [x] Complete

> **Dev notes**: API Dockerfile: multi-stage (Gradle build → JRE 21 slim). Frontend Dockerfile: multi-stage (npm build → nginx). Nginx serves static files and proxies `/api/*` to the api service. Docker healthcheck on API via /actuator/health. Frontend depends on API health.

---

## Epic: Auth ✅
Status: Complete

### Story: User registration endpoint
`POST /api/auth/register` accepts email and password. Passwords are hashed with bcrypt. Returns the created user (without password hash). Reject duplicate emails.
- [x] Complete

> **Dev notes**: Email replaces username as the login identifier — no separate username field. USERS table: id, email (unique), password_hash (bcrypt, one-way), created_at. Password min 8 chars, configurable via application.yml. Response: `201 { id, email, createdAt }`. Errors: 400 (validation), 409 (duplicate email). springdoc annotations on controller. Service + controller tests.

### Story: User login endpoint
`POST /api/auth/login` accepts email and password, validates credentials, returns a JWT. `POST /api/auth/logout` is a no-op 200 (client discards token).
- [x] Complete

> **Dev notes**: JWT signed with JWT_SECRET env var, 24h expiry, contains user ID and email in claims. Uses `jjwt` library. 401 on bad credentials — same message for wrong email and wrong password (prevent enumeration). Logout has no server-side invalidation for MVP.

### Story: JWT auth filter
Secure all `/api/*` endpoints except `/api/auth/**` and `/actuator/health`. Requests without a valid JWT receive 401. Every authenticated endpoint must scope queries to the logged-in user's ID.
- [x] Complete

> **Dev notes**: Custom `JwtAuthenticationFilter` (OncePerRequestFilter). Reads `Authorization: Bearer <token>`. Also permit `/swagger-ui/**` and `/v3/api-docs/**`. `SecurityUtils.getCurrentUserId()` utility for downstream code. Returns 401 (not 403) via HttpStatusEntryPoint.

### Story: Login and registration UI
Frontend pages for login and registration. After successful login, store JWT and redirect to dashboard. Show validation errors on bad input or failed login.
- [x] Complete

> **Dev notes**: Install `react-router-dom`. Routes: `/login`, `/register`, `/dashboard` (placeholder). JWT stored in localStorage. Auth context/provider redirects unauthenticated users to `/login`. API client module at `src/lib/api.ts`. Auto-login after registration.

---

---

## Stretch Goals (not part of MVP)

These are tracked here for future planning but will not be groomed or worked until the MVP is complete.

- Forgot password
- Logo
- Budgets (monthly limits per category, progress bars, copy month-to-month)
- Recurring transaction auto-generation (monthly bills/income)
- Savings goals with progress tracking
- Year-over-year spending comparison
- DB backup/restore via the UI
- Mortgage tracking
- Student loan accounts
- Retirement / 401k accounts
- Investment portfolio tracking
- OAuth / SSO support (Authelia, Keycloak)
- Plaid integration as alternative to SimpleFIN
- Mobile-responsive PWA
- AI-assisted transaction categorization (analyze transaction history, suggest category rules, auto-categorize on first syncs)
- Break scheduled sync (nightly job) into its own container/service — shares DB and sync logic but scales independently, no impact on user-facing API performance. Worker pattern for multi-user scale.
- Dark mode (light/dark theme toggle using Tailwind's dark variant, persist in localStorage)
- CSV/PDF export (server-side transaction export — browser Print works for quick snapshots)
- E2E test personas (5-20 mock users with sample data for release validation). Establish a dev-only Flyway migration location (`db/migration/dev/`) that runs only under the `dev` profile. Seed MOCK provider, test users, sample accounts/transactions here. This pattern also replaces the `@PostConstruct` MOCK provider seeding with a declarative, versioned approach.

