package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WaitUtils;

import static java.lang.invoke.MethodHandles.lookup;

public class AddEmployeePage {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private final WebDriver driver;

    private final By firstNameInput = By.name("firstName");
    private final By lastNameInput  = By.name("lastName");
    private final By saveButton     = By.cssSelector("button[type=submit]");
    // OrangeHRM shows a toast on success; falls back to URL redirect check
    private final By successToast   = By.cssSelector(".oxd-toast--success");

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

    public void clickSave() {
        log.info("Clicking Save");
        WaitUtils.waitForElementClickable(driver, saveButton).click();
    }

    /**
     * Returns true if the employee was saved.
     * Checks for the success toast first (fast); falls back to URL redirect
     * to the personal-details page (OrangeHRM's default post-save navigation).
     */
    public boolean isEmployeeSaved() {
        try {
            WaitUtils.waitForElementVisible(driver, successToast, 5);
            log.info("Employee saved — success toast visible");
            return true;
        } catch (Exception e) {
            boolean redirected = WaitUtils.waitForUrlContains(driver, "/pim/viewPersonalDetails", 15);
            log.info("Employee saved via URL redirect: {}", redirected);
            return redirected;
        }
    }
}
