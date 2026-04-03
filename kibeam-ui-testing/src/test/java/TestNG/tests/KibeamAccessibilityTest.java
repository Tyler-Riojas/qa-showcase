package TestNG.tests;

import base.BaseAccessibilityTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import utils.KibeamUrls;
import utils.WaitUtils;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Accessibility tests for Kibeam.com main pages.
 *
 * <p>This test class validates WCAG compliance across the primary
 * pages of the Kibeam website. It extends {@link BaseAccessibilityTest}
 * which provides automatic accessibility scanning after each test.</p>
 *
 * <h2>Pages Tested:</h2>
 * <ul>
 *   <li>Home page - Main landing page</li>
 *   <li>Educators page - Information for educators</li>
 *   <li>Contact page - Contact information and support</li>
 *   <li>About page - Company information</li>
 * </ul>
 *
 * <h2>Configuration (test.properties):</h2>
 * <pre>
 * accessibility.enabled=true
 * accessibility.fail.on.critical=false
 * accessibility.tags=wcag2a,wcag2aa
 * </pre>
 *
 * <h2>Running Tests:</h2>
 * <pre>
 * # Run all accessibility tests
 * mvn test -Dtest=KibeamAccessibilityTest
 *
 * # Run with strict mode (fail on critical)
 * mvn test -Dtest=KibeamAccessibilityTest -Daccessibility.fail.on.critical=true
 * </pre>
 *
 * @see BaseAccessibilityTest
 * @see utils.AccessibilityUtils
 * @see utils.AccessibilityChecker
 */
public class KibeamAccessibilityTest extends BaseAccessibilityTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    // ==================== HOME PAGE ====================

    /**
     * Tests accessibility compliance of the Kibeam home page.
     *
     * <p>The home page is the primary entry point for users and should
     * meet all WCAG 2.0/2.1 Level AA requirements.</p>
     */
    @Test(description = "Verify home page accessibility compliance",
          groups = {"accessibility", "regression", "smoke"},
          priority = 1)
    public void testHomePageAccessibility() {
        log.info("Testing accessibility: Home page");

        getDriver().get(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(getDriver());

        log.info("Home page loaded: {}", getDriver().getCurrentUrl());
        // Automatic accessibility check runs after test via BaseAccessibilityTest
    }

    // ==================== EDUCATORS PAGE ====================

    /**
     * Tests accessibility compliance of the Educators page.
     *
     * <p>The educators page contains information about Kibeam's
     * educational offerings and should be accessible to all users,
     * including those using assistive technologies.</p>
     */
    @Test(description = "Verify educators page accessibility compliance",
          groups = {"accessibility", "regression"},
          priority = 2)
    public void testEducatorsPageAccessibility() {
        log.info("Testing accessibility: Educators page");

        getDriver().get(KibeamUrls.getEducatorsUrl());
        WaitUtils.waitForPageLoad(getDriver());

        log.info("Educators page loaded: {}", getDriver().getCurrentUrl());
        // Automatic accessibility check runs after test
    }

    // ==================== CONTACT PAGE ====================

    /**
     * Tests accessibility compliance of the Contact page.
     *
     * <p>The contact page is critical for user communication and must
     * be fully accessible. This includes form elements, labels, and
     * contact information.</p>
     */
    @Test(description = "Verify contact page accessibility compliance",
          groups = {"accessibility", "regression"},
          priority = 3)
    public void testContactPageAccessibility() {
        log.info("Testing accessibility: Contact page");

        getDriver().get(KibeamUrls.getContactUrl());
        WaitUtils.waitForPageLoad(getDriver());

        log.info("Contact page loaded: {}", getDriver().getCurrentUrl());
        // Automatic accessibility check runs after test
    }

    // ==================== ABOUT PAGE ====================

    /**
     * Tests accessibility compliance of the About page.
     *
     * <p>The about page provides company information and should
     * maintain proper heading hierarchy and content structure.</p>
     */
    @Test(description = "Verify about page accessibility compliance",
          groups = {"accessibility", "regression"},
          priority = 4)
    public void testAboutPageAccessibility() {
        log.info("Testing accessibility: About page");

        getDriver().get(KibeamUrls.getAboutUrl());
        WaitUtils.waitForPageLoad(getDriver());

        log.info("About page loaded: {}", getDriver().getCurrentUrl());
        // Automatic accessibility check runs after test
    }

    // ==================== MULTI-PAGE SCAN ====================

    /**
     * Performs accessibility scan across all main pages in a single test.
     *
     * <p>This test manually triggers accessibility checks at each page
     * to accumulate results across the entire user journey. Useful for
     * comprehensive reporting.</p>
     */
    @Test(description = "Scan all main pages for accessibility issues",
          groups = {"accessibility", "regression"},
          priority = 10)
    public void testAllPagesAccessibility() {
        log.info("Testing accessibility: Full site scan");

        // Home page
        log.info("Scanning: Home page");
        getDriver().get(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(getDriver());
        checkAccessibilityNow();

        // Educators page
        log.info("Scanning: Educators page");
        getDriver().get(KibeamUrls.getEducatorsUrl());
        WaitUtils.waitForPageLoad(getDriver());
        checkAccessibilityNow();

        // Contact page
        log.info("Scanning: Contact page");
        getDriver().get(KibeamUrls.getContactUrl());
        WaitUtils.waitForPageLoad(getDriver());
        checkAccessibilityNow();

        // About page
        log.info("Scanning: About page");
        getDriver().get(KibeamUrls.getAboutUrl());
        WaitUtils.waitForPageLoad(getDriver());
        checkAccessibilityNow();

        log.info("Full site scan complete");
        // Final automatic check also runs after test
    }

    // ==================== CRITICAL-ONLY CHECK ====================

    /**
     * Quick smoke test checking only for critical accessibility violations.
     *
     * <p>This test is designed for fast feedback in CI/CD pipelines.
     * It only fails if critical/severe violations are found.</p>
     */
    @Test(description = "Quick check for critical accessibility issues on home page",
          groups = {"accessibility", "smoke"},
          priority = 0)
    public void testHomeCriticalAccessibility() {
        log.info("Quick accessibility check: Home page (critical only)");

        getDriver().get(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(getDriver());

        // Perform check and get results
        AccessibilityCheckResult result = checkAccessibilityNow();

        // Log summary
        log.info("Check complete: {} total issues, critical: {}",
                result.getTotalCount(), result.hasCriticalViolations());

        // Skip automatic end-of-test check since we did manual
        skipAccessibilityCheck();
    }
}
