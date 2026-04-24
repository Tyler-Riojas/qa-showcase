# QA Testing Portfolio

A collection of automated tests demonstrating API testing, Performance Testing, Accessibility Testing and Chrome Extension testing.

---

## Quick Start

### Prerequisites

- **Java 17+** - [Download](https://adoptium.net/)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Chrome Browser** - Latest version

### Run API Tests (RestAssured)

```bash
cd rest-assured
mvn test
```

### Run Staging App UI Tests (Selenium)

```bash
cd selenium-extension

# Auth tests — desktop
mvn test -Ddevice=DESKTOP -Dtest=AuthFlowTest

# Auth + Payment tests — desktop
mvn test -Ddevice=DESKTOP -Dtest="AuthFlowTest,PaymentFlowTest"

# Auth tests across all devices (Desktop, Tablet, Mobile)
mvn test -Pauth
```

That's it! Reports will be generated automatically.

---

## Project Structure

```
qa-showcase/
├── postman/                    # Postman API collections
├── rest-assured/               # Java RestAssured API tests
├── selenium-extension/         # Chrome Extension tests (Selenium + TestNG)
├── kibeam-ui-testing/          # Real Selenium test suite against a live public site — navigation, responsive, accessibility, and device testing
├── Extensions/                 # Chrome extensions under test
│   └── gptzero-chrome-extension-takehome-2025-8-10/
└── docs/                       # Additional documentation
```

---

## Selenium UI Testing — GPTZero Staging App

### What's Being Tested

The [GPTZero staging app](https://staging-app.gptzero.me) — 29 automated tests across two modules:

| Module | Tests | Coverage |
|---|---|---|
| **AuthFlowTest** | 21 | Google auth UI, email signup validation, login flows, error messages |
| **PaymentFlowTest** | 8 | Stripe iframe loading, context switching, form fields, window handling |

> For the full test list and best practices detail, see [`selenium-extension/README.md`](selenium-extension/README.md).

### Test Commands

```bash
cd selenium-extension

# All staging tests — Desktop
mvn test -Ddevice=DESKTOP -Dtest="AuthFlowTest,PaymentFlowTest"

# Auth tests only
mvn test -Pauth

# Auth tests across Desktop + Tablet + Mobile (parallel)
mvn test -Pdesktop   # Desktop only
mvn test -Pmobile    # Mobile only (375x812, iPhone X)

# Single test method
mvn test -Ddevice=DESKTOP -Dtest="AuthFlowTest#testGoogleAuth_UIAppearsCorrectly"

# Multiple methods
mvn test -Ddevice=DESKTOP \
  -Dtest="AuthFlowTest#testEmailSignup_ValidEmailAccepted+testEmailSignup_PasswordVisibilityToggle"
```

### Device Profiles

Tests run on 3 screen sizes using Chrome DevTools Protocol emulation — no physical devices needed:

| Device | Resolution | Emulation |
|---|---|---|
| `DESKTOP` | 1920×1080 | Standard Chrome window |
| `TABLET` | 768×1024 | iPad portrait, touch events enabled |
| `MOBILE` | 375×812 | iPhone X, mobile user-agent |

### View Test Results

```bash
# macOS
open selenium-extension/reports/ExtentReport_*.html
```

Each report includes pass/fail charts, per-test device and category tags, failure screenshots embedded inline, and a full execution timeline.

---

## Kibeam.com UI Testing — Live Public Site

Real-world Selenium test suite against [kibeam.com](https://kibeam.com) — a publicly accessible e-commerce education platform.

### What's Tested

| Test Class | What It Tests |
|---|---|
| KibeamNavigationTest | Header nav, footer, page-to-page navigation |
| KibeamResponsiveTest | Horizontal overflow, touch targets, element overlap, font sizes, image bounds |
| KibeamContactTest | Phone number, address, email, FAQ, contact form visibility |
| KibeamEducatorsTest | Hero section, video embed, CTA buttons, feature sections |
| KibeamAccessibilityTest | Images alt text, form labels, heading structure, ARIA, keyboard |
| KibeamDeviceAccessibilityTest | Accessibility on mobile, tablet, desktop viewports |
| HomePageAccessibilityTest | Axe-core + custom checks on home page per device |
| EducatorsPageAccessibilityTest | Axe-core + custom checks on educators page per device |
| ContactPageAccessibilityTest | Axe-core + custom checks on contact page per device |
| AboutPageAccessibilityTest | Axe-core + custom checks on about page per device |
| AllPagesAccessibilityTest | All 4 pages × all 3 device types |

### Quick Run

```bash
cd kibeam-ui-testing

mvn test                                         # Default suite
mvn test -PtestngKibeamDevices                   # All 11 device profiles
mvn test -PtestngAccessibilityAll                # Accessibility on all devices
mvn test -PtestngRegression                      # Full regression suite
mvn test -Dheadless=true                         # Headless mode (CI)
```

For the full test list, device profiles, and run commands, see [`kibeam-ui-testing/README.md`](kibeam-ui-testing/README.md).

---

## API Testing

### Postman

1. Open Postman
2. Import collection from `postman/collections/`
3. Import environment from `postman/environments/`
4. Select "Restful Booker" environment
5. Right-click collection → Run collection

### RestAssured

```bash
cd rest-assured
mvn test
```

Tests the [Restful Booker API](https://restful-booker.herokuapp.com).

---

## Technologies Used

| Tool | Purpose |
|------|---------|
| **Selenium 4** | Browser automation |
| **TestNG** | Test framework with parallel execution |
| **ExtentReports 5** | HTML test reports with screenshots |
| **WebDriverManager** | Automatic ChromeDriver management |
| **RestAssured** | API testing |
| **Postman/Newman** | Manual + CLI API testing |
| **Maven** | Build and dependency management |

---

## Key Technical Features

- **Explicit waits throughout** — `WebDriverWait` + `ExpectedConditions`, zero `Thread.sleep()` calls
- **Graceful alert handling** — `alertIsPresent()` + `try-catch` + `switchTo().defaultContent()` on every alert interaction
- **Fresh Chrome session per test** — `--disable-extensions`, no user profile; no state leaks between tests
- **Automatic failure screenshots** — captured and embedded directly in the HTML report
- **Parallel execution across devices** — Desktop, Tablet, Mobile run concurrently via TestNG thread pool
- **Stripe iframe handling** — `switchTo().frame()` / `switchTo().defaultContent()` with nested iframe support
- **CDP device emulation** — accurate mobile viewports with touch events, no physical devices required
- **Thread-safe reporting** — `ThreadLocal<ExtentTest>` prevents race conditions in parallel runs
