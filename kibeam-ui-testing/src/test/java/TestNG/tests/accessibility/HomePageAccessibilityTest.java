package TestNG.tests.accessibility;

import org.testng.annotations.Test;
import utils.DeviceEmulation.Device;

/**
 * Accessibility tests for Kibeam HOME PAGE across all device types.
 *
 * <h2>Usage:</h2>
 * <pre>
 * mvn test -Dtest=HomePageAccessibilityTest
 * mvn test -Dgroups=accessibility,home
 * mvn test -Dgroups=mobile,home
 * </pre>
 */
public class HomePageAccessibilityTest extends BaseDeviceAccessibilityTest {

    private static final String PAGE_NAME = "Home";
    private static final String PAGE_URL = HOME_URL;

    // ==================== MOBILE ====================

    @Test(dataProvider = "mobileDevices",
          description = "Home page accessibility on mobile device",
          groups = {"accessibility", "home", "mobile"})
    public void testMobile(Device device) {
        log.info("Testing {} page on mobile: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== TABLET ====================

    @Test(dataProvider = "tabletDevices",
          description = "Home page accessibility on tablet device",
          groups = {"accessibility", "home", "tablet"})
    public void testTablet(Device device) {
        log.info("Testing {} page on tablet: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== DESKTOP ====================

    @Test(dataProvider = "desktopDevices",
          description = "Home page accessibility on desktop viewport",
          groups = {"accessibility", "home", "desktop"})
    public void testDesktop(Device device) {
        log.info("Testing {} page on desktop: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }
}
