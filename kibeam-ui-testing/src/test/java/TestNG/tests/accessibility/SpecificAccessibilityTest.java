package TestNG.tests.accessibility;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.DeviceEmulation.Device;

/**
 * Specific accessibility tests for debugging particular device/page combinations.
 *
 * <p>Use this class when you need to investigate specific accessibility issues
 * on particular device/page combinations without running the full suite.</p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * # Run all specific combinations
 * mvn test -Dtest=SpecificAccessibilityTest
 *
 * # Run specific troubleshooting test
 * mvn test -Dtest=SpecificAccessibilityTest#testTroubleshooting
 *
 * # Run single device test
 * mvn test -Dtest=SpecificAccessibilityTest#testSingleDevice
 * </pre>
 *
 * <h2>Customization:</h2>
 * <p>Modify the data providers below to focus on specific combinations you need to debug.</p>
 */
public class SpecificAccessibilityTest extends BaseDeviceAccessibilityTest {

    // ==================== DATA PROVIDERS ====================

    /**
     * Define specific device/page combinations for troubleshooting.
     * Modify this to focus on problematic combinations.
     *
     * Format: { Device, URL, PageName }
     */
    @DataProvider(name = "troubleshootingCombos", parallel = true)
    public Object[][] troubleshootingCombosProvider() {
        return new Object[][] {
            // Mobile + Home - common trouble spots
            { Device.IPHONE_SE, HOME_URL, "Home" },
            { Device.IPHONE_12, HOME_URL, "Home" },

            // Mobile + Educators - video player accessibility
            { Device.IPHONE_12, EDUCATORS_URL, "Educators" },

            // Tablet + Contact - form accessibility
            { Device.IPAD, CONTACT_URL, "Contact" },

            // Desktop + About - heading hierarchy
            { Device.DESKTOP_FHD, ABOUT_URL, "About" },
        };
    }

    /**
     * Single device for quick iteration during debugging.
     */
    @DataProvider(name = "singleDevice", parallel = true)
    public Object[][] singleDeviceProvider() {
        return new Object[][] {
            { Device.IPHONE_12 }
        };
    }

    /**
     * All pages for a single device - useful for device-specific debugging.
     */
    @DataProvider(name = "allPagesOneDevice", parallel = true)
    public Object[][] allPagesOneDeviceProvider() {
        Device device = Device.IPHONE_12; // Change as needed
        return new Object[][] {
            { device, HOME_URL, "Home" },
            { device, EDUCATORS_URL, "Educators" },
            { device, CONTACT_URL, "Contact" },
            { device, ABOUT_URL, "About" },
        };
    }

    /**
     * Critical viewports - smallest and largest of each type.
     */
    @DataProvider(name = "criticalViewports", parallel = true)
    public Object[][] criticalViewportsProvider() {
        return new Object[][] {
            // Smallest mobile
            { Device.IPHONE_SE, HOME_URL, "Home" },
            // Largest mobile
            { Device.IPHONE_14_PRO_MAX, HOME_URL, "Home" },
            // Smallest tablet
            { Device.IPAD, HOME_URL, "Home" },
            // Largest tablet
            { Device.IPAD_PRO_12, HOME_URL, "Home" },
            // Smallest desktop
            { Device.LAPTOP, HOME_URL, "Home" },
            // Largest desktop
            { Device.DESKTOP_FHD, HOME_URL, "Home" },
        };
    }

    // ==================== TROUBLESHOOTING TESTS ====================

    /**
     * Test specific device/page combinations for troubleshooting.
     */
    @Test(dataProvider = "troubleshootingCombos",
          description = "Troubleshoot specific device/page combinations",
          groups = {"accessibility", "specific", "troubleshooting"})
    public void testTroubleshooting(Device device, String url, String pageName) {
        log.info("TROUBLESHOOTING: {} on {}", pageName, device.name());

        emulateDevice(device);
        PageResult result = checkPage(url, pageName);

        // Extra detailed logging for troubleshooting
        logDetailedResult(device, result);
        attachToReport(device, result);
    }

    /**
     * Quick single-device test for rapid iteration.
     */
    @Test(dataProvider = "singleDevice",
          description = "Quick test on single device",
          groups = {"accessibility", "specific", "quick"})
    public void testSingleDevice(Device device) {
        log.info("QUICK TEST: All pages on {}", device.name());

        DeviceResult result = checkAllPages(device);

        logDeviceResult(result);
        attachToReport(result);
    }

    /**
     * Test all pages on a single device for focused debugging.
     */
    @Test(dataProvider = "allPagesOneDevice",
          description = "All pages on single device for debugging",
          groups = {"accessibility", "specific"})
    public void testAllPagesOneDevice(Device device, String url, String pageName) {
        log.info("FOCUSED TEST: {} on {}", pageName, device.name());

        emulateDevice(device);
        PageResult result = checkPage(url, pageName);

        logDetailedResult(device, result);
        attachToReport(device, result);
    }

    /**
     * Test critical viewport sizes (edge cases).
     */
    @Test(dataProvider = "criticalViewports",
          description = "Critical viewport size testing",
          groups = {"accessibility", "specific", "critical-viewports"})
    public void testCriticalViewports(Device device, String url, String pageName) {
        log.info("CRITICAL VIEWPORT: {} ({}) on {}",
                device.name(), device.getWidth() + "x" + device.getHeight(), pageName);

        emulateDevice(device);
        PageResult result = checkPage(url, pageName);

        logDetailedResult(device, result);
        attachToReport(device, result);
    }

    // ==================== DETAILED LOGGING ====================

    private void logDetailedResult(Device device, PageResult result) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("DETAILED RESULT: {} on {}", result.pageName, device.name());
        log.info("────────────────────────────────────────────────────────────");
        log.info("Device: {} ({}x{}, {})",
                device.name(),
                device.getWidth(),
                device.getHeight(),
                device.isMobile() ? "Mobile" : device.isTablet() ? "Tablet" : "Desktop");
        log.info("URL: {}", result.url);
        log.info("────────────────────────────────────────────────────────────");

        // Axe-core violations
        log.info("AXE-CORE VIOLATIONS: {}", result.axeViolations.size());
        if (!result.axeViolations.isEmpty()) {
            result.axeViolations.forEach(v -> {
                log.info("  [{:8}] {}: {}",
                        v.getImpact().toUpperCase(),
                        v.getRuleId(),
                        v.getDescription());
                log.info("            Fix: {}", v.getHelpUrl());
            });
        }

        // Custom issues
        log.info("CUSTOM ISSUES: {}", result.customIssues.size());
        if (!result.customIssues.isEmpty()) {
            result.customIssues.forEach(i -> {
                log.info("  [{:8}] {}: {}",
                        i.getSeverity().name(),
                        i.getType().name(),
                        i.getDescription());
                log.info("            Element: {}", i.getElementSelector());
                log.info("            Fix: {}", i.getRecommendation());
            });
        }

        log.info("────────────────────────────────────────────────────────────");
        log.info("SUMMARY: {} Axe ({} severe), {} Custom ({} critical)",
                result.axeViolations.size(),
                result.getSevereCount(),
                result.customIssues.size(),
                result.getCriticalCount());
        log.info("════════════════════════════════════════════════════════════");
    }
}
