package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WaitUtils;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

public class MyInfoPage {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private final WebDriver driver;

    private final By firstNameInput = By.name("firstName");
    private final By lastNameInput  = By.name("lastName");
    // OrangeHRM Personal Details does not have a "Nickname" placeholder input.
    // The "Other Id" field is a plain text input in the Personal Details form —
    // use it as the writable "nickname-style" field for the smoke test.
    private final By nicknameInput  = By.xpath(
            "//label[normalize-space()='Other Id']" +
            "/ancestor::div[@class='oxd-input-group oxd-input-field-bottom-space']" +
            "//input[@class='oxd-input oxd-input--active']");
    // Multiple save buttons on the page; first one is the Personal Details section
    private final By saveButtons    = By.cssSelector("button[type=submit]");
    // The success toast element: class oxd-toast--success is confirmed in the DOM
    private final By successToast   = By.cssSelector(".oxd-toast--success");

    public MyInfoPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isLoaded() {
        // OrangeHRM redirects /pim/viewMyDetails to /pim/viewPersonalDetails/empNumber/N
        // Accept both URL forms.
        return WaitUtils.waitForUrlContains(driver, "/pim/view", 15);
    }

    public void updateNickname(String nickname) {
        log.info("Updating nickname to: {}", nickname);
        WebElement field = WaitUtils.waitForElementVisible(driver, nicknameInput, 10);
        field.clear();
        field.sendKeys(nickname);
    }

    /** Clicks the Save button in the Personal Details section. */
    public void clickSave() {
        log.info("Clicking Save on Personal Details");
        // The Personal Details section is the first oxd-form on the page.
        // Select the first submit button (index 1 in XPath = first match).
        By personalDetailsSaveBtn = By.xpath(
                "(//button[@type='submit'])[1]");
        WaitUtils.waitForElementClickable(driver, personalDetailsSaveBtn, 10).click();
    }

    /**
     * Returns true once the success toast appears.
     * OrangeHRM toasts are brief — wait up to 8 seconds.
     */
    public boolean isUpdateSuccessful() {
        try {
            WaitUtils.waitForElementVisible(driver, successToast, 8);
            log.info("Update successful — success toast visible");
            return true;
        } catch (Exception e) {
            log.debug("Success toast not found: {}", e.getMessage());
            return false;
        }
    }

    public boolean isFirstNameFieldEditable() {
        try {
            WebElement field = WaitUtils.waitForElementVisible(driver, firstNameInput, 5);
            return field.isEnabled() && field.getAttribute("readonly") == null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLastNameFieldEditable() {
        try {
            WebElement field = WaitUtils.waitForElementVisible(driver, lastNameInput, 5);
            return field.isEnabled() && field.getAttribute("readonly") == null;
        } catch (Exception e) {
            return false;
        }
    }
}
