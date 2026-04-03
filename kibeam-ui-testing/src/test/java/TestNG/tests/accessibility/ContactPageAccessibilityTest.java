package TestNG.tests.accessibility;

import org.testng.annotations.Test;
import utils.DeviceEmulation.Device;

/**
 * Accessibility tests for Kibeam CONTACT PAGE across all device types.
 *
 * <h2>Usage:</h2>
 * <pre>
 * mvn test -Dtest=ContactPageAccessibilityTest
 * mvn test -Dgroups=accessibility,contact
 * mvn test -Dgroups=mobile,contact
 * </pre>
 */
public class ContactPageAccessibilityTest extends BaseDeviceAccessibilityTest {

    private static final String PAGE_NAME = "Contact";
    private static final String PAGE_URL = CONTACT_URL;

    // ==================== MOBILE ====================

    @Test(dataProvider = "mobileDevices",
          description = "Contact page accessibility on mobile device",
          groups = {"accessibility", "contact", "mobile"})
    public void testMobile(Device device) {
        log.info("Testing {} page on mobile: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== TABLET ====================

    @Test(dataProvider = "tabletDevices",
          description = "Contact page accessibility on tablet device",
          groups = {"accessibility", "contact", "tablet"})
    public void testTablet(Device device) {
        log.info("Testing {} page on tablet: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }

    // ==================== DESKTOP ====================

    @Test(dataProvider = "desktopDevices",
          description = "Contact page accessibility on desktop viewport",
          groups = {"accessibility", "contact", "desktop"})
    public void testDesktop(Device device) {
        log.info("Testing {} page on desktop: {}", PAGE_NAME, device.name());

        emulateDevice(device);
        PageResult result = checkPage(PAGE_URL, PAGE_NAME);

        logPageResult(device, result);
        attachToReport(device, result);
    }
}
