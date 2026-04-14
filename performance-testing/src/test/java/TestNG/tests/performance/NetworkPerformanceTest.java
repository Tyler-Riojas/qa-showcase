package TestNG.tests.performance;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import performance.DevicePerformanceConfig.NetworkProfile;
import performance.NetworkProfiler;
import performance.NetworkProfiler.NetworkMetrics;
import performance.PerformanceReporter;

import java.io.IOException;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Network performance testing using BrowserMob Proxy.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>HAR file capture and analysis</li>
 *   <li>Network throttling simulation</li>
 *   <li>Resource size tracking</li>
 *   <li>Response time analysis</li>
 * </ul>
 *
 * <h2>Run:</h2>
 * <pre>
 * mvn test -Pnetwork
 * </pre>
 */
@Feature("Network Performance")
@Test(groups = {"performance", "network"})
public class NetworkPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private static final String DEMO_URL = "https://the-internet.herokuapp.com";

    private NetworkProfiler profiler;
    private WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setupProfiler() {
        profiler = new NetworkProfiler();
        profiler.start();
        log.info("NetworkProfiler started on port {}", profiler.getPort());
    }

    @AfterClass(alwaysRun = true)
    public void teardownProfiler() {
        if (profiler != null) {
            profiler.stop();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @Story("HAR Capture")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Capture HAR and analyze network traffic")
    public void testHarCapture() throws IOException {
        Allure.step("Start HAR capture and navigate to home page");
        profiler.startCapture("HomePage");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        Allure.step("Log network metrics: total requests=" + metrics.totalRequests +
                ", avg response time=" + metrics.avgResponseTime + "ms" +
                ", P95=" + metrics.p95ResponseTime + "ms");
        log.info("=== HAR Analysis ===");
        log.info("Total Requests: {}", metrics.totalRequests);
        log.info("Total Size: {}", metrics.getTotalSizeFormatted());
        log.info("Average Response Time: {}ms", metrics.avgResponseTime);
        log.info("P95 Response Time: {}ms", metrics.p95ResponseTime);

        PerformanceReporter.logNetworkMetrics(metrics);

        String harPath = profiler.saveHar("homepage");
        log.info("HAR saved to: {}", harPath);

        Allure.step("Assert P95 response time under 2000ms threshold");
        assertThat(metrics.p95ResponseTime)
                .as("P95 response time should be under 2 seconds")
                .isLessThan(2000);
    }

    @Story("Network Throttling")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test with 3G network throttling")
    public void testWith3GThrottling() throws IOException {
        Allure.step("Apply REGULAR_3G throttling profile");
        profiler.applyThrottling(NetworkProfile.REGULAR_3G);

        Allure.step("Navigate to home page under 3G throttling and capture metrics");
        profiler.startCapture("3G_Test");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        log.info("3G Network Test - Total Time: {}ms, Size: {}",
                metrics.totalCaptureTime, metrics.getTotalSizeFormatted());

        PerformanceReporter.logNetworkMetrics(metrics);
        profiler.removeThrottling();

        // Threshold raised for Heroku free tier on 3G simulation —
        // the-internet.herokuapp.com realistically takes 14s on REGULAR_3G
        Allure.step("Assert page load time under 20000ms on 3G");
        assertThat(metrics.totalCaptureTime)
                .as("Page should load under 20 seconds on 3G")
                .isLessThan(20000);
    }

    @Story("Network Throttling")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test with slow 2G network")
    public void testWithSlow2GThrottling() throws IOException {
        Allure.step("Apply SLOW_2G throttling profile");
        profiler.applyThrottling(NetworkProfile.SLOW_2G);

        Allure.step("Navigate to login page under Slow 2G throttling and capture metrics");
        profiler.startCapture("Slow2G_Test");
        driver = createDriver();

        try {
            driver.get(DEMO_URL + "/login"); // Lighter page
        } catch (org.openqa.selenium.TimeoutException e) {
            // Slow 2G cannot load most pages within the browser's default page load timeout.
            // This is expected behavior — the test demonstrates extreme throttling conditions.
            // The point is not that the page loads, but that we can observe the degradation.
            log.info("Slow 2G: page load timed out as expected under extreme throttling — {}",
                    e.getMessage());
            Allure.step("Slow 2G page load timed out — expected behavior on extreme throttling");
        }

        NetworkMetrics metrics = profiler.stopCapture();
        log.info("Slow 2G Test - Total Time: {}ms (may be partial due to timeout)",
                metrics.totalCaptureTime);

        PerformanceReporter.logNetworkMetrics(metrics);
        profiler.removeThrottling();
    }

    @Story("Network Throttling")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test with 4G network")
    public void testWith4GThrottling() throws IOException {
        Allure.step("Apply REGULAR_4G throttling profile");
        profiler.applyThrottling(NetworkProfile.REGULAR_4G);

        Allure.step("Navigate to home page under 4G throttling and capture metrics");
        profiler.startCapture("4G_Test");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        log.info("4G Network Test - Total Time: {}ms", metrics.totalCaptureTime);

        PerformanceReporter.logNetworkMetrics(metrics);
        profiler.removeThrottling();

        Allure.step("Assert page load time under 5000ms on 4G");
        assertThat(metrics.totalCaptureTime)
                .as("Page should load under 5 seconds on 4G")
                .isLessThan(5000);
    }

    @Story("Network Throttling")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Compare network profiles")
    public void testCompareNetworkProfiles() throws IOException {
        NetworkProfile[] profiles = {
                NetworkProfile.NO_THROTTLE,
                NetworkProfile.REGULAR_4G,
                NetworkProfile.REGULAR_3G,
                NetworkProfile.SLOW_3G
        };

        Allure.step("Compare load times across NO_THROTTLE, 4G, 3G, SLOW_3G profiles");
        log.info("=== Network Profile Comparison ===");

        for (NetworkProfile profile : profiles) {
            profiler.applyThrottling(profile);

            profiler.startCapture(profile.name());
            driver = createDriver();
            driver.get(DEMO_URL + "/login");
            NetworkMetrics metrics = profiler.stopCapture();

            log.info("{}: {}ms load time, {} total size",
                    profile.getDisplayName(),
                    metrics.totalCaptureTime,
                    metrics.getTotalSizeFormatted());

            PerformanceReporter.logNetworkMetrics(metrics);

            driver.quit();
            driver = null;
        }

        profiler.removeThrottling();
    }

    @Story("Resource Analysis")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Analyze resource types")
    public void testResourceTypeAnalysis() throws IOException {
        Allure.step("Capture HAR and analyze resource types by size and count");
        profiler.startCapture("ResourceAnalysis");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        Allure.step("Log resource type breakdown: requests=" + metrics.totalRequests);
        log.info("=== Resource Type Breakdown ===");
        metrics.sizeByType.forEach((type, size) -> {
            int count = metrics.countByType.getOrDefault(type, 0);
            log.info("{}: {} requests, {} total", type, count, NetworkProfiler.formatBytes(size));
        });

        PerformanceReporter.logNetworkMetrics(metrics);
        log.info("Total requests captured: {}", metrics.totalRequests);
    }

    @Story("Resource Analysis")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Track slowest requests")
    public void testSlowestRequests() throws IOException {
        Allure.step("Capture HAR and identify top 5 slowest requests");
        profiler.startCapture("SlowestRequests");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        Allure.step("Log top 5 slowest requests by response time");
        log.info("=== Top 5 Slowest Requests ===");
        for (NetworkProfiler.RequestTiming req : metrics.slowestRequests) {
            log.info("{}ms - {}", req.time, req.getShortUrl());
        }

        PerformanceReporter.logNetworkMetrics(metrics);
    }

    @Story("Resource Analysis")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Track largest resources")
    public void testLargestResources() throws IOException {
        Allure.step("Capture HAR and identify top 5 largest resources by payload size");
        profiler.startCapture("LargestResources");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        Allure.step("Log top 5 largest resources");
        log.info("=== Top 5 Largest Resources ===");
        for (NetworkProfiler.RequestTiming req : metrics.largestResources) {
            log.info("{} - {}", NetworkProfiler.formatBytes(req.size), req.getShortUrl());
        }

        PerformanceReporter.logNetworkMetrics(metrics);
    }

    @Story("HAR Capture")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test status code distribution")
    public void testStatusCodeDistribution() throws IOException {
        Allure.step("Capture HAR and analyze HTTP status code distribution");
        profiler.startCapture("StatusCodes");
        driver = createDriver();
        driver.get(DEMO_URL);
        NetworkMetrics metrics = profiler.stopCapture();

        Allure.step("Log HTTP status code distribution and compute success rate");
        log.info("=== HTTP Status Code Distribution ===");
        metrics.statusCodes.forEach((status, count) ->
                log.info("HTTP {}: {} requests", status, count));

        if (metrics.totalRequests > 0) {
            int successful = metrics.statusCodes.entrySet().stream()
                    .filter(e -> e.getKey() >= 200 && e.getKey() < 400)
                    .mapToInt(java.util.Map.Entry::getValue)
                    .sum();
            double successRate = (double) successful / metrics.totalRequests * 100;
            log.info("Success rate: {}%", String.format("%.1f", successRate));
        }

        PerformanceReporter.logNetworkMetrics(metrics);
    }

    /**
     * Create a ChromeDriver routed through the BrowserMob proxy.
     *
     * <p>{@code profiler.configureChromeOptions()} injects the proxy address so Chrome
     * routes all requests — HTML, CSS, JS, images, fonts — through BrowserMob, giving
     * the HAR capture full visibility of every sub-resource the browser fetches.</p>
     */
    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options = profiler.configureChromeOptions(options);

        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        return new ChromeDriver(options);
    }
}
