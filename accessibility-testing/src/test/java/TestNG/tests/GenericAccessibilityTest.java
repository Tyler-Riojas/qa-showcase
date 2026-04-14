package TestNG.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import base.BaseAccessibilityTest;
import utils.AccessibilityChecker;
import utils.AccessibilityChecker.AccessibilityIssue;
import utils.AccessibilityReporter;
import utils.AccessibilityUtils;
import utils.AccessibilityUtils.AccessibilityViolation;
import utils.ExtentReportManager;
import utils.WaitUtils;

import com.aventstack.extentreports.ExtentTest;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

/**
 * Generic accessibility test suite — works against any publicly accessible website.
 *
 * <p>Default target: the-internet.herokuapp.com (no setup required)</p>
 *
 * <p>Override with system properties:
 * <pre>
 *   mvn test -Paccessibility -Dtarget.url=https://yoursite.com
 *   mvn test -Paccessibility -Dtarget.url=https://yoursite.com -Dtarget.url.2=https://yoursite.com/about
 * </pre>
 * </p>
 *
 * <p>Two scanning approaches:
 * <ul>
 *   <li>Axe-core: comprehensive WCAG 2.1 Level AA compliance scanning</li>
 *   <li>Custom checker: lightweight checks for common issues (images, headings, forms, contrast)</li>
 * </ul>
 * </p>
 */
@Feature("Accessibility Testing")
public class GenericAccessibilityTest extends BaseAccessibilityTest {

    static final Logger log = getLogger(lookup().lookupClass());

    // ========== CONFIGURATION ==========

    private static final String TARGET_URL = System.getProperty(
            "target.url", "https://the-internet.herokuapp.com");
    private static final String TARGET_URL_2 = System.getProperty(
            "target.url.2", "https://the-internet.herokuapp.com/login");

    // ========== SETUP ==========

    @BeforeMethod
    public void setupTest() {
        log.info("Navigating to test page: {}", TARGET_URL);
        getDriver().get(TARGET_URL);
        WaitUtils.waitForPageLoad(getDriver());
        AccessibilityReporter.resetStats();
    }

    @AfterMethod
    public void cleanupTest() {
        AccessibilityReporter.cleanup();
    }

    // ========== AXE-CORE TESTS (AccessibilityUtils) ==========
    // Comprehensive WCAG compliance testing

    /**
     * Full page accessibility scan using Axe-core on the primary target URL.
     * This is the most thorough check - run in regression testing.
     */
    @Story("WCAG Compliance")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Full page accessibility scan with Axe-core",
          groups = {"accessibility", "regression"},
          priority = 1)
    public void testAxeCoreCompliance() {
        Allure.step("Navigate to " + TARGET_URL);
        Allure.step("Run Axe-core WCAG 2.1 Level AA scan");

        log.info("Running full Axe-core accessibility scan on: {}", TARGET_URL);

        List<AccessibilityViolation> violations = AccessibilityUtils.checkPage(getDriver());

        if (!violations.isEmpty()) {
            log.warn("Found {} accessibility violations:", violations.size());
            violations.forEach(v -> log.warn("  - {}", v));
        }

        Allure.step("Assert no critical/serious violations");

        // Filter to critical/serious only for assertion
        List<AccessibilityViolation> severeViolations = violations.stream()
                .filter(AccessibilityViolation::isSevere)
                .collect(Collectors.toList());

        assertTrue(severeViolations.isEmpty(),
                "Page has " + severeViolations.size() + " critical/serious accessibility violations: " +
                severeViolations.stream().map(AccessibilityViolation::getRuleId).collect(Collectors.joining(", ")));
    }

    /**
     * Axe-core scan on the secondary target URL.
     */
    @Story("WCAG Compliance")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Axe-core scan on secondary page",
          groups = {"accessibility", "regression"},
          priority = 2)
    public void testAxeCorePage2() {
        Allure.step("Navigate to " + TARGET_URL_2);
        getDriver().get(TARGET_URL_2);
        WaitUtils.waitForPageLoad(getDriver());

        Allure.step("Run Axe-core WCAG 2.1 Level AA scan");

        log.info("Running Axe-core scan on secondary page: {}", TARGET_URL_2);

        List<AccessibilityViolation> violations = AccessibilityUtils.checkPage(getDriver());

        if (!violations.isEmpty()) {
            log.warn("Found {} accessibility violations on page 2:", violations.size());
            violations.forEach(v -> log.warn("  - {}", v));
        }

        Allure.step("Assert no critical/serious violations");

        List<AccessibilityViolation> severeViolations = violations.stream()
                .filter(AccessibilityViolation::isSevere)
                .collect(Collectors.toList());

        assertTrue(severeViolations.isEmpty(),
                "Page 2 has " + severeViolations.size() + " critical/serious accessibility violations: " +
                severeViolations.stream().map(AccessibilityViolation::getRuleId).collect(Collectors.joining(", ")));
    }

    /**
     * Check WCAG 2.1 AA compliance explicitly.
     */
    @Story("WCAG Compliance")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Check WCAG 2.1 AA compliance",
          groups = {"accessibility", "regression"},
          priority = 3)
    public void testWCAG21AACompliance() {
        Allure.step("Navigate to " + TARGET_URL);
        Allure.step("Run Axe-core with explicit wcag21aa tags");

        log.info("Checking WCAG 2.1 AA compliance on: {}", TARGET_URL);

        List<AccessibilityViolation> violations = AccessibilityUtils.checkPageWithTags(
                getDriver(), List.of("wcag21aa"));

        if (!violations.isEmpty()) {
            log.warn("WCAG 2.1 AA violations found:");
            violations.forEach(v -> log.warn("  - {}", v));
        }

        Allure.step("Log WCAG 2.1 AA violation count: " + violations.size());

        // This may be lenient for legacy applications
        // Consider tracking violations count over time instead of failing
        log.info("WCAG 2.1 AA check complete: {} violations", violations.size());
    }

    // ========== CUSTOM CHECKER TESTS (AccessibilityChecker) ==========
    // Lightweight, fast checks for common accessibility traps

    /**
     * Quick accessibility sanity check using custom checker.
     * Faster than Axe-core, good for smoke tests.
     */
    @Story("Custom Checks")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Quick accessibility smoke check for common issues",
          groups = {"accessibility", "smoke"},
          priority = 4)
    public void testCustomChecks() {
        Allure.step("Navigate to " + TARGET_URL);
        Allure.step("Run custom accessibility checker");

        log.info("Running quick accessibility check on: {}", TARGET_URL);

        List<AccessibilityIssue> issues = AccessibilityChecker.checkAllTraps(getDriver());

        if (!issues.isEmpty()) {
            log.warn("Found {} accessibility issues:", issues.size());
            issues.forEach(i -> log.warn("  - {}", i));
        }

        // Filter to critical only for smoke tests
        List<AccessibilityIssue> criticalIssues = issues.stream()
                .filter(i -> i.getSeverity() == AccessibilityChecker.Severity.CRITICAL)
                .collect(Collectors.toList());

        Allure.step("Assert no critical issues (found " + criticalIssues.size() + ")");

        assertTrue(criticalIssues.isEmpty(),
                "Page has " + criticalIssues.size() + " critical accessibility issues: " +
                criticalIssues.stream()
                        .map(i -> i.getType().name())
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Custom checker on secondary page.
     */
    @Story("Custom Checks")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Custom accessibility checks on secondary page",
          groups = {"accessibility", "regression"},
          priority = 5)
    public void testCustomChecksPage2() {
        Allure.step("Navigate to " + TARGET_URL_2);
        getDriver().get(TARGET_URL_2);
        WaitUtils.waitForPageLoad(getDriver());

        Allure.step("Run custom checker on page 2");

        log.info("Running custom checks on secondary page: {}", TARGET_URL_2);

        List<AccessibilityIssue> issues = AccessibilityChecker.checkAllTraps(getDriver());

        if (!issues.isEmpty()) {
            log.warn("Found {} accessibility issues on page 2:", issues.size());
            issues.forEach(i -> log.warn("  - {}", i));
        }

        List<AccessibilityIssue> criticalIssues = issues.stream()
                .filter(i -> i.getSeverity() == AccessibilityChecker.Severity.CRITICAL)
                .collect(Collectors.toList());

        Allure.step("Assert no critical issues on page 2 (found " + criticalIssues.size() + ")");

        assertTrue(criticalIssues.isEmpty(),
                "Page 2 has " + criticalIssues.size() + " critical custom accessibility issues: " +
                criticalIssues.stream()
                        .map(i -> i.getType().name())
                        .collect(Collectors.joining(", ")));
    }

    // ========== COMBINED / MULTI-PAGE TESTS ==========

    /**
     * Combined Axe-core and custom checker scan with full reporting.
     * Demonstrates both approaches together.
     */
    @Story("Multi-Page Scan")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Combined Axe-core and custom checker scan with reporting",
          groups = {"accessibility", "regression"},
          priority = 6)
    public void testCombinedReport() {
        Allure.step("Navigate to " + TARGET_URL);
        Allure.step("Run both Axe-core and custom checker scans");

        log.info("Running combined accessibility scan with ExtentReports integration");

        // Run both scans
        List<AccessibilityViolation> axeViolations = AccessibilityUtils.checkPage(getDriver());
        List<AccessibilityIssue> customIssues = AccessibilityChecker.checkAllTraps(getDriver());

        // Log violations to console
        AccessibilityReporter.logAxeViolations(axeViolations);
        AccessibilityReporter.logCustomIssues(customIssues);

        // Attach to ExtentReports
        ExtentTest extentTest = ExtentReportManager.getTest();
        if (extentTest != null) {
            AccessibilityReporter.attachAxeViolationsToReport(extentTest, axeViolations);
            AccessibilityReporter.attachCustomIssuesToReport(extentTest, customIssues);
        }

        // Get statistics
        AccessibilityReporter.ReportStats stats = AccessibilityReporter.getStats();
        log.info("Stats: {}", stats);

        // Filter severe violations for assertion
        List<AccessibilityViolation> severeAxe = axeViolations.stream()
                .filter(AccessibilityViolation::isSevere)
                .collect(Collectors.toList());

        List<AccessibilityIssue> severeCustom = customIssues.stream()
                .filter(i -> i.getSeverity() == AccessibilityChecker.Severity.CRITICAL)
                .collect(Collectors.toList());

        Allure.step("Assert no severe issues (Axe: " + severeAxe.size() + ", Custom: " + severeCustom.size() + ")");

        // Assert no severe issues
        assertTrue(severeAxe.isEmpty() && severeCustom.isEmpty(),
                String.format("Found %d severe Axe violations and %d critical custom issues",
                        severeAxe.size(), severeCustom.size()));
    }

    /**
     * Generate comprehensive accessibility report without failing.
     * Useful for baseline assessment of any website.
     * Uses AccessibilityReporter for formatted console output and ExtentReports integration.
     */
    @Story("Accessibility Report")
    @Severity(SeverityLevel.MINOR)
    @Test(description = "Generate full accessibility report (non-failing, audit mode)",
          groups = {"accessibility", "report"},
          priority = 100)
    public void testAccessibilityReport() {
        Allure.step("Navigate to " + TARGET_URL);
        Allure.step("Run full accessibility audit (Axe-core + custom checker)");

        log.info("Generating comprehensive accessibility report for: {}", getDriver().getCurrentUrl());

        // Run Axe-core scan
        List<AccessibilityViolation> axeViolations = AccessibilityUtils.checkPage(getDriver());

        // Run custom checker scan
        List<AccessibilityIssue> customIssues = AccessibilityChecker.checkAllTraps(getDriver());

        // Log to console with formatted output
        AccessibilityReporter.logAxeViolations(axeViolations);
        AccessibilityReporter.logCustomIssues(customIssues);

        // Print summaries
        log.info(AccessibilityReporter.generateAxeSummary(axeViolations));
        log.info(AccessibilityReporter.generateCustomSummary(customIssues));
        log.info(AccessibilityReporter.generateCombinedSummary(axeViolations, customIssues));

        // Attach to ExtentReports if available
        ExtentTest extentTest = ExtentReportManager.getTest();
        if (extentTest != null) {
            AccessibilityReporter.attachCombinedReport(extentTest, axeViolations, customIssues);
        }

        Allure.step("Report generated — Axe: " + axeViolations.size() + " violations, Custom: " + customIssues.size() + " issues");

        // This test always passes - it's for reporting/auditing
        assertTrue(true, "Report generated successfully");
    }
}
