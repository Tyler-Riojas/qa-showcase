package base;

import core.ExtensionDriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base class for all Chrome extension tests.
 *
 * <p>Manages the shared WebDriver lifecycle across the suite and provides
 * screenshot capture on test failure.</p>
 *
 * <p>A single browser instance is used for the entire suite because Chrome
 * extension IDs are assigned at browser startup — spinning up a new browser
 * per test would require re-discovering the ID on every test and would also
 * be extremely slow.</p>
 */
public class BaseExtensionTest extends base.BaseTestTestNG {

    private static final Logger log = LoggerFactory.getLogger(BaseExtensionTest.class);

    /** Shared WebDriver instance for the entire test suite. Static so all test class instances share it. */
    protected static WebDriver driver;

    /** Runtime extension ID discovered at suite startup. */
    protected static String extensionId;

    /** chrome-extension:// popup URL discovered at suite startup. */
    protected static String popupUrl;

    // ==================== Suite Lifecycle ====================

    @BeforeSuite(alwaysRun = true)
    public void suiteSetUp() {
        log.info("=== Chrome Extension Test Suite Starting ===");
        driver = ExtensionDriverFactory.createDriver();
        extensionId = ExtensionDriverFactory.getExtensionId();
        popupUrl = ExtensionDriverFactory.getPopupUrl();
        log.info("Extension ID : {}", extensionId);
        log.info("Popup URL    : {}", popupUrl);
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        log.info("=== Chrome Extension Test Suite Finished ===");
        ExtensionDriverFactory.quitDriver(driver);
    }

    // ==================== Method Lifecycle ====================

    @BeforeMethod(alwaysRun = true)
    public void resetState() {
        if (driver != null) {
            driver.get("about:blank");
        }
    }

    @AfterMethod(alwaysRun = true)
    public void captureOnFailure(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            takeScreenshot(result.getMethod().getMethodName());
        }
    }

    // ==================== BaseTestTestNG contract ====================

    /**
     * Exposes the shared driver instance so that {@link listeners.ExtentTestNGListener}
     * can capture screenshots on failure.
     */
    @Override
    public WebDriver getDriver() {
        return driver;
    }

    // ==================== Helpers ====================

    private void takeScreenshot(String testName) {
        if (driver == null) {
            log.warn("Driver is null — cannot take screenshot for: {}", testName);
            return;
        }
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] bytes = ts.getScreenshotAs(OutputType.BYTES);

            Path screenshotsDir = Paths.get("screenshots");
            if (!Files.exists(screenshotsDir)) {
                Files.createDirectories(screenshotsDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = testName + "_FAILED_" + timestamp + ".png";
            Path filePath = screenshotsDir.resolve(fileName);

            Files.write(filePath, bytes);
            log.info("Screenshot saved: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save screenshot for {}: {}", testName, e.getMessage());
        }
    }
}
