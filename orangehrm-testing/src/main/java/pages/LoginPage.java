package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WaitUtils;

import static java.lang.invoke.MethodHandles.lookup;

public class LoginPage {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private static final String LOGIN_URL =
            "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login";

    private final WebDriver driver;

    private final By usernameInput  = By.name("username");
    private final By passwordInput  = By.name("password");
    private final By loginButton    = By.cssSelector("button[type=submit]");
    private final By errorMessage   = By.cssSelector(".oxd-alert-content");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    public LoginPage navigateTo() {
        log.info("Navigating to login page");
        driver.get(LOGIN_URL);
        WaitUtils.waitForElementVisible(driver, usernameInput);
        return this;
    }

    public void login(String username, String password) {
        log.info("Logging in as: {}", username);
        WaitUtils.waitForElementVisible(driver, usernameInput).clear();
        driver.findElement(usernameInput).sendKeys(username);
        WaitUtils.waitForElementVisible(driver, passwordInput).clear();
        driver.findElement(passwordInput).sendKeys(password);
        WaitUtils.waitForElementClickable(driver, loginButton).click();
    }

    /**
     * Returns true once the URL contains /dashboard, meaning login succeeded.
     */
    public boolean isLoginSuccessful() {
        return WaitUtils.waitForUrlContains(driver, "/dashboard", 15);
    }

    public String getErrorMessage() {
        return WaitUtils.waitForElementVisible(driver, errorMessage).getText();
    }
}
