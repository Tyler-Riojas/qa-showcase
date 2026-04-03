package pages;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import base.BasePage;
import utils.KibeamUrls;

/**
 * Page Object for Kibeam.com Contact/Support Page
 * Handles contact information and support interactions
 */
public class KibeamContactPage extends BasePage {

    static final Logger log = getLogger(lookup().lookupClass());

    // ========== CONTACT INFO LOCATORS ==========
    private final By phoneNumber = By.cssSelector("a[href^='tel:']");
    private final By address = By.cssSelector("address, [class*='address'], [class*='location'], p");
    private final By supportHours = By.cssSelector("[class*='hours'], [class*='support-hours'], [class*='business-hours']");

    // ========== EMAIL LOCATORS ==========
    private final By emailLink = By.cssSelector("a[href*='support.kibeam.com'], a[href^='mailto:']");

    // ========== FAQ LOCATORS ==========
    private final By faqLink = By.cssSelector("a[href*='support.kibeam.com/hc']");

    // ========== CONTACT FORM LOCATORS (if present) ==========
    private final By contactForm = By.cssSelector("form[class*='contact'], form[id*='contact'], .contact-form");

    // ========== CONSTRUCTOR ==========
    public KibeamContactPage(WebDriver driver) {
        super(driver);
    }

    // ========== NAVIGATION ACTIONS ==========

    /**
     * Navigate directly to Contact page
     */
    public KibeamContactPage navigateToContact() {
        log.info("Navigating to Kibeam Contact page");
        visit(KibeamUrls.getContactUrl());
        return this;
    }

    /**
     * Click Email Us link
     */
    public KibeamContactPage clickEmailUs() {
        log.info("Clicking Email Us link");
        click(emailLink);
        return this;
    }

    /**
     * Click FAQs link
     */
    public KibeamContactPage clickFAQs() {
        log.info("Clicking FAQs link");
        click(faqLink);
        return this;
    }

    // ========== VERIFICATIONS ==========

    /**
     * Check if we're on the Contact page
     */
    public boolean isOnContactPage() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("contact");
    }

    /**
     * Check if phone number is displayed
     */
    public boolean isPhoneDisplayed() {
        try {
            return isDisplayed(phoneNumber);
        } catch (Exception e) {
            log.debug("Phone number not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get phone number text
     */
    public String getPhoneNumber() {
        try {
            return getText(phoneNumber);
        } catch (Exception e) {
            log.debug("Could not get phone number: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if address is displayed
     */
    public boolean isAddressDisplayed() {
        try {
            return isDisplayed(address);
        } catch (Exception e) {
            log.debug("Address not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get address text
     */
    public String getAddress() {
        try {
            return getText(address);
        } catch (Exception e) {
            log.debug("Could not get address: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if support hours are displayed
     */
    public boolean isSupportHoursDisplayed() {
        try {
            return isDisplayed(supportHours);
        } catch (Exception e) {
            log.debug("Support hours not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if email link exists
     */
    public boolean isEmailLinkPresent() {
        try {
            return isDisplayed(emailLink);
        } catch (Exception e) {
            log.debug("Email link not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if FAQ link exists
     */
    public boolean isFaqLinkPresent() {
        try {
            return isDisplayed(faqLink);
        } catch (Exception e) {
            log.debug("FAQ link not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if contact form is present
     */
    public boolean isContactFormPresent() {
        try {
            return isDisplayed(contactForm);
        } catch (Exception e) {
            log.debug("Contact form not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get page title
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Get current URL
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
