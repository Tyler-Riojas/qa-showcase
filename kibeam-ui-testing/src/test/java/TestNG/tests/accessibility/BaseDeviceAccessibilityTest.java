package TestNG.tests.accessibility;

import base.BaseAccessibilityTest;
import config.Configuration;
import listeners.AnnotationTransformer;
import listeners.ExtentTestNGListener;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import utils.AccessibilityChecker;
import utils.AccessibilityChecker.AccessibilityIssue;
import utils.AccessibilityChecker.Severity;
import utils.AccessibilityReporter;
import utils.AccessibilityUtils;
import utils.AccessibilityUtils.AccessibilityViolation;
import utils.DeviceEmulation;
import utils.DeviceEmulation.Device;
import utils.ExtentReportManager;
import utils.KibeamUrls;
import utils.WaitUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Base class for device-specific accessibility tests.
 *
 * <p>Provides common functionality for all accessibility test classes including:
 * device emulation, data providers, result containers, and logging utilities.</p>
 *
 * <p>Extend this class for page-specific or device-specific accessibility tests.</p>
 *
 * <p><b>Note:</b> The {@code @Listeners} annotation ensures ExtentReports and
 * other listeners are registered even when running with {@code -Dgroups} flag
 * (which bypasses TestNG suite XML files).</p>
 */
@Listeners({ExtentTestNGListener.class, AnnotationTransformer.class})
public abstract class BaseDeviceAccessibilityTest extends BaseAccessibilityTest {

    protected static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    // Device groups
    protected static final Device[] MOBILE_DEVICES = {
            Device.IPHONE_SE, Device.IPHONE_12, Device.IPHONE_14_PRO_MAX,
            Device.PIXEL_5, Device.SAMSUNG_S21
    };

    protected static final Device[] TABLET_DEVICES = {
            Device.IPAD, Device.IPAD_PRO_11, Device.IPAD_PRO_12, Device.GALAXY_TAB_S7
    };

    protected static final Device[] DESKTOP_DEVICES = {
            Device.LAPTOP, Device.DESKTOP_HD, Device.DESKTOP_FHD
    };

    // Page URLs
    protected static final String HOME_URL = KibeamUrls.getBaseUrl();
    protected static final String EDUCATORS_URL = KibeamUrls.getEducatorsUrl();
    protected static final String CONTACT_URL = KibeamUrls.getContactUrl();
    protected static final String ABOUT_URL = KibeamUrls.getAboutUrl();

    @BeforeMethod(alwaysRun = true)
    public void prepareForDeviceTest() {
        skipAccessibilityCheck();
        log.debug("Device test setup complete, automatic a11y check skipped");
    }

    // ==================== DATA PROVIDERS ====================

    @DataProvider(name = "mobileDevices", parallel = true)
    public Object[][] mobileDeviceProvider() {
        return toDataProvider(MOBILE_DEVICES);
    }

    @DataProvider(name = "tabletDevices", parallel = true)
    public Object[][] tabletDeviceProvider() {
        return toDataProvider(TABLET_DEVICES);
    }

    @DataProvider(name = "desktopDevices", parallel = true)
    public Object[][] desktopDeviceProvider() {
        return toDataProvider(DESKTOP_DEVICES);
    }

    @DataProvider(name = "allDevices", parallel = true)
    public Object[][] allDeviceProvider() {
        List<Device> all = new ArrayList<>();
        all.addAll(List.of(MOBILE_DEVICES));
        all.addAll(List.of(TABLET_DEVICES));
        all.addAll(List.of(DESKTOP_DEVICES));
        return toDataProvider(all.toArray(new Device[0]));
    }

    /**
     * Converts device array to TestNG data provider format.
     *
     * <p>Supports filtering by a single device via the {@code -Ddevice} system property.
     * If a device is specified, only that device is returned (if it exists in the array).
     * If no device is specified, all devices in the array are returned.</p>
     *
     * <h3>Usage:</h3>
     * <pre>
     * # Run accessibility tests on a single device
     * mvn test -Dtest=HomePageAccessibilityTest -Ddevice=IPHONE_12
     * mvn test -Dtest=EducatorsPageAccessibilityTest -Ddevice=IPAD
     * mvn test -Dtest=ContactPageAccessibilityTest -Ddevice=DESKTOP_FHD
     * </pre>
     *
     * @param devices array of devices to include
     * @return data provider array, filtered if -Ddevice is specified
     */
    protected Object[][] toDataProvider(Device[] devices) {
        // Check for single device filter via system property
        String specifiedDevice = System.getProperty("device");

        if (specifiedDevice != null && !specifiedDevice.isEmpty()) {
            // Try to find the specified device in the array
            Device targetDevice = DeviceEmulation.getDeviceByName(specifiedDevice);

            if (targetDevice != null) {
                // Check if the target device is in this array
                boolean deviceInArray = Arrays.asList(devices).contains(targetDevice);

                if (deviceInArray) {
                    log.info("Single device mode: Running only on {}", targetDevice.name());
                    return new Object[][] {{ targetDevice }};
                } else {
                    log.debug("Device {} not in this device group, returning empty", specifiedDevice);
                    return new Object[0][0]; // Empty - device not in this group
                }
            } else {
                log.warn("Unknown device specified: {}. Running all devices.", specifiedDevice);
            }
        }

        // No filter or unknown device - return all devices
        Object[][] data = new Object[devices.length][1];
        for (int i = 0; i < devices.length; i++) {
            data[i][0] = devices[i];
        }
        return data;
    }

    /**
     * Gets all available device names for documentation/help.
     *
     * @return list of all available device names
     */
    public static List<String> getAvailableDeviceNames() {
        List<String> names = new ArrayList<>();
        for (Device d : MOBILE_DEVICES) names.add(d.name());
        for (Device d : TABLET_DEVICES) names.add(d.name());
        for (Device d : DESKTOP_DEVICES) names.add(d.name());
        return names;
    }

    // ==================== DEVICE EMULATION ====================

    protected void emulateDevice(Device device) {
        log.info("Emulating device: {} ({}x{})",
                device.name(), device.getWidth(), device.getHeight());
        getDriver().manage().window().setSize(
                new Dimension(device.getWidth(), device.getHeight()));
    }

    // ==================== PAGE CHECKING ====================

    protected PageResult checkPage(String url, String pageName) {
        getDriver().get(url);
        WaitUtils.waitForPageLoad(getDriver());

        List<AccessibilityViolation> axeViolations = new ArrayList<>();
        List<AccessibilityIssue> customIssues = new ArrayList<>();

        if (Configuration.getInstance().getBoolean("accessibility.use.axe", true)) {
            try {
                axeViolations = AccessibilityUtils.checkPage(getDriver());
            } catch (Exception e) {
                log.warn("Axe-core scan failed on {}: {}", pageName, e.getMessage());
            }
        }

        if (Configuration.getInstance().getBoolean("accessibility.use.custom", true)) {
            try {
                customIssues = AccessibilityChecker.checkAllTraps(getDriver());
            } catch (Exception e) {
                log.warn("Custom checker failed on {}: {}", pageName, e.getMessage());
            }
        }

        return new PageResult(pageName, url, axeViolations, customIssues);
    }

    protected DeviceResult checkAllPages(Device device) {
        emulateDevice(device);
        DeviceResult result = new DeviceResult(device);

        result.addPage(checkPage(HOME_URL, "Home"));
        result.addPage(checkPage(EDUCATORS_URL, "Educators"));
        result.addPage(checkPage(CONTACT_URL, "Contact"));
        result.addPage(checkPage(ABOUT_URL, "About"));

        return result;
    }

    // ==================== LOGGING ====================

    protected void logPageResult(Device device, PageResult result) {
        String deviceType = device.isMobile() ? "Mobile" : device.isTablet() ? "Tablet" : "Desktop";

        log.info("───────────────────────────────────────────────────────────");
        log.info("{} on {} ({}) - {}x{}",
                result.pageName, device.name(), deviceType,
                device.getWidth(), device.getHeight());

        if (result.hasViolations()) {
            log.warn("  Axe: {} | Custom: {} | Total: {}",
                    result.axeViolations.size(),
                    result.customIssues.size(),
                    result.getTotalCount());
        } else {
            log.info("  No violations found");
        }
    }

    protected void logDeviceResult(DeviceResult result) {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("DEVICE: {} ({}x{}) - {}",
                result.device.name(),
                result.device.getWidth(),
                result.device.getHeight(),
                result.device.isMobile() ? "Mobile" : result.device.isTablet() ? "Tablet" : "Desktop");
        log.info("───────────────────────────────────────────────────────────");

        int totalAxe = 0, totalCustom = 0;
        for (PageResult page : result.pages.values()) {
            totalAxe += page.axeViolations.size();
            totalCustom += page.customIssues.size();

            String status = page.hasViolations() ? "⚠️" : "✓";
            log.info("  {} {}: {} Axe, {} Custom",
                    status, page.pageName,
                    page.axeViolations.size(),
                    page.customIssues.size());
        }

        log.info("───────────────────────────────────────────────────────────");
        log.info("  TOTAL: {} Axe violations, {} Custom issues", totalAxe, totalCustom);
        log.info("═══════════════════════════════════════════════════════════");
    }

    // ==================== REPORTING ====================

    protected void attachToReport(Device device, PageResult result) {
        var test = ExtentReportManager.getTest();
        if (test == null) return;

        try {
            // Add device and page context as header info
            String deviceType = device.isMobile() ? "📱 Mobile" : device.isTablet() ? "📱 Tablet" : "🖥️ Desktop";
            String contextInfo = String.format(
                "<div style='background: #f0f4f8; padding: 10px 15px; border-radius: 5px; margin-bottom: 15px;'>" +
                "<strong>%s</strong> | <strong>Page:</strong> %s | <strong>Device:</strong> %s (%dx%d)" +
                "</div>",
                deviceType, result.pageName, device.name(), device.getWidth(), device.getHeight());
            test.info(contextInfo);

            // Pass WebDriver for element screenshots on failures - uses enhanced format
            AccessibilityReporter.attachCombinedReport(test, result.axeViolations, result.customIssues);
        } catch (Exception e) {
            log.error("Failed to attach report for {} on {}: {}", result.pageName, device.name(), e.getMessage());
            test.warning("Report attachment failed: " + e.getMessage());
        }
    }

    protected void attachToReport(DeviceResult result) {
        var test = ExtentReportManager.getTest();
        if (test == null) return;

        try {
            // Add device context header
            String deviceType = result.device.isMobile() ? "📱 Mobile" :
                               result.device.isTablet() ? "📱 Tablet" : "🖥️ Desktop";
            String deviceHeader = String.format(
                "<div style='background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); " +
                "color: white; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>" +
                "<h4 style='margin: 0;'>%s %s</h4>" +
                "<small>%dx%d viewport</small>" +
                "</div>",
                deviceType, result.device.name(), result.device.getWidth(), result.device.getHeight());
            test.info(deviceHeader);

            // Create collapsible page sections
            for (PageResult page : result.pages.values()) {
                try {
                    var node = test.createNode("📄 " + page.pageName + " Page");
                    AccessibilityReporter.attachCombinedReport(
                            node, page.axeViolations, page.customIssues);
                } catch (Exception e) {
                    log.error("Failed to attach report for page {}: {}", page.pageName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to attach device report for {}: {}", result.device.name(), e.getMessage());
            test.warning("Device report attachment failed: " + e.getMessage());
        }
    }

    // ==================== RESULT CLASSES ====================

    public static class PageResult {
        public final String pageName;
        public final String url;
        public final List<AccessibilityViolation> axeViolations;
        public final List<AccessibilityIssue> customIssues;

        public PageResult(String pageName, String url,
                          List<AccessibilityViolation> axeViolations,
                          List<AccessibilityIssue> customIssues) {
            this.pageName = pageName;
            this.url = url;
            this.axeViolations = axeViolations != null ? axeViolations : List.of();
            this.customIssues = customIssues != null ? customIssues : List.of();
        }

        public boolean hasViolations() {
            return !axeViolations.isEmpty() || !customIssues.isEmpty();
        }

        public int getTotalCount() {
            return axeViolations.size() + customIssues.size();
        }

        public long getSevereCount() {
            return axeViolations.stream().filter(AccessibilityViolation::isSevere).count();
        }

        public long getCriticalCount() {
            return customIssues.stream()
                    .filter(i -> i.getSeverity() == Severity.CRITICAL).count();
        }
    }

    public static class DeviceResult {
        public final Device device;
        public final Map<String, PageResult> pages = new LinkedHashMap<>();

        public DeviceResult(Device device) {
            this.device = device;
        }

        public void addPage(PageResult result) {
            pages.put(result.pageName, result);
        }

        public int getTotalAxeCount() {
            return pages.values().stream()
                    .mapToInt(p -> p.axeViolations.size()).sum();
        }

        public int getTotalCustomCount() {
            return pages.values().stream()
                    .mapToInt(p -> p.customIssues.size()).sum();
        }

        public boolean hasViolations() {
            return pages.values().stream().anyMatch(PageResult::hasViolations);
        }
    }
}
