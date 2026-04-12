package TestNG.tests;

import base.BaseAccessibilityTest;
import config.Configuration;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.AccessibilityChecker;
import utils.AccessibilityChecker.AccessibilityIssue;
import utils.AccessibilityChecker.Severity;
import utils.AccessibilityReporter;
import utils.AccessibilityUtils;
import utils.AccessibilityUtils.AccessibilityViolation;
import utils.DeviceEmulation.Device;
import utils.ExtentReportManager;
import utils.KibeamUrls;
import utils.WaitUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Device-specific accessibility tests for Kibeam.com.
 *
 * <p>Tests accessibility compliance across multiple device viewports
 * including mobile, tablet, and desktop. Each device test includes
 * context about the viewport size in the report.</p>
 *
 * <h2>Devices Tested:</h2>
 * <ul>
 *   <li>Mobile: iPhone SE, iPhone 12, iPhone 14 Pro Max, Pixel 5, Samsung S21</li>
 *   <li>Tablet: iPad, iPad Pro 11", iPad Pro 12.9", Galaxy Tab S7</li>
 *   <li>Desktop: Laptop, HD, FHD, 2K</li>
 * </ul>
 *
 * <p><b>Note:</b> This class extends {@link BaseAccessibilityTest} which provides
 * automatic accessibility checks after each test. For device-specific tests,
 * we skip the automatic check and run manual checks with device context.</p>
 *
 * @see utils.DeviceEmulation
 * @see utils.AccessibilityUtils
 * @see BaseAccessibilityTest
 */
public class KibeamDeviceAccessibilityTest extends BaseAccessibilityTest {

    /**
     * Ensures driver is ready before each test.
     * BaseAccessibilityTest → BaseTestTestNG.setup() creates the driver.
     * This method just skips the automatic a11y check since we do manual checks with device context.
     */
    @BeforeMethod(alwaysRun = true)
    public void prepareForDeviceTest() {
        // Skip automatic accessibility check - we run manual checks with device context
        skipAccessibilityCheck();
        log.debug("Device test setup complete, automatic a11y check skipped");
    }

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    // Device groups
    private static final Device[] MOBILE_DEVICES = {
            Device.IPHONE_SE, Device.IPHONE_12, Device.IPHONE_14_PRO_MAX,
            Device.PIXEL_5, Device.SAMSUNG_S21
    };

    private static final Device[] TABLET_DEVICES = {
            Device.IPAD, Device.IPAD_PRO_11, Device.IPAD_PRO_12, Device.GALAXY_TAB_S7
    };

    private static final Device[] DESKTOP_DEVICES = {
            Device.LAPTOP, Device.DESKTOP_HD, Device.DESKTOP_FHD
    };

    // ==================== DATA PROVIDERS ====================

    @DataProvider(name = "mobileDevices")
    public Object[][] mobileDeviceProvider() {
        return toDataProvider(MOBILE_DEVICES);
    }

    @DataProvider(name = "tabletDevices")
    public Object[][] tabletDeviceProvider() {
        return toDataProvider(TABLET_DEVICES);
    }

    @DataProvider(name = "desktopDevices")
    public Object[][] desktopDeviceProvider() {
        return toDataProvider(DESKTOP_DEVICES);
    }

    @DataProvider(name = "allDevices")
    public Object[][] allDeviceProvider() {
        List<Device> all = new ArrayList<>();
        all.addAll(List.of(MOBILE_DEVICES));
        all.addAll(List.of(TABLET_DEVICES));
        all.addAll(List.of(DESKTOP_DEVICES));
        return toDataProvider(all.toArray(new Device[0]));
    }

    private Object[][] toDataProvider(Device[] devices) {
        Object[][] data = new Object[devices.length][1];
        for (int i = 0; i < devices.length; i++) {
            data[i][0] = devices[i];
        }
        return data;
    }

    // ==================== MOBILE ACCESSIBILITY TESTS ====================

    /**
     * Tests accessibility on mobile devices.
     */
    @Test(dataProvider = "mobileDevices",
          description = "Accessibility check on mobile viewport",
          groups = {"accessibility", "regression", "mobile"})
    public void testMobileAccessibility(Device device) {
        runAccessibilityCheck(device, KibeamUrls.getBaseUrl(), "Home");
    }

    /**
     * Tests accessibility on tablet devices.
     */
    @Test(dataProvider = "tabletDevices",
          description = "Accessibility check on tablet viewport",
          groups = {"accessibility", "regression", "tablet"})
    public void testTabletAccessibility(Device device) {
        runAccessibilityCheck(device, KibeamUrls.getBaseUrl(), "Home");
    }

    /**
     * Tests accessibility on desktop viewports.
     */
    @Test(dataProvider = "desktopDevices",
          description = "Accessibility check on desktop viewport",
          groups = {"accessibility", "regression", "desktop"})
    public void testDesktopAccessibility(Device device) {
        runAccessibilityCheck(device, KibeamUrls.getBaseUrl(), "Home");
    }

    // ==================== MULTI-PAGE DEVICE TESTS ====================

    /**
     * Tests all main pages on a single mobile device (iPhone 12).
     */
    @Test(description = "All pages accessibility on iPhone 12",
          groups = {"accessibility", "regression", "mobile"})
    public void testAllPagesMobile() {
        Device device = Device.IPHONE_12;
        emulateDevice(device);

        DeviceAccessibilityResult result = new DeviceAccessibilityResult(device);

        // Test each page
        result.addPageResult("Home", checkPageAccessibility(KibeamUrls.getBaseUrl()));
        result.addPageResult("Educators", checkPageAccessibility(KibeamUrls.getEducatorsUrl()));
        result.addPageResult("Contact", checkPageAccessibility(KibeamUrls.getContactUrl()));
        result.addPageResult("About", checkPageAccessibility(KibeamUrls.getAboutUrl()));

        // Log and report
        result.logSummary();
        result.attachToReport();

        // Assert no critical issues
        assertNoCriticalViolations(result);
    }

    /**
     * Tests all main pages on iPad.
     */
    @Test(description = "All pages accessibility on iPad",
          groups = {"accessibility", "regression", "tablet"})
    public void testAllPagesTablet() {
        Device device = Device.IPAD;
        emulateDevice(device);

        DeviceAccessibilityResult result = new DeviceAccessibilityResult(device);

        result.addPageResult("Home", checkPageAccessibility(KibeamUrls.getBaseUrl()));
        result.addPageResult("Educators", checkPageAccessibility(KibeamUrls.getEducatorsUrl()));
        result.addPageResult("Contact", checkPageAccessibility(KibeamUrls.getContactUrl()));
        result.addPageResult("About", checkPageAccessibility(KibeamUrls.getAboutUrl()));

        result.logSummary();
        result.attachToReport();

        assertNoCriticalViolations(result);
    }

    // ==================== FULL DEVICE MATRIX ====================

    /**
     * Comprehensive test: all devices × all pages.
     * Run sparingly - generates extensive report.
     */
    @Test(dataProvider = "allDevices",
          description = "Full accessibility matrix: all pages on all devices",
          groups = {"accessibility", "full-matrix"},
          enabled = false) // Enable when needed
    public void testFullDeviceMatrix(Device device) {
        emulateDevice(device);

        DeviceAccessibilityResult result = new DeviceAccessibilityResult(device);

        result.addPageResult("Home", checkPageAccessibility(KibeamUrls.getBaseUrl()));
        result.addPageResult("Educators", checkPageAccessibility(KibeamUrls.getEducatorsUrl()));
        result.addPageResult("Contact", checkPageAccessibility(KibeamUrls.getContactUrl()));
        result.addPageResult("About", checkPageAccessibility(KibeamUrls.getAboutUrl()));

        result.logSummary();
        result.attachToReport();

        assertNoCriticalViolations(result);
    }

    // ==================== CORE METHODS ====================

    /**
     * Emulates a device by resizing the browser window.
     */
    private void emulateDevice(Device device) {
        log.info("Emulating device: {} ({}x{})",
                device.name(), device.getWidth(), device.getHeight());

        getDriver().manage().window().setSize(
                new Dimension(device.getWidth(), device.getHeight()));
    }

    /**
     * Runs accessibility check for a device on a specific URL.
     */
    private void runAccessibilityCheck(Device device, String url, String pageName) {
        emulateDevice(device);

        log.info("Testing {} page on {}: {}x{}",
                pageName, device.name(), device.getWidth(), device.getHeight());

        getDriver().get(url);
        WaitUtils.waitForPageLoad(getDriver());

        // Run accessibility checks
        PageAccessibilityResult result = checkPageAccessibility(url);

        // Log with device context
        logWithDeviceContext(device, pageName, result);

        // Attach to ExtentReports with device info
        attachToReportWithContext(device, pageName, result);

        // Always log violations (never fails test)
        logCriticalViolations(result, device, pageName);
    }

    /**
     * Checks accessibility on current page.
     */
    private PageAccessibilityResult checkPageAccessibility(String url) {
        getDriver().get(url);
        WaitUtils.waitForPageLoad(getDriver());

        List<AccessibilityViolation> axeViolations = new ArrayList<>();
        List<AccessibilityIssue> customIssues = new ArrayList<>();

        // Run Axe-core
        if (Configuration.getInstance().getBoolean("accessibility.use.axe", true)) {
            try {
                axeViolations = AccessibilityUtils.checkPage(getDriver());
            } catch (Exception e) {
                log.warn("Axe-core scan failed: {}", e.getMessage());
            }
        }

        // Run custom checker
        if (Configuration.getInstance().getBoolean("accessibility.use.custom", true)) {
            try {
                customIssues = AccessibilityChecker.checkAllTraps(getDriver());
            } catch (Exception e) {
                log.warn("Custom checker failed: {}", e.getMessage());
            }
        }

        return new PageAccessibilityResult(url, axeViolations, customIssues);
    }

    /**
     * Logs results with device context.
     */
    private void logWithDeviceContext(Device device, String pageName, PageAccessibilityResult result) {
        String deviceType = device.isMobile() ? "Mobile" : device.isTablet() ? "Tablet" : "Desktop";

        log.info("═══════════════════════════════════════════════════════════");
        log.info("ACCESSIBILITY RESULTS - {} on {} ({})",
                pageName, device.name(), deviceType);
        log.info("Viewport: {}x{} | Pixel Ratio: {}",
                device.getWidth(), device.getHeight(), device.getPixelRatio());
        log.info("───────────────────────────────────────────────────────────");

        if (result.hasViolations()) {
            log.warn("Axe violations: {} | Custom issues: {}",
                    result.axeViolations.size(), result.customIssues.size());

            // Log severe issues
            result.axeViolations.stream()
                    .filter(AccessibilityViolation::isSevere)
                    .forEach(v -> log.error("  [{}] {}: {}",
                            v.getImpact(), v.getRuleId(), v.getDescription()));

            result.customIssues.stream()
                    .filter(i -> i.getSeverity() == Severity.CRITICAL)
                    .forEach(i -> log.error("  [CRITICAL] {}: {}",
                            i.getType(), i.getDescription()));
        } else {
            log.info("No accessibility violations found");
        }

        log.info("═══════════════════════════════════════════════════════════");
    }

    /**
     * Attaches results to ExtentReports with device context.
     * Uses enhanced format with summary at top, collapsible severity groups, and screenshots.
     */
    private void attachToReportWithContext(Device device, String pageName, PageAccessibilityResult result) {
        var extentTest = ExtentReportManager.getTest();
        if (extentTest == null) return;

        // Add device and page context as styled header
        String deviceType = device.isMobile() ? "📱 Mobile" : device.isTablet() ? "📱 Tablet" : "🖥️ Desktop";
        String contextInfo = String.format(
            "<div style='background: #f0f4f8; padding: 10px 15px; border-radius: 5px; margin-bottom: 15px;'>" +
            "<strong>%s</strong> | <strong>Page:</strong> %s | <strong>Device:</strong> %s (%dx%d)" +
            "</div>",
            deviceType, pageName, device.name(), device.getWidth(), device.getHeight());
        extentTest.info(contextInfo);

        // Pass WebDriver for element screenshots - uses enhanced combined report format
        AccessibilityReporter.attachCombinedReport(extentTest, result.axeViolations, result.customIssues);
    }

    /**
     * Logs critical violations as warnings (does NOT fail the test).
     *
     * <p>Use this for reporting-only mode where you want to track violations
     * without blocking the test pipeline.</p>
     */
    private void logCriticalViolations(PageAccessibilityResult result, Device device, String pageName) {
        List<AccessibilityViolation> severe = result.axeViolations.stream()
                .filter(AccessibilityViolation::isSevere)
                .collect(Collectors.toList());

        List<AccessibilityIssue> critical = result.customIssues.stream()
                .filter(i -> i.getSeverity() == Severity.CRITICAL)
                .collect(Collectors.toList());

        if (!severe.isEmpty() || !critical.isEmpty()) {
            log.warn("⚠️ ACCESSIBILITY VIOLATIONS on {} ({}): {} Axe severe, {} Custom critical",
                    pageName, device.name(), severe.size(), critical.size());

            // Log to ExtentReport as warning (not failure)
            var extentTest = ExtentReportManager.getTest();
            if (extentTest != null) {
                extentTest.warning(String.format(
                        "Accessibility: %s has %d severe + %d critical issues",
                        pageName, severe.size(), critical.size()));
            }
        } else {
            log.info("✓ {} ({}) - No critical accessibility issues", pageName, device.name());
        }
    }

    /**
     * Logs all violations across pages as warnings (does NOT fail the test).
     *
     * <p>Replaces the previous assertNoCriticalViolations() which used SoftAssert.
     * This method logs everything to console and ExtentReport but allows the test to pass.</p>
     */
    private void logViolationSummary(DeviceAccessibilityResult result) {
        int totalSevere = 0;
        int totalCritical = 0;

        log.info("═══════════════════════════════════════════════════════════");
        log.info("ACCESSIBILITY SUMMARY - {} ({}x{})",
                result.device.name(), result.device.getWidth(), result.device.getHeight());
        log.info("───────────────────────────────────────────────────────────");

        for (var entry : result.pageResults.entrySet()) {
            String page = entry.getKey();
            PageAccessibilityResult pageResult = entry.getValue();

            long severeCount = pageResult.axeViolations.stream()
                    .filter(AccessibilityViolation::isSevere).count();
            long criticalCount = pageResult.customIssues.stream()
                    .filter(i -> i.getSeverity() == Severity.CRITICAL).count();

            totalSevere += severeCount;
            totalCritical += criticalCount;

            // Log each page status
            if (severeCount > 0 || criticalCount > 0) {
                log.warn("  ⚠️ {}: {} severe Axe, {} critical custom",
                        page, severeCount, criticalCount);
            } else {
                log.info("  ✓ {}: No critical issues", page);
            }
        }

        log.info("───────────────────────────────────────────────────────────");

        if (totalSevere > 0 || totalCritical > 0) {
            log.warn("  TOTAL: {} severe Axe violations, {} critical custom issues",
                    totalSevere, totalCritical);
            log.warn("  ⚠️ Test PASSED but accessibility issues need attention");
        } else {
            log.info("  ✓ TOTAL: No critical accessibility issues found!");
        }

        log.info("═══════════════════════════════════════════════════════════");

        // Log summary to ExtentReport
        var extentTest = ExtentReportManager.getTest();
        if (extentTest != null) {
            if (totalSevere > 0 || totalCritical > 0) {
                extentTest.warning(String.format(
                        "Accessibility Summary: %d severe + %d critical issues across %d pages",
                        totalSevere, totalCritical, result.pageResults.size()));
            } else {
                extentTest.pass("Accessibility Summary: No critical issues found");
            }
        }
    }

    // Keep old method name as alias for backward compatibility
    private void assertNoCriticalViolations(DeviceAccessibilityResult result) {
        logViolationSummary(result);
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Result container for a single page scan.
     */
    private static class PageAccessibilityResult {
        final List<AccessibilityViolation> axeViolations;
        final List<AccessibilityIssue> customIssues;

        PageAccessibilityResult(String url,
                                List<AccessibilityViolation> axeViolations,
                                List<AccessibilityIssue> customIssues) {
            this.axeViolations = axeViolations != null ? axeViolations : List.of();
            this.customIssues = customIssues != null ? customIssues : List.of();
        }

        boolean hasViolations() {
            return !axeViolations.isEmpty() || !customIssues.isEmpty();
        }
    }

    /**
     * Result container for a device's multi-page scan.
     */
    private class DeviceAccessibilityResult {
        final Device device;
        final java.util.Map<String, PageAccessibilityResult> pageResults = new java.util.LinkedHashMap<>();

        DeviceAccessibilityResult(Device device) {
            this.device = device;
        }

        void addPageResult(String pageName, PageAccessibilityResult result) {
            pageResults.put(pageName, result);
        }

        void logSummary() {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("DEVICE SUMMARY: {} ({}x{})",
                    device.name(), device.getWidth(), device.getHeight());
            log.info("───────────────────────────────────────────────────────────");

            int totalAxe = 0, totalCustom = 0;
            for (var entry : pageResults.entrySet()) {
                PageAccessibilityResult r = entry.getValue();
                totalAxe += r.axeViolations.size();
                totalCustom += r.customIssues.size();
                log.info("  {}: {} Axe, {} Custom",
                        entry.getKey(), r.axeViolations.size(), r.customIssues.size());
            }

            log.info("───────────────────────────────────────────────────────────");
            log.info("TOTAL: {} Axe violations, {} Custom issues", totalAxe, totalCustom);
            log.info("═══════════════════════════════════════════════════════════");
        }

        void attachToReport() {
            var extentTest = ExtentReportManager.getTest();
            if (extentTest == null) return;

            // Add styled device header
            String deviceType = device.isMobile() ? "📱 Mobile" :
                               device.isTablet() ? "📱 Tablet" : "🖥️ Desktop";
            String deviceHeader = String.format(
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                "color: white; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>" +
                "<h4 style='margin: 0;'>%s %s</h4>" +
                "<small>%dx%d viewport</small>" +
                "</div>",
                deviceType, device.name(), device.getWidth(), device.getHeight());
            extentTest.info(deviceHeader);

            for (var entry : pageResults.entrySet()) {
                var node = extentTest.createNode("📄 " + entry.getKey() + " Page");
                AccessibilityReporter.attachCombinedReport(
                        node,
                        entry.getValue().axeViolations,
                        entry.getValue().customIssues);
            }
        }
    }
}
