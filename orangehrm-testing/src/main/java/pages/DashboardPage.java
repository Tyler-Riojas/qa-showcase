package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WaitUtils;

import static java.lang.invoke.MethodHandles.lookup;

public class DashboardPage {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private final WebDriver driver;

    private final By mainMenu       = By.cssSelector(".oxd-main-menu");
    private final By userMenu       = By.cssSelector(".oxd-userdropdown");
    // Nav items are anchor tags with class oxd-main-menu-item; the span text is not
    // reliably set in headless Chrome, so use href-based CSS selectors instead.
    private final By pimMenuItem    = By.cssSelector("a.oxd-main-menu-item[href*='/pim/viewPimModule']");
    private final By myInfoMenuItem = By.cssSelector("a.oxd-main-menu-item[href*='/pim/viewMyDetails']");
    // Logout link lives in the user dropdown; XPath text() match works once the
    // dropdown is open.  Using the specific link class for reliability.
    private final By logoutMenuItem = By.cssSelector("a.oxd-userdropdown-link[href*='/auth/logout']");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
    }

    /** Returns true once the dashboard URL is confirmed. */
    public boolean isLoaded() {
        return WaitUtils.waitForUrlContains(driver, "/dashboard", 15);
    }

    public void navigateToPIM() {
        log.info("Navigating to PIM");
        WaitUtils.waitForElementClickable(driver, pimMenuItem).click();
        WaitUtils.waitForUrlContains(driver, "/pim", 10);
    }

    public void navigateToMyInfo() {
        log.info("Navigating to My Info");
        WaitUtils.waitForElementClickable(driver, myInfoMenuItem).click();
        // OrangeHRM redirects /pim/viewMyDetails to /pim/viewPersonalDetails/empNumber/N
        // for the logged-in user; accept either URL as confirmation.
        WaitUtils.waitForUrlContains(driver, "/pim/view", 10);
    }

    public void logout() {
        log.info("Logging out");
        // Must click the user dropdown first to expand it before logout link appears
        WaitUtils.waitForElementClickable(driver, userMenu).click();
        WaitUtils.waitForElementClickable(driver, logoutMenuItem).click();
        WaitUtils.waitForUrlContains(driver, "/auth/login", 10);
    }
}
