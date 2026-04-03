# Kibeam.com UI Test Automation

A professional Selenium WebDriver test suite against a real live public website demonstrating **Page Object Model**, **device emulation**, **accessibility testing**, **responsive validation**, and **rich Extent Reports**.

---

## What This Demonstrates

| Skill | Implementation |
|-------|----------------|
| **Page Object Model** | `BasePage` + 4 page classes with fluent navigation API |
| **Device Emulation** | 14 Chrome DevTools Protocol profiles (mobile / tablet / desktop) |
| **Responsive Testing** | Viewport overflow, touch targets, element overlap, image bounds, font size |
| **Accessibility Testing** | Custom `AccessibilityChecker` + Axe-core WCAG scanning across pages and device profiles |
| **Soft Assertions** | `SoftAssert` pattern — all checks run even when one fails |
| **Real Defect Found** | Image overflow bug identified on live site (`Untitled_design_20.png`, 90px overflow on tablet) |
| **Parallel Execution** | TestNG thread pools with `ThreadLocal` WebDriver |
| **Retry Logic** | 2-retry `AnnotationTransformer` applied automatically to all tests |
| **Rich Reporting** | Extent Reports 5 with embedded screenshots on failure |
| **Configuration** | 4-tier property resolution — system props → env vars → env file → defaults |

---

## Quick Start

**Prerequisites:** Java 17+, Maven 3.6+, Chrome (latest)

```bash
cd kibeam-ui-testing
mvn test                           # All tests
mvn test -Dheadless=true           # Headless mode (faster)
open reports/*.html                # View report
```

---

## Project Structure

```
kibeam-ui-testing/
├── src/
│   ├── main/java/
│   │   ├── base/
│   │   │   ├── BaseTestTestNG.java          # ThreadLocal WebDriver lifecycle
│   │   │   ├── BaseAccessibilityTest.java   # Auto a11y scan after each test
│   │   │   └── BasePage.java               # Shared page utilities + explicit waits
│   │   ├── config/
│   │   │   ├── Configuration.java          # 4-tier property resolution singleton
│   │   │   └── FeatureToggle.java          # Runtime feature flags (retry, headless)
│   │   ├── core/
│   │   │   └── DriverFactory.java          # Browser creation + WebDriverManager
│   │   ├── enums/
│   │   │   └── BrowserType.java            # Browser enum with factory support
│   │   ├── listeners/
│   │   │   ├── ExtentTestNGListener.java   # Suite/test lifecycle → Extent Report
│   │   │   ├── AnnotationTransformer.java  # Auto-applies RetryAnalyzer to all tests
│   │   │   ├── RetryAnalyzer.java          # 2-retry logic on failure
│   │   │   └── TestListenerTestNG.java     # Console logging listener
│   │   ├── pages/
│   │   │   ├── KibeamHomePage.java         # Home + main navigation page object
│   │   │   ├── KibeamEducatorsPage.java    # Educators page object
│   │   │   ├── KibeamContactPage.java      # Contact page object
│   │   │   └── KibeamCollectionsPage.java  # Collections page object
│   │   └── utils/
│   │       ├── AccessibilityChecker.java   # Custom WCAG checker (no external deps)
│   │       ├── AccessibilityUtils.java     # Axe-core runner + violation parser
│   │       ├── AccessibilityReporter.java  # A11y results → Extent Report
│   │       ├── DeviceEmulation.java        # 14-device CDP emulation profiles
│   │       ├── ExtentReportManager.java    # Singleton report manager (ThreadLocal)
│   │       ├── KibeamUrls.java             # Centralised URL constants
│   │       ├── ScreenshotUtils.java        # Base64 + file screenshot capture
│   │       ├── WaitUtils.java              # Explicit wait library (12 methods)
│   │       └── ConfigReader.java           # Properties file reader
│   └── test/
│       ├── java/TestNG/tests/
│       │   ├── KibeamNavigationTest.java           # Header nav, footer, page routing
│       │   ├── KibeamResponsiveTest.java           # Overflow, touch targets, overlap
│       │   ├── KibeamContactTest.java              # Contact page content validation
│       │   ├── KibeamEducatorsTest.java            # Educators page content validation
│       │   ├── KibeamAccessibilityTest.java        # WCAG compliance per page
│       │   └── KibeamDeviceAccessibilityTest.java  # A11y across device matrix
│       └── resources/
│           ├── testngKibeam.xml                # Default suite (Chrome, all classes)
│           ├── testngKibeamDevices.xml         # Device emulation suite
│           ├── testngRegression.xml            # Full regression suite
│           ├── testngAccessibilityAll.xml      # All accessibility tests
│           ├── testngAccessibilityDesktop.xml  # Desktop accessibility
│           ├── testngAccessibilityMobile.xml   # Mobile accessibility
│           ├── testngAccessibilityTablet.xml   # Tablet accessibility
│           └── config/
│               └── test.properties            # Default configuration
├── reports/                                   # Generated HTML reports (gitignored)
└── pom.xml
```

---

## Test Coverage

### KibeamNavigationTest.java

Core navigation flows — verifies the site routes correctly between pages and critical UI elements are present.

| Test Method | Description | What It Validates |
|-------------|-------------|-------------------|
| `testHomePageLoads` | Verify home page loads successfully | Nav visible, page title not null/empty |
| `testHeaderNavigationVisible` | Verify header navigation is visible | Header/nav element is displayed |
| `testEducatorsNavigation` | Verify Educators link navigates to Educators page | Page object returned, URL contains `educators` |
| `testSupportNavigation` | Verify Support/Contact link navigates to Contact page | Page object returned, URL contains `contact` |
| `testFooterVisible` | Verify footer is visible on home page | Footer element is displayed |
| `testContactSalesButtonPresent` | Verify Contact Sales button is present | TidyCal/Calendly link is present (informational) |
| `testNavigationMethodChaining` | Verify navigation returns correct page objects | Fluent API chains correctly, back-navigation works |
| `testEducatorsLinkPresent` | Verify Educators link is present in navigation | Educators anchor is in the nav |

---

### KibeamResponsiveTest.java

Responsive design and visual integrity tests — catches layout regressions across viewport sizes.

| Test Method | Description | What It Validates |
|-------------|-------------|-------------------|
| `testNoHorizontalScrollHomePage` | Verify no horizontal scrollbar on home page | `scrollWidth <= clientWidth` via JS |
| `testNoHorizontalScrollEducatorsPage` | Verify no horizontal scrollbar on educators page | Same overflow check on educators URL |
| `testNoHorizontalScrollContactPage` | Verify no horizontal scrollbar on contact page | Same overflow check on contact URL |
| `testNavigationLinksVisible` | Verify all navigation links are visible | Nav element visible at current viewport |
| `testFooterWithinViewport` | Verify footer is visible and within viewport width | Footer width ≤ viewport width |
| `testHeaderNoOverlap` | Verify header doesn't overlap page content | Content top ≥ header bottom (−10px tolerance) |
| `testNoElementOverlapHomePage` | Verify no critical element overlaps on home page | Section-to-section overlap < 50px |
| `testImagesResponsive` | Verify images don't overflow container | `imgX + imgWidth ≤ viewportWidth + 5px` per image |
| `testLogViewportInfo` | Log viewport and device information | Informational — always passes |

> **🔍 Real Defect Found**
>
> `testImagesResponsive` identified a genuine responsive design defect on kibeam.com:
> `Untitled_design_20.png` overflows the tablet viewport (800px) by 90px.
> The image (200px wide) starts at x=690, extending to x=890 — 90px beyond the viewport edge.
>
> Root cause: image uploaded via CMS without `max-width: 100%` CSS applied.
> Also flagged: missing `alt` text on the same image (accessibility violation).
>
> This demonstrates the framework catching real defects on a live production site.

---

### KibeamContactTest.java

Contact page content validation — verifies the support page has the expected contact information.

| Test Method | Description | What It Validates |
|-------------|-------------|-------------------|
| `testContactPageLoads` | Verify Contact page loads successfully | On correct page, title not null |
| `testContactPageUrl` | Verify URL is correct on Contact page | URL contains `contact` and `kibeam.com` |
| `testPhoneNumberDisplayed` | Verify phone number is displayed on Contact page | Phone element visible + text not null |
| `testAddressDisplayed` | Verify address is displayed on Contact page | Address element visible + text not null |
| `testSupportHoursDisplayed` | Verify support hours are displayed on Contact page | Support hours element present (informational) |
| `testEmailLinkExists` | Verify Email Us link exists on Contact page | Email/support anchor is present |
| `testFaqLinkExists` | Verify FAQ link exists on Contact page | FAQ anchor is present (informational) |
| `testContactFormPresence` | Verify contact form presence (if applicable) | Form element present (informational) |
| `testContactInfoHasContent` | Verify contact information section has content | At least one of: phone OR address OR email |

---

### KibeamEducatorsTest.java

Educators page content validation — verifies the educators landing page loads and its key content is present.

| Test Method | Description | What It Validates |
|-------------|-------------|-------------------|
| `testEducatorsPageLoads` | Verify Educators page loads successfully | On correct page, title not null |
| `testHeroSectionDisplayed` | Verify hero section is displayed on Educators page | Hero element is visible |
| `testVideoPlayerPresent` | Verify video player is present on Educators page | Video element present (informational) |
| `testCtaButtonsPresent` | Verify CTA buttons are present on Educators page | At least one CTA button visible |
| `testCtaButtonsClickable` | Verify CTA buttons are clickable | Buttons present and page stable after check |
| `testFeatureSectionsPresent` | Verify Educators page has feature sections | Feature section count > 0 |
| `testEducatorsPageUrl` | Verify URL is correct on Educators page | URL contains `educators` and `kibeam.com` |
| `testHeroTitleExists` | Verify hero title exists | Hero title element not null |

---

### KibeamAccessibilityTest.java

WCAG compliance scanning across all main pages using the custom `AccessibilityChecker`.

| Test Method | Description | What It Validates |
|-------------|-------------|-------------------|
| `testHomeCriticalAccessibility` | Quick check for critical accessibility issues on home page | Critical violations only — fast CI smoke check |
| `testHomePageAccessibility` | Verify home page accessibility compliance | Full WCAG scan, auto-check via `BaseAccessibilityTest` |
| `testEducatorsPageAccessibility` | Verify educators page accessibility compliance | Full WCAG scan on educators URL |
| `testContactPageAccessibility` | Verify contact page accessibility compliance | Full WCAG scan — form labels, inputs |
| `testAboutPageAccessibility` | Verify about page accessibility compliance | Heading hierarchy, content structure |
| `testAllPagesAccessibility` | Scan all main pages for accessibility issues | Manual `checkAccessibilityNow()` at each page in sequence |

---

### KibeamDeviceAccessibilityTest.java

Accessibility compliance across the full device matrix — parameterised with `@DataProvider` so each device runs as a separate test entry.

| Test Method | Description | What It Validates |
|-------------|-------------|-------------------|
| `testMobileAccessibility(device)` | Accessibility check on mobile viewport | 5 mobile devices × home page WCAG scan |
| `testTabletAccessibility(device)` | Accessibility check on tablet viewport | 4 tablet devices × home page WCAG scan |
| `testDesktopAccessibility(device)` | Accessibility check on desktop viewport | 3 desktop viewports × home page WCAG scan |
| `testAllPagesMobile` | All pages accessibility on iPhone 12 | 4 pages × iPhone 12 — critical violations asserted |
| `testAllPagesTablet` | All pages accessibility on iPad | 4 pages × iPad — critical violations asserted |

---

## Run Commands

### Quick Reference

| Command | Description |
|---------|-------------|
| `mvn test` | All tests, all classes |
| `mvn test -Dheadless=true` | Headless Chrome — faster, CI-friendly |
| `mvn test -Pkibeam` | Core UI tests via testng XML |
| `mvn test -PtestngKibeamDevices` | All tests across device profiles |
| `mvn test -PtestngAccessibilityAll` | Full accessibility audit |
| `mvn test -PtestngAccessibilityDesktop` | Desktop accessibility |
| `mvn test -PtestngAccessibilityMobile` | Mobile accessibility |
| `mvn test -PtestngAccessibilityTablet` | Tablet accessibility |
| `mvn test -Pregression` | Full regression suite |
| `mvn test -Dtest=KibeamNavigationTest` | Navigation tests only |
| `mvn test -Dtest=KibeamResponsiveTest` | Responsive tests only |
| `mvn test -Dtest=KibeamAccessibilityTest` | Accessibility tests only |
| `mvn test -Denv=staging` | Use staging environment config |

---

#### All Tests (`mvn test`)

**What it does:** Runs the complete test suite using `testngKibeam.xml` — navigation, responsive, contact, and educators tests in Chrome.

**When to use:**
- Before committing code changes
- After updating page objects or utilities
- For a full baseline run

```bash
mvn test
mvn test -Dheadless=true    # Faster — no visible browser window
```

---

#### Device Tests (`-PtestngKibeamDevices`)

**What it does:** Runs all test classes at multiple viewport sizes using Chrome DevTools Protocol emulation. Each device profile sets the viewport dimensions, pixel ratio, and mobile user agent.

**Tests included:**
- All navigation, responsive, contact, and educator tests
- Run once per configured device profile

**When to use:**
- Validating responsive breakpoints
- Checking mobile-specific layout changes
- Reproducing device-specific bugs

**Why it matters:**
Real device testing is expensive and slow. CDP emulation lets you catch the majority of responsive layout bugs — including the image overflow defect found on this project — at desktop speed.

```bash
mvn test -PtestngKibeamDevices
mvn test -PtestngKibeamDevices -Ddevice=IPHONE_12
```

---

#### Accessibility Tests (`-PtestngAccessibilityAll/Desktop/Mobile/Tablet`)

**What it does:** Runs `AccessibilityChecker` (custom WCAG scanner) and Axe-core against each page at the specified viewport category.

**Checks performed:**
- Missing or empty `alt` attributes on images
- Form inputs without associated labels
- Broken heading hierarchy (skipped levels, multiple `<h1>`)
- Interactive elements without accessible names
- Keyboard navigation traps and positive `tabindex` usage
- Basic colour contrast ratio estimation

**When to use:**
- Verifying WCAG 2.0/2.1 Level AA compliance
- Accessibility audit before a release
- Catching regressions after CMS content changes

**Why it matters:**
Accessibility failures can be legal liabilities and actively exclude users. Automated scanning catches a significant proportion of WCAG violations with zero manual effort.

```bash
mvn test -PtestngAccessibilityAll
mvn test -PtestngAccessibilityDesktop
mvn test -PtestngAccessibilityMobile
mvn test -PtestngAccessibilityTablet

# Fail the build on critical violations
mvn test -PtestngAccessibilityAll -Daccessibility.fail.on.critical=true
```

---

#### Regression Suite (`-Pregression`)

**What it does:** Runs the full regression suite covering all test classes and all pages.

**When to use:**
- Before a production release
- After significant code or CMS changes
- Scheduled nightly runs in CI

```bash
mvn test -Pregression
mvn test -Pregression -Dheadless=true
```

---

#### By Test Class or Method

```bash
# Single class
mvn test -Dtest=KibeamNavigationTest
mvn test -Dtest=KibeamResponsiveTest
mvn test -Dtest=KibeamAccessibilityTest

# Single method
mvn test -Dtest=KibeamNavigationTest#testHomePageLoads
mvn test -Dtest=KibeamResponsiveTest#testImagesResponsive

# Pattern matching
mvn test -Dtest=Kibeam*Test
```

---

#### Environment Configuration

```bash
mvn test -Denv=staging          # Uses config/staging.properties
mvn test -Denv=prod             # Uses config/prod.properties

# Override specific properties inline
mvn test -Dbase.url=https://staging.kibeam.com
mvn test -Dtimeout=20
mvn test -Dheadless=true -Dbrowser=chrome
```

---

## Key Code Patterns

### Page Object Model

Navigation methods return typed page objects, enabling fluent test chains without raw driver interactions in test code.

```java
// KibeamHomePage.java
public KibeamEducatorsPage clickEducators() {
    log.info("Clicking Educators link");
    clickWithFallback(educatorsLink);       // standard click with JS fallback
    waitForUrlContains("educators");         // explicit wait — no Thread.sleep
    return new KibeamEducatorsPage(driver);  // returns next page object
}

// KibeamNavigationTest.java — test code stays clean
KibeamEducatorsPage educatorsPage = homePage.clickEducators();
assertTrue(educatorsPage.isOnEducatorsPage(), "Should land on Educators page");
```

- **`clickWithFallback`** — attempts a standard WebDriver click first; if intercepted by an overlay or animation, falls back to a JavaScript click without the test failing noisily
- **Return typed page objects** — the compiler enforces that you're on the right page, and IDEs autocomplete only methods relevant to the current page
- **`waitForUrlContains`** — explicit URL wait replaces fragile `Thread.sleep` calls

---

### Soft Assertions

`SoftAssert` collects all failures in a test instead of stopping at the first one. Every responsive check runs regardless of whether an earlier check failed.

```java
// KibeamResponsiveTest.java
@Test(description = "Verify images don't overflow container")
public void testImagesResponsive() {
    SoftAssert softAssert = new SoftAssert();
    int viewportWidth = getViewportWidth();

    for (WebElement img : getDriver().findElements(By.tagName("img"))) {
        if (img.isDisplayed()) {
            int imgX = img.getLocation().getX();
            int imgWidth = img.getSize().getWidth();

            if (imgX + imgWidth > viewportWidth + 5) {
                String srcName = img.getAttribute("src");
                int overflowBy = (imgX + imgWidth) - viewportWidth;
                softAssert.fail(String.format(
                    "Image overflow: src=%s overflows by %dpx (image: %dpx at x=%d, viewport: %dpx)",
                    srcName, overflowBy, imgWidth, imgX, viewportWidth));
            }
        }
    }
    softAssert.assertAll(); // collects and reports all failures at once
}
```

- **`softAssert.fail(message)` per image** — each overflow is a separate named failure in the report, not a single count assertion
- **`assertAll()`** at the end — TestNG marks the test failed and prints all messages together
- **Hard assertions** are still used in other tests where a failure makes the rest of the test meaningless

---

### Device Emulation

Chrome DevTools Protocol is used to emulate device metrics directly in the browser — no Android/iOS simulators required.

```java
// DeviceEmulation.java
public static ChromeOptions getChromeOptionsForDevice(Device device) {
    Map<String, Object> deviceMetrics = new HashMap<>();
    deviceMetrics.put("width", device.getWidth());
    deviceMetrics.put("height", device.getHeight());
    deviceMetrics.put("pixelRatio", device.getPixelRatio());
    deviceMetrics.put("mobile", device.isMobile() || device.isTablet());

    Map<String, Object> mobileEmulation = new HashMap<>();
    mobileEmulation.put("deviceMetrics", deviceMetrics);
    if (device.getUserAgent() != null) {
        mobileEmulation.put("userAgent", device.getUserAgent());
    }

    ChromeOptions options = new ChromeOptions();
    options.setExperimentalOption("mobileEmulation", mobileEmulation);
    return options;
}

// Usage in KibeamDeviceAccessibilityTest
emulateDevice(Device.IPAD);  // resizes viewport + injects iPad user agent
```

- **`mobileEmulation` experimental option** — sets viewport, pixel ratio, and user agent in a single Chrome capability
- **`getUserAgent()`** — injects the real device UA string so responsive CSS media queries and server-side detection behave as on the actual device
- **`isMobile() || isTablet()`** — sets the `mobile` flag which triggers touch event handling in the browser

---

### Accessibility Scanning

Two-layer accessibility checking: custom `AccessibilityChecker` for structural issues, plus Axe-core for full WCAG 2.x rule coverage.

```java
// BaseAccessibilityTest.java — runs automatically after every @Test
@AfterMethod
public void checkAccessibility() {
    if (accessibilityCheckSkipped) return;

    // Layer 1: Custom structural checks (images, labels, headings, ARIA, contrast)
    List<AccessibilityIssue> issues = AccessibilityChecker.checkAll(getDriver());

    // Layer 2: Axe-core WCAG scan
    List<AccessibilityViolation> axeViolations = AccessibilityUtils.runAxe(getDriver());

    // Report all findings — fail only on critical if configured
    AccessibilityReporter.report(issues, axeViolations, ExtentReportManager.getTest());

    if (Configuration.getInstance().shouldFailOnCritical()) {
        assertNoCritical(issues, axeViolations);
    }
}
```

- **`@AfterMethod` auto-scan** — no test code needed; accessibility is verified for every page visited
- **`checkAccessibilityNow()`** — call explicitly mid-test to scan before navigating away
- **`skipAccessibilityCheck()`** — used in device tests where the check runs manually with device context attached
- **`shouldFailOnCritical()`** — configurable via `-Daccessibility.fail.on.critical=true`; defaults to log-only

---

## Device Profiles

14 profiles covering the full range from small mobile to 2K desktop.

| Device | Width | Height | Type | User Agent |
|--------|-------|--------|------|------------|
| `IPHONE_SE` | 375 | 667 | Mobile | iPhone iOS 15 / WebKit |
| `IPHONE_12` | 390 | 844 | Mobile | iPhone iOS 15 / WebKit |
| `IPHONE_14_PRO_MAX` | 430 | 932 | Mobile | iPhone iOS 16 / WebKit |
| `PIXEL_5` | 393 | 851 | Mobile | Android 11 / Chrome |
| `SAMSUNG_S21` | 360 | 800 | Mobile | Android 12 / Chrome |
| `IPAD` | 768 | 1024 | Tablet | iPad iOS 15 / WebKit |
| `IPAD_PRO_11` | 834 | 1194 | Tablet | iPad iOS 15 / WebKit |
| `IPAD_PRO_12` | 1024 | 1366 | Tablet | iPad iOS 15 / WebKit |
| `GALAXY_TAB_S7` | 800 | 1280 | Tablet | Android 11 / Chrome |
| `LAPTOP` | 1280 | 800 | Desktop | — (native Chrome UA) |
| `LAPTOP_L` | 1440 | 900 | Desktop | — |
| `DESKTOP_HD` | 1366 | 768 | Desktop | — |
| `DESKTOP_FHD` | 1920 | 1080 | Desktop | — |
| `DESKTOP_2K` | 2560 | 1440 | Desktop | — |

---

## Real Defect Example — Interview Talking Point

### What the test does

`testImagesResponsive` in `KibeamResponsiveTest` iterates over every `<img>` tag on the page, reads its rendered position (`getLocation().getX()`) and width (`getSize().getWidth()`), and calls `softAssert.fail()` with a detailed message if `imgX + imgWidth > viewportWidth + 5px`.

### What it found

Running against an iPad viewport (800px wide), the test produced:

```
SOFT ASSERT FAILURE:
Image overflow: src=Untitled_design_20.png alt=(no alt) overflows by 90px
(image: 200px wide at x=690, viewport: 800px)
```

### Breaking it down

- The image is **200px wide** and positioned at **x=690** (near the right edge of a content block)
- It extends to **x=890** — **90px beyond** the 800px viewport edge
- On a real iPad, this image is clipped; users would need to scroll horizontally to see it
- The image also has **no `alt` attribute**, caught simultaneously by the accessibility checker

### What it means for real users

On actual tablet devices, this image is partially hidden. On mobile (375px viewport) the overflow would be even more severe — the image would be almost entirely off-screen.

### How to reproduce manually

1. Open `https://kibeam.com` in Chrome
2. Open DevTools → Toggle device toolbar → Select iPad (768px)
3. Scroll to the section containing `Untitled_design_20.png`
4. The image visibly bleeds beyond the right edge of its container

### What the fix would be

```css
/* Global fix */
img {
    max-width: 100%;
    height: auto;
}

/* Scoped fix */
.your-content-block img {
    max-width: 100%;
}
```

A single CSS line. The fact that automated testing caught it before manual review is the core demonstration value of this project.

### Why this matters in interviews

This section of your portfolio proves that your automation framework doesn't just run — **it finds real bugs on real websites**. It demonstrates: test design that catches genuine defects, failure messages that pinpoint root cause without manual investigation, and accessibility awareness bundled into responsive testing.

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Runtime |
| Selenium WebDriver | 4.39.0 | Browser automation |
| TestNG | 7.11.0 | Test framework |
| WebDriverManager | 6.3.3 | Automatic driver management |
| Axe-core (Deque) | 4.11.0 | WCAG accessibility scanning |
| Extent Reports | 5.1.2 | HTML reporting |
| Allure | 2.32.0 | Alternative report format |
| AssertJ | 3.27.6 | Fluent assertions |
| DataFaker | 2.5.3 | Test data generation |
| SLF4J + Logback | 2.0.17 + 1.5.23 | Structured logging |
| Maven Surefire | 3.5.4 | Test execution + profiles |

---

## Reports

Reports are generated automatically after every run:

- **Location:** `reports/` folder at project root (gitignored — not committed)
- **Format:** Single self-contained HTML file per run with timestamp in the filename
- **Screenshots:** Embedded directly in the HTML as base64 on test failure — no separate image files
- **Naming:** Dynamic — uses test class name when running with `-Dtest=`, suite name otherwise

```bash
# View the latest report (macOS)
open reports/*.html

# View a specific run
open reports/KibeamNavigation_Report_2026-04-02_14-30-00.html

# Optional: generate Allure report
mvn allure:report
open target/site/allure-maven-plugin/index.html
```

---

## Interview Talking Points

This section maps common interview questions to what this project demonstrates.

---

**"Tell me about your Page Object Model implementation."**

Every page in this project has its own class extending `BasePage`, which provides shared utilities like `waitForElementClickable`, `isDisplayed`, and `visit`. Page methods return typed page objects rather than `void`, enabling method chaining. For example, `homePage.clickEducators()` returns a `KibeamEducatorsPage` — the compiler enforces you're on the right page and IDEs autocomplete only the methods relevant to that page. This eliminates the need for test code to know about locators or wait strategies.

---

**"How do you handle test failures without stopping the suite?"**

`KibeamResponsiveTest` uses TestNG's `SoftAssert` pattern. Each `@Test` method creates a local `SoftAssert` instance, calls `softAssert.fail(detailedMessage)` for each individual failure (e.g. each overflowing image), and then calls `softAssert.assertAll()` at the end. Every responsive check in the test runs regardless of earlier failures, and the report shows all issues at once. I chose `softAssert.fail()` per item rather than a count assertion so each failure has its own descriptive message identifying exactly which element caused it.

---

**"Have you found real bugs with your automation?"**

Yes — `testImagesResponsive` identified a genuine layout defect on live kibeam.com. An image (`Untitled_design_20.png`) overflows the tablet viewport by 90px: it's 200px wide, positioned at x=690 on an 800px viewport, extending 90px off-screen. The failure message included the filename, alt text, exact overflow amount, and pixel dimensions, so the root cause (missing `max-width: 100%` CSS) was immediately obvious without manual investigation. The same image was also flagged by the accessibility checker for missing `alt` text in the same test run.

---

**"How do you handle parallel execution?"**

`BaseTestTestNG` uses three `ThreadLocal` variables: one for `WebDriver`, one for `BrowserType`, and one for the current device name. `DriverFactory` also stores its driver in a separate `ThreadLocal`. Each test thread creates, uses, and destroys its own browser instance with no shared state. `ExtentReportManager` uses a `ThreadLocal<ExtentTest>` so each thread writes to its own test node in the report. The TestNG `threadCount` is set to 4 in `pom.xml`, and the XML suites use `parallel="tests"` or `parallel="methods"` depending on the profile.

---

**"How do you test responsive design without real devices?"**

Chrome DevTools Protocol device emulation. `DeviceEmulation.getChromeOptionsForDevice(Device)` builds a `mobileEmulation` capability that sets the viewport width and height, pixel ratio, mobile flag, and the real device user agent string. CSS media queries, JavaScript `navigator.userAgent` checks, and touch event handling all behave as they would on the actual device — in a regular desktop Chrome process running at full speed. 14 device profiles are defined covering iPhone SE (375px) through 2K desktop (2560px).

---

**"Walk me through your reporting setup."**

`ExtentTestNGListener` implements both `ITestListener` and `ISuiteListener`. `onStart(ISuite)` initialises the singleton `ExtentReportManager` with a dynamically generated report name (using the `-Dtest` system property if set, falling back to the suite name from the XML). `onTestStart` creates an `ExtentTest` node stored in a `ThreadLocal`. On failure, `attachScreenshot` captures a base64 screenshot and embeds it directly in the HTML — no separate files. `onFinish(ISuite)` flushes the report. The listener is registered both in `testngKibeam.xml` and via `@Listeners(ExtentTestNGListener.class)` on `BaseTestTestNG`, ensuring it fires even when Surefire bypasses the XML with `-Dtest=`.

---

**"How does your configuration management work?"**

`Configuration.java` is a double-checked-locking singleton that resolves properties in four-tier priority: (1) JVM system property `-Dkey=value`, (2) environment variable `KEY_NAME` (dots to underscores, uppercase), (3) environment-specific properties file (e.g. `staging.properties` when `-Denv=staging`), (4) default `test.properties`. Required properties (`base.url`, `browser`, `timeout`) are validated at startup and throw a descriptive `ConfigurationException` if missing. Sensitive keys like `password` and `token` are masked in log output.

---

**"How do you approach accessibility testing in automation?"**

This project uses a two-layer approach. The first layer is a custom `AccessibilityChecker` with no external dependencies — it runs structural checks for missing `alt` tags, unlabelled form inputs, broken heading hierarchies, missing ARIA labels, and basic colour contrast. The second layer is Axe-core (Deque's WCAG library) for comprehensive rule-based scanning. Both layers run automatically via an `@AfterMethod` hook in `BaseAccessibilityTest` — so accessibility is verified on every page visited without any test code. Results feed directly into the Extent Report alongside the test results, and the severity threshold for build failure is configurable.

---

## License

MIT License

Built to demonstrate professional UI test automation practices against a real production website.
