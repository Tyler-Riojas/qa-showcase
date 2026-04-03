package base;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import utils.WaitUtils;

public class BasePage {

    static final Logger log = getLogger(lookup().lookupClass());

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected int timeoutSec = 10; // wait timeout (10 seconds by default)

    // Constructor receives driver from BaseTest
    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
    }

    // Allow changing timeout if needed
    public void setTimeoutSec(int timeoutSec) {
        this.timeoutSec = timeoutSec;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
    }

    // Navigate to URL with page load wait
    public void visit(String url) {
        driver.get(url);
        WaitUtils.waitForPageLoad(driver, timeoutSec);
    }

    // Find element with visibility wait
    public WebElement find(By locator) {
        return WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
    }

    // Click methods - overloaded with clickable wait
    public void click(WebElement element) {
        element.click();
    }

    public void click(By locator) {
        WebElement element = WaitUtils.waitForElementClickable(driver, locator, timeoutSec);
        element.click();
    }

    // Type methods - overloaded with visibility wait
    public void type(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    public void type(By locator, String text) {
        WebElement element = WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
        element.clear();
        element.sendKeys(text);
    }

    // Get text methods with visibility wait
    public String getText(WebElement element) {
        return element.getText();
    }

    public String getText(By locator) {
        WebElement element = WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
        return element.getText();
    }

    // IsDisplayed methods - overloaded with waits
    public boolean isDisplayed(WebElement element) {
        return isDisplayed(ExpectedConditions.visibilityOf(element));
    }

    public boolean isDisplayed(By locator) {
        return isDisplayed(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public boolean isDisplayed(ExpectedCondition<?> expectedCondition) {
        try {
            wait.until(expectedCondition);
            return true;
        } catch (TimeoutException e) {
            log.warn("Timeout of {} seconds waiting for element", timeoutSec);
            return false;
        }
    }

    // Wait for element to be clickable
    public void waitForClickable(By locator) {
        WaitUtils.waitForElementClickable(driver, locator, timeoutSec);
    }

    // Wait for element to be visible
    public void waitForVisible(By locator) {
        WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
    }

    // Select from dropdown with visibility wait
    public void selectByVisibleText(By locator, String text) {
        WebElement element = WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
        Select select = new Select(element);
        select.selectByVisibleText(text);
    }

    public void selectByValue(By locator, String value) {
        WebElement element = WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
        Select select = new Select(element);
        select.selectByValue(value);
    }

    // Get attribute value with visibility wait
    public String getAttribute(By locator, String attribute) {
        WebElement element = WaitUtils.waitForElementVisible(driver, locator, timeoutSec);
        return element.getAttribute(attribute);
    }

    // Check if element exists (without waiting) - KEPT AS IS
    public boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // Scroll to element with presence wait
    public void scrollToElement(By locator) {
        WebElement element = WaitUtils.waitForElementPresent(driver, locator, timeoutSec);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView(true);", element);
    }

    // ========== ADDITIONAL WAIT METHODS (Delegating to WaitUtils) ==========

    // Wait for element to be present (may not be visible)
    public void waitForPresent(By locator) {
        WaitUtils.waitForElementPresent(driver, locator, timeoutSec);
    }

    // Wait for element to become invisible
    public void waitForInvisible(By locator) {
        WaitUtils.waitForElementInvisible(driver, locator, timeoutSec);
    }

    // Wait for specific text to appear in element
    public void waitForTextPresent(By locator, String text) {
        WaitUtils.waitForTextPresent(driver, locator, text, timeoutSec);
    }

    // Wait for URL to contain specific text
    public void waitForUrlContains(String urlFragment) {
        WaitUtils.waitForUrlContains(driver, urlFragment, timeoutSec);
    }

    // Wait for page to fully load
    public void waitForPageLoad() {
        WaitUtils.waitForPageLoad(driver, timeoutSec);
    }

 // Wait for specific number of elements
    public List<WebElement> waitForNumberOfElements(By locator, int count) {
        return WaitUtils.waitForNumberOfElements(driver, locator, count, timeoutSec);
    }

    // Wait for attribute to have specific value
    public void waitForAttributeToBe(By locator, String attribute, String value) {
        WaitUtils.waitForAttributeToBe(driver, locator, attribute, value, timeoutSec);
    }

}