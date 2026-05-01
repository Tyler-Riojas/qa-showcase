package utils;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

public class WaitUtils {
    
    static final Logger log = getLogger(lookup().lookupClass());
    
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_POLLING = 500; // milliseconds
    
    /**
     * Wait for element to be visible
     */
    public static WebElement waitForElementVisible(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be visible: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    public static WebElement waitForElementVisible(WebDriver driver, By locator) {
        return waitForElementVisible(driver, locator, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for element to be clickable
     */
    public static WebElement waitForElementClickable(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be clickable: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    public static WebElement waitForElementClickable(WebDriver driver, By locator) {
        return waitForElementClickable(driver, locator, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for element to be present in DOM (may not be visible)
     */
    public static WebElement waitForElementPresent(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be present: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }
    
    public static WebElement waitForElementPresent(WebDriver driver, By locator) {
        return waitForElementPresent(driver, locator, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for element to disappear/become invisible
     */
    public static boolean waitForElementInvisible(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for element to be invisible: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }
    
    public static boolean waitForElementInvisible(WebDriver driver, By locator) {
        return waitForElementInvisible(driver, locator, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for text to be present in element
     */
    public static boolean waitForTextPresent(WebDriver driver, By locator, String text, int timeoutSeconds) {
        log.debug("Waiting for text '" + text + "' in element: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }
    
    public static boolean waitForTextPresent(WebDriver driver, By locator, String text) {
        return waitForTextPresent(driver, locator, text, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for all elements to be visible
     */
    public static List<WebElement> waitForAllElementsVisible(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting for all elements to be visible: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }
    
    public static List<WebElement> waitForAllElementsVisible(WebDriver driver, By locator) {
        return waitForAllElementsVisible(driver, locator, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for page to load completely
     */
    public static void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting for page to load completely");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until((ExpectedCondition<Boolean>) wd ->
            ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete")
        );
    }
    
    public static void waitForPageLoad(WebDriver driver) {
        waitForPageLoad(driver, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for jQuery to finish (if page uses jQuery)
     */
    public static void waitForJQueryLoad(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting for jQuery to finish");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until((ExpectedCondition<Boolean>) wd -> {
            JavascriptExecutor js = (JavascriptExecutor) wd;
            return (Boolean) js.executeScript("return jQuery.active == 0");
        });
    }
    
    public static void waitForJQueryLoad(WebDriver driver) {
        waitForJQueryLoad(driver, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for specific number of elements to be present
     */
    public static @Nullable List<WebElement> waitForNumberOfElements(WebDriver driver, By locator, int expectedCount, int timeoutSeconds) {
        log.debug("Waiting for " + expectedCount + " elements: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.numberOfElementsToBe(locator, expectedCount));
    }
    
    public static @Nullable List<WebElement> waitForNumberOfElements(WebDriver driver, By locator, int expectedCount) {
        return waitForNumberOfElements(driver, locator, expectedCount, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for URL to contain specific text
     */
    public static boolean waitForUrlContains(WebDriver driver, String urlFragment, int timeoutSeconds) {
        log.debug("Waiting for URL to contain: " + urlFragment);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }
    
    public static boolean waitForUrlContains(WebDriver driver, String urlFragment) {
        return waitForUrlContains(driver, urlFragment, DEFAULT_TIMEOUT);
    }
    
    /**
     * Wait for attribute to have specific value
     */
    public static boolean waitForAttributeToBe(WebDriver driver, By locator, String attribute, String value, int timeoutSeconds) {
        log.debug("Waiting for attribute '" + attribute + "' to be '" + value + "' on element: " + locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }
    
    public static boolean waitForAttributeToBe(WebDriver driver, By locator, String attribute, String value) {
        return waitForAttributeToBe(driver, locator, attribute, value, DEFAULT_TIMEOUT);
    }
    
    /**
     * Custom wait with polling interval
     */
    public static <T> T waitForCondition(WebDriver driver, ExpectedCondition<T> condition, int timeoutSeconds, int pollingMillis) {
        log.debug("Waiting for custom condition");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds), Duration.ofMillis(pollingMillis));
        return wait.until(condition);
    }
    
    public static <T> T waitForCondition(WebDriver driver, ExpectedCondition<T> condition, int timeoutSeconds) {
        return waitForCondition(driver, condition, timeoutSeconds, DEFAULT_POLLING);
    }
    
    /**
     * Fluent wait - useful for highly dynamic elements
     */
    public static void fluentWait(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Using fluent wait for: " + locator);
        org.openqa.selenium.support.ui.FluentWait<WebDriver> wait = new org.openqa.selenium.support.ui.FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(timeoutSeconds))
            .pollingEvery(Duration.ofMillis(500))
            .ignoring(org.openqa.selenium.NoSuchElementException.class);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    /**
     * Simple sleep (use sparingly - explicit waits are better)
     */
    public static void hardWait(int seconds) {
        log.warn("Using hard wait for " + seconds + " seconds (not recommended)");
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Hard wait interrupted", e);
        }
    }
}