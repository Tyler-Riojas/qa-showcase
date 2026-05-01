# QA Automation Portfolio

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Tyler%20Riojas-blue?logo=linkedin)](https://linkedin.com/in/tyler-riojas)
[![GitHub](https://img.shields.io/badge/GitHub-Tyler--Riojas-black?logo=github)](https://github.com/Tyler-Riojas)

Eight projects covering API testing, multi-device UI automation, WCAG accessibility, performance metrics, Chrome extension testing, cross-device responsive layout, and enterprise HR application CRUD — 242 automated tests across Java + Selenium, RestAssured, JMeter, K6, and Postman.

---

## Portfolio at a Glance

| Project | Type | Tests | Stack |
|---|---|---|---|
| [API Testing](#api-testing) | REST API — collections + automation | 14 automated + 6 Postman | RestAssured, TestNG, Postman |
| [selenium-extension](#selenium-extension--ui-staging-app) | UI — staging app, 3 device profiles | 50 | Selenium 4, TestNG, CDP |
| [kibeam-ui-testing](#kibeam-ui-testing--live-public-site) | UI — live site, responsive + accessibility | 71 | Selenium 4, TestNG, Axe-core |
| [accessibility-testing](#accessibility-testing--wcag-suite) | WCAG 2.1 AA — generic, any URL | 14 | Selenium 4, Axe-core, TestNG |
| [performance-testing](#performance-testing--metrics--load) | UI timing, network capture, load testing | 41 | Selenium, BrowserMob, JMeter DSL, K6 |
| [chrome-extension-testing](#chrome-extension-testing) | Browser extension — manifest to content script | 20 | Selenium 4, Chrome for Testing, TestNG |
| [responsive-testing](#responsive-testing) | Cross-device responsive layout — any URL | 25 | Selenium 4, TestNG, CDP |
| [orangehrm-testing](#orangehrm-testing) | Enterprise HR application CRUD | 7 | Selenium, POM, TestNG |

---

## Live Test Reports

Allure reports are generated automatically on every CI run and published here:

🔗 **[View Live Allure Report](https://tyler-riojas.github.io/qa-showcase/)**

Updated on every push to main. Shows test results across all projects with trend history, feature grouping, and step-by-step execution detail.

---

## API Testing

Two complementary approaches to REST API testing against [Restful Booker](https://restful-booker.herokuapp.com): a versioned Postman collection for exploratory and manual verification, and a fully automated RestAssured suite for regression.

### What This Demonstrates

| Skill | Implementation |
|---|---|
| Full CRUD coverage | Create, read, update, partial update, delete — booking lifecycle end-to-end |
| Auth token lifecycle | Acquire token → inject into PATCH/DELETE headers → verify rejection without token |
| JSON schema validation | Every response validated against expected schema before field assertions |
| Negative path coverage | Invalid credentials, missing required fields, non-existent booking IDs |
| Allure reporting | `@Step`, `@Feature`, `@Severity` on all 14 tests |

### Run

```bash
# Postman — GUI
# Import postman/collections/ and postman/environments/ → Select "Restful Booker" env → Run

# Postman — CLI
cd postman
newman run "collections/My Collection.postman_collection.json" \
       -e environments/RestfulBooker.postman_environment.json

# RestAssured — automated
cd rest-assured
mvn test                          # All 14 tests
mvn test -Dtest=BookingCRUDTest   # Single class
allure serve target/allure-results
```

---

## selenium-extension — UI Staging App

57 automated tests against the [GPTZero staging app](https://staging-app.gptzero.me) across two test modules and three device profiles — all via Chrome DevTools Protocol emulation, no physical devices required.

### What This Demonstrates

| Skill | Implementation |
|---|---|
| Stripe iframe handling | `switchTo().frame()` into nested iframes; `switchTo().defaultContent()` teardown on every test |
| CDP device emulation | Accurate viewport + user-agent + touch events for Mobile (375×812), Tablet (768×1024), Desktop (1920×1080) |
| Alert handling | `alertIsPresent()` + `try-catch` on every alert interaction; never leaves alert open |
| Thread-safe reporting | `ThreadLocal<ExtentTest>` — parallel device runs write to the same report without race conditions |
| Zero sleep policy | Every wait uses `WebDriverWait` + `ExpectedConditions` — no `Thread.sleep()` anywhere |

### Test Coverage

| Module | Tests | Coverage |
|---|---|---|
| AuthFlowTest | 21 | Google auth UI, email signup validation, password visibility toggle, error messages, redirect flows |
| FTUEFlowTest | 12 | First-time user experience — onboarding modal, feature highlights, dismissal and re-open |
| ExtensionInstallTest | 6 | Extension icon presence, popup open/close, page action state |
| PaymentFlowTest | 8 | Stripe iframe load detection, form field interaction, window handle switching |
| SalePopupTest | 3 | Sale popup trigger, dismiss, and re-trigger behavior |

### Run

```bash
cd selenium-extension

mvn test -Ddevice=DESKTOP -Dtest="AuthFlowTest,PaymentFlowTest"  # All tests, desktop
mvn test -Pauth                                                   # Auth suite across all 3 devices
mvn test -Pmobile                                                 # Mobile only
mvn test -Ddevice=DESKTOP -Dtest="AuthFlowTest#testValidLogin"   # Single method

open reports/ExtentReport_*.html                                  # View report (macOS)
```

---

## kibeam-ui-testing — Live Public Site

77 automated tests against [kibeam.com](https://kibeam.com) — a live, publicly accessible e-commerce education platform. Covers navigation, responsive layout, contact information, educators page, and accessibility across three device profiles.

### What This Demonstrates

| Skill | Implementation |
|---|---|
| Real-site testing | Tests run against a live production URL — no mock server, no test environment |
| Responsive layout assertions | Horizontal overflow detection, touch target sizing (≥44px), element overlap checks, font size bounds |
| Axe-core integration | Programmatic WCAG 2.1 AA scans; violations reported with selector, impact level, and fix effort |
| Multi-device coverage | Desktop (1920×1080), Tablet (768×1024), Mobile (375×812) via CDP emulation |
| Parallel execution | All device profiles run concurrently via TestNG thread pool |

### Test Coverage

| Test Class | Tests | Coverage |
|---|---|---|
| KibeamNavigationTest | 8 | Header nav links, footer links, page-to-page routing, logo click |
| KibeamResponsiveTest | 11 | Overflow, touch targets, font sizes, image bounds per device |
| KibeamContactTest | 9 | Phone number, address, email visibility, FAQ, contact form presence |
| KibeamEducatorsTest | 8 | Hero section, video embed, CTA buttons, feature card layout |
| KibeamAccessibilityTest | 6 | Alt text, form labels, heading hierarchy, ARIA roles, keyboard focus |
| KibeamDeviceAccessibilityTest | 6 | Accessibility re-run across Mobile, Tablet, Desktop viewports |
| Axe page suites | 23 | Home, Educators, Contact, About × device profiles; axe-core + custom checks |

### Run

```bash
cd kibeam-ui-testing

mvn test                             # Default suite
mvn test -PtestngKibeamDevices       # All device profiles
mvn test -PtestngAccessibilityAll    # Accessibility on all pages and devices
mvn test -PtestngRegression          # Full regression suite
mvn test -Dheadless=true             # Headless (CI)
```

---

## accessibility-testing — WCAG Suite

14 automated tests combining Axe-core automated scanning with custom hand-written checks. Designed as a generic tool — runs against any publicly accessible URL with no site-specific configuration.

### What This Demonstrates

| Skill | Implementation |
|---|---|
| Axe-core (Deque) | Full WCAG 2.1 AA scan: color contrast, ARIA roles, form labels, landmarks, headings |
| Custom `AccessibilityChecker` | Image alt text, label-input association, heading hierarchy order, keyboard focus order |
| Device coverage | Each check runs across Mobile, Tablet, and Desktop viewports via CDP |
| Violation reporting | Each violation card includes: selector, WCAG criterion, impact level, fix effort, recommended fix |

### Run

```bash
cd accessibility-testing

mvn test -Paccessibility                                         # Default target (the-internet.herokuapp.com)
mvn test -Paccessibility -Dtarget.url=https://yoursite.com      # Any URL
mvn test -Paccessibility-smoke                                   # Fast CI check
mvn test -Paccessibility -Dheadless=true                        # Headless
```

---

## performance-testing — Metrics & Load

41 tests across four capability areas: browser-side UI timing metrics, network HAR capture with throttling, multi-device load measurement, and API load testing with both JMeter DSL and K6.

### What This Demonstrates

| Capability | Tool | What It Measures |
|---|---|---|
| UI timing metrics | Selenium + Navigation Timing Level 2 | FCP, LCP, TTFB, DOM interactive, full load — from JavaScript in the browser |
| Network capture | BrowserMob Proxy | HAR files with per-request timing; simulated throttling (3G, cable, fiber) |
| Device load comparison | Chrome DevTools Protocol | How load times change across Mobile, Tablet, and Desktop (11 profiles) |
| API load testing | JMeter DSL | 100 virtual users, configurable ramp-up, p95/p99 thresholds — from Java, no JMeter GUI |
| Script-based load testing | K6 | Threshold assertions, percentile targets, VU scaling |

### Run

```bash
cd performance-testing

mvn test -Pperformance-smoke          # UI + network tests (no setup required)
mvn test -Pperformance-devices        # Device comparison suite (11 profiles)
mvn test -Pperformance-all            # All suites (requires local Restful Booker for load tests)
```

---

## chrome-extension-testing

20 automated tests against a demo Chrome extension — covering manifest structure, extension load verification, popup UI interaction, and content script DOM injection. Uses **Chrome for Testing** to work around the `--load-extension` restriction present in Google Chrome stable.

### What This Demonstrates

| Skill | Implementation |
|---|---|
| Chrome for Testing | Auto-downloads the correct CfT binary at runtime — no manual setup, version-matched to ChromeDriver |
| Extension ID discovery | Reads `chrome://extensions-internals` to find the extension by path — works reliably across Chrome versions |
| Shared driver strategy | Single browser instance across the full suite — extension IDs are assigned at browser startup |
| Content script verification | Navigates to target pages and asserts injected DOM elements, text content, and styles |
| Allure reporting | `@Step`, `@Feature`, `@Severity`, `@Story` on all 20 tests |

### Test Coverage

| Test Class | Tests | Coverage |
|---|---|---|
| ManifestValidationTest | 5 | JSON structure, required fields (`name`, `version`, `manifest_version`), permissions format |
| ExtensionLoadTest | 4 | Extension registered in Chrome, ID is valid 32-char string, popup URL reachable |
| PopupUITest | 5 | Page title, status element text, action button visibility, button click → result update, all data-testid elements |
| ContentScriptTest | 6 | DOM injection confirmed, injected element text, CSS class applied, multiple page navigations |

### Run

```bash
cd chrome-extension-testing

mvn test                              # Full suite — all 20 tests
mvn test -Pextension-smoke            # Manifest + load only (no browser required for manifest)
mvn test -Pextension-popup            # Popup UI tests only
mvn test -Pextension-content          # Content script tests only

allure serve target/allure-results    # Interactive Allure report
```

---

## responsive-testing

25 tests validating cross-device responsive layout against any publicly accessible website. Configurable target URL, configurable page list, three device profiles run in parallel — no site-specific code required.

### What This Demonstrates

| Skill | Implementation |
|---|---|
| CDP device emulation | Chrome DevTools Protocol emulation for Mobile (iPhone 12), Tablet (iPad Pro 11), Desktop — accurate viewport, user-agent, and touch events |
| Configurable target | `System.getProperty("target.url")` with fallback default — swap sites with a single flag, no code changes |
| Horizontal overflow detection | `scrollWidth > clientWidth` via JavaScript — catches layout breakage invisible to visual inspection |
| Touch target validation | Iterates buttons and anchors, flags elements smaller than the 44px WCAG/Apple minimum |
| Image overflow detection | `imgX + imgWidth > viewportWidth` — the same check that found the kibeam.com production defect |
| Parallel device execution | TestNG `parallel="tests"` with `thread-count="3"` — all three device profiles run concurrently |
| ThreadLocal driver safety | `BaseTestTestNG` stores driver in `ThreadLocal<WebDriver>` — zero state leakage between threads |

### Run

```bash
cd responsive-testing

mvn test -Presponsive                                              # Default target (the-internet.herokuapp.com)
mvn test -Presponsive -Dtarget.url=https://yoursite.com           # Any site
mvn test -Presponsive -Dtarget.pages='/,/about,/contact'          # Specific pages
mvn test -Presponsive-mobile -Dtarget.url=https://yoursite.com    # Mobile only
mvn test -Presponsive-desktop                                     # Desktop only
mvn test -Presponsive -Dheadless=true                             # Headless (CI)
```

---

## orangehrm-testing

7 automated tests covering full CRUD operations against [OrangeHRM](https://opensource-demo.orangehrmlive.com) — a live enterprise HR management system used by real companies worldwide.

### 🏢 OrangeHRM Enterprise Testing

**What it tests:** Full CRUD operations against a live enterprise HR management system

**Real world highlight:** OrangeHRM is used by real companies worldwide. Tests handle Vue.js dynamic rendering, AJAX navigation, toast notifications, modal confirmations, and shared demo data — the same challenges found in real enterprise automation projects.

**Run it:**
```bash
cd orangehrm-testing
mvn test -Porangehrm-smoke
```

**Key concepts:**
- Page Object Model for complex multi-page workflows
- Vue.js locator strategies (href-based vs text-based)
- Dynamic URL handling after record creation
- Timestamp-based unique test data to avoid conflicts on shared demo

### What This Demonstrates

| Skill | Implementation |
|---|---|
| Enterprise app automation | Tests against OrangeHRM — a live HR system with real authentication, session management, and AJAX navigation |
| Vue.js locator strategies | href-based CSS selectors (`a[href*='/pim/viewPimModule']`) instead of brittle text-based XPaths that fail in headless Chrome |
| Dynamic URL handling | `/pim/viewMyDetails` redirects to `/pim/viewPersonalDetails/empNumber/N` — URL fragment matching handles this transparently |
| Toast notification handling | `waitForElementVisible` with short timeout on `.oxd-toast--success`; falls back to URL check |
| Modal confirmation flow | Delete → confirm modal → verify row removed — three-step interaction with explicit waits at each step |
| Shared demo isolation | Timestamp-suffixed names (`TF{epoch}`, `TL{epoch}`) prevent data conflicts on the shared public demo |

### Test Coverage

| Test Class | Tests | Coverage |
|---|---|---|
| AddEmployeeTest | 2 | Add employee via PIM, verify name appears in employee list |
| UpdateMyInfoTest | 2 | Update "Other Id" field in Personal Details, verify success toast, assert fields editable |
| DeleteEmployeeTest | 3 | Create employee, delete via trash icon + modal confirm, verify removed; cancel-delete flow |

### Run

```bash
cd orangehrm-testing

mvn test -Porangehrm-smoke    # Add + update tests (fastest, 2 tests)
mvn test -Porangehrm-full     # All 7 tests in parallel
mvn test -Dheadless=true      # Headless mode
```

---

## Real Findings

These are actual defects caught by running these tests against live sites — not contrived failures against a purpose-built test app.

### Image overflow on kibeam.com tablet viewport

**Found by:** `KibeamResponsiveTest.testImagesResponsive`
**Site:** [kibeam.com](https://kibeam.com) — live production

`Untitled_design_20.png` is positioned at x=690 on a tablet viewport (800px wide). At 200px wide, it extends to x=890 — **90px beyond the visible area**. On an actual iPad, users scroll horizontally to see the full image or the image is clipped entirely depending on `overflow` settings.

Root cause: image uploaded via CMS without `max-width: 100%` applied at the component level. The same image has no `alt` attribute, making it a WCAG 1.1.1 violation as well.

The test logs: `Image overflow: src=Untitled_design_20.png alt=(no alt) overflows by 90px (image: 200px wide at x=690, viewport: 800px)`.

---

## Tech Stack

| Tool | Version | Used In |
|---|---|---|
| Java | 17 | All Java projects |
| Selenium WebDriver | 4.39 | selenium-extension, kibeam-ui-testing, accessibility-testing, performance-testing, chrome-extension-testing, responsive-testing |
| TestNG | 7.11 | All Java projects |
| WebDriverManager | 6.3.3 | All Selenium projects |
| Chrome for Testing | auto-downloaded | chrome-extension-testing |
| RestAssured | 5.x | rest-assured |
| Axe-core (Selenium) | 4.x | kibeam-ui-testing, accessibility-testing |
| BrowserMob Proxy | 2.1.5 | performance-testing |
| JMeter DSL | latest | performance-testing |
| K6 | latest | performance-testing |
| Allure | 2.32 | rest-assured, performance-testing, chrome-extension-testing |
| ExtentReports | 5.x | selenium-extension, kibeam-ui-testing |
| Maven | 3.6+ | All Java projects |
| Postman / Newman | latest | postman |

---

## Quick Start

```bash
# Prerequisites: Java 17+, Maven 3.6+, Chrome

# API automation — no browser required
cd rest-assured && mvn test

# UI tests — Chrome opens automatically
cd selenium-extension && mvn test -Ddevice=DESKTOP -Dtest="AuthFlowTest,PaymentFlowTest"
cd kibeam-ui-testing  && mvn test

# Extension tests — Chrome for Testing is downloaded automatically on first run (~170MB, one time)
cd chrome-extension-testing && mvn test
```

---

## Contact

**Tyler Riojas**
[LinkedIn](https://linkedin.com/in/tyler-riojas) · [GitHub](https://github.com/Tyler-Riojas/qa-showcase)
