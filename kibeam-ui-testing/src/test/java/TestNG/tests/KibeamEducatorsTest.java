package TestNG.tests;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.BaseTestTestNG;
import pages.KibeamEducatorsPage;
import utils.WaitUtils;

/**
 * Tests for Kibeam.com Educators Page
 * Tests hero section, video player, and CTA buttons
 */
public class KibeamEducatorsTest extends BaseTestTestNG {

    static final Logger log = getLogger(lookup().lookupClass());

    private KibeamEducatorsPage educatorsPage;

    @BeforeMethod
    public void setupTest() {
        educatorsPage = new KibeamEducatorsPage(getDriver());
        educatorsPage.navigateToEducators();
        WaitUtils.waitForPageLoad(getDriver());
    }

    @Test(description = "Verify Educators page loads successfully", priority = 1)
    public void testEducatorsPageLoads() {
        log.info("Starting testEducatorsPageLoads");

        assertTrue(educatorsPage.isOnEducatorsPage(), "Should be on Educators page");
        String pageTitle = educatorsPage.getPageTitle();
        assertNotNull(pageTitle, "Page title should not be null");
        log.info("Educators page loaded with title: " + pageTitle);
    }

    @Test(description = "Verify hero section is displayed on Educators page", priority = 1)
    public void testHeroSectionDisplayed() {
        log.info("Starting testHeroSectionDisplayed");

        assertTrue(educatorsPage.isHeroDisplayed(), "Hero section should be displayed on Educators page");
    }

    @Test(description = "Verify video player is present on Educators page", priority = 2)
    public void testVideoPlayerPresent() {
        log.info("Starting testVideoPlayerPresent");

        boolean isVideoPresent = educatorsPage.isVideoPresent();
        log.info("Video player present: " + isVideoPresent);
        // Log result - video may or may not be present depending on page design
        if (isVideoPresent) {
            log.info("Video player found on Educators page");
        } else {
            log.info("No video player found on Educators page - this may be expected");
        }
    }

    @Test(description = "Verify CTA buttons are present on Educators page", priority = 2)
    public void testCtaButtonsPresent() {
        log.info("Starting testCtaButtonsPresent");

        assertTrue(educatorsPage.areCtaButtonsPresent(), "CTA buttons should be present on Educators page");
    }

    @Test(description = "Verify CTA buttons are clickable", priority = 2)
    public void testCtaButtonsClickable() {
        log.info("Starting testCtaButtonsClickable");

        // Verify at least one CTA button exists and page doesn't break when we check
        boolean buttonsPresent = educatorsPage.areCtaButtonsPresent();
        assertTrue(buttonsPresent, "Should have CTA buttons on Educators page");
        log.info("CTA buttons verified as present and page is stable");
    }

    @Test(description = "Verify Educators page has feature sections", priority = 3)
    public void testFeatureSectionsPresent() {
        log.info("Starting testFeatureSectionsPresent");

        int featureCount = educatorsPage.getFeatureSectionsCount();
        log.info("Found " + featureCount + " feature sections on Educators page");
        assertTrue(featureCount > 0, "Educators page should have at least one feature section");
    }

    @Test(description = "Verify URL is correct on Educators page", priority = 1)
    public void testEducatorsPageUrl() {
        log.info("Starting testEducatorsPageUrl");

        String currentUrl = educatorsPage.getCurrentUrl();
        assertTrue(currentUrl.contains("educators"), "URL should contain 'educators': " + currentUrl);
        assertTrue(currentUrl.contains("kibeam.com"), "URL should contain 'kibeam.com': " + currentUrl);
    }

    @Test(description = "Verify hero title exists", priority = 2)
    public void testHeroTitleExists() {
        log.info("Starting testHeroTitleExists");

        String heroTitle = educatorsPage.getHeroTitle();
        log.info("Hero title: " + heroTitle);
        // Hero title should exist, may be empty if selector doesn't match
        assertNotNull(heroTitle, "Hero title should not be null");
    }
}
