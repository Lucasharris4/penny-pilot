# Backlog

## Epic: Accounts & SimpleFIN Integration
Status: In progress 🔄

### Story: TransactionProvider interface and MockProvider 
Define the `TransactionProvider` interface with methods for fetching accounts and transactions. Implement `MockProvider` with preset fake data scenarios (normal spending, refunds, duplicates, zero-amount, missing merchant names). MockProvider should be selectable as a provider type when linking an account.
- [x] Complete ✅

> **Dev notes**:
> - **Provider DTOs live in `dto.provider`** — `ProviderAccount` and `ProviderTransaction` are records, not entities. Each provider maps its third-party model into these shared DTOs (adapter pattern). Services never see provider-specific data models.
> - **MockProvider** loads James Bond test data from `mock-provider-data.json` (2 accounts, 7 transactions). `@Component` for now — will need a factory or qualifier when SimpleFINProvider arrives to select by `ProviderType`.
> - **Account entity fleshed out** with `providerType`, `providerAccountId`, `accountName`, `balanceCents`, `lastSyncedAt`.
> - **Provider selection design decision (pending):** Provider type should be chosen at registration (SimpleFIN, Plaid, Manual CSV, Mock). Mock only available in controlled test environments (behind env flag like `ENABLE_MOCK_PROVIDER=true`). Provider can be changed later in Settings. Groom this when working on the Onboarding/Settings stories.

### Story: Account CRUD API ✅
REST endpoints for linked bank accounts. `POST /api/accounts/link` fetches accounts from the provider and persists them. `GET /api/accounts` lists accounts with balance and last sync time. `DELETE /api/accounts/{id}` unlinks an account and cascade-deletes its transactions.
- [x] Complete

> **Dev notes**:
> - **New `Provider` entity + lookup table.** Seeded on startup: MOCK, SIMPLEFIN, PLAID. `providers` table: `id`, `name`, `description`. `accounts.provider_id` FK replaces the `providerType` enum column on the Account entity. `ProviderType` enum still exists in code for programmatic reference, but DB relationship is normalized.
> - **`POST /api/accounts/link`** — body: `{ providerId }`. Resolves provider, calls `provider.fetchAccounts()`, creates one `Account` row per returned `ProviderAccount`. Returns `List<AccountResponse>` (201). Rejects if user already has linked accounts (one provider per user for MVP).
> - **`GET /api/accounts`** → 200, `List<AccountResponse>`. Response shape: `{ id, providerId, providerName, providerAccountId, accountName, balanceCents, lastSyncedAt }`.
> - **`DELETE /api/accounts/{id}`** → 204. Cascade-deletes all transactions for that account. 404 if not found or not owned by user.
> - **No `PUT`.** Account data comes from the provider — nothing user-editable.
> - **Provider resolution:** `providerId` → lookup `Provider` entity → map `provider.name` to `ProviderType` enum → switch to correct `TransactionProvider` Spring bean. Simple switch for now (only MOCK active).
> - **New classes:** `Provider` entity, `ProviderRepository`, `ProviderSeeder` (ApplicationRunner), `AccountRepository`, `AccountService`, `AccountController`, `LinkAccountsRequest` DTO, `AccountResponse` DTO, `AccountNotFoundException`.
> - **`AccountRepository`:** `findByUserId`, `findByIdAndUserId`, `existsByUserId` (for duplicate-link prevention).
> - **Tests:** `AccountServiceTest` (plain JUnit, mocked repos, real MockProvider), `AccountControllerTest` (`@WebMvcTest`, stubbed service).

### Story: Sync endpoint and pipeline ✅
`POST /api/accounts/{id}/sync` triggers a sync for a single account. Pulls transactions from the provider, deduplicates by `external_id` (updates if data changed), applies auto-categorization via user's category rules, and persists new/updated transactions. Also serves as the foundation for scheduled background sync.
- [x] Complete

> **Dev notes**:
> - **Endpoint:** `POST /api/accounts/{id}/sync` → 200, `SyncResponse { transactionsAdded, transactionsUpdated, transactionsSkipped, accountBalanceCents, syncedAt }`.
> - **Pipeline steps:**
>   1. Load `Account` by id + userId → 404 if not found/not owned
>   2. Resolve `TransactionProvider` via `ProviderResolver` and `ProviderCredentials` via `CredentialResolver` (null for Mock)
>   3. `provider.fetchTransactions(credentials, providerAccountId, sinceDate, LocalDate.now())` — sinceDate = `lastSyncedAt` or epoch if never synced
>   4. `provider.fetchAccounts(credentials)` → find matching account by `providerAccountId` → update `account.balanceCents` (SimpleFIN returns all accounts in one call anyway; for MockProvider filter client-side)
>   5. Batch-load existing `externalId`s for this account from DB (efficient Set lookup)
>   6. For each provider transaction:
>      - externalId exists + data matches → **skip**
>      - externalId exists + data differs → **update** existing transaction
>      - new → auto-categorize via `CategoryRuleService.findMatchingCategoryId(rules, merchantName)` (merchant name only, no description fallback), then **create**
>   7. Batch persist new/updated transactions
>   8. Update `account.lastSyncedAt`
>   9. Return `SyncResponse`
> - **No merchant name map.** Categorization uses only user's `CategoryRule`s (seeded defaults + user-created). Built-in merchant map deferred.
> - **No recurring transaction detection.** Users categorize as "Subscription"/"Recurring" manually. `is_recurring` field not needed.
> - **Dedup query:** `TransactionRepository.findByAccountIdAndExternalIdIn(Long accountId, Collection<String> externalIds)` — returns existing transactions for comparison. Batch lookup, not per-transaction.
> - **Sync triggers (implement in-app for now):**
>   1. **Manual:** `POST /api/accounts/{id}/sync` escape hatch for users who need fresh data
>   2. **On login:** trigger sync if `lastSyncedAt` > 1 hour stale (configurable)
>   3. **Scheduled:** `@Scheduled` cron job (e.g., daily 2 AM) syncs all users' accounts. Configurable via `application.yml`.
> - **Design for parallelism:** `syncAccount(accountId)` must be a self-contained, stateless unit of work so the scheduled job can process many users' accounts concurrently (thread pool) in the future.
> - **Error handling:** Provider failures (network, auth) return 502 with error message. Scheduled sync logs errors per-account and continues to next account.
> - **New classes:** `SyncService`, `SyncResponse` DTO. Sync endpoint can live on `AccountController` (nested resource pattern: `/api/accounts/{id}/sync`).
> - **Tests:** `SyncServiceTest` (plain JUnit, mocked repos + mocked provider, real `CategoryRuleService` + real `GlobMatcher`), controller test for sync endpoint.

### Story: SimpleFIN provider implementation ✅
Implement `SimpleFINProvider` against the SimpleFIN Bridge API. Exchange setup token for access URL. Fetch accounts and transactions from the access URL. Map SimpleFIN data to internal models.
- [x] Complete

> **Dev notes**:
> - **`ProviderCredentials` marker interface** — each provider defines its own credential shape. `SimpleFINCredentials(String accessUrl)` implements `ProviderCredentials`. MockProvider receives `null` (no credentials needed). Future providers (e.g., Plaid) define their own record implementing the interface. Open/Closed compliant — adding a provider never modifies existing credential classes.
> - **`TransactionProvider` interface change** — methods gain `ProviderCredentials credentials` param (nullable) and `fetchTransactions` gains `LocalDate until` param. MockProvider accepts and ignores both. Full updated interface:
>   ```
>   List<ProviderAccount> fetchAccounts(ProviderCredentials credentials);
>   List<ProviderTransaction> fetchTransactions(ProviderCredentials credentials, String accountId, LocalDate since, LocalDate until);
>   ```
> - **`SimpleFINProvider`** — `@Component`, implements `TransactionProvider`. Uses Spring `RestClient` for HTTP calls.
>   - `claimSetupToken(String setupToken)` → `POST https://bridge.simplefin.org/simplefin/claim` with setup token as body → returns access URL string. One-time call during account linking. Provider owns the API call, service layer owns persistence.
>   - `fetchAccounts(ProviderCredentials)` → casts to `SimpleFINCredentials`, calls `GET {accessUrl}/accounts` → maps to `List<ProviderAccount>`.
>   - `fetchTransactions(ProviderCredentials, accountId, since, until)` → `GET {accessUrl}/accounts?account={accountId}&start-date={since}&end-date={until}` (unix timestamps) → maps embedded transactions to `List<ProviderTransaction>`.
> - **Data mapping:**
>   - Amounts: decimal strings (e.g., `"-45.99"`) → multiply by 100, round to int (cents). Negative = DEBIT, positive = CREDIT.
>   - Dates: `posted` (unix timestamp) → `LocalDate`.
>   - Fields: `payee` → `merchantName`, `memo`/`description` → `description`, `id` → `externalId` (for dedup).
> - **`UserProviderCredential` entity** — `user_id` (FK), `provider_id` (FK), `credential` (String, AES-encrypted). One row per user-provider pair.
> - **AES encryption at rest** — access URL contains HTTP Basic Auth creds, first-class PI. Encrypt with key from `CREDENTIAL_ENCRYPTION_KEY` env var. Homelabbers set this in `docker-compose.yml` alongside `JWT_SECRET`. Cloud deployment would use secrets manager.
> - **`CredentialResolver`** — loads `UserProviderCredential` from DB, decrypts, deserializes into the correct `ProviderCredentials` subtype based on provider. Returns `null` for providers that don't need credentials (Mock).
> - **Account linking flow change:** `LinkAccountsRequest` grows: `{ providerId, setupToken? }`. For MOCK, `setupToken` is null/ignored. For SIMPLEFIN, it's required — `SimpleFINProvider.claimSetupToken()` is called, access URL stored via `CredentialResolver`/DAO, then `fetchAccounts` proceeds.
> - **Error handling:** `ProviderAuthException` (401/403 from SimpleFIN), `ProviderConnectionException` (network/timeout). Controller maps provider errors to 502.
> - **HTTP client:** Spring `RestClient` (synchronous, simple, fits our use case).
> - **Ripple effects:** Interface change touches `MockProvider`, `ProviderResolver`, `AccountService`, `SyncService` (Story 3), and all related tests. Coordinate with Sync story — if Sync is done first, this story updates the interface and fixes the callsites. If done together, build interface change into this story.
> - **Tests:** `SimpleFINProviderTest` — mock `RestClient` to return canned SimpleFIN JSON. Test data mapping (amounts→cents, timestamps→dates, sign→transaction type). Test `claimSetupToken` flow. Test error scenarios (auth failure, network error). Test `CredentialResolver` encryption round-trip.

### Story: Accounts UI
Frontend page listing linked accounts with balance and last sync time. Add account flow (select provider type, enter SimpleFIN setup token or select MockProvider). Manual sync trigger button. Remove account.
- [ ] Complete

### Story: Onboarding flow
First-login experience: prompt user to link a bank account and set up categories. Guide through SimpleFIN token entry or MockProvider selection. After linking, trigger initial sync.
- [ ] Complete

---

## Epic: Dashboard
Status: Not Started

### Story: Dashboard API endpoints
`GET /api/dashboard/summary` returns total income, total expenses, and net cash flow for a time range. `GET /api/dashboard/by-category` returns spending per category with amounts and percentages. `GET /api/dashboard/subscriptions` returns detected recurring charges with monthly total. `GET /api/dashboard/available-months` returns a list of months that have transaction data (used by the month selector).
- [ ] Complete

> **Note:** A `GET /api/transactions/summary` endpoint was previously implemented and removed during Sprint 2 (wasn't groomed). During grooming, evaluate whether the dashboard needs a dedicated summary endpoint or can compute summaries from existing transaction data on the frontend.

### Story: Dashboard UI — time range selection
Default view on login shows the most recent month that has transaction data (not the current calendar month). Month selector dropdown populated from available-months endpoint. Selecting a month with no data shows "No data for [Month Year]" message. Custom date range picker with presets (Quarter, Year). All dashboard components update when the range changes.
- [ ] Complete

### Story: Dashboard UI — data display
For the selected time range, show: income total, expenditures total, net cash flow (income minus expenditures). Category spending pie/donut chart showing percentage breakdown. Subscription tracker listing recurring charges with monthly total. Recent transactions preview (last 20, chronological) showing date, amount, category, and payee — with a link to the full transactions page.
- [ ] Complete

---

## Epic: Settings & Data Management
Status: Not Started

### Story: Settings page
Frontend page with: change password form, SimpleFIN token management, and default category configuration.
- [ ] Complete

### Story: CSV export
Export transactions as a downloadable CSV file. Filterable by the same parameters as the transactions list (date range, category, account).
- [ ] Complete

### Story: Dark mode
Add a light/dark theme toggle using Tailwind's dark variant. Persist the user's preference in localStorage.
- [ ] Complete

---

## Epic: Polish
Status: Not Started

### Story: Error handling and loading states
Add consistent error handling across all API calls in the frontend. Show user-friendly error messages. Add loading spinners/skeletons for async operations. Handle network failures gracefully.
- [ ] Complete

### Story: Input validation
Comprehensive input validation on both API (reject bad data with clear error messages) and frontend (inline form validation before submission). Cover: required fields, email format, password strength, amount formatting, date ranges.
- [ ] Complete

### Story: Responsive layout
Ensure all pages work at common screen widths. Sidebar collapses on smaller screens. Tables become scrollable or stack on narrow viewports.
- [ ] Complete

## Done

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
