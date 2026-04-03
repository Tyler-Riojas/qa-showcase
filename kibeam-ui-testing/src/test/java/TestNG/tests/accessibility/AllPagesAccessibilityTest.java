package TestNG.tests.accessibility;

import org.testng.annotations.Test;
import utils.DeviceEmulation.Device;

/**
 * Accessibility tests for ALL Kibeam pages across device types.
 *
 * <p>Tests all 4 main pages (Home, Educators, Contact, About) on each device.
 * Use this for comprehensive regression testing.</p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * mvn test -Dtest=AllPagesAccessibilityTest
 * mvn test -Dtest=AllPagesAccessibilityTest#testMobileAllPages
 * mvn test -Dgroups=accessibility,regression
 * </pre>
 */
public class AllPagesAccessibilityTest extends BaseDeviceAccessibilityTest {

    // ==================== MOBILE ====================

    /**
     * Tests all pages on mobile devices.
     * Runs 5 devices × 4 pages = 20 page scans.
     */
    @Test(dataProvider = "mobileDevices",
          description = "All pages accessibility on mobile device",
          groups = {"accessibility", "regression", "mobile", "all-pages"})
    public void testMobileAllPages(Device device) {
        log.info("Testing ALL PAGES on mobile: {}", device.name());

        DeviceResult result = checkAllPages(device);

        logDeviceResult(result);
        attachToReport(result);
    }

    // ==================== TABLET ====================

    /**
     * Tests all pages on tablet devices.
     * Runs 4 devices × 4 pages = 16 page scans.
     */
    @Test(dataProvider = "tabletDevices",
          description = "All pages accessibility on tablet device",
          groups = {"accessibility", "regression", "tablet", "all-pages"})
    public void testTabletAllPages(Device device) {
        log.info("Testing ALL PAGES on tablet: {}", device.name());

        DeviceResult result = checkAllPages(device);

        logDeviceResult(result);
        attachToReport(result);
    }

    // ==================== DESKTOP ====================

    /**
     * Tests all pages on desktop viewports.
     * Runs 3 devices × 4 pages = 12 page scans.
     */
    @Test(dataProvider = "desktopDevices",
          description = "All pages accessibility on desktop viewport",
          groups = {"accessibility", "regression", "desktop", "all-pages"})
    public void testDesktopAllPages(Device device) {
        log.info("Testing ALL PAGES on desktop: {}", device.name());

        DeviceResult result = checkAllPages(device);

        logDeviceResult(result);
        attachToReport(result);
    }
}
