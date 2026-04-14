package TestNG.tests.performance;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import performance.DevicePerformanceConfig;
import performance.DevicePerformanceConfig.*;
import performance.NetworkProfiler;
import performance.NetworkProfiler.NetworkMetrics;
import performance.PerformanceReporter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Device-specific performance testing with network throttling.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Testing across different device viewports</li>
 *   <li>Mobile network simulation</li>
 *   <li>Combined device + network scenarios</li>
 *   <li>Real-world user condition testing</li>
 * </ul>
 *
 * <h2>Run:</h2>
 * <pre>
 * mvn test -P device-performance
 * </pre>
 */
@Feature("Device Performance")
@Test(groups = {"performance", "device"})
public class DevicePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private static final String DEMO_URL = "https://the-internet.herokuapp.com";
    private static final long MAX_MOBILE_LOAD_TIME = 5000;
    private static final long MAX_DESKTOP_LOAD_TIME = 3000;

    private NetworkProfiler profiler;
    private WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setupProfiler() {
        profiler = new NetworkProfiler();
        profiler.start();
    }

    @AfterClass(alwaysRun = true)
    public void teardown() {
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

    @Story("Mobile Devices")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Test mobile device on 3G network")
    public void testMobileOn3G() throws IOException {
        Allure.step("Execute MOBILE_3G scenario: mobile device on 3G network throttling");
        TestScenario scenario = TestScenario.MOBILE_3G;
        performDeviceTest(scenario);
    }

    @Story("Mobile Devices")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Test mobile device on 4G network")
    public void testMobileOn4G() throws IOException {
        Allure.step("Execute MOBILE_4G scenario: mobile device on 4G network throttling");
        TestScenario scenario = TestScenario.MOBILE_4G;
        performDeviceTest(scenario);
    }

    @Story("Tablet Devices")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test tablet on WiFi")
    public void testTabletOnWifi() throws IOException {
        Allure.step("Execute TABLET_WIFI scenario: tablet device on WiFi connection");
        TestScenario scenario = TestScenario.TABLET_WIFI;
        performDeviceTest(scenario);
    }

    @Story("Desktop Devices")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test desktop on fast connection")
    public void testDesktopFast() throws IOException {
        Allure.step("Execute DESKTOP_FAST scenario: desktop on fast connection");
        TestScenario scenario = TestScenario.DESKTOP_FAST;
        performDeviceTest(scenario);
    }

    @Story("Slow Network Conditions")
    @Severity(SeverityLevel.MINOR)
    @Test(description = "Test worst case mobile scenario")
    public void testWorstCaseMobile() throws IOException {
        Allure.step("Execute WORST_CASE_MOBILE scenario: mobile device on Slow 2G (50 Kbps / 2000ms latency)");
        TestScenario scenario = TestScenario.WORST_CASE_MOBILE;

        long loadTime = performDeviceTest(scenario);

        // SLOW_2G = 50 Kbps download / 2000 ms latency.
        // At 50 Kbps a 500 KB page takes ~80 s of transfer alone — 15 s was never
        // achievable. The threshold is set to 60 s: if the page takes longer than
        // that, it indicates an infrastructure problem (proxy not throttling,
        // ChromeDriver hung) rather than expected slow-network behaviour.
        // pageLoadTimeout for this scenario is 90 s (see performDeviceTest),
        // so a genuine timeout records ~90 000 ms and fails this assertion clearly.
        Allure.step("Assert load time under 60000ms (60s) threshold for Slow 2G worst case");
        assertThat(loadTime)
                .as("Page should load within 60 seconds on Slow 2G (50 Kbps / 2000 ms latency)")
                .isLessThan(60_000);
    }

    @Story("Mobile Devices")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Compare all mobile devices")
    public void testAllMobileDevices() throws IOException {
        Allure.step("Iterate all mobile device profiles and measure load time on REGULAR_4G");
        log.info("=== Mobile Device Comparison ===");

        for (DeviceProfile device : DevicePerformanceConfig.getMobileDevices()) {
            profiler.applyThrottling(NetworkProfile.REGULAR_4G);

            driver = createDriverForDevice(device);
            profiler.startCapture(device.name());

            long startTime = System.currentTimeMillis();
            driver.get(DEMO_URL);
            long loadTime = System.currentTimeMillis() - startTime;

            NetworkMetrics metrics = profiler.stopCapture();

            log.info("{} ({}x{}): {}ms load time, {} transferred",
                    device.getDisplayName(),
                    device.getWidth(),
                    device.getHeight(),
                    loadTime,
                    metrics.getTotalSizeFormatted());

            PerformanceReporter.logDevicePerformance(device, NetworkProfile.REGULAR_4G, metrics, loadTime);

            driver.quit();
            driver = null;
        }

        profiler.removeThrottling();
    }

    @Story("Network Profile Comparison")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Compare all network profiles on iPhone")
    public void testAllNetworkProfilesOnIPhone() throws IOException {
        DeviceProfile device = DeviceProfile.IPHONE_14_PRO;

        Allure.step("Compare SLOW_3G, REGULAR_3G, REGULAR_4G, REGULAR_WIFI profiles on " + device.getDisplayName());
        log.info("=== Network Profile Comparison on {} ===", device.getDisplayName());

        NetworkProfile[] profiles = {
                NetworkProfile.SLOW_3G,
                NetworkProfile.REGULAR_3G,
                NetworkProfile.REGULAR_4G,
                NetworkProfile.REGULAR_WIFI
        };

        for (NetworkProfile network : profiles) {
            profiler.applyThrottling(network);

            driver = createDriverForDevice(device);
            profiler.startCapture(network.name());

            long startTime = System.currentTimeMillis();
            driver.get(DEMO_URL + "/login"); // Lighter page
            long loadTime = System.currentTimeMillis() - startTime;

            NetworkMetrics metrics = profiler.stopCapture();

            log.info("{}: {}ms load time",
                    network.getDisplayName(), loadTime);

            PerformanceReporter.logDevicePerformance(device, network, metrics, loadTime);

            driver.quit();
            driver = null;
        }

        profiler.removeThrottling();
    }

    @Story("Mobile Devices")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Quick test scenarios for CI/CD")
    public void testQuickScenarios() throws IOException {
        Allure.step("Run quick test scenarios (MOBILE_4G, TABLET_WIFI, DESKTOP_CABLE) for CI/CD pipeline");
        for (TestScenario scenario : DevicePerformanceConfig.getQuickTestScenarios()) {
            long loadTime = performDeviceTest(scenario);

            long maxTime = scenario.getDevice().getType() == DeviceType.MOBILE
                    ? MAX_MOBILE_LOAD_TIME
                    : MAX_DESKTOP_LOAD_TIME;

            assertThat(loadTime)
                    .as(scenario.getDisplayName() + " should load under " + maxTime + "ms")
                    .isLessThan(maxTime);
        }
    }

    @Story("Desktop Devices")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Test viewport responsiveness")
    public void testViewportResponsiveness() {
        DeviceProfile[] viewports = {
                DeviceProfile.IPHONE_SE,
                DeviceProfile.IPAD,
                DeviceProfile.DESKTOP_HD
        };

        Allure.step("Test viewport responsiveness across iPhone SE, iPad, and Desktop HD profiles");
        for (DeviceProfile viewport : viewports) {
            driver = createDriverForDevice(viewport);
            driver.get(DEMO_URL);

            // Verify actual viewport size
            Dimension actualSize = driver.manage().window().getSize();

            log.info("{}: Target {}x{}, Actual {}x{}",
                    viewport.getDisplayName(),
                    viewport.getWidth(), viewport.getHeight(),
                    actualSize.getWidth(), actualSize.getHeight());

            // Check that content is visible (no horizontal scroll needed)
            Long documentWidth = (Long) ((JavascriptExecutor) driver)
                    .executeScript("return document.documentElement.scrollWidth");

            PerformanceReporter.logViewportResult(viewport,
                    viewport.getWidth(), viewport.getHeight(),
                    actualSize.getWidth(), actualSize.getHeight(),
                    documentWidth);

            assertThat(documentWidth)
                    .as("Content should fit viewport width")
                    .isLessThanOrEqualTo(actualSize.getWidth() + 20); // Small tolerance

            driver.quit();
            driver = null;
        }
    }

    @DataProvider(name = "testScenarios")
    public Object[][] testScenariosProvider() {
        return new Object[][]{
                {TestScenario.MOBILE_4G},
                {TestScenario.TABLET_WIFI},
                {TestScenario.DESKTOP_CABLE}
        };
    }

    @Story("Network Profile Comparison")
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "testScenarios", description = "Data-driven device performance test")
    public void testWithDataProvider(TestScenario scenario) throws IOException {
        Allure.step("Execute data-driven scenario: " + scenario.getDisplayName());
        long loadTime = performDeviceTest(scenario);

        log.info("{}: {} ms", scenario.getDisplayName(), loadTime);

        assertThat(loadTime)
                .as(scenario.getDisplayName() + " load time")
                .isLessThan(10000);
    }

    // ==================== Helper Methods ====================

    /**
     * Perform a device test and return load time.
     */
    private long performDeviceTest(TestScenario scenario) throws IOException {
        DeviceProfile device = scenario.getDevice();
        NetworkProfile network = scenario.getNetwork();

        log.info("Testing {} with {}", device.getDisplayName(), network.getDisplayName());

        // Apply network throttling
        profiler.applyThrottling(network);

        // Create driver with device settings
        driver = createDriverForDevice(device);

        // Set page load timeout proportional to network speed.
        //
        // WHY HERE (not in createDriverForDevice):
        // createDriverForDevice knows only about the device viewport — it has no
        // network context. The timeout must be set where both device and network
        // are known so it can be calibrated to avoid two failure modes:
        //
        //   Too short → ChromeDriver throws TimeoutException on a network that
        //               should legitimately complete (e.g. SLOW_3G at 400 Kbps
        //               timing out at 20 s when the page takes 25 s).
        //
        //   Too long  → Selenium's internal JdkHttpClient (~3 min default) fires
        //               before ChromeDriver, producing an opaque HttpTimeoutException
        //               instead of a clean, measurable TimeoutException.
        //
        // Thresholds are derived from worst-case page size (~500 KB) at each speed:
        //   SLOW_2G   50 Kbps  →  ~80 s transfer + 2000 ms latency/hop  → 90 s
        //   REGULAR_2G 250 Kbps → ~16 s transfer + 1400 ms latency/hop  → 45 s
        //   SLOW_3G  400 Kbps  →  ~10 s transfer +  800 ms latency/hop  → 30 s
        //   Everything faster                                             → 20 s
        long pageLoadSeconds = network.getDownloadKbps() <=  50 ? 90
                             : network.getDownloadKbps() <= 250 ? 45
                             : network.getDownloadKbps() <= 400 ? 30
                             : 20;
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(pageLoadSeconds));
        log.debug("pageLoadTimeout set to {}s for {} ({} Kbps)",
                pageLoadSeconds, network.getDisplayName(), network.getDownloadKbps());

        // Start capture
        profiler.startCapture(scenario.name());

        // Navigate and measure.
        // WHY CATCH TimeoutException:
        // If the page does not finish loading within pageLoadTimeout, ChromeDriver
        // throws TimeoutException. We catch it and record the elapsed time so the
        // test assertion can report a meaningful "was Xms, expected <Yms" failure
        // rather than an infrastructure-level HttpTimeoutException.
        long startTime = System.currentTimeMillis();
        try {
            driver.get(DEMO_URL);
        } catch (TimeoutException e) {
            log.warn("Page load timed out for {} — recording elapsed time as load time",
                    scenario.getDisplayName());
        }
        long loadTime = System.currentTimeMillis() - startTime;

        // Get metrics
        NetworkMetrics metrics = profiler.stopCapture();

        log.info("{}: {}ms load time, {} requests, {}",
                scenario.getDisplayName(),
                loadTime,
                metrics.totalRequests,
                metrics.getTotalSizeFormatted());

        // Log to Extent Report
        PerformanceReporter.logDevicePerformance(scenario, metrics, loadTime);

        // Cleanup
        profiler.removeThrottling();

        return loadTime;
    }

    /**
     * Create WebDriver configured for a device profile.
     */
    private WebDriver createDriverForDevice(DeviceProfile device) {
        ChromeOptions options = new ChromeOptions();

        // Set mobile emulation if applicable
        if (device.getUserAgent() != null) {
            Map<String, Object> mobileEmulation = new HashMap<>();
            mobileEmulation.put("deviceMetrics", Map.of(
                    "width", device.getWidth(),
                    "height", device.getHeight(),
                    "pixelRatio", 2.0
            ));
            mobileEmulation.put("userAgent", device.getUserAgent());
            options.setExperimentalOption("mobileEmulation", mobileEmulation);
        }

        // Configure proxy for network capture
        options = profiler.configureChromeOptions(options);

        // Headless for CI
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless");
        }

        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        WebDriver driver = new ChromeDriver(options);

        // Set window size for desktop
        if (device.getUserAgent() == null) {
            driver.manage().window().setSize(device.getDimension());
        }

        return driver;
    }
}
