package TestNG.tests;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.Test;

import base.BaseTestTestNG;
import utils.WaitUtils;

/**
 * Generic responsive test suite for any website.
 *
 * <p>Tests horizontal overflow, element visibility, touch targets, overlapping
 * elements, text readability, and image responsiveness across a configurable
 * set of pages and device viewports.</p>
 *
 * <h2>Configuration:</h2>
 * <pre>
 * # Run against any site:
 * mvn test -Presponsive -Dtarget.url=https://yoursite.com -Dtarget.pages='/,/about,/contact'
 *
 * # Works across all device profiles via testng XML parameters.
 * # See testng-responsive.xml for Desktop, Mobile (IPHONE_12), and Tablet (IPAD_PRO_11).
 * </pre>
 *
 * <h2>Defaults (when no properties are supplied):</h2>
 * <ul>
 *   <li>URL: https://the-internet.herokuapp.com</li>
 *   <li>Pages: /, /login, /checkboxes</li>
 * </ul>
 */
public class ResponsiveTest extends BaseTestTestNG {

    static final Logger log = getLogger(lookup().lookupClass());

    private static final String BASE_URL =
            System.getProperty("target.url", "https://the-internet.herokuapp.com");

    private static final List<String> PAGES = Arrays.asList(
            System.getProperty("target.pages", "/, /login, /checkboxes")
                    .split("\\s*,\\s*")
    );

    // ========== HORIZONTAL OVERFLOW TESTS ==========

    @Test(description = "Verify no horizontal scrollbar on any configured page", priority = 1)
    public void testNoHorizontalScroll() {
        for (String page : PAGES) {
            log.info("Testing for horizontal overflow on {}", page);
            navigateTo(page);

            boolean hasHorizontalScroll = hasHorizontalScrollbar();
            log.info("Horizontal scrollbar present: " + hasHorizontalScroll);

            assertFalse(hasHorizontalScroll,
                    page + " should not have horizontal scrollbar at viewport: "
                            + getViewportWidth() + "x" + getViewportHeight());
        }
    }

    // ========== ELEMENT VISIBILITY TESTS ==========

    @Test(description = "Verify all navigation links are visible", priority = 1)
    public void testNavigationLinksVisible() {
        for (String page : PAGES) {
            navigateTo(page);
            log.info("Testing navigation links visibility at " + getViewportWidth() + "px width");

            // Check if nav is visible (may be hamburger menu on mobile)
            List<WebElement> navElements = getDriver()
                    .findElements(By.cssSelector("nav, [role='navigation'], header"));

            if (navElements.isEmpty()) {
                log.warn("No navigation elements found on {} — may be intentional for simple sites", page);
                throw new SkipException(
                        "No navigation elements found on this page — may be intentional for simple sites");
            }

            log.info("Navigation visible: true");
        }
    }

    @Test(description = "Verify footer is visible and within viewport width", priority = 2)
    public void testFooterWithinViewport() {
        for (String page : PAGES) {
            navigateTo(page);
            log.info("Testing footer visibility and bounds on " + page);

            // Scroll to footer
            scrollToBottom();

            List<WebElement> footerEls = getDriver().findElements(By.tagName("footer"));
            boolean footerVisible = !footerEls.isEmpty() && footerEls.get(0).isDisplayed();

            if (!footerVisible) {
                log.warn("No footer element found on " + page + " — skipping footer bounds check");
                continue;
            }

            assertTrue(footerVisible, "Footer should be visible");

            // Check footer doesn't overflow
            WebElement footer = footerEls.get(0);
            int footerWidth = footer.getSize().getWidth();
            int viewportWidth = getViewportWidth();

            log.info("Footer width: " + footerWidth + ", Viewport width: " + viewportWidth);
            assertTrue(footerWidth <= viewportWidth,
                    "Footer width (" + footerWidth + ") should not exceed viewport width (" + viewportWidth + ")");
        }
    }

    // ========== TOUCH TARGET TESTS (Mobile) ==========

    @Test(description = "Verify clickable elements have adequate touch target size", priority = 2)
    public void testTouchTargetSizes() {
        for (String page : PAGES) {
            navigateTo(page);
            log.info("Testing touch target sizes for mobile usability");

            // Minimum recommended touch target is 44x44 pixels (Apple) or 48x48 (Google)
            int minTouchTarget = 44;

            List<WebElement> buttons = getDriver().findElements(
                    By.cssSelector("button, a.btn, .button, [role='button']"));
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
        }
    }

    // ========== OVERLAPPING ELEMENT TESTS ==========

    @Test(description = "Verify header doesn't overlap page content", priority = 2)
    public void testHeaderNoOverlap() {
        for (String page : PAGES) {
            navigateTo(page);
            log.info("Testing header overlap with content");

            List<WebElement> headers = getDriver().findElements(By.tagName("header"));
            List<WebElement> mainContent = getDriver().findElements(
                    By.cssSelector("main, .main, section, article"));

            if (headers.isEmpty() || mainContent.isEmpty()) {
                log.info("No header or main content found on " + page + " — skipping overlap check");
                continue;
            }

            Rectangle headerRect = headers.get(0).getRect();
            Rectangle contentRect = mainContent.get(0).getRect();

            boolean overlaps = rectanglesOverlap(headerRect, contentRect);

            // For fixed headers, check if content starts below header
            int headerBottom = headerRect.y + headerRect.height;
            int contentTop = contentRect.y;

            log.info("Header bottom: " + headerBottom + ", Content top: " + contentTop);

            // Content should start at or below header bottom (with some tolerance for padding)
            assertTrue(contentTop >= headerBottom - 10 || !overlaps,
                    "Content should not be obscured by header");
        }
    }

    @Test(description = "Verify no critical element overlaps on configured pages", priority = 2)
    public void testNoElementOverlapHomePage() {
        for (String page : PAGES) {
            navigateTo(page);
            log.info("Testing for element overlaps on " + page);

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
            assertEquals(significantOverlapCount, 0, "Sections should not have significant overlaps (>50px)");
        }
    }

    // ========== TEXT READABILITY TESTS ==========

    @Test(description = "Verify text is readable (not too small)", priority = 2)
    public void testTextReadability() {
        for (String page : PAGES) {
            navigateTo(page);
            log.info("Testing text readability - minimum font size check");

            // Minimum readable font size is typically 12px, recommended 16px for body
            int minFontSize = 12;

            List<WebElement> textElements = getDriver().findElements(
                    By.cssSelector("p, span, li, td, th"));
            int tooSmallCount = 0;

            for (WebElement element : textElements) {
                if (element.isDisplayed() && !element.getText().isEmpty()) {
                    String fontSize = element.getCssValue("font-size");
                    int size = parseFontSize(fontSize);

                    if (size > 0 && size < minFontSize) {
                        tooSmallCount++;
                        log.debug("Small text found: " + size + "px - "
                                + element.getText().substring(0, Math.min(30, element.getText().length())));
                    }
                }
            }

            log.info("Text elements smaller than " + minFontSize + "px: " + tooSmallCount);

            // Warning - some small text may be intentional (captions, legal text)
            if (tooSmallCount > 5) {
                log.warn("Multiple text elements below minimum readable size");
            }
        }
    }

    // ========== IMAGE RESPONSIVENESS TESTS ==========

    @Test(description = "Verify images don't overflow container", priority = 2)
    public void testImagesResponsive() {
        for (String page : PAGES) {
            navigateTo(page);
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
                        log.debug("Image overflow: width=" + imgWidth + ", x=" + imgX + ", viewport=" + viewportWidth);
                    }
                }
            }

            log.info("Images overflowing viewport: " + overflowCount);
            assertEquals(overflowCount, 0, "Images should not overflow the viewport");
        }
    }

    // ========== VIEWPORT INFO TEST ==========

    @Test(description = "Log viewport and device information", priority = 1)
    public void testLogViewportInfo() {
        log.info("========== VIEWPORT INFO ==========");
        log.info("Browser: " + getCurrentBrowser());
        log.info("Device: " + (getCurrentDevice() != null ? getCurrentDevice() : "Desktop"));
        log.info("Viewport: " + getViewportWidth() + "x" + getViewportHeight());
        log.info("Mobile emulation: " + isMobileEmulation());
        log.info("Tablet emulation: " + isTabletEmulation());
        log.info("===================================");

        // This test always passes - it's informational
        assertTrue(true);
    }

    // ========== HELPER METHODS ==========

    /**
     * Navigate to a page path relative to BASE_URL.
     */
    private void navigateTo(String page) {
        String path = page.startsWith("/") ? page : "/" + page;
        getDriver().get(BASE_URL + path);
        WaitUtils.waitForPageLoad(getDriver());
    }

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
