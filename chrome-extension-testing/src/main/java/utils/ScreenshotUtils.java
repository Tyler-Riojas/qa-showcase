package utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for capturing Selenium screenshots.
 */
public class ScreenshotUtils {

    static final Logger log = getLogger(lookup().lookupClass());

    private static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/screenshots";
    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    public static String captureScreenshot(WebDriver driver, String screenshotName) {
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));

            String browserName = getBrowserName(driver);
            String timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            String fileName = screenshotName + "_" + browserName + "_" + timestamp + ".png";
            String destination = SCREENSHOT_DIR + "/" + fileName;

            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] bytes = ts.getScreenshotAs(OutputType.BYTES);
            Files.write(Paths.get(destination), bytes);

            log.info("Screenshot captured: {}", fileName);
            return destination;
        } catch (IOException e) {
            log.error("Failed to capture screenshot: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String captureScreenshotAsBase64(WebDriver driver) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            return ts.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.error("Failed to capture screenshot as Base64: {}", e.getMessage(), e);
            return null;
        }
    }

    private static String getBrowserName(WebDriver driver) {
        try {
            Capabilities capabilities = ((RemoteWebDriver) driver).getCapabilities();
            String browserName = capabilities.getBrowserName();
            return browserName != null ? browserName.toLowerCase() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
