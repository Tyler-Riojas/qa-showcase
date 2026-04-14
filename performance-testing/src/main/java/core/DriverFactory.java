package core;

import config.Configuration;
import enums.BrowserType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Thread-safe WebDriver factory for parallel test execution.
 * Uses ThreadLocal to ensure each test thread has its own driver instance.
 *
 * Usage:
 *   WebDriver driver = DriverFactory.getDriver();           // Uses config browser
 *   WebDriver driver = DriverFactory.createDriver(CHROME);  // Explicit browser
 *   DriverFactory.quitDriver();                             // Cleanup
 */
public class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    // ThreadLocal storage for parallel execution safety
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<BrowserType> browserTypeThreadLocal = new ThreadLocal<>();

    // Prevent instantiation
    private DriverFactory() {
    }

    /**
     * Get the WebDriver instance for the current thread.
     * Creates a new driver using Configuration settings if none exists.
     *
     * @return WebDriver instance for current thread
     */
    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            Configuration config = Configuration.getInstance();
            BrowserType browserType = BrowserType.fromString(config.getBrowser());
            createDriver(browserType);
        }
        return driverThreadLocal.get();
    }

    /**
     * Create a new WebDriver instance for the specified browser.
     * Quits any existing driver for the current thread first.
     *
     * @param browserType Browser to create
     * @return New WebDriver instance
     */
    public static WebDriver createDriver(BrowserType browserType) {
        // Cleanup any existing driver for this thread
        if (driverThreadLocal.get() != null) {
            log.debug("Existing driver found for thread, cleaning up first");
            quitDriver();
        }

        Configuration config = Configuration.getInstance();
        boolean headless = config.isHeadless();
        int timeout = config.getTimeout();

        log.info("Creating {} driver (headless: {})", browserType.getName(), headless);

        WebDriver driver = switch (browserType) {
            case CHROME -> createChromeDriver(headless);
            case FIREFOX -> createFirefoxDriver(headless);
            case EDGE -> createEdgeDriver(headless);
            case SAFARI -> createSafariDriver();
        };

        // Configure timeouts
        driver.manage().timeouts().implicitlyWait(Duration.ZERO); // Using explicit waits
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout * 3L));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(timeout * 2L));

        // Store in ThreadLocal
        driverThreadLocal.set(driver);
        browserTypeThreadLocal.set(browserType);

        log.info("{} driver created successfully for thread: {}",
                browserType.getName(), Thread.currentThread().getName());

        return driver;
    }

    /**
     * Create Chrome driver with options
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        // Standard Chrome options for stability
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");
        options.addArguments("--remote-allow-origins=*");

        // Prevent password save popups
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        return new ChromeDriver(options);
    }

    /**
     * Create Firefox driver with options
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        return new FirefoxDriver(options);
    }

    /**
     * Create Edge driver with options
     */
    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();

        EdgeOptions options = new EdgeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        return new EdgeDriver(options);
    }

    /**
     * Create Safari driver (headless not supported)
     */
    private static WebDriver createSafariDriver() {
        // Safari doesn't support headless mode
        // SafariDriver is included with Safari, no setup needed
        log.info("Safari does not support headless mode, running with UI");
        return new SafariDriver();
    }

    /**
     * Quit the WebDriver for the current thread and clean up ThreadLocal.
     * Always removes from ThreadLocal to prevent memory leaks.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        BrowserType browserType = browserTypeThreadLocal.get();

        if (driver != null) {
            try {
                log.info("Quitting {} driver for thread: {}",
                        browserType != null ? browserType.getName() : "unknown",
                        Thread.currentThread().getName());
                driver.quit();
            } catch (Exception e) {
                log.warn("Error while quitting driver: {}", e.getMessage());
            } finally {
                // Always remove from ThreadLocal to prevent memory leaks
                driverThreadLocal.remove();
                browserTypeThreadLocal.remove();
            }
        }
    }

    /**
     * Check if a driver exists for the current thread
     */
    public static boolean hasDriver() {
        return driverThreadLocal.get() != null;
    }

    /**
     * Get the browser type for the current thread's driver
     */
    public static BrowserType getCurrentBrowserType() {
        return browserTypeThreadLocal.get();
    }

    /**
     * Create driver with custom ChromeOptions (for device emulation)
     */
    public static WebDriver createChromeDriverWithOptions(ChromeOptions options) {
        if (driverThreadLocal.get() != null) {
            quitDriver();
        }

        WebDriverManager.chromedriver().setup();

        // Add standard options
        Configuration config = Configuration.getInstance();
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);

        // Configure timeouts
        int timeout = config.getTimeout();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout * 3L));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(timeout * 2L));

        driverThreadLocal.set(driver);
        browserTypeThreadLocal.set(BrowserType.CHROME);

        log.info("Chrome driver with custom options created for thread: {}",
                Thread.currentThread().getName());

        return driver;
    }
}
