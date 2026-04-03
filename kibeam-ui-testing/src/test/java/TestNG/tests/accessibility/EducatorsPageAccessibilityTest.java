package TestNG.tests.accessibility;

import org.testng.annotations.Test;
import utils.DeviceEmulation.Device;

/**
 * Accessibility tests for Kibeam EDUCATORS PAGE across all device types.
 *
 * <h2>Usage:</h2>
 * <pre>
 * mvn test -Dtest=EducatorsPageAccessibilityTest
 * mvn test -Dgroups=accessibility,educators
 * mvn test -Dgroups=mobile,educators
 * </pre>
 */
public class EducatorsPageAccessibilityTest extends BaseDeviceAccessibilityTest {

    private static final String PAGE_NAME = "Educators";
    private static final String PAGE_URL = EDUCATORS_URL;

    // ==================== MOBILE ====================

    @Test(dataProvider = "mobileDevices",
          description = "Educators page accessibility on mobile device",
          groups = {"accessibility", "educators", "mobile"})
    public void testMobile(Device device) {
        log.info("Testing {} page on mobile: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== TABLET ====================

    @Test(dataProvider = "tabletDevices",
          description = "Educators page accessibility on tablet device",
          groups = {"accessibility", "educators", "tablet"})
    public void testTablet(Device device) {
        log.info("Testing {} page on tablet: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== DESKTOP ====================

    @Test(dataProvider = "desktopDevices",
          description = "Educators page accessibility on desktop viewport",
          groups = {"accessibility", "educators", "desktop"})
    public void testDesktop(Device device) {
        log.info("Testing {} page on desktop: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }
}
