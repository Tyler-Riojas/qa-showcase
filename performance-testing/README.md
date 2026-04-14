# Performance Testing Showcase

Comprehensive performance testing suite demonstrating UI metrics, network analysis, device emulation, and API load testing.

---

## What This Demonstrates

| Capability | Tools | Target |
|---|---|---|
| UI timing metrics (FCP, LCP, TTFB, load) | Selenium + Navigation Timing Level 2 | the-internet.herokuapp.com |
| Network HAR capture and throttling | BrowserMob Proxy | the-internet.herokuapp.com |
| Device emulation across 11 profiles | Chrome DevTools Protocol | the-internet.herokuapp.com |
| API load testing with JMeter DSL | us.pe.dsl:jmeter-java-dsl | Restful Booker (localhost:3001) |
| K6 script-based load testing | K6 CLI | Restful Booker (localhost:3001) |
| Allure reporting with steps and severity | Allure TestNG | All suites |

---

## Quick Start

### Path A — No Setup Required

Tests UI and network performance against a live public website. Just run:

```bash
cd performance-testing
mvn test -Pperformance-smoke
```

This runs `UIPerformanceTest` and `NetworkPerformanceTest` against `https://the-internet.herokuapp.com`.

Optionally headless:

```bash
mvn test -Pperformance-smoke -Dheadless=true
```

### Path B — With Docker (API Load Tests)

Spin up the Restful Booker API locally, then run load tests:

```bash
# Start the API server
docker run -d -p 3001:3001 mwinteringham/restfulbooker

# Run JMeter and K6 load tests
mvn test -Pload-tests
```

For device emulation tests:

```bash
mvn test -Pdevice-performance
```

---

## Project Structure

```
performance-testing/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── base/               # BaseTestTestNG (ThreadLocal WebDriver)
│   │   │   ├── config/             # Configuration, FeatureToggle
│   │   │   ├── core/               # DriverFactory
│   │   │   ├── enums/              # BrowserType
│   │   │   ├── listeners/          # ExtentTestNGListener, AnnotationTransformer, RetryAnalyzer
│   │   │   ├── performance/        # JMeterRunner, K6Runner, NetworkProfiler, PerformanceReporter
│   │   │   │                       # DevicePerformanceConfig, LighthouseRunner
│   │   │   └── utils/              # WaitUtils, ScreenshotUtils, ExtentReportManager
│   │   └── resources/
│   │       ├── config/
│   │       │   └── test.properties
│   │       └── logback.xml
│   └── test/
│       ├── java/
│       │   └── TestNG/tests/performance/
│       │       ├── UIPerformanceTest.java
│       │       ├── NetworkPerformanceTest.java
│       │       ├── DevicePerformanceTest.java
│       │       └── ApiLoadTest.java
│       └── resources/
│           ├── allure.properties
│           ├── environment.properties
│           ├── testng-performance-smoke.xml
│           ├── testng-load-tests.xml
│           ├── testng-device-performance.xml
│           └── k6/
│               ├── booking-load-test.js
│               ├── booking-stress-test.js
│               └── booking-spike-test.js
└── pom.xml
```

---

## The Four Performance Layers

### 1. UI Performance (UIPerformanceTest)

Measures real browser paint and load timing using the Navigation Timing Level 2 API (`performance.getEntriesByType('navigation')[0]`). Captures:

- **TTFB** — Time to First Byte: how fast the server responds
- **FCP** — First Contentful Paint: when the user sees the first pixel
- **LCP** — Largest Contentful Paint: when the main content is visible
- **DOM Interactive / DOM Complete** — JavaScript parse and render milestones
- **Full page load time** — `loadEventEnd - startTime`

Thresholds relax automatically under CI (`-Dci=true`) to account for shared runner latency.

### 2. Network Performance (NetworkPerformanceTest)

Uses BrowserMob Proxy to intercept all browser traffic and write a HAR file. Analyzes:

- P95 response time across all sub-resources (HTML, CSS, JS, images, fonts)
- Resource type breakdown by count and total payload size
- Top 5 slowest requests and top 5 largest resources
- HTTP status code distribution and success rate
- Simulated throttling: Slow 2G, Regular 3G, Regular 4G, No throttle

### 3. Device Performance (DevicePerformanceTest)

Combines Chrome DevTools Protocol device emulation with BrowserMob network throttling to simulate real user conditions. Tests 11 device/network combinations including:

- iPhone 14 Pro on 3G and 4G
- iPad on WiFi
- Desktop on cable
- Worst case: oldest mobile device on Slow 2G (50 Kbps / 2000 ms latency)

Page load timeouts are calibrated per-profile to avoid spurious infrastructure failures vs. genuine slow-network behaviour.

### 4. API Load Testing (ApiLoadTest)

Two load testing engines against the Restful Booker REST API:

**JMeter DSL** — programmatic Java API for JMeter test plans. Runs:
- Simple GET load tests (`/booking`, `/booking/{id}`, `/ping`)
- POST write load test (`/booking`)
- Multi-endpoint combined test
- Stress test (ramping threads)
- Spike test (sudden burst)

**K6** — modern JavaScript-based load testing. Three scripts:
- `booking-load-test.js` — steady load, P95 < 500 ms threshold
- `booking-stress-test.js` — ramping stress, error rate < 1%
- `booking-spike-test.js` — spike burst, P95 < 1500 ms tolerance

---

## Test Coverage

### UIPerformanceTest

| Test | What It Measures | Assertion |
|---|---|---|
| testPageLoadTime | Full page load via Navigation Timing L2 | < threshold ms |
| testDetailedTimingBreakdown | TTFB, DNS, TCP, DOM milestones | Logged, no hard fail |
| testMultiplePagePerformance | Load time across multiple pages | Logged comparison |
| testFirstContentfulPaint | FCP via PerformanceObserver | < 3000 ms |
| testLargestContentfulPaint | LCP via PerformanceObserver | < 4000 ms |

### NetworkPerformanceTest

| Test | What It Measures | Assertion |
|---|---|---|
| testHarCapture | Full HAR + P95 response time | P95 < 2000 ms |
| testWith3GThrottling | Load time under 3G (1.6 Mbps) | < 10000 ms |
| testWithSlow2GThrottling | Load time under Slow 2G (50 Kbps) | < 30000 ms |
| testWith4GThrottling | Load time under 4G (20 Mbps) | < 5000 ms |
| testCompareNetworkProfiles | Side-by-side comparison across 4 profiles | Logged |
| testResourceTypeAnalysis | Breakdown by resource type | Logged |
| testSlowestRequests | Top 5 slowest URLs | Logged |
| testLargestResources | Top 5 largest payloads | Logged |
| testStatusCodeDistribution | HTTP status counts + success rate | Logged |

### DevicePerformanceTest

| Test | Scenario | Assertion |
|---|---|---|
| testMobileOn3G | iPhone + 3G | Load time logged |
| testMobileOn4G | iPhone + 4G | Load time logged |
| testTabletOnWifi | iPad + WiFi | Load time logged |
| testDesktopFast | Desktop + Fast | Load time logged |
| testWorstCaseMobile | Mobile + Slow 2G | < 60000 ms |
| testAllMobileDevices | 5 phones × 4G | Logged comparison |
| testAllNetworkProfilesOnIPhone | iPhone 14 Pro × 4 profiles | Logged comparison |
| testQuickScenarios | CI-friendly: Mobile 4G, Tablet WiFi, Desktop Cable | Mobile < 5000 ms, Desktop < 3000 ms |
| testViewportResponsiveness | iPhone SE, iPad, Desktop HD | Content fits viewport |
| testWithDataProvider | Data-driven: 3 scenarios | < 10000 ms each |

### ApiLoadTest

| Test | Tool | Assertion |
|---|---|---|
| testGetBookingsLoad | JMeter | Error < 1%, P95 < 500 ms, throughput > 50 req/s |
| testGetBookingByIdLoad | JMeter | Same thresholds |
| testCreateBookingLoad | JMeter | Error < 1%, P95 < 500 ms (write, no throughput) |
| testMultiEndpointLoad | JMeter | Same thresholds across 3 endpoints |
| testStressTest | JMeter | Error < 1% at peak load |
| testSpikeTest | JMeter | Error < 1% during spike |
| testK6SimpleLoad | K6 | Exit 0, error < 1%, P95 < 500 ms |
| testK6StressTest | K6 | Exit 0, error < 1%, P95 < 500 ms |
| testK6SpikeTest | K6 | Exit 0, error < 1%, P95 < 1500 ms |
| testPerformanceThresholds | JMeter | Error < 1%, P95 < 500 ms, throughput > 50 req/s |

---

## Run Commands

```bash
# UI + Network performance (no Docker)
mvn test -Pperformance-smoke

# UI + Network headless
mvn test -Pperformance-smoke -Dheadless=true

# API load tests (requires Docker)
mvn test -Pload-tests

# API load tests against public Heroku demo
mvn test -Pload-tests -Dapi.base.url=https://restful-booker.herokuapp.com

# Device performance tests
mvn test -Pdevice-performance

# Single test class
mvn test -Dtest=UIPerformanceTest

# Single test method
mvn test -Dtest=UIPerformanceTest#testFirstContentfulPaint

# CI mode (relaxed thresholds)
mvn test -Pperformance-smoke -Dci=true -Dheadless=true
```

---

## Docker Setup (Load Tests)

```bash
# Pull and start
docker run -d -p 3001:3001 mwinteringham/restfulbooker

# Verify
curl http://localhost:3001/ping
# → 201 Created

# Stop and remove
docker stop $(docker ps -q --filter ancestor=mwinteringham/restfulbooker)
```

The `@AfterClass` in `ApiLoadTest` automatically deletes all test bookings after each run when targeting `localhost`. The container does not need a restart between runs unless `testCreateBookingLoad` was executed (that test creates hundreds of bookings).

---

## K6 Setup

```bash
# macOS
brew install k6

# Verify
k6 version

# K6 tests run automatically as part of -Pload-tests
# Skip K6 tests if k6 is not installed — they soft-skip via K6Runner.isInstalled()
```

---

## Real Data From Test Runs

Device performance results on the-internet.herokuapp.com measured locally:

| Scenario | Load Time | Grade |
|---|---|---|
| Desktop Cable | ~800 ms | A |
| Tablet WiFi | ~1185 ms | A |
| Mobile 4G | ~1605 ms | B |
| Mobile 3G | ~4200 ms | C |
| Mobile Slow 3G | ~11424 ms | F |

Key insight: **latency beats bandwidth**. The jump from 4G (~1600 ms) to 3G (~4200 ms) is driven primarily by higher round-trip latency (150 ms vs 40 ms per hop) rather than bandwidth reduction alone. A page with 30 sequential requests multiplies that latency difference into seconds of perceived delay — the same reason HTTP/2 multiplexing was invented.

---

## Allure Reports

```bash
# Generate and open Allure report
mvn allure:serve

# Or generate static report
mvn allure:report
# Open: target/site/allure-maven-plugin/index.html
```

Tests are annotated with:
- `@Feature` — groups tests by layer (UI Performance, Network Performance, Device Performance, API Load Testing)
- `@Story` — groups by scenario type (Page Load Timing, Core Web Vitals, HAR Capture, Network Throttling, etc.)
- `@Severity` — CRITICAL for load/FCP/LCP, NORMAL for most, MINOR for Lighthouse and worst-case slow network
- `Allure.step()` — each test records measurable checkpoints visible in the report timeline

---

## Tech Stack

| Category | Technology | Version |
|---|---|---|
| Browser Automation | Selenium WebDriver | 4.x |
| Test Framework | TestNG | 7.11.0 |
| Browser Management | WebDriverManager | 6.x |
| Performance Capture | Navigation Timing Level 2 API | Browser native |
| Network Proxy | BrowserMob Proxy | 2.1.5 |
| Load Testing (Java) | JMeter Java DSL | 1.29.x |
| Load Testing (Script) | K6 | Latest |
| Assertions | AssertJ | 3.x |
| Reporting (HTML) | ExtentReports | 5.x |
| Reporting (Allure) | Allure TestNG | 2.32.0 |
| Logging | SLF4J + Logback | 2.x / 1.5.x |
| Build | Maven | 3.6+ |
| Java | OpenJDK | 17+ |

---

## Interview Talking Points

1. **Two measurement planes** — Navigation Timing Level 2 measures what the browser experiences end-to-end; BrowserMob HAR capture measures every individual sub-resource the browser fetched. Together they identify whether a slow page load is one slow server response or many small requests adding up.

2. **Calibrated timeouts, not magic numbers** — `DevicePerformanceTest` derives `pageLoadTimeout` from actual network speed: `SLOW_2G` at 50 Kbps needs 90 seconds before the test declares a failure; `REGULAR_4G` at 20 Mbps needs 20 seconds. A fixed 30-second timeout would either produce false failures on slow networks or hide real hangs on fast ones.

3. **Latency vs bandwidth insight** — The device test results show that the Mobile 3G to Mobile 4G gap is mostly latency (40 ms vs 150 ms per hop), not raw bandwidth. This is why HTTP/2 multiplexing, CDN edge caching, and connection keep-alive matter more than simply upgrading bandwidth.

4. **Two load testing engines** — JMeter DSL gives Java-native control (fluent builder, typed results, JUnit-style assertions). K6 gives a JavaScript scripting layer where thresholds live in the script alongside the virtual user logic — closer to how real teams define SLOs. Knowing both shows awareness of trade-offs: JMeter integrates with CI without extra binaries; K6 is more expressive for complex ramping scenarios.

5. **Environment-aware thresholds** — P95 and throughput assertions change automatically when `-Dci=true` is passed. Local: P95 < 500 ms, throughput > 50 req/s. CI: P95 < 2000 ms, throughput > 20 req/s. This prevents flaky failures on shared GitHub Actions runners without hiding real regressions in local development.

6. **Cleanup discipline** — `ApiLoadTest.@AfterClass` deletes every booking it created, but only against `localhost`. It never wipes shared or staging environments. This makes the suite safe to run repeatedly without accumulating test data or degrading `GET /booking` performance over time.

7. **Soft K6 skip** — If K6 is not installed, the K6 tests log a warning and return without failing. This lets the CI pipeline run the JMeter tests on any machine without requiring K6 to be pre-installed, while still running K6 tests in environments where it is available.

8. **Allure steps as audit trail** — `Allure.step()` calls create a timeline inside each test report entry: navigate, capture, assert. This means a failing test shows exactly which step failed and what the measured value was, without needing to read logs or add print statements.
