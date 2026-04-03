package pages;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import base.BasePage;
import utils.KibeamUrls;

/**
 * Page Object for Kibeam.com Educators Page
 * Handles educators landing page interactions
 */
public class KibeamEducatorsPage extends BasePage {

    static final Logger log = getLogger(lookup().lookupClass());

    // ========== HERO SECTION LOCATORS ==========
    private final By heroSection = By.cssSelector(".hero, .hero-section, [class*='hero'], section:first-of-type");
    private final By heroTitle = By.cssSelector(".hero h1, .hero-section h1, section h1");

    // ========== VIDEO PLAYER LOCATORS ==========
    private final By videoPlayer = By.cssSelector("video, iframe[src*='youtube'], iframe[src*='vimeo'], .video-container, [class*='video']");
    private final By playButton = By.cssSelector(".play-button, [class*='play'], button[aria-label*='play']");

    // ========== FEATURE SECTIONS LOCATORS ==========
    private final By featureSections = By.cssSelector(".feature, .features, [class*='feature'], section");

    // ========== CTA BUTTONS ==========
    private final By ctaButtons = By.cssSelector(".cta, .btn, button, a.button, [class*='cta']");
    private final By contactSalesButton = By.cssSelector("a[href*='tidycal'], a[href*='calendly'], [class*='contact']");

    // ========== CONSTRUCTOR ==========
    public KibeamEducatorsPage(WebDriver driver) {
        super(driver);
    }

    // ========== NAVIGATION ACTIONS ==========

    /**
     * Navigate directly to Educators page
     */
    public KibeamEducatorsPage navigateToEducators() {
        log.info("Navigating to Kibeam Educators page");
        visit(KibeamUrls.getEducatorsUrl());
        return this;
    }

    /**
     * Play video if video player is present
     */
    public KibeamEducatorsPage playVideo() {
        log.info("Attempting to play video");
        if (isVideoPresent()) {
            try {
                click(playButton);
                log.info("Clicked play button");
            } catch (Exception e) {
                log.debug("Play button not found or video auto-plays: " + e.getMessage());
            }
        }
        return this;
    }

    /**
     * Click Contact Sales CTA button
     */
    public KibeamEducatorsPage clickContactSales() {
        log.info("Clicking Contact Sales on Educators page");
        click(contactSalesButton);
        return this;
    }

    /**
     * Click a CTA button by index (0-based)
     */
    public KibeamEducatorsPage clickCtaButton(int index) {
        log.info("Clicking CTA button at index: " + index);
        var buttons = driver.findElements(ctaButtons);
        if (index < buttons.size()) {
            buttons.get(index).click();
        } else {
            log.warn("CTA button index " + index + " out of bounds. Found " + buttons.size() + " buttons.");
        }
        return this;
    }

    // ========== VERIFICATIONS ==========

    /**
     * Check if we're on the Educators page
     */
    public boolean isOnEducatorsPage() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("educators");
    }

    /**
     * Check if hero section is displayed
     */
    public boolean isHeroDisplayed() {
        try {
            return isDisplayed(heroSection);
        } catch (Exception e) {
            log.debug("Hero section check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if video player is present on the page
     */
    public boolean isVideoPresent() {
        try {
            return isDisplayed(videoPlayer);
        } catch (Exception e) {
            log.debug("Video player not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if CTA buttons are present
     */
    public boolean areCtaButtonsPresent() {
        try {
            return !driver.findElements(ctaButtons).isEmpty();
        } catch (Exception e) {
            log.debug("CTA buttons check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get number of feature sections
     */
    public int getFeatureSectionsCount() {
        try {
            return driver.findElements(featureSections).size();
        } catch (Exception e) {
            log.debug("Feature sections count failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get hero title text
     */
    public String getHeroTitle() {
        try {
            return getText(heroTitle);
        } catch (Exception e) {
            log.debug("Could not get hero title: " + e.getMessage());
            return "";
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
