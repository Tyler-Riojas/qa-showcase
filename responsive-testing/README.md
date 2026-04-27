# Responsive Testing Showcase

> Generic cross-device responsive test suite — works against any publicly accessible website via `-Dtarget.url=`

---

## What This Demonstrates

| Skill | Implementation |
|---|---|
| CDP device emulation | Chrome DevTools Protocol emulation for Mobile (iPhone 12), Tablet (iPad Pro 11), Desktop — no physical devices |
| Configurable target | `System.getProperty("target.url")` with fallback default — swap target sites with a single flag |
| Horizontal overflow detection | `scrollWidth > clientWidth` via JavaScript — catches layout breakage invisible to visual inspection |
| Touch target validation | Iterates buttons and anchors, flags elements smaller than the 44px WCAG/Apple minimum |
| Element overlap detection | `Rectangle` intersection math on adjacent `<section>` elements — finds z-index and margin bugs |
| Text readability checks | `getCssValue("font-size")` parsed to int — reports elements below 12px minimum |
| Image overflow detection | `imgX + imgWidth > viewportWidth` — the same check that found the kibeam.com production defect |
| Parallel device execution | TestNG `parallel="tests"` with `thread-count="3"` — all three device profiles run concurrently |
| ThreadLocal driver safety | `BaseTestTestNG` stores driver in `ThreadLocal<WebDriver>` — zero state leakage between parallel threads |
| ExtentReports | Per-test pass/fail with device name, failure screenshot embedded inline |
| Allure | `@Step` steps and severity annotations on all tests |

---

## How It Differs From Site-Specific Responsive Testing

This project shows the reusable pattern. [kibeam-ui-testing](../kibeam-ui-testing/) demonstrates it on a real site where it found a genuine production defect.

`kibeam-ui-testing/KibeamResponsiveTest` identified `Untitled_design_20.png` overflowing the tablet viewport by 90px on kibeam.com. The image is 200px wide, positioned at x=690 on an 800px viewport — 90px past the visible edge. The same `testImagesResponsive` logic in this project can find the identical class of defect on any site.

The difference:
- **This project** — generic, any URL, no site-specific selectors, works out of the box on `the-internet.herokuapp.com`
- **kibeam-ui-testing** — site-specific test class with kibeam.com page structure knowledge, additional contact/nav/educators coverage

---

## Quick Start

```bash
cd responsive-testing

# Default — runs against the-internet.herokuapp.com (no setup required)
mvn test -Presponsive

# Against any site
mvn test -Presponsive -Dtarget.url=https://yoursite.com

# Specific pages (default: /, /login, /checkboxes)
mvn test -Presponsive -Dtarget.url=https://yoursite.com -Dtarget.pages='/,/about,/contact'

# Headless (CI)
mvn test -Presponsive -Dheadless=true

# Mobile only
mvn test -Presponsive-mobile -Dtarget.url=https://yoursite.com

# Desktop only
mvn test -Presponsive-desktop -Dtarget.url=https://yoursite.com

# View report (macOS)
open reports/Responsive_Tests_*.html

# Allure report
allure serve target/allure-results
```

---

## What Gets Tested

8 test methods run against each configured page on each device profile:

| Test Method | Priority | What It Checks |
|---|---|---|
| `testLogViewportInfo` | 1 | Logs browser, device name, viewport dimensions, emulation mode — always passes, informational |
| `testNoHorizontalScroll` | 1 | `scrollWidth > clientWidth` — page must not produce a horizontal scrollbar at current viewport |
| `testNavigationLinksVisible` | 1 | `nav`, `[role='navigation']`, `header` elements are present and visible |
| `testFooterWithinViewport` | 2 | Footer exists, is displayed, and its width does not exceed viewport width |
| `testTouchTargetSizes` | 2 | Buttons and anchors meet 44px minimum (logs violations, does not fail — threshold may need tuning per site) |
| `testHeaderNoOverlap` | 2 | Header bottom ≤ main content top (±10px tolerance for intentional overlapping designs) |
| `testNoElementOverlapHomePage` | 2 | Adjacent `<section>` elements must not overlap by more than 50px |
| `testTextReadability` | 2 | Text elements (`p`, `span`, `li`, `td`, `th`) must not be below 12px font size |
| `testImagesResponsive` | 2 | `imgX + imgWidth ≤ viewportWidth + 5px` — images must not overflow the visible area |

---

## Device Profiles

| Profile | Viewport | User Agent | Emulation |
|---|---|---|---|
| Desktop | 1920×1080 | Standard Chrome | None — window maximized |
| Mobile (iPhone 12) | 390×844 | iOS 15 WebKit | Chrome DevTools Protocol `mobileEmulation` |
| Tablet (iPad Pro 11) | 834×1194 | iPadOS 15 WebKit | Chrome DevTools Protocol `mobileEmulation` |

Device configs are defined in `DeviceEmulation.java`. Additional devices (`IPHONE_SE`, `PIXEL_5`, `SAMSUNG_S21`, `IPAD`, `GALAXY_TAB_S7`, etc.) are available — add a `<test>` block to the XML to include them.

---

## Project Structure

```
responsive-testing/
├── pom.xml
├── src/
│   ├── main/java/
│   │   ├── base/BaseTestTestNG.java         # ThreadLocal driver lifecycle, device emulation helpers
│   │   ├── config/
│   │   │   ├── Configuration.java           # Singleton config: system props > env vars > properties file
│   │   │   └── FeatureToggle.java           # Feature flags: retry, screenshots, headless
│   │   ├── core/DriverFactory.java          # Thread-safe WebDriver factory (local + Grid)
│   │   ├── enums/BrowserType.java           # CHROME | FIREFOX | EDGE | SAFARI
│   │   ├── listeners/
│   │   │   ├── AnnotationTransformer.java   # Auto-applies RetryAnalyzer to all @Test methods
│   │   │   ├── ExtentTestNGListener.java    # Extent Reports integration (suite + test lifecycle)
│   │   │   ├── RetryAnalyzer.java           # Configurable retry (default 2 attempts)
│   │   │   └── TestListenerTestNG.java      # Console logging + screenshot on failure
│   │   └── utils/
│   │       ├── DeviceEmulation.java         # CDP emulation configs for 14 device presets
│   │       ├── ExtentReportManager.java     # ThreadLocal ExtentTest, HTML report generation
│   │       ├── ScreenshotUtils.java         # File + Base64 screenshots
│   │       └── WaitUtils.java              # 12 explicit wait methods, zero Thread.sleep
│   ├── main/resources/
│   │   ├── config/test.properties           # Default config (base URL, browser, timeouts)
│   │   └── logback.xml                      # SLF4J + Logback console config
│   └── test/
│       ├── java/TestNG/tests/
│       │   └── ResponsiveTest.java          # 8 generic responsive test methods
│       └── resources/
│           ├── testng-responsive.xml        # All 3 device profiles, parallel=tests
│           ├── testng-responsive-mobile.xml # Mobile only (iPhone 12)
│           ├── testng-responsive-desktop.xml# Desktop only
│           ├── allure.properties            # allure.results.directory=target/allure-results
│           └── environment.properties       # Allure environment metadata
```

---

## Run Commands

| Command | What Runs |
|---|---|
| `mvn test -Presponsive` | All 3 device profiles in parallel (24 tests total: 8 × 3) |
| `mvn test -Presponsive-mobile` | Mobile only — iPhone 12 emulation (8 tests) |
| `mvn test -Presponsive-desktop` | Desktop only (8 tests) |
| `mvn test -Presponsive -Dtarget.url=https://yoursite.com` | All devices, custom target |
| `mvn test -Presponsive -Dtarget.pages='/,/about,/contact'` | All devices, specific pages |
| `mvn test -Presponsive -Dheadless=true` | Headless (CI mode) |
| `allure serve target/allure-results` | Interactive Allure report |
| `open reports/Responsive_Tests_*.html` | Extent HTML report (macOS) |

---

## Key Code Pattern

The configurable URL system property pattern that makes this suite generic:

```java
// ResponsiveTest.java — runs against any site with no code changes
private static final String BASE_URL =
        System.getProperty("target.url", "https://the-internet.herokuapp.com");

private static final List<String> PAGES = Arrays.asList(
        System.getProperty("target.pages", "/, /login, /checkboxes")
                .split("\\s*,\\s*")
);

// Usage:
// mvn test -Presponsive                                           → default site
// mvn test -Presponsive -Dtarget.url=https://yoursite.com        → any site
// mvn test -Presponsive -Dtarget.pages='/,/about,/contact'       → specific pages
```

The same pattern is used in `accessibility-testing` and `performance-testing` — all three suites work out of the box on the default target, and can be pointed at any public URL without touching the test code.

---

## Real-World Usage

**When to use this project:**

| Scenario | Recommendation |
|---|---|
| Quick check on a new site | `mvn test -Presponsive -Dtarget.url=https://newsite.com` — results in 2–3 minutes |
| Mobile regression before a launch | `mvn test -Presponsive-mobile -Dtarget.url=https://yoursite.com -Dheadless=true` |
| CI gate for any public-facing site | Add to GitHub Actions with `-Dheadless=true` |
| Finding overflow defects like the kibeam one | `testImagesResponsive` is the most productive test — catches CSS `max-width` omissions |

**When to build a site-specific suite instead:**

Use `kibeam-ui-testing` as the template when you need custom selectors, knowledge of the site's specific page structure, or additional coverage beyond the generic responsive checks (contact forms, educator sections, navigation link text, etc.).

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Selenium WebDriver | 4.39 | Browser automation |
| TestNG | 7.11 | Test framework, parallel execution |
| WebDriverManager | 6.3.3 | Automatic ChromeDriver management |
| ExtentReports | 5.1.2 | HTML test reports with screenshots |
| Allure TestNG | 2.32.0 | Interactive Allure reports |
| AspectJ Weaver | 1.9.24 | Allure bytecode instrumentation |
| SLF4J + Logback | 2.0.17 / 1.5.23 | Structured logging |
| Maven | 3.6+ | Build and dependency management |

---

## Interview Talking Points

1. **Why `System.getProperty` instead of a config file?** — It allows overriding at the command line without editing files. CI pipelines, local dev, and staging environments all use the same artifact with different `-D` flags. The config file provides the fallback default.

2. **Why parallel="tests" and not parallel="methods"?** — Each `<test>` block represents a distinct device profile. Parallelizing at the `tests` level means each device gets its own thread and its own WebDriver, so `IPHONE_12`, `IPAD_PRO_11`, and Desktop all run concurrently without sharing state. Parallelizing at `methods` would mix device contexts in the same thread.

3. **Why `ThreadLocal<WebDriver>` in `BaseTestTestNG`?** — In parallel execution, multiple threads are creating and using drivers simultaneously. Without `ThreadLocal`, thread A's driver could be returned to thread B's `getDriver()` call, causing cross-contamination. `ThreadLocal` ensures each thread always gets its own instance.

4. **How does CDP device emulation differ from just resizing the browser window?** — `setSize()` only changes the viewport dimensions. CDP `mobileEmulation` additionally sets the user-agent string, device pixel ratio, and `mobile: true` flag — so the site's JavaScript detects a real mobile device and renders accordingly. Some sites serve completely different markup to mobile user agents.

5. **What would make `testTouchTargetSizes` a hard failure?** — Right now it logs a warning instead of failing, because the 44px minimum is a guideline and many sites intentionally use smaller inline text links. To harden it: add a configurable `maxAllowedSmallTargets` system property and fail if exceeded, or filter by element type (standalone buttons should be strict, inline anchors lenient).

6. **How did this framework find a real bug?** — The same `testImagesResponsive` logic (identify images where `imgX + imgWidth > viewportWidth`) found that `Untitled_design_20.png` on kibeam.com overflows the tablet viewport by 90px. Root cause: image uploaded via CMS without `max-width: 100%` CSS. The test logs the src, alt, overflow amount, image width, position, and viewport width — everything needed to file a complete bug report.
