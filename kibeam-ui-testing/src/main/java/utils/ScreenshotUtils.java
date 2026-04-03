package utils;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

public class ScreenshotUtils {
    
    static final Logger log = getLogger(lookup().lookupClass());
    
    private static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/screenshots";
    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    
    /**
     * Captures a screenshot and saves it to the screenshots folder
     * @param driver WebDriver instance
     * @param screenshotName Name for the screenshot file
     * @return Path to the saved screenshot
     */
    public static String captureScreenshot(WebDriver driver, String screenshotName) {
        // Create screenshots directory if it doesn't exist
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            boolean created = screenshotDir.mkdirs();
            if (created) {
                log.debug("Created screenshots directory: " + SCREENSHOT_DIR);
            }
        }
        
        // Get browser name for better organization
        String browserName = getBrowserName(driver);
        
        // Generate timestamp for unique filename
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        String fileName = screenshotName + "_" + browserName + "_" + timestamp + ".png";
        String destination = screenshotDir.getAbsolutePath() + "/" + fileName;
        
        try {
            // Capture screenshot
            TakesScreenshot ts = (TakesScreenshot) driver;
            File source = ts.getScreenshotAs(OutputType.FILE);
            
            // Copy to destination
            FileUtils.copyFile(source, new File(destination));
            
            log.info("Screenshot captured: " + fileName);
            return destination;
            
        } catch (IOException e) {
            log.error("Failed to capture screenshot: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error capturing screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Captures screenshot with default "screenshot" name
     */
    public static String captureScreenshot(WebDriver driver) {
        return captureScreenshot(driver, "screenshot");
    }
    
    /**
     * Get browser name from WebDriver
     */
    private static String getBrowserName(WebDriver driver) {
        try {
            Capabilities capabilities = ((RemoteWebDriver) driver).getCapabilities();
            String browserName = capabilities.getBrowserName();
            return browserName != null ? browserName.toLowerCase() : "unknown";
        } catch (Exception e) {
            log.debug("Could not determine browser name: " + e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Captures screenshot and returns as Base64 (useful for reporting)
     */
    public static String captureScreenshotAsBase64(WebDriver driver) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            return ts.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.error("Failed to capture screenshot as Base64: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Captures a screenshot of a specific element and saves it to the screenshots folder.
     * The element is highlighted before capture for visibility.
     *
     * @param driver WebDriver instance
     * @param element WebElement to capture
     * @param screenshotName Name for the screenshot file
     * @return Path to the saved screenshot, or null if failed
     */
    public static String captureElementScreenshot(WebDriver driver, WebElement element, String screenshotName) {
        // Create screenshots directory if it doesn't exist
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }

        String browserName = getBrowserName(driver);
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        String sanitizedName = sanitizeFileName(screenshotName);
        String fileName = sanitizedName + "_" + browserName + "_" + timestamp + ".png";
        String destination = screenshotDir.getAbsolutePath() + "/" + fileName;

        try {
            // Highlight the element before screenshot
            highlightElement(driver, element);

            // Scroll element into view
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);

            // Small delay to ensure scroll completes
            Thread.sleep(100);

            // Capture screenshot of the element
            File source = element.getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(source, new File(destination));

            // Remove highlight
            unhighlightElement(driver, element);

            log.info("Element screenshot captured: {}", fileName);
            return destination;

        } catch (Exception e) {
            log.error("Failed to capture element screenshot: {}", e.getMessage(), e);
            // Try to capture full page screenshot as fallback
            return captureScreenshotWithHighlight(driver, element, screenshotName);
        }
    }

    /**
     * Captures a screenshot of an element found by CSS selector.
     *
     * @param driver WebDriver instance
     * @param cssSelector CSS selector to find the element
     * @param screenshotName Name for the screenshot file
     * @return Path to the saved screenshot, or null if failed
     */
    public static String captureElementScreenshot(WebDriver driver, String cssSelector, String screenshotName) {
        try {
            WebElement element = driver.findElement(By.cssSelector(cssSelector));
            return captureElementScreenshot(driver, element, screenshotName);
        } catch (Exception e) {
            log.warn("Could not find element with selector '{}', capturing full page instead", cssSelector);
            return captureScreenshot(driver, screenshotName + "_FULL_PAGE");
        }
    }

    /**
     * Captures a screenshot of an element as Base64 (useful for Extent Reports embedding).
     *
     * @param driver WebDriver instance
     * @param element WebElement to capture
     * @return Base64 encoded screenshot string, or null if failed
     */
    public static String captureElementScreenshotAsBase64(WebDriver driver, WebElement element) {
        try {
            // Highlight the element
            highlightElement(driver, element);

            // Scroll element into view
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);

            Thread.sleep(100);

            // Capture element screenshot as Base64
            String base64 = element.getScreenshotAs(OutputType.BASE64);

            // Remove highlight
            unhighlightElement(driver, element);

            return base64;

        } catch (Exception e) {
            log.error("Failed to capture element screenshot as Base64: {}", e.getMessage());
            // Fallback to full page screenshot
            return captureScreenshotAsBase64(driver);
        }
    }

    /**
     * Captures a screenshot of an element found by CSS selector as Base64.
     *
     * @param driver WebDriver instance
     * @param cssSelector CSS selector to find the element
     * @return Base64 encoded screenshot string, or null if failed
     */
    public static String captureElementScreenshotAsBase64(WebDriver driver, String cssSelector) {
        try {
            WebElement element = driver.findElement(By.cssSelector(cssSelector));
            return captureElementScreenshotAsBase64(driver, element);
        } catch (Exception e) {
            log.warn("Could not find element with selector '{}', capturing full page", cssSelector);
            return captureScreenshotAsBase64(driver);
        }
    }

    /**
     * Captures full page screenshot with a specific element highlighted.
     *
     * @param driver WebDriver instance
     * @param element Element to highlight
     * @param screenshotName Name for the screenshot file
     * @return Path to the saved screenshot
     */
    public static String captureScreenshotWithHighlight(WebDriver driver, WebElement element, String screenshotName) {
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }

        String browserName = getBrowserName(driver);
        String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        String sanitizedName = sanitizeFileName(screenshotName);
        String fileName = sanitizedName + "_HIGHLIGHTED_" + browserName + "_" + timestamp + ".png";
        String destination = screenshotDir.getAbsolutePath() + "/" + fileName;

        try {
            // Highlight the element
            highlightElement(driver, element);

            // Scroll element into view
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);

            Thread.sleep(100);

            // Capture full page screenshot
            TakesScreenshot ts = (TakesScreenshot) driver;
            File source = ts.getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(source, new File(destination));

            // Remove highlight
            unhighlightElement(driver, element);

            log.info("Highlighted screenshot captured: {}", fileName);
            return destination;

        } catch (Exception e) {
            log.error("Failed to capture highlighted screenshot: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Captures full page screenshot with element highlighted as Base64.
     *
     * @param driver WebDriver instance
     * @param element Element to highlight
     * @return Base64 encoded screenshot string
     */
    public static String captureScreenshotWithHighlightAsBase64(WebDriver driver, WebElement element) {
        try {
            highlightElement(driver, element);

            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);

            Thread.sleep(100);

            TakesScreenshot ts = (TakesScreenshot) driver;
            String base64 = ts.getScreenshotAs(OutputType.BASE64);

            unhighlightElement(driver, element);

            return base64;

        } catch (Exception e) {
            log.error("Failed to capture highlighted screenshot as Base64: {}", e.getMessage());
            return captureScreenshotAsBase64(driver);
        }
    }

    /**
     * Highlights an element with a red border for visibility.
     */
    public static void highlightElement(WebDriver driver, WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "arguments[0].setAttribute('data-original-style', arguments[0].getAttribute('style') || '');" +
                "arguments[0].style.border = '3px solid red';" +
                "arguments[0].style.backgroundColor = 'rgba(255, 0, 0, 0.1)';",
                element);
        } catch (Exception e) {
            log.debug("Could not highlight element: {}", e.getMessage());
        }
    }

    /**
     * Removes highlight from an element.
     */
    public static void unhighlightElement(WebDriver driver, WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "var originalStyle = arguments[0].getAttribute('data-original-style');" +
                "if (originalStyle) { arguments[0].setAttribute('style', originalStyle); }" +
                "else { arguments[0].removeAttribute('style'); }" +
                "arguments[0].removeAttribute('data-original-style');",
                element);
        } catch (Exception e) {
            log.debug("Could not unhighlight element: {}", e.getMessage());
        }
    }

    /**
     * Sanitizes a string for use as a filename.
     */
    private static String sanitizeFileName(String name) {
        if (name == null) return "element";
        // Remove or replace invalid filename characters
        return name.replaceAll("[^a-zA-Z0-9._-]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }

    /**
     * Clean up old screenshots (keep last N days)
     */
    public static void cleanupOldScreenshots(int daysToKeep) {
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            return;
        }
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
        int deletedCount = 0;
        
        File[] files = screenshotDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }
        }
        
        if (deletedCount > 0) {
            log.info("Cleaned up " + deletedCount + " old screenshot(s)");
        }
    }
}