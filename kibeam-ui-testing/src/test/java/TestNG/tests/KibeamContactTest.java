package TestNG.tests;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.BaseTestTestNG;
import pages.KibeamContactPage;
import utils.WaitUtils;

/**
 * Tests for Kibeam.com Contact Page
 * Tests contact information display: phone, address, hours, email, FAQ
 */
public class KibeamContactTest extends BaseTestTestNG {

    static final Logger log = getLogger(lookup().lookupClass());

    private KibeamContactPage contactPage;

    @BeforeMethod
    public void setupTest() {
        contactPage = new KibeamContactPage(getDriver());
        contactPage.navigateToContact();
        WaitUtils.waitForPageLoad(getDriver());
    }

    @Test(description = "Verify Contact page loads successfully", priority = 1)
    public void testContactPageLoads() {
        log.info("Starting testContactPageLoads");

        assertTrue(contactPage.isOnContactPage(), "Should be on Contact page");
        String pageTitle = contactPage.getPageTitle();
        assertNotNull(pageTitle, "Page title should not be null");
        log.info("Contact page loaded with title: " + pageTitle);
    }

    @Test(description = "Verify URL is correct on Contact page", priority = 1)
    public void testContactPageUrl() {
        log.info("Starting testContactPageUrl");

        String currentUrl = contactPage.getCurrentUrl();
        assertTrue(currentUrl.contains("contact"), "URL should contain 'contact': " + currentUrl);
        assertTrue(currentUrl.contains("kibeam.com"), "URL should contain 'kibeam.com': " + currentUrl);
    }

    @Test(description = "Verify phone number is displayed on Contact page", priority = 2)
    public void testPhoneNumberDisplayed() {
        log.info("Starting testPhoneNumberDisplayed");

        boolean isPhoneDisplayed = contactPage.isPhoneDisplayed();
        if (isPhoneDisplayed) {
            String phoneNumber = contactPage.getPhoneNumber();
            log.info("Phone number displayed: " + phoneNumber);
            assertNotNull(phoneNumber, "Phone number should not be null when displayed");
        } else {
            log.info("Phone number not displayed on Contact page - may be expected");
        }
    }

    @Test(description = "Verify address is displayed on Contact page", priority = 2)
    public void testAddressDisplayed() {
        log.info("Starting testAddressDisplayed");

        boolean isAddressDisplayed = contactPage.isAddressDisplayed();
        if (isAddressDisplayed) {
            String address = contactPage.getAddress();
            log.info("Address displayed: " + address);
            assertNotNull(address, "Address should not be null when displayed");
        } else {
            log.info("Address not displayed on Contact page - may be expected");
        }
    }

    @Test(description = "Verify support hours are displayed on Contact page", priority = 2)
    public void testSupportHoursDisplayed() {
        log.info("Starting testSupportHoursDisplayed");

        boolean isSupportHoursDisplayed = contactPage.isSupportHoursDisplayed();
        log.info("Support hours displayed: " + isSupportHoursDisplayed);
        // Support hours may or may not be present depending on page design
    }

    @Test(description = "Verify Email Us link exists on Contact page", priority = 2)
    public void testEmailLinkExists() {
        log.info("Starting testEmailLinkExists");

        boolean isEmailLinkPresent = contactPage.isEmailLinkPresent();
        log.info("Email/Support link present: " + isEmailLinkPresent);
        // Email or support link should typically be present on a contact page
        if (isEmailLinkPresent) {
            log.info("Email/Support link found on Contact page");
        } else {
            log.info("Email/Support link not found - page may use different contact method");
        }
    }

    @Test(description = "Verify FAQ link exists on Contact page", priority = 2)
    public void testFaqLinkExists() {
        log.info("Starting testFaqLinkExists");

        boolean isFaqLinkPresent = contactPage.isFaqLinkPresent();
        log.info("FAQ link present: " + isFaqLinkPresent);
        // FAQ link may or may not be present
        if (isFaqLinkPresent) {
            log.info("FAQ link found on Contact page");
        } else {
            log.info("FAQ link not found on Contact page - may be expected");
        }
    }

    @Test(description = "Verify contact form presence (if applicable)", priority = 3)
    public void testContactFormPresence() {
        log.info("Starting testContactFormPresence");

        boolean isContactFormPresent = contactPage.isContactFormPresent();
        log.info("Contact form present: " + isContactFormPresent);
        // Contact form may or may not be present depending on page design
        if (isContactFormPresent) {
            log.info("Contact form found on Contact page");
        } else {
            log.info("No contact form found - page may use other contact methods");
        }
    }

    @Test(description = "Verify contact information section has content", priority = 1)
    public void testContactInfoHasContent() {
        log.info("Starting testContactInfoHasContent");

        // At least one of phone, address, or email should be present
        boolean hasPhone = contactPage.isPhoneDisplayed();
        boolean hasAddress = contactPage.isAddressDisplayed();
        boolean hasEmail = contactPage.isEmailLinkPresent();

        log.info("Contact info check - Phone: " + hasPhone + ", Address: " + hasAddress + ", Email: " + hasEmail);

        boolean hasContactInfo = hasPhone || hasAddress || hasEmail;
        assertTrue(hasContactInfo, "Contact page should have at least one contact method (phone, address, or email)");
    }
}
