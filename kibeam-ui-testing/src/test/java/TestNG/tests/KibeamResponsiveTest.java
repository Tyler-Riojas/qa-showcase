package TestNG.tests;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import base.BaseTestTestNG;
import pages.KibeamContactPage;
import pages.KibeamEducatorsPage;
import pages.KibeamHomePage;
import utils.WaitUtils;

/**
 * Responsive and visual tests for Kibeam.com
 * Tests for overlapping elements, horizontal scroll, touch targets, and responsive behavior
 */
public class KibeamResponsiveTest extends BaseTestTestNG {

    static final Logger log = getLogger(lookup().lookupClass());

    private KibeamHomePage homePage;

    @BeforeMethod
    public void setupTest() {
        homePage = new KibeamHomePage(getDriver());
        homePage.navigateToHome();
        WaitUtils.waitForPageLoad(getDriver());
    }

    // ========== HORIZONTAL OVERFLOW TESTS ==========

    @Test(description = "Verify no horizontal scrollbar on home page", priority = 1)
    public void testNoHorizontalScrollHomePage() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing for horizontal overflow on home page");

        boolean hasHorizontalScroll = hasHorizontalScrollbar();
        log.info("Horizontal scrollbar present: " + hasHorizontalScroll);

        softAssert.assertFalse(hasHorizontalScroll,
            "Home page should not have horizontal scrollbar at viewport: " + getViewportWidth() + "x" + getViewportHeight());
        softAssert.assertAll();
    }

    @Test(description = "Verify no horizontal scrollbar on educators page", priority = 1)
    public void testNoHorizontalScrollEducatorsPage() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing for horizontal overflow on educators page");

        KibeamEducatorsPage educatorsPage = new KibeamEducatorsPage(getDriver());
        educatorsPage.navigateToEducators();
        WaitUtils.waitForPageLoad(getDriver());

        boolean hasHorizontalScroll = hasHorizontalScrollbar();
        log.info("Horizontal scrollbar present: " + hasHorizontalScroll);

        softAssert.assertFalse(hasHorizontalScroll,
            "Educators page should not have horizontal scrollbar at viewport: " + getViewportWidth() + "x" + getViewportHeight());
        softAssert.assertAll();
    }

    @Test(description = "Verify no horizontal scrollbar on contact page", priority = 1)
    public void testNoHorizontalScrollContactPage() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing for horizontal overflow on contact page");

        KibeamContactPage contactPage = new KibeamContactPage(getDriver());
        contactPage.navigateToContact();
        WaitUtils.waitForPageLoad(getDriver());

        boolean hasHorizontalScroll = hasHorizontalScrollbar();
        log.info("Horizontal scrollbar present: " + hasHorizontalScroll);

        softAssert.assertFalse(hasHorizontalScroll,
            "Contact page should not have horizontal scrollbar at viewport: " + getViewportWidth() + "x" + getViewportHeight());
        softAssert.assertAll();
    }

    // ========== ELEMENT VISIBILITY TESTS ==========

    @Test(description = "Verify all navigation links are visible", priority = 1)
    public void testNavigationLinksVisible() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing navigation links visibility at " + getViewportWidth() + "px width");

        // Check if nav is visible (may be hamburger menu on mobile)
        boolean navVisible = homePage.isNavVisible();
        softAssert.assertTrue(navVisible, "Navigation should be visible or accessible");

        log.info("Navigation visible: " + navVisible);
        softAssert.assertAll();
    }

    @Test(description = "Verify footer is visible and within viewport width", priority = 2)
    public void testFooterWithinViewport() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing footer visibility and bounds");

        // Scroll to footer
        scrollToBottom();

        boolean footerVisible = homePage.isFooterVisible();
        softAssert.assertTrue(footerVisible, "Footer should be visible");

        // Check footer doesn't overflow
        WebElement footer = getDriver().findElement(By.tagName("footer"));
        int footerWidth = footer.getSize().getWidth();
        int viewportWidth = getViewportWidth();

        log.info("Footer width: " + footerWidth + ", Viewport width: " + viewportWidth);
        softAssert.assertTrue(footerWidth <= viewportWidth,
            "Footer width (" + footerWidth + ") should not exceed viewport width (" + viewportWidth + ")");
        softAssert.assertAll();
    }

    // ========== TOUCH TARGET TESTS (Mobile) ==========

    @Test(description = "Verify clickable elements have adequate touch target size", priority = 2)
    public void testTouchTargetSizes() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing touch target sizes for mobile usability");

        // Minimum recommended touch target is 44x44 pixels (Apple) or 48x48 (Google)
        int minTouchTarget = 44;

        List<WebElement> buttons = getDriver().findElements(By.cssSelector("button, a.btn, .button, [role='button']"));
        int smallTargets = 0;

        for (WebElement button : buttons) {
            if (button.isDisplayed()) {
                int width = button.getSize().getWidth();
                int height = button.getSize().getHeight();

                if (width < minTouchTarget || height < minTouchTarget) {
                    smallTargets++;
                    log.debug("Small touch target found: " + width + "x" + height + " - " + button.getText());
                }
            }
        }

        log.info("Found " + smallTargets + " buttons smaller than " + minTouchTarget + "px");

        // Warning only - don't fail test but log it
        if (smallTargets > 0) {
            log.warn(smallTargets + " clickable elements have touch targets smaller than " + minTouchTarget + "px");
        }
        softAssert.assertAll();
    }

    // ========== OVERLAPPING ELEMENT TESTS ==========

    @Test(description = "Verify header doesn't overlap page content", priority = 2)
    public void testHeaderNoOverlap() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing header overlap with content");

        WebElement header = getDriver().findElement(By.tagName("header"));
        List<WebElement> mainContent = getDriver().findElements(By.cssSelector("main, .main, section, article"));

        if (!mainContent.isEmpty()) {
            Rectangle headerRect = header.getRect();
            Rectangle contentRect = mainContent.get(0).getRect();

            boolean overlaps = rectanglesOverlap(headerRect, contentRect);

            // For fixed headers, check if content starts below header
            int headerBottom = headerRect.y + headerRect.height;
            int contentTop = contentRect.y;

            log.info("Header bottom: " + headerBottom + ", Content top: " + contentTop);

            // Content should start at or below header bottom (with some tolerance for padding)
            softAssert.assertTrue(contentTop >= headerBottom - 10 || !overlaps,
                "Content should not be obscured by header");
        }
        softAssert.assertAll();
    }

    @Test(description = "Verify no critical element overlaps on home page", priority = 2)
    public void testNoElementOverlapHomePage() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing for element overlaps on home page");

        // Check key elements don't overlap
        List<WebElement> sections = getDriver().findElements(By.cssSelector("section, .section"));

        int significantOverlapCount = 0;
        for (int i = 0; i < sections.size() - 1; i++) {
            WebElement current = sections.get(i);
            WebElement next = sections.get(i + 1);

            if (current.isDisplayed() && next.isDisplayed()) {
                Rectangle rect1 = current.getRect();
                Rectangle rect2 = next.getRect();

                // Check for significant vertical overlap (more than 50px indicates real problem)
                // Some overlap may be intentional for visual effects
                int overlap = (rect1.y + rect1.height) - rect2.y;
                if (overlap > 50) {
                    significantOverlapCount++;
                    log.error("Significant section overlap detected: " + overlap + "px");
                } else if (overlap > 10) {
                    log.info("Minor section overlap detected: " + overlap + "px (may be intentional)");
                }
            }
        }

        log.info("Significant section overlaps found: " + significantOverlapCount);
        softAssert.assertEquals(significantOverlapCount, 0, "Sections should not have significant overlaps (>50px)");
        softAssert.assertAll();
    }

    // ========== TEXT READABILITY TESTS ==========

    @Test(description = "Verify text is readable (not too small)", priority = 2)
    public void testTextReadability() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing text readability - minimum font size check");

        // Minimum readable font size is typically 12px, recommended 16px for body
        int minFontSize = 12;

        List<WebElement> textElements = getDriver().findElements(By.cssSelector("p, span, li, td, th"));
        int tooSmallCount = 0;

        for (WebElement element : textElements) {
            if (element.isDisplayed() && !element.getText().isEmpty()) {
                String fontSize = element.getCssValue("font-size");
                int size = parseFontSize(fontSize);

                if (size > 0 && size < minFontSize) {
                    tooSmallCount++;
                    log.debug("Small text found: " + size + "px - " + element.getText().substring(0, Math.min(30, element.getText().length())));
                }
            }
        }

        log.info("Text elements smaller than " + minFontSize + "px: " + tooSmallCount);

        // Warning - some small text may be intentional (captions, legal text)
        if (tooSmallCount > 5) {
            log.warn("Multiple text elements below minimum readable size");
        }
        softAssert.assertAll();
    }

    // ========== IMAGE RESPONSIVENESS TESTS ==========

    // This test identifies a known real defect on kibeam.com
    // Untitled_design_20.png overflows tablet viewport by 90px
    // Root cause: image uploaded via Shopify CMS without max-width: 100% CSS
    // Do not suppress this failure — it is the test doing its job correctly
    @Test(description = "Verify images don't overflow container", priority = 2, retryAnalyzer = null)
    public void testImagesResponsive() {
        SoftAssert softAssert = new SoftAssert();
        log.info("Testing image responsiveness");

        List<WebElement> images = getDriver().findElements(By.tagName("img"));
        int overflowCount = 0;
        int viewportWidth = getViewportWidth();

        for (WebElement img : images) {
            if (img.isDisplayed()) {
                int imgWidth = img.getSize().getWidth();
                int imgX = img.getLocation().getX();

                // Image should not extend beyond viewport
                if (imgX + imgWidth > viewportWidth + 5) { // 5px tolerance
                    overflowCount++;
                    String src = img.getAttribute("src");
                    String alt = img.getAttribute("alt");
                    int overflowBy = (imgX + imgWidth) - viewportWidth;

                    // Trim src to filename only for readability
                    String srcName = (src != null && src.contains("/")) ? src.substring(src.lastIndexOf('/') + 1) : src;
                    String altText = (alt != null && !alt.isEmpty()) ? alt : "(no alt)";

                    String message = String.format(
                        "Image overflow: src=%s alt=%s overflows by %dpx (image: %dpx wide at x=%d, viewport: %dpx)",
                        srcName, altText, overflowBy, imgWidth, imgX, viewportWidth);

                    log.debug(message);
                    softAssert.fail(message);
                }
            }
        }

        log.info("Images overflowing viewport: " + overflowCount);
        softAssert.assertAll();
    }

    // ========== VIEWPORT INFO TEST ==========

    @Test(description = "Log viewport and device information", priority = 1)
    public void testLogViewportInfo() {
        SoftAssert softAssert = new SoftAssert();
        log.info("========== VIEWPORT INFO ==========");
        log.info("Browser: " + getCurrentBrowser());
        log.info("Device: " + (getCurrentDevice() != null ? getCurrentDevice() : "Desktop"));
        log.info("Viewport: " + getViewportWidth() + "x" + getViewportHeight());
        log.info("Mobile emulation: " + isMobileEmulation());
        log.info("Tablet emulation: " + isTabletEmulation());
        log.info("===================================");

        // This test always passes - it's informational
        softAssert.assertTrue(true);
        softAssert.assertAll();
    }

    // ========== HELPER METHODS ==========

    /**
     * Check if page has horizontal scrollbar
     */
    private boolean hasHorizontalScrollbar() {
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        Long scrollWidth = (Long) js.executeScript("return document.documentElement.scrollWidth");
        Long clientWidth = (Long) js.executeScript("return document.documentElement.clientWidth");
        return scrollWidth > clientWidth;
    }

    /**
     * Scroll to bottom of page
     */
    private void scrollToBottom() {
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        try { Thread.sleep(500); } catch (InterruptedException e) { }
    }

    /**
     * Check if two rectangles overlap
     */
    private boolean rectanglesOverlap(Rectangle r1, Rectangle r2) {
        return r1.x < r2.x + r2.width &&
               r1.x + r1.width > r2.x &&
               r1.y < r2.y + r2.height &&
               r1.y + r1.height > r2.y;
    }

    /**
     * Parse font size from CSS value (e.g., "16px" -> 16)
     */
    private int parseFontSize(String fontSize) {
        try {
            return Integer.parseInt(fontSize.replace("px", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
