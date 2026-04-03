package TestNG.tests.accessibility;

import org.testng.annotations.Test;
import utils.DeviceEmulation.Device;

/**
 * Accessibility tests for Kibeam ABOUT PAGE across all device types.
 *
 * <h2>Usage:</h2>
 * <pre>
 * mvn test -Dtest=AboutPageAccessibilityTest
 * mvn test -Dgroups=accessibility,about
 * mvn test -Dgroups=mobile,about
 * </pre>
 */
public class AboutPageAccessibilityTest extends BaseDeviceAccessibilityTest {

    private static final String PAGE_NAME = "About";
    private static final String PAGE_URL = ABOUT_URL;

    // ==================== MOBILE ====================

    @Test(dataProvider = "mobileDevices",
          description = "About page accessibility on mobile device",
          groups = {"accessibility", "about", "mobile"})
    public void testMobile(Device device) {
        log.info("Testing {} page on mobile: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== TABLET ====================

    @Test(dataProvider = "tabletDevices",
          description = "About page accessibility on tablet device",
          groups = {"accessibility", "about", "tablet"})
    public void testTablet(Device device) {
        log.info("Testing {} page on tablet: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== DESKTOP ====================

    @Test(dataProvider = "desktopDevices",
          description = "About page accessibility on desktop viewport",
          groups = {"accessibility", "about", "desktop"})
    public void testDesktop(Device device) {
        log.info("Testing {} page on desktop: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }
}
