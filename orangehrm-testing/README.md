# OrangeHRM Enterprise Testing

> Real-world CRUD automation against a live enterprise HR management system

---

## What This Demonstrates

| Skill | Implementation |
|---|---|
| Page Object Model | 5 page classes (`LoginPage`, `DashboardPage`, `EmployeeListPage`, `AddEmployeePage`, `MyInfoPage`) with fluent method returns |
| CRUD test lifecycle | Create employee → verify in list → update → delete — full data lifecycle across 3 test classes |
| Dynamic test data | Timestamp-suffixed names (`TF1234567890`) isolate each run on the shared demo site without cleanup scripts |
| Toast notification handling | `WebDriverWait` with short timeout for `.oxd-toast--success` before falling back to URL redirect check |
| Modal confirmation handling | `EmployeeListPage.deleteFirstEmployee()` opens trash dialog, asserts both Cancel and Delete buttons, then confirms |
| Chrome options for enterprise apps | `--disable-save-password-bubble` + `prefs` map disables Chrome's credential-save popup during form interactions |
| `@BeforeMethod` chaining | Child `@BeforeMethod` (navigation, data setup) runs after parent's (driver + login) — TestNG inheritance guarantee |
| Failure screenshots | `BaseOrangeHRMTest.teardown(ITestResult)` captures screenshot on `ITestResult.FAILURE` before quitting driver |
| Allure annotations | `@Feature`, `@Story`, `@Severity` on all 7 test methods |

---

## Target Application

[OrangeHRM](https://www.orangehrm.com/) is an open-source HR management system used by thousands of companies worldwide. The public demo at `https://opensource-demo.orangehrmlive.com` runs the full production version with real data persistence — employees added by one test run are visible to others until deleted.

This is not a purpose-built test sandbox. It is a real application with:
- AJAX-driven navigation (no full page reloads between sections)
- Dynamic table IDs that change between sessions
- Toast notifications that appear and disappear in under 3 seconds
- A shared data environment where other automation runs may affect list counts

**Credentials:** `Admin` / `admin123` (public demo, no registration required)

---

## Test Scenarios

These three scenarios map directly to common QA interview challenges for enterprise application testing:

| Scenario | Class | Tests |
|---|---|---|
| Add new employee | `AddEmployeeTest` | Save succeeds + employee appears in search |
| Update personal info | `UpdateMyInfoTest` | Nickname update toast + fields are editable |
| Delete employee | `DeleteEmployeeTest` | Employee removed from list + cancel preserves it |

---

## Quick Start

```bash
cd orangehrm-testing

# Smoke — 2 tests, no browser left open between them
mvn test -Porangehrm-smoke

# Full suite — all 7 tests, parallel=classes (2 browsers)
mvn test -Porangehrm-full

# Headless (CI)
mvn test -Porangehrm-smoke -Dheadless=true

# View Allure report
allure serve target/allure-results

# View Extent HTML report (macOS)
open reports/OrangeHRM_*.html
```

---

## Test Coverage

| Class | Tests | What It Covers |
|---|---|---|
| `AddEmployeeTest` | 2 | PIM → Add Employee form, save success (toast/redirect), employee appears in search results |
| `UpdateMyInfoTest` | 2 | My Info → Personal Details, nickname field update, success toast, field editability |
| `DeleteEmployeeTest` | 3 | Employee list delete flow, confirmation modal Cancel/Delete buttons, post-delete search confirms removal |

**Total: 7 tests**

---

## Why This Is Different From Demo Sites

OrangeHRM is a real enterprise HR system used by companies worldwide. Testing it requires handling complex UI patterns that purpose-built test applications like The Internet do not have:

- **Dynamic tables** — employee count changes between test runs because the site is shared. Tests never assert an absolute count; they search by the unique name they just created.
- **Toast notifications** — `.oxd-toast--success` appears for approximately 2 seconds. Explicit waits with tight timeouts catch it before it disappears; the code falls back to URL redirect detection when the toast is missed.
- **Modal confirmations** — delete operations require two interactions: clicking the trash icon, then clicking the danger button in the overlay. The modal must be asserted before interacting.
- **AJAX navigation** — clicking PIM or My Info in the sidebar does not trigger a page load. `WaitUtils.waitForUrlContains()` detects the Angular router update instead of waiting for `document.readyState`.
- **Shared state** — other automation runs (CI pipelines, other developers, QA candidates) add and delete employees continuously. Timestamp names prevent false positives from leftover data.

---

## Key Challenges Solved

| Challenge | Solution |
|---|---|
| Chrome password-save popup blocks field interaction | `--disable-save-password-bubble` arg + `credentials_enable_service: false` pref in `ChromeOptions` |
| Toast disappears before assertion | `waitForElementVisible(driver, successToast, 5)` with fallback to `waitForUrlContains(driver, "/pim/viewPersonalDetails", 15)` |
| Shared demo site — stale employee data | Timestamp suffix on every created name; no assertions on absolute employee counts |
| `@BeforeMethod` ordering in inheritance | Parent `setup()` creates driver + logs in; child `@BeforeMethod` runs after, navigates to the right module |
| Slow AJAX navigation after login | `pageLoadTimeout(Duration.ofSeconds(30))` + URL-fragment waits instead of element presence |
| Delete confirmation modal | Explicit wait for both `.oxd-button--ghost` (Cancel) and `.oxd-button--label-danger` (Delete) before interacting |

---

## Run Commands

| Command | What Runs |
|---|---|
| `mvn test -Porangehrm-smoke` | 2-test smoke (Add + Update nickname) |
| `mvn test -Porangehrm-full` | All 7 tests, parallel=classes, thread-count=2 |
| `mvn test -Porangehrm-smoke -Dheadless=true` | Headless smoke (CI mode) |
| `mvn test -Porangehrm-full -Dheadless=true` | Headless full suite (CI mode) |
| `allure serve target/allure-results` | Interactive Allure report |
| `open reports/OrangeHRM_*.html` | Extent HTML report (macOS) |

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Selenium WebDriver | 4.39 | Browser automation |
| TestNG | 7.11 | Test framework, `@BeforeMethod` chaining |
| WebDriverManager | 6.3.3 | Automatic ChromeDriver management |
| ExtentReports | 5.1.2 | HTML test reports with failure screenshots |
| Allure TestNG | 2.32.0 | Interactive Allure reports |
| AspectJ Weaver | 1.9.24 | Allure bytecode instrumentation |
| SLF4J + Logback | 2.0.17 / 1.5.23 | Structured logging |
| Maven | 3.6+ | Build and dependency management |

---

## Interview Talking Points

1. **Why timestamp names instead of random strings?** — Timestamps are sortable and debuggable. When a test leaves a stale employee in the demo site, you can tell from the name when it was created. `Instant.now().getEpochSecond()` is short (10 digits) and collision-free across parallel runs at class level.

2. **Why is each test method given a fresh browser session?** — OrangeHRM's AJAX state can bleed between tests when the driver is reused. A stale XHR response or partially loaded module can cause a subsequent test to start in an unexpected state. Fresh sessions are slower but deterministic.

3. **Why does `isEmployeeSaved()` have a fallback?** — The success toast is the primary signal, but it disappears in ~2 seconds. On slow CI machines or network throttled environments, the WebDriverWait may miss it. The redirect to `/pim/viewPersonalDetails` is always present and is a reliable secondary signal.

4. **How does `@BeforeMethod` chaining work here?** — TestNG guarantees that a parent class's `@BeforeMethod` runs before a child class's. `BaseOrangeHRMTest.setup()` creates the driver and logs in; `AddEmployeeTest.navigateToPim()` runs after and can safely use `dashboardPage` because `setup()` has already initialized it.

5. **Why `parallel="classes"` and not `parallel="methods"`?** — Each test method in `DeleteEmployeeTest` creates its own employee in `@BeforeMethod`, so methods can run concurrently across classes. Running methods in parallel within a class would share the `createdFirstName` / `createdLastName` instance variables, causing race conditions.

6. **What would you change for a private OrangeHRM instance?** — Move `BASE_URL`, `ADMIN_USER`, and `ADMIN_PASS` to `test.properties` (or environment variables for CI). The `Configuration` singleton already reads from system properties with the highest priority, so `-Dbase.url=https://internal-hrm.company.com` would work without code changes.
