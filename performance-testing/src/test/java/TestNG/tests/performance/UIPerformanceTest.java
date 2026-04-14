package TestNG.tests.performance;

import base.BaseTestTestNG;
import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import performance.LighthouseRunner;
import performance.LighthouseRunner.LighthouseResult;
import performance.NetworkProfiler;
import performance.NetworkProfiler.NetworkMetrics;
import performance.PerformanceReporter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI performance testing examples.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Page load time measurement</li>
 *   <li>Navigation timing API usage</li>
 *   <li>Lighthouse integration (if installed)</li>
 *   <li>HAR capture and analysis</li>
 * </ul>
 *
 * <h2>Run:</h2>
 * <pre>
 * mvn test -P ui-performance
 * </pre>
 */
@Feature("UI Performance")
@Test(groups = {"performance", "ui-performance"})
public class UIPerformanceTest extends BaseTestTestNG {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    // Test URLs - using public sites for demonstration
    private static final String DEMO_URL = "https://the-internet.herokuapp.com";
    private static final long MAX_PAGE_LOAD_TIME_MS = 5000;
    private static final long MAX_DOM_CONTENT_LOADED_MS = 3000;

    @Story("Page Load Timing")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Measure page load time using Navigation Timing API")
    public void testPageLoadTime() {
        Allure.step("Navigate to home page");
        driver.get(DEMO_URL);

        Allure.step("Capture navigation timing metrics via Navigation Timing Level 2 API");
        // All values from getNavigationTiming() are relative to startTime = 0
        // (the navigation start). No subtraction from navigationStart is needed —
        // loadEventEnd and domContentLoadedEventEnd are already page-load durations.
        Map<String, Object> timing = getNavigationTiming();

        long loadEventEnd        = ((Number) timing.get("loadEventEnd")).longValue();
        long domContentLoaded    = ((Number) timing.get("domContentLoadedEventEnd")).longValue();
        long domainLookupStart   = ((Number) timing.get("domainLookupStart")).longValue();
        long domainLookupEnd     = ((Number) timing.get("domainLookupEnd")).longValue();
        long connectStart        = ((Number) timing.get("connectStart")).longValue();
        long connectEnd          = ((Number) timing.get("connectEnd")).longValue();
        long requestStart        = ((Number) timing.get("requestStart")).longValue();
        long responseStart       = ((Number) timing.get("responseStart")).longValue();

        long pageLoadTime        = loadEventEnd;         // relative to navigation start
        long domContentLoadedTime = domContentLoaded;    // relative to navigation start
        long dnsLookup           = domainLookupEnd - domainLookupStart;
        long tcpConnection       = connectEnd - connectStart;
        long ttfb                = responseStart - requestStart;

        // Log to Extent Report with rich visualization
        PerformanceReporter.logUITimingMetrics(
                "Home Page",
                pageLoadTime,
                domContentLoadedTime,
                ttfb,
                dnsLookup,
                tcpConnection
        );

        log.info("Page Load Time: {}ms", pageLoadTime);
        log.info("DOM Content Loaded: {}ms", domContentLoadedTime);

        Allure.step("Assert load time under " + MAX_PAGE_LOAD_TIME_MS + "ms threshold");
        assertThat(pageLoadTime)
                .as("Page load time should be under " + MAX_PAGE_LOAD_TIME_MS + "ms")
                .isLessThan(MAX_PAGE_LOAD_TIME_MS);

        assertThat(domContentLoadedTime)
                .as("DOM Content Loaded should be under " + MAX_DOM_CONTENT_LOADED_MS + "ms")
                .isLessThan(MAX_DOM_CONTENT_LOADED_MS);
    }

    @Story("Detailed Timing Breakdown")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Measure detailed timing breakdown")
    public void testDetailedTimingBreakdown() {
        Allure.step("Navigate to login page");
        driver.get(DEMO_URL + "/login");

        Allure.step("Capture full timing waterfall: DNS, TCP, TTFB, DOM, load");
        // All values are relative to startTime = 0 (navigation start).
        // Delta metrics (DNS, TCP, TTFB) subtract two relative timestamps — unchanged.
        // pageLoadTime and domContentLoadedTime are direct values, no subtraction needed.
        Map<String, Object> timing = getNavigationTiming();

        long domainLookupStart    = ((Number) timing.get("domainLookupStart")).longValue();
        long domainLookupEnd      = ((Number) timing.get("domainLookupEnd")).longValue();
        long connectStart         = ((Number) timing.get("connectStart")).longValue();
        long connectEnd           = ((Number) timing.get("connectEnd")).longValue();
        long requestStart         = ((Number) timing.get("requestStart")).longValue();
        long responseStart        = ((Number) timing.get("responseStart")).longValue();
        long domContentLoaded     = ((Number) timing.get("domContentLoadedEventEnd")).longValue();
        long loadEventEnd         = ((Number) timing.get("loadEventEnd")).longValue();

        // Calculate metrics
        long dnsLookup            = domainLookupEnd - domainLookupStart;
        long tcpConnection        = connectEnd - connectStart;
        long ttfb                 = responseStart - requestStart;  // Time To First Byte
        long pageLoadTime         = loadEventEnd;      // relative to navigation start
        long domContentLoadedTime = domContentLoaded;  // relative to navigation start

        // Log to Extent Report with rich visualization
        PerformanceReporter.logUITimingMetrics(
                "/login",
                pageLoadTime,
                domContentLoadedTime,
                ttfb,
                dnsLookup,
                tcpConnection
        );

        log.info("=== Performance Timing Breakdown ===");
        log.info("DNS Lookup: {}ms", dnsLookup);
        log.info("TCP Connection: {}ms", tcpConnection);
        log.info("Time To First Byte (TTFB): {}ms", ttfb);
        log.info("DOM Content Loaded: {}ms", domContentLoadedTime);
        log.info("Page Load: {}ms", pageLoadTime);

        Allure.step("Assert TTFB is under 1000ms");
        // Assert TTFB is reasonable
        assertThat(ttfb)
                .as("TTFB should be under 1000ms")
                .isLessThan(1000);
    }

    @Story("Page Load Timing")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Measure multiple page navigations")
    public void testMultiplePagePerformance() {
        String[] pages = {
                "/login",
                "/checkboxes",
                "/dropdown",
                "/inputs"
        };

        Allure.step("Navigate through " + pages.length + " pages and capture timing for each");
        for (String page : pages) {
            driver.get(DEMO_URL + page);

            Map<String, Object> timing = getNavigationTiming();

            long pageLoadTime     = ((Number) timing.get("loadEventEnd")).longValue();
            long domContentLoaded = ((Number) timing.get("domContentLoadedEventEnd")).longValue();
            long dnsLookup        = ((Number) timing.get("domainLookupEnd")).longValue()
                                  - ((Number) timing.get("domainLookupStart")).longValue();
            long tcpConnection    = ((Number) timing.get("connectEnd")).longValue()
                                  - ((Number) timing.get("connectStart")).longValue();
            long ttfb             = ((Number) timing.get("responseStart")).longValue()
                                  - ((Number) timing.get("requestStart")).longValue();

            PerformanceReporter.logUITimingMetrics(page, pageLoadTime, domContentLoaded,
                    ttfb, dnsLookup, tcpConnection);

            log.info("Page {} loaded in {}ms", page, pageLoadTime);

            assertThat(pageLoadTime)
                    .as("Page " + page + " should load under " + MAX_PAGE_LOAD_TIME_MS + "ms")
                    .isLessThan(MAX_PAGE_LOAD_TIME_MS);
        }
    }

    @Story("Core Web Vitals")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Test First Contentful Paint")
    public void testFirstContentfulPaint() {
        Allure.step("Navigate to home page");
        driver.get(DEMO_URL);

        Allure.step("Capture navigation timing waterfall");
        // Navigation timing waterfall — gives context for where FCP falls in the load
        Map<String, Object> timing = getNavigationTiming();
        long pageLoadTime     = ((Number) timing.get("loadEventEnd")).longValue();
        long domContentLoaded = ((Number) timing.get("domContentLoadedEventEnd")).longValue();
        long dnsLookup        = ((Number) timing.get("domainLookupEnd")).longValue()
                              - ((Number) timing.get("domainLookupStart")).longValue();
        long tcpConnection    = ((Number) timing.get("connectEnd")).longValue()
                              - ((Number) timing.get("connectStart")).longValue();
        long ttfb             = ((Number) timing.get("responseStart")).longValue()
                              - ((Number) timing.get("requestStart")).longValue();

        PerformanceReporter.logUITimingMetrics("Home Page", pageLoadTime, domContentLoaded,
                ttfb, dnsLookup, tcpConnection);

        Allure.step("Extract First Contentful Paint from Performance API paint entries");
        // Get paint timing
        @SuppressWarnings("unchecked")
        var paintEntries = (java.util.List<Map<String, Object>>) ((JavascriptExecutor) driver)
                .executeScript("return performance.getEntriesByType('paint')");

        Long fcp = null;
        for (Map<String, Object> entry : paintEntries) {
            if ("first-contentful-paint".equals(entry.get("name"))) {
                fcp = ((Number) entry.get("startTime")).longValue();
                break;
            }
        }

        Allure.step("Assert FCP under 2000ms threshold");
        if (fcp != null) {
            PerformanceReporter.logCoreWebVitals("Home Page", fcp, null, null, null);
            log.info("First Contentful Paint: {}ms", fcp);
            assertThat(fcp)
                    .as("FCP should be under 2000ms")
                    .isLessThan(2000);
        } else {
            log.warn("FCP not available in this browser");
        }
    }

    @Story("Core Web Vitals")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Test Largest Contentful Paint")
    public void testLargestContentfulPaint() {
        Allure.step("Navigate to home page");
        driver.get(DEMO_URL);

        Allure.step("Capture navigation timing waterfall before waiting for LCP");
        // Navigation timing waterfall — captured immediately after load before the sleep
        Map<String, Object> timing = getNavigationTiming();
        long pageLoadTime     = ((Number) timing.get("loadEventEnd")).longValue();
        long domContentLoaded = ((Number) timing.get("domContentLoadedEventEnd")).longValue();
        long dnsLookup        = ((Number) timing.get("domainLookupEnd")).longValue()
                              - ((Number) timing.get("domainLookupStart")).longValue();
        long tcpConnection    = ((Number) timing.get("connectEnd")).longValue()
                              - ((Number) timing.get("connectStart")).longValue();
        long ttfb             = ((Number) timing.get("responseStart")).longValue()
                              - ((Number) timing.get("requestStart")).longValue();

        PerformanceReporter.logUITimingMetrics("Home Page", pageLoadTime, domContentLoaded,
                ttfb, dnsLookup, tcpConnection);

        // Wait a moment for the page to fully render and LCP to be recorded
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Allure.step("Observe Largest Contentful Paint via PerformanceObserver with buffered entries");
        // Get LCP using PerformanceObserver with buffered entries
        Number lcpValue = (Number) ((JavascriptExecutor) driver).executeScript(
                "return new Promise(resolve => { " +
                        "  try { " +
                        "    new PerformanceObserver(list => { " +
                        "      const entries = list.getEntries(); " +
                        "      if (entries.length > 0) { " +
                        "        resolve(entries[entries.length - 1].startTime); " +
                        "      } else { " +
                        "        resolve(null); " +
                        "      } " +
                        "    }).observe({type: 'largest-contentful-paint', buffered: true}); " +
                        "    setTimeout(() => resolve(null), 3000); " +
                        "  } catch (e) { " +
                        "    resolve(null); " +
                        "  } " +
                        "});"
        );

        Allure.step("Assert LCP under 2500ms (Core Web Vitals threshold)");
        if (lcpValue != null) {
            long lcp = lcpValue.longValue();
            PerformanceReporter.logCoreWebVitals("Home Page", null, lcp, null, null);
            log.info("Largest Contentful Paint: {}ms", lcp);
            assertThat(lcp)
                    .as("LCP should be under 2500ms (Core Web Vitals threshold)")
                    .isLessThan(2500);
        } else {
            log.warn("LCP metric not available - browser may not support PerformanceObserver for LCP");
        }
    }

    @Story("Time to First Byte")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Lighthouse performance audit", enabled = false)
    public void testLighthouseAudit() throws IOException, InterruptedException {
        Allure.step("Check if Lighthouse CLI is installed");
        // Skip if Lighthouse not installed
        if (!LighthouseRunner.isInstalled()) {
            log.warn("Lighthouse not installed, skipping test. Install with: npm install -g lighthouse");
            return;
        }

        Allure.step("Run Lighthouse desktop audit against " + DEMO_URL);
        LighthouseResult result = new LighthouseRunner()
                .desktop()
                .withHeadless(true)
                .runAudit(DEMO_URL);

        // Log to Extent Report with rich visualization
        PerformanceReporter.logLighthouseResults(result);

        log.info("Lighthouse Results:");
        log.info("Performance Score: {}", result.performanceScore);
        log.info("Accessibility Score: {}", result.accessibilityScore);
        log.info("Best Practices Score: {}", result.bestPracticesScore);
        log.info("SEO Score: {}", result.seoScore);
        log.info("HTML Report: {}", result.htmlReportPath);

        Allure.step("Assert performance score >= 70");
        assertThat(result.performanceScore)
                .as("Performance score should be at least 70")
                .isGreaterThanOrEqualTo(70);
    }

    @Story("Lighthouse Audit")
    @Severity(SeverityLevel.MINOR)
    @Test(description = "Lighthouse mobile performance", enabled = false)
    public void testLighthouseMobile() throws IOException, InterruptedException {
        Allure.step("Check if Lighthouse CLI is installed");
        if (!LighthouseRunner.isInstalled()) {
            log.warn("Lighthouse not installed, skipping test");
            return;
        }

        Allure.step("Run Lighthouse mobile audit against " + DEMO_URL);
        LighthouseResult result = new LighthouseRunner()
                .mobile()
                .withHeadless(true)
                .runAudit(DEMO_URL);

        log.info("Mobile Performance Score: {}", result.performanceScore);
        log.info("Mobile LCP: {}ms", result.largestContentfulPaint);

        Allure.step("Assert mobile performance score >= 50");
        // Mobile scores tend to be lower
        assertThat(result.performanceScore)
                .as("Mobile performance score should be at least 50")
                .isGreaterThanOrEqualTo(50);
    }

    // ==================== Helper Methods ====================

    /**
     * Returns the first Navigation Timing Level 2 entry as a property map.
     *
     * WHY NOT performance.timing.toJSON():
     * performance.timing is deprecated in all modern browsers and removed from
     * the spec. Its replacement is performance.getEntriesByType('navigation')[0],
     * a PerformanceNavigationTiming object (Navigation Timing Level 2).
     *
     * KEY DIFFERENCE IN TIMESTAMP SEMANTICS:
     * performance.timing returned absolute epoch timestamps (ms since Unix epoch),
     * so durations required subtraction: loadEventEnd - navigationStart.
     *
     * PerformanceNavigationTiming returns DOMHighResTimeStamps relative to the
     * navigation start (startTime = 0). Every property is already a duration from
     * the moment navigation began — loadEventEnd IS the page load time directly.
     * Delta calculations (DNS, TCP, TTFB) are unchanged: they subtract two relative
     * timestamps, which works identically in both APIs.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNavigationTiming() {
        Map<String, Object> entry = (Map<String, Object>) ((JavascriptExecutor) driver)
                .executeScript(
                        "var nav = performance.getEntriesByType('navigation');" +
                        "return nav.length ? nav[0].toJSON() : null;");
        if (entry == null) {
            throw new IllegalStateException(
                    "No navigation entry found — page may not have fully loaded");
        }
        return entry;
    }

    /**
     * Returns page load time from a Navigation Timing Level 2 entry.
     *
     * loadEventEnd is already relative to startTime (= 0 = navigation start),
     * so it equals the total page load duration with no further subtraction needed.
     */
    private long calculatePageLoadTime(Map<String, Object> timing) {
        return ((Number) timing.get("loadEventEnd")).longValue();
    }
}
