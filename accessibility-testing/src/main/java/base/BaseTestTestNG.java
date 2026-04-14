package base;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import core.DriverFactory;
import enums.BrowserType;
import utils.DeviceEmulation;
import utils.DeviceEmulation.Device;

/**
 * Thread-safe base test class for parallel execution.
 *
 * <p>Uses ThreadLocal storage to ensure each test thread has its own:
 * <ul>
 *   <li>WebDriver instance</li>
 *   <li>Browser type</li>
 *   <li>Device configuration</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * public class MyTest extends BaseTestTestNG {
 *     {@literal @}Test
 *     public void testSomething() {
 *         getDriver().get("https://example.com");
 *         // or use the 'driver' field for backward compatibility
 *         driver.get("https://example.com");
 *     }
 * }
 * </pre>
 *
 * <h2>Parallel Execution:</h2>
 * Safe for all TestNG parallel modes: methods, classes, tests.
 */
public class BaseTestTestNG {

    static final Logger log = getLogger(lookup().lookupClass());

    // ==================== THREAD-LOCAL STORAGE ====================

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<BrowserType> browserTypeThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> deviceThreadLocal = new ThreadLocal<>();

    /**
     * @deprecated Use {@link #getDriver()} for thread-safe access.
     *             This field is maintained for backward compatibility but
     *             may not be reliable in parallel execution with methods mode.
     */
    @Deprecated
    public WebDriver driver;

    // ==================== THREAD-SAFE GETTERS ====================

    /**
     * Get the WebDriver instance for the current thread.
     * This is the preferred, thread-safe way to access the driver.
     *
     * @return WebDriver for current thread
     */
    public WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * Get the browser type for the current thread.
     *
     * @return BrowserType for current thread
     */
    public BrowserType getCurrentBrowserType() {
        return browserTypeThreadLocal.get();
    }

    /**
     * Get current device name for the current thread (if emulating).
     *
     * @return Device name or null if not emulating
     */
    public String getCurrentDevice() {
        return deviceThreadLocal.get();
    }

    /**
     * Get current browser name as string.
     *
     * @return Browser name in lowercase
     */
    public String getCurrentBrowser() {
        BrowserType type = browserTypeThreadLocal.get();
        return type != null ? type.name().toLowerCase() : null;
    }

    // ==================== SETUP ====================

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser", "device"})
    public void setup(@Optional("") String browser, @Optional("") String device) {
        // Store device in ThreadLocal
        deviceThreadLocal.set(device);

        // Parse browser type (DriverFactory uses Configuration if browser param is empty)
        BrowserType browserType = (browser == null || browser.isEmpty())
                ? BrowserType.fromString(config.Configuration.getInstance().getBrowser())
                : BrowserType.fromString(browser);
        browserTypeThreadLocal.set(browserType);

        boolean hasDevice = device != null && !device.isEmpty();
        Device emulatedDevice = hasDevice ? DeviceEmulation.getDeviceByName(device) : null;

        log.info("========================================");
        log.info("[Thread: {}] Setting up {} browser{}",
                Thread.currentThread().getName(),
                browserType,
                (hasDevice ? " with device: " + device : ""));
        log.info("========================================");

        try {
            // Use DriverFactory to create driver (also stores in its own ThreadLocal)
            WebDriver createdDriver = DriverFactory.createDriver(browserType);

            // Store in our ThreadLocal
            driverThreadLocal.set(createdDriver);

            // Backward compatibility: also set the instance field
            // WARNING: Not reliable in parallel methods mode!
            this.driver = createdDriver;

            // Handle device emulation window sizing
            if (emulatedDevice != null) {
                createdDriver.manage().window().setSize(DeviceEmulation.getDimension(emulatedDevice));
                log.info("Window resized for device emulation: {}x{}",
                        emulatedDevice.getWidth(), emulatedDevice.getHeight());
            } else {
                createdDriver.manage().window().maximize();
            }

            log.info("{} browser started successfully!", browserType);

        } catch (Exception e) {
            log.error("FAILED to start {} browser!", browserType, e);
            throw e;
        }
    }

    // ==================== DEVICE EMULATION HELPERS ====================

    /**
     * Check if running in mobile emulation mode.
     *
     * @return true if current device is a mobile device
     */
    public boolean isMobileEmulation() {
        String device = deviceThreadLocal.get();
        if (device == null || device.isEmpty()) {
            return false;
        }
        Device emulatedDevice = DeviceEmulation.getDeviceByName(device);
        return emulatedDevice != null && emulatedDevice.isMobile();
    }

    /**
     * Check if running in tablet emulation mode.
     *
     * @return true if current device is a tablet
     */
    public boolean isTabletEmulation() {
        String device = deviceThreadLocal.get();
        if (device == null || device.isEmpty()) {
            return false;
        }
        Device emulatedDevice = DeviceEmulation.getDeviceByName(device);
        return emulatedDevice != null && emulatedDevice.isTablet();
    }

    /**
     * Get current viewport width.
     *
     * @return viewport width in pixels
     */
    public int getViewportWidth() {
        WebDriver currentDriver = driverThreadLocal.get();
        return currentDriver != null
                ? currentDriver.manage().window().getSize().getWidth()
                : 0;
    }

    /**
     * Get current viewport height.
     *
     * @return viewport height in pixels
     */
    public int getViewportHeight() {
        WebDriver currentDriver = driverThreadLocal.get();
        return currentDriver != null
                ? currentDriver.manage().window().getSize().getHeight()
                : 0;
    }

    // ==================== TEARDOWN ====================

    @AfterMethod(alwaysRun = true)
    public void teardown() {
        BrowserType browserType = browserTypeThreadLocal.get();

        log.info("[Thread: {}] Closing {} browser",
                Thread.currentThread().getName(),
                browserType != null ? browserType : "unknown");

        // Quit driver via DriverFactory (handles its own ThreadLocal cleanup)
        DriverFactory.quitDriver();

        // Clean up our ThreadLocal storage
        driverThreadLocal.remove();
        browserTypeThreadLocal.remove();
        deviceThreadLocal.remove();

        // Clear deprecated field
        this.driver = null;
    }
}
