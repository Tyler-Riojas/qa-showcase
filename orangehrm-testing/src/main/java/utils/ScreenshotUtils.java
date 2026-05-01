package utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.apache.commons.io.FileUtils;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    /**
     * Captures screenshot of a specific element and returns as Base64.
     *
     * @param driver  WebDriver instance
     * @param element WebElement to capture
     * @return Base64 encoded screenshot string, or null on failure
     */
    public static String captureElementScreenshotAsBase64(WebDriver driver, WebElement element) {
        try {
            return element.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.debug("Failed to capture element screenshot as Base64: {}", e.getMessage());
            return null;
        }
    }
}