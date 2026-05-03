package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WaitUtils;

import static java.lang.invoke.MethodHandles.lookup;

public class AddEmployeePage {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private final WebDriver driver;

    private final By firstNameInput  = By.name("firstName");
    private final By lastNameInput   = By.name("lastName");
    // Employee ID has no name/id/placeholder — anchor on the label text
    private final By employeeIdInput = By.xpath(
            "//label[normalize-space()='Employee Id']/../..//input");
    private final By saveButton      = By.cssSelector("button[type=submit]");
    // OrangeHRM shows a toast on success; falls back to URL redirect check
    private final By successToast   = By.cssSelector(".oxd-toast--success");
    private final By errorToast     = By.cssSelector(".oxd-toast--error");

    public AddEmployeePage(WebDriver driver) {
        this.driver = driver;
    }

    public void enterFirstName(String name) {
        WebElement field = WaitUtils.waitForElementVisible(driver, firstNameInput);
        field.clear();
        field.sendKeys(name);
    }

    public void enterLastName(String name) {
        WebElement field = WaitUtils.waitForElementVisible(driver, lastNameInput);
        field.clear();
        field.sendKeys(name);
    }

    /**
     * Sets the Employee ID field via JavaScript to properly trigger Vue.js reactivity.
     * Plain clear() + sendKeys() breaks Vue's reactive model, leaving the form invalid.
     */
    public void enterEmployeeId(String id) {
        WebElement field = WaitUtils.waitForElementVisible(driver, employeeIdInput, 10);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "setter.call(arguments[0], arguments[1]);" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                field, id);
        log.info("Employee ID set to: {}", id);
    }

    public void clickSave() {
        log.info("Clicking Save");
        // Wait for any loading overlay to disappear — oxd-form-loader intercepts clicks
        By formLoader = By.cssSelector(".oxd-form-loader");
        try {
            WaitUtils.waitForElementInvisible(driver, formLoader, 10);
        } catch (Exception e) {
            log.debug("No form loader found or already gone");
        }
        WebElement btn = WaitUtils.waitForElementClickable(driver, saveButton, 15);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true)", btn);
        try {
            btn.click();
        } catch (Exception e) {
            log.debug("Regular click failed, trying JS click");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click()", btn);
        }
    }

    /**
     * Returns true if the employee was saved.
     * Checks for error toast first — if present, save failed (e.g. "Employee already exists").
     * Then checks success toast; falls back to URL redirect check.
     */
    public boolean isEmployeeSaved() {
        try {
            WaitUtils.waitForElementVisible(driver, errorToast, 3);
            log.warn("Employee save failed — error toast visible");
            return false;
        } catch (Exception ignored) {
            // No error toast — proceed to success checks
        }
        try {
            WaitUtils.waitForElementVisible(driver, successToast, 5);
            log.info("Employee saved — success toast visible");
            return true;
        } catch (Exception e) {
            // Fall through to URL redirect check
        }
        try {
            boolean redirected = WaitUtils.waitForUrlContains(driver, "/pim/viewPersonalDetails", 15);
            log.info("Employee saved via URL redirect: {}", redirected);
            return redirected;
        } catch (Exception e) {
            log.warn("Employee save: no toast and no redirect — save may have failed");
            return false;
        }
    }
}
