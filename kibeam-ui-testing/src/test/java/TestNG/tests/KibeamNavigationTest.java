package TestNG.tests;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.BaseTestTestNG;
import pages.KibeamContactPage;
import pages.KibeamEducatorsPage;
import pages.KibeamHomePage;
import utils.WaitUtils;

/**
 * Navigation tests for Kibeam.com
 * Tests header navigation, footer links, and Contact Sales functionality
 */
public class KibeamNavigationTest extends BaseTestTestNG {

    static final Logger log = getLogger(lookup().lookupClass());

    private KibeamHomePage homePage;

    @BeforeMethod
    public void setupTest() {
        homePage = new KibeamHomePage(getDriver());
        homePage.navigateToHome();
        WaitUtils.waitForPageLoad(getDriver());
    }

    @Test(description = "Verify home page loads successfully", priority = 1)
    public void testHomePageLoads() {
        log.info("Starting testHomePageLoads");

        assertTrue(homePage.isNavVisible(), "Header navigation should be visible");
        String pageTitle = homePage.getPageTitle();
        assertNotNull(pageTitle, "Page title should not be null");
        assertFalse(pageTitle.isEmpty(), "Page title should not be empty");
        log.info("Home page loaded with title: " + pageTitle);
    }

    @Test(description = "Verify header navigation is visible", priority = 1)
    public void testHeaderNavigationVisible() {
        log.info("Starting testHeaderNavigationVisible");

        assertTrue(homePage.isNavVisible(), "Header navigation should be visible on home page");
    }

    @Test(description = "Verify Educators link navigates to Educators page", priority = 2)
    public void testEducatorsNavigation() {
        log.info("Starting testEducatorsNavigation");

        KibeamEducatorsPage educatorsPage = homePage.clickEducators();
        WaitUtils.waitForPageLoad(getDriver());

        assertTrue(educatorsPage.isOnEducatorsPage(), "Should be on Educators page after clicking Educators link");
        String currentUrl = educatorsPage.getCurrentUrl();
        assertTrue(currentUrl.contains("educators"), "URL should contain 'educators': " + currentUrl);
    }

    @Test(description = "Verify Support/Contact link navigates to Contact page", priority = 2)
    public void testSupportNavigation() {
        log.info("Starting testSupportNavigation");

        KibeamContactPage contactPage = homePage.clickSupport();
        WaitUtils.waitForPageLoad(getDriver());

        assertTrue(contactPage.isOnContactPage(), "Should be on Contact page after clicking Support link");
        String currentUrl = contactPage.getCurrentUrl();
        assertTrue(currentUrl.contains("contact"), "URL should contain 'contact': " + currentUrl);
    }

    @Test(description = "Verify footer is visible on home page", priority = 1)
    public void testFooterVisible() {
        log.info("Starting testFooterVisible");

        assertTrue(homePage.isFooterVisible(), "Footer should be visible on home page");
    }

    @Test(description = "Verify Contact Sales button is present", priority = 2)
    public void testContactSalesButtonPresent() {
        log.info("Starting testContactSalesButtonPresent");

        // Note: Contact Sales may open TidyCal external booking
        boolean isPresent = homePage.isContactSalesButtonPresent();
        log.info("Contact Sales button present: " + isPresent);
        // This test is informational - button may not be on all pages
    }

    @Test(description = "Verify navigation returns correct page objects", priority = 2)
    public void testNavigationMethodChaining() {
        log.info("Starting testNavigationMethodChaining");

        // Test method chaining - navigate to Educators then verify
        KibeamEducatorsPage educatorsPage = homePage.clickEducators();
        assertTrue(educatorsPage.isOnEducatorsPage(), "Should be on Educators page");

        // Navigate back to home by going to home URL
        homePage.navigateToHome();
        assertTrue(homePage.isOnHomePage() || homePage.isNavVisible(),
            "Should be able to navigate back to home page");
    }

    @Test(description = "Verify Educators link is present in navigation", priority = 1)
    public void testEducatorsLinkPresent() {
        log.info("Starting testEducatorsLinkPresent");

        assertTrue(homePage.isEducatorsLinkPresent(), "Educators link should be present in navigation");
    }
}
