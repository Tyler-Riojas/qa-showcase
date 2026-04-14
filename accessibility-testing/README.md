# Accessibility Testing Showcase

> Generic WCAG compliance testing suite using Axe-core and custom checks — works against any publicly accessible website. No site-specific configuration required.

## What This Demonstrates

| Approach | Tool | What It Checks |
|----------|------|----------------|
| WCAG Compliance | Axe-core (Deque) | Full WCAG 2.1 Level AA — color contrast, ARIA, labels, headings, landmarks |
| Custom Checks | AccessibilityChecker | Images, form labels, heading hierarchy, link text, interactive elements |
| Device Emulation | Chrome DevTools Protocol | Accessibility across mobile, tablet, and desktop viewports |
| Reporting | Extent Reports + Allure | Violation cards with selectors, WCAG links, fix effort, manual test tips |

## Quick Start

```bash
# Default — runs against the-internet.herokuapp.com (no setup required)
mvn test -Paccessibility

# Run against your own site
mvn test -Paccessibility -Dtarget.url=https://yoursite.com

# Smoke check only (fast CI check)
mvn test -Paccessibility-smoke

# Headless
mvn test -Paccessibility -Dheadless=true -Dtarget.url=https://yoursite.com

# Two pages
mvn test -Paccessibility \
  -Dtarget.url=https://yoursite.com \
  -Dtarget.url.2=https://yoursite.com/about
```

## Two Testing Approaches

### A) Axe-core — Comprehensive WCAG Scanning

[Axe-core](https://github.com/dequelabs/axe-core) by Deque Systems is the industry-standard accessibility testing engine. It runs a full WCAG 2.1 Level AA rule set:

- **Color contrast** — text and background meet 4.5:1 ratio (normal text) / 3:1 ratio (large text)
- **ARIA usage** — roles, states, and properties are valid and correct
- **Form labels** — every input has an associated label
- **Image alternatives** — all `<img>` have meaningful alt text
- **Heading structure** — h1→h2→h3 hierarchy is logical
- **Landmark regions** — main, nav, footer regions are present
- **Link purpose** — link text is descriptive, not "click here"
- **Keyboard accessibility** — focusable elements are reachable and operable

### B) Custom Checker — Lightweight Common Issues

`AccessibilityChecker.java` runs fast, targeted checks without a full WCAG engine:

| Check | What It Looks For |
|-------|------------------|
| Images | `<img>` without alt, decorative images without `alt=""` |
| Headings | Missing `<h1>`, multiple `<h1>`, skipped heading levels |
| Forms | `<input>`, `<select>`, `<textarea>` without associated labels |
| Links | Empty anchor text, generic "click here" / "read more" text |
| Contrast | Text elements with insufficient foreground/background contrast |
| Interactive | Buttons without accessible names, missing focus indicators |

## Project Structure

```
src/
├── main/java/
│   ├── utils/
│   │   ├── AccessibilityUtils.java    # Axe-core runner + result parsing
│   │   ├── AccessibilityChecker.java  # Custom lightweight checker
│   │   ├── AccessibilityReporter.java # Extent Report violation cards
│   │   ├── DeviceEmulation.java       # 11 device profiles (CDP)
│   │   └── ...                        # WaitUtils, ScreenshotUtils
│   ├── base/
│   │   ├── BaseAccessibilityTest.java # Sets up Axe-core + custom checker
│   │   └── BaseTestTestNG.java        # WebDriver lifecycle
│   └── ...                            # listeners, config
└── test/
    ├── java/TestNG/tests/
    │   ├── GenericAccessibilityTest.java      # Main test class (this file)
    │   └── accessibility/
    │       └── BaseDeviceAccessibilityTest.java  # Device-aware base
    └── resources/
        ├── testng-accessibility.xml       # Full suite (parallel=methods)
        └── testng-accessibility-smoke.xml # Smoke only (serial)
```

## Test Coverage

| Test Method | Approach | Group | What It Asserts |
|-------------|----------|-------|-----------------|
| `testAxeCoreCompliance` | Axe-core | regression | Zero critical/serious violations on TARGET_URL |
| `testAxeCorePage2` | Axe-core | regression | Zero critical/serious violations on TARGET_URL_2 |
| `testCustomChecks` | Custom checker | smoke | Images, headings, forms, links on TARGET_URL |
| `testCustomChecksPage2` | Custom checker | regression | Same checks on TARGET_URL_2 |
| `testCombinedReport` | Both | regression | Combined violation card report |
| `testAccessibilityReport` | Both | report | Non-failing report — all violations logged |

## Run Commands

```bash
# Full accessibility scan
mvn test -Paccessibility

# Smoke only (CI-friendly, fast)
mvn test -Paccessibility-smoke

# Against a specific site
mvn test -Paccessibility -Dtarget.url=https://example.com

# Headless (for CI)
mvn test -Paccessibility -Dheadless=true

# If you hit OutOfMemoryError on large sites
export MAVEN_OPTS='-Xms512m -Xmx4096m'
mvn test -Paccessibility
```

## Configuring Target Site

The suite works against any publicly accessible URL. No code changes required:

| Property | Default | Description |
|----------|---------|-------------|
| `target.url` | `https://the-internet.herokuapp.com` | Primary page to scan |
| `target.url.2` | `https://the-internet.herokuapp.com/login` | Secondary page |
| `browser` | `chrome` | Browser (chrome/firefox/edge) |
| `headless` | `false` | Headless mode |

Set via Maven: `-Dtarget.url=https://yoursite.com`

## Understanding The Report

Violations are grouped by **severity**:

| Severity | Meaning | Example |
|----------|---------|---------|
| **Critical** | Blocks access entirely | Form input with no label — screen reader has no idea what to type |
| **Serious** | Significant barrier | Low color contrast — text unreadable for low-vision users |
| **Moderate** | Partial barrier | Heading levels skipped — navigation confused for screen reader users |
| **Minor** | Best practice | Redundant alt text — minor confusion, not blocking |

Each violation card in the report includes:
- **Element selectors** — the exact CSS selector to find the failing element
- **WCAG criteria links** — clickable links to W3C quickref for the specific success criterion
- **User impact** — a plain-English description of who is affected and how
- **Fix effort** — Significant / Moderate / Minor based on severity
- **Manual test tip** — how to verify the issue without a tool

## Allure Reports

```bash
mvn allure:serve     # Generate and open in browser
mvn allure:report    # Generate static HTML
```

The Allure report organizes tests under:
- **Feature**: Accessibility Testing
- **Stories**: WCAG Compliance / Custom Checks / Multi-Page Scan / Accessibility Report

## Real World Usage

To apply this test suite to any project:

1. **Default run** — `mvn test -Paccessibility -Dtarget.url=https://yoursite.com`
2. **Pick your threshold** — edit `accessibility.fail.on.critical=true` in test.properties to fail the build on critical violations
3. **Add to CI** — `mvn test -Paccessibility-smoke -Dheadless=true` as a pre-merge gate
4. **Full audit** — `mvn test -Paccessibility -Dheadless=true` nightly

The suite is intentionally decoupled from any specific site. The URL is the only input.

## Relationship To Kibeam Showcase

This project demonstrates the **reusable pattern**.

[kibeam-ui-testing](../kibeam-ui-testing/) demonstrates it **applied to a real production site** where it found a genuine defect:

> `testImagesResponsive` identified `Untitled_design_20.png` overflowing the tablet viewport by 90px. Root cause: image uploaded via Shopify CMS without `max-width: 100%` CSS. The accessibility scanner also flagged missing alt text on the same image.

The combination — a generic, portable framework + a real-world application — is more compelling than either alone.

## Tech Stack

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 17 | Language |
| Selenium | 4.x | Browser automation |
| TestNG | 7.11.0 | Test framework |
| Axe-core (selenium) | 4.11.0 | WCAG 2.1 engine |
| Allure | 2.32.0 | Test reporting |
| Extent Reports | 5.1.2 | HTML dashboards |
| Maven | 3.x | Build |

## Interview Talking Points

1. **Two-tool strategy** — Axe-core catches WCAG rule violations; the custom checker catches structural issues that Axe-core's rule set might not flag (e.g., heading hierarchy, generic link text). Using both gives broader coverage with minimal overlap.

2. **Separation of concerns** — `AccessibilityUtils` runs the scan, `AccessibilityChecker` runs custom checks, `AccessibilityReporter` formats the output. Each is independently testable and reusable.

3. **Memory-conscious design** — Accessibility scans hold violation data + DOM snapshots in memory. `thread-count=2` (not 4) prevents heap exhaustion on large sites. `MAX_VIOLATIONS_IN_REPORT=10` caps HTML report size. These are production-hardened decisions, not arbitrary choices.

4. **Actionable violation cards** — Each card shows the element selector, WCAG success criterion with a link to the W3C quickref, the user impact in plain English, and a manual testing tip. This bridges the gap between a test failure and a developer fix.

5. **Fail-fast vs report-only** — `accessibility.fail.on.critical` in properties lets teams choose: fail the build on critical violations (strict mode) or log everything without failing (audit mode). Most teams start with audit mode and tighten over time.

6. **Real defect found** — Applied to kibeam.com, the scanner found a missing alt text on an image uploaded through Shopify CMS. The image also caused a viewport overflow. Two violations from one real-world CMS operation — this is exactly what automated accessibility testing is for.

7. **WCAG 2.1 Level AA** — The standard required by WCAG, most accessibility laws (ADA, EN 301 549, AODA), and most enterprise procurement requirements. Not WCAG 2.0 or 3.0 — Level AA of 2.1 is the current practical standard.

8. **Portable by design** — The only input is a URL. No page objects, no site-specific selectors, no knowledge of the application structure. This makes it trivially applicable to any project as a reusable assessment tool.
