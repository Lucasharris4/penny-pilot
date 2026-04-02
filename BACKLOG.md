# Backlog

## Epic: Project Scaffolding
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

## Epic: Auth
Status: Not Started

### Story: User registration endpoint
`POST /api/auth/register` accepts username, password, and optional email. Passwords are hashed with bcrypt. Returns the created user (without password hash). Reject duplicate usernames.
- [ ] Complete

### Story: User login endpoint
`POST /api/auth/login` accepts username and password, validates credentials, returns a JWT. `POST /api/auth/logout` invalidates the token.
- [ ] Complete

### Story: JWT auth filter
Secure all `/api/*` endpoints except `/api/auth/*` and `/api/health`. Requests without a valid JWT receive 401. Every authenticated endpoint must scope queries to the logged-in user's ID.
- [ ] Complete

### Story: Login and registration UI
Frontend pages for login and registration. After successful login, store JWT and redirect to dashboard. Show validation errors on bad input or failed login.
- [ ] Complete

---

## Epic: Categories
Status: Not Started

### Story: Category CRUD API
REST endpoints for managing budget categories (create, list, update, delete). Support one level of nesting via `parent_category_id`. Include icon, color, and `is_subscription` flag. All operations scoped to the authenticated user.
- [ ] Complete

### Story: Default category seeding
On first registration, seed a set of default categories for the new user: Groceries, Dining, Transportation, Entertainment, Subscriptions, Shopping, Bills, Income, Other.
- [ ] Complete

### Story: Category rules API
CRUD endpoints for category rules. Each rule has a `match_pattern` (glob-style) and a `category_id`. Rules have a `priority` field to resolve conflicts. Scoped to the authenticated user.
- [ ] Complete

### Story: Categories management UI
Frontend page listing categories with icon, color, and subcategories. Create, edit, and delete categories. Manage category rules (add/edit/remove pattern-to-category mappings).
- [ ] Complete

---

## Epic: Transactions
Status: Not Started

### Story: Transaction CRUD API
REST endpoints for transactions. `GET /api/transactions` supports filtering by date range, category, account, amount range, and text search on description/merchant. Supports pagination and sorting by date, amount, or category. `PUT /api/transactions/{id}` for updating (primarily category assignment).
- [ ] Complete

### Story: Bulk categorization endpoint
`PUT /api/transactions/bulk-categorize` accepts a list of transaction IDs and a category ID. Assigns the category to all specified transactions in one operation.
- [ ] Complete

### Story: Transaction summary endpoint
`GET /api/transactions/summary` returns aggregated spending by category for a given time range. Used by the dashboard and reports.
- [ ] Complete

### Story: Transactions list UI
Frontend page showing a paginated, filterable, sortable list of transactions. Inline category assignment (click a transaction's category to change it). Bulk select and categorize multiple transactions at once.
- [ ] Complete

---

## Epic: Accounts & SimpleFIN Integration
Status: Not Started

### Story: TransactionProvider interface and MockProvider
Define the `TransactionProvider` interface with methods for fetching accounts and transactions. Implement `MockProvider` with preset fake data scenarios (normal spending, refunds, duplicates, zero-amount, missing merchant names). MockProvider should be selectable as a provider type when linking an account.
- [ ] Complete

### Story: Account CRUD API
REST endpoints for linked bank accounts. `POST /api/accounts` to link a new account (with provider type and config). `GET /api/accounts` to list accounts with balance and last sync time. `DELETE /api/accounts/{id}` to unlink.
- [ ] Complete

### Story: Sync endpoint and pipeline
`POST /api/accounts/{id}/sync` triggers a sync. Pulls transactions from the provider, deduplicates by `external_id`, applies auto-categorization (merchant name map + user category rules), detects recurring transactions, and persists new transactions.
- [ ] Complete

### Story: SimpleFIN provider implementation
Implement `SimpleFINProvider` against the SimpleFIN Bridge API. Exchange setup token for access URL. Fetch accounts and transactions from the access URL. Map SimpleFIN data to internal models.
- [ ] Complete

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
