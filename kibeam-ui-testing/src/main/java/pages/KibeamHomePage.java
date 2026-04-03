package pages;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import base.BasePage;
import utils.KibeamUrls;
import utils.WaitUtils;

/**
 * Page Object for Kibeam.com Home Page
 * Handles main site navigation and header/footer interactions
 */
public class KibeamHomePage extends BasePage {

    static final Logger log = getLogger(lookup().lookupClass());

    // ========== HEADER NAV LOCATORS ==========
    private final By headerNav = By.cssSelector("header, nav, .header");
    private final By educatorsLink = By.cssSelector("a[href*='educators']");
    private final By aboutLink = By.cssSelector("a[href*='about']");
    private final By supportLink = By.cssSelector("a[href*='contact']");
    private final By cartIcon = By.cssSelector("a[href*='/cart'], .cart-icon, .header__cart");

    // ========== CONTACT SALES BUTTON ==========
    private final By contactSalesButton = By.cssSelector("a[href*='tidycal'], a[href*='calendly']");

    // ========== FOOTER LOCATORS ==========
    private final By footer = By.tagName("footer");

    // ========== CONSTRUCTOR ==========
    public KibeamHomePage(WebDriver driver) {
        super(driver);
    }

    // ========== NAVIGATION ACTIONS ==========

    /**
     * Navigate to Kibeam home page
     */
    public KibeamHomePage navigateToHome() {
        log.info("Navigating to Kibeam home page");
        visit(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(driver);
        return this;
    }

    /**
     * Click element with JavaScript fallback for better reliability
     */
    private void clickWithFallback(By locator) {
        try {
            WebElement element = WaitUtils.waitForElementClickable(driver, locator, timeoutSec);
            element.click();
        } catch (Exception e) {
            log.debug("Standard click failed, trying JavaScript click: " + e.getMessage());
            try {
                WebElement element = WaitUtils.waitForElementPresent(driver, locator, timeoutSec);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception jsException) {
                log.warn("JavaScript click also failed: " + jsException.getMessage());
                throw e;
            }
        }
    }

    /**
     * Click Educators link in navigation
     */
    public KibeamEducatorsPage clickEducators() {
        log.info("Clicking Educators link");
        clickWithFallback(educatorsLink);
        waitForUrlContains("educators");
        return new KibeamEducatorsPage(driver);
    }

    /**
     * Click About link in navigation
     */
    public KibeamHomePage clickAbout() {
        log.info("Clicking About link");
        clickWithFallback(aboutLink);
        waitForUrlContains("about");
        return this;
    }

    /**
     * Click Support/Contact link in navigation
     */
    public KibeamContactPage clickSupport() {
        log.info("Clicking Support link");
        clickWithFallback(supportLink);
        waitForUrlContains("contact");
        return new KibeamContactPage(driver);
    }

    // Login flow excluded — requires credentials

    /**
     * Click Contact Sales button (opens external booking - TidyCal)
     * Note: This may open in a new tab/window
     */
    public KibeamHomePage clickContactSales() {
        log.info("Clicking Contact Sales button");
        click(contactSalesButton);
        return this;
    }

    /**
     * Click cart icon
     */
    public KibeamHomePage clickCart() {
        log.info("Clicking cart icon");
        click(cartIcon);
        return this;
    }

    // ========== VERIFICATIONS ==========

    /**
     * Check if we're on the home page
     */
    public boolean isOnHomePage() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.equals(KibeamUrls.getBaseUrl())
            || currentUrl.equals(KibeamUrls.getBaseUrl() + "/")
            || currentUrl.contains("kibeam.com") && !currentUrl.contains("/pages/");
    }

    /**
     * Check if header navigation is visible
     */
    public boolean isNavVisible() {
        try {
            return isDisplayed(headerNav);
        } catch (Exception e) {
            log.debug("Header nav not found with primary selector, checking for any header");
            return isDisplayed(By.tagName("header"));
        }
    }

    /**
     * Check if footer is visible
     */
    public boolean isFooterVisible() {
        return isDisplayed(footer);
    }

    /**
     * Check if Educators link is present
     */
    public boolean isEducatorsLinkPresent() {
        try {
            WaitUtils.waitForElementPresent(driver, educatorsLink, 5);
            return true;
        } catch (Exception e) {
            log.debug("Educators link not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if Contact Sales button is present
     */
    public boolean isContactSalesButtonPresent() {
        try {
            WaitUtils.waitForElementPresent(driver, contactSalesButton, 5);
            return true;
        } catch (Exception e) {
            log.debug("Contact Sales button not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current page URL
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Get page title
     */
    public String getPageTitle() {
        return driver.getTitle();
    }
}
