package TestNG.tests.accessibility;

import base.BaseAccessibilityTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import utils.AccessibilityChecker;
import utils.AccessibilityUtils;
import utils.KibeamUrls;
import utils.WaitUtils;

import java.util.Arrays;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Debug test to compare axe-core vs custom accessibility checks.
 *
 * <p>Use this test class to verify axe-core is running properly,
 * compare results between different scanning methods, and troubleshoot
 * configuration issues.</p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * # Run all debug tests
 * mvn test -Dtest=AxeDebugTest
 *
 * # Run specific comparison test
 * mvn test -Dtest=AxeDebugTest#compareCustomVsAxeCore
 *
 * # Run configuration verification
 * mvn test -Dtest=AxeDebugTest#verifyAxeConfiguration
 * </pre>
 */
public class AxeDebugTest extends BaseAccessibilityTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    @Test(description = "Compare custom checks vs axe-core results",
          groups = {"accessibility", "debug"})
    public void compareCustomVsAxeCore() {
        log.info("=".repeat(80));
        log.info("ACCESSIBILITY DEBUGGING TEST");
        log.info("=".repeat(80));

        getDriver().get(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(getDriver());

        // Run custom checks
        log.info("\n--- CUSTOM CHECKS ---");
        List<AccessibilityChecker.AccessibilityIssue> customIssues =
            AccessibilityChecker.checkAllTraps(getDriver());
        log.info("Custom checks found: {} issues", customIssues.size());
        customIssues.forEach(issue -> log.info("  - [{}] {}: {}",
                issue.getSeverity(), issue.getType(), issue.getDescription()));

        // Run axe-core with configured tags/rules
        log.info("\n--- AXE-CORE SCAN (configured) ---");
        List<AccessibilityUtils.AccessibilityViolation> axeIssues =
            AccessibilityUtils.checkPage(getDriver());
        log.info("Axe-core found: {} violations", axeIssues.size());
        axeIssues.forEach(violation -> log.info("  - [{}] {}: {} ({} nodes)",
                violation.getImpact().toUpperCase(),
                violation.getRuleId(),
                violation.getDescription(),
                violation.getNodeCount()));

        // Run axe-core with ONLY contrast rule
        log.info("\n--- AXE-CORE CONTRAST ONLY ---");
        List<AccessibilityUtils.AccessibilityViolation> contrastIssues =
            AccessibilityUtils.checkPageWithRules(getDriver(), Arrays.asList("color-contrast"));
        log.info("Axe-core contrast check found: {} violations", contrastIssues.size());
        contrastIssues.forEach(violation -> {
            log.info("  - [{}] {}: {}", violation.getImpact().toUpperCase(),
                    violation.getRuleId(), violation.getDescription());
            if (violation.hasElementInfo()) {
                violation.getElementSelectors().forEach(sel ->
                        log.info("      Element: {}", sel));
            }
        });

        // Run with comprehensive WCAG 2.1 AA tags
        log.info("\n--- AXE-CORE WCAG 2.1 AA ---");
        List<AccessibilityUtils.AccessibilityViolation> wcagIssues =
            AccessibilityUtils.checkPageWithTags(getDriver(),
                    Arrays.asList("wcag2a", "wcag2aa", "wcag21a", "wcag21aa"));
        log.info("WCAG 2.1 AA check found: {} violations", wcagIssues.size());

        // Summary
        log.info("\n" + "=".repeat(80));
        log.info("SUMMARY:");
        log.info("  Custom Issues: {}", customIssues.size());
        log.info("  Axe Violations (configured): {}", axeIssues.size());
        log.info("  Contrast Issues: {}", contrastIssues.size());
        log.info("  WCAG 2.1 AA Issues: {}", wcagIssues.size());
        log.info("=".repeat(80));

        // Categorize by severity
        long criticalCount = axeIssues.stream()
                .filter(v -> "critical".equalsIgnoreCase(v.getImpact())).count();
        long seriousCount = axeIssues.stream()
                .filter(v -> "serious".equalsIgnoreCase(v.getImpact())).count();
        long moderateCount = axeIssues.stream()
                .filter(v -> "moderate".equalsIgnoreCase(v.getImpact())).count();
        long minorCount = axeIssues.stream()
                .filter(v -> "minor".equalsIgnoreCase(v.getImpact())).count();

        log.info("BY SEVERITY:");
        log.info("  Critical: {}", criticalCount);
        log.info("  Serious: {}", seriousCount);
        log.info("  Moderate: {}", moderateCount);
        log.info("  Minor: {}", minorCount);
        log.info("=".repeat(80));

        skipAccessibilityCheck(); // Don't run automatic check since we did manual
    }

    @Test(description = "Verify axe-core configuration is loaded",
          groups = {"accessibility", "debug"})
    public void verifyAxeConfiguration() {
        log.info("=".repeat(80));
        log.info("VERIFYING AXE-CORE CONFIGURATION");
        log.info("=".repeat(80));

        log.info("Accessibility enabled: {}", AccessibilityUtils.isEnabled());

        getDriver().get(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(getDriver());

        // This will log configuration via our debug logging
        log.info("\nRunning scan to verify configuration...");
        List<AccessibilityUtils.AccessibilityViolation> violations =
                AccessibilityUtils.checkPage(getDriver());

        log.info("\n" + "=".repeat(80));
        log.info("CONFIGURATION VERIFICATION COMPLETE");
        log.info("  Total violations found: {}", violations.size());
        log.info("  (Check logs above for tags/rules being used)");
        log.info("=".repeat(80));

        skipAccessibilityCheck();
    }

    @Test(description = "Test retry logic with scan",
          groups = {"accessibility", "debug"})
    public void testRetryLogic() {
        log.info("=".repeat(80));
        log.info("TESTING RETRY LOGIC");
        log.info("=".repeat(80));

        getDriver().get(KibeamUrls.getBaseUrl());
        WaitUtils.waitForPageLoad(getDriver());

        log.info("Running scan with retry logic (max 3 attempts)...");
        List<AccessibilityUtils.AccessibilityViolation> violations =
                AccessibilityUtils.checkPageWithRetry(getDriver());

        log.info("\n" + "=".repeat(80));
        log.info("RETRY TEST COMPLETE");
        log.info("  Violations found: {}", violations.size());
        log.info("=".repeat(80));

        skipAccessibilityCheck();
    }

    @Test(description = "Test all page URLs for accessibility",
          groups = {"accessibility", "debug"})
    public void scanAllPages() {
        log.info("=".repeat(80));
        log.info("SCANNING ALL KIBEAM PAGES");
        log.info("=".repeat(80));

        String[] pages = {
                KibeamUrls.getBaseUrl(),
                KibeamUrls.getEducatorsUrl(),
                KibeamUrls.getContactUrl(),
                KibeamUrls.getAboutUrl()
        };

        int totalViolations = 0;
        int totalCustomIssues = 0;

        for (String url : pages) {
            log.info("\n--- Scanning: {} ---", url);
            getDriver().get(url);
            WaitUtils.waitForPageLoad(getDriver());

            List<AccessibilityUtils.AccessibilityViolation> violations =
                    AccessibilityUtils.checkPage(getDriver());
            List<AccessibilityChecker.AccessibilityIssue> customIssues =
                    AccessibilityChecker.checkAllTraps(getDriver());

            log.info("  Axe violations: {}", violations.size());
            log.info("  Custom issues: {}", customIssues.size());

            totalViolations += violations.size();
            totalCustomIssues += customIssues.size();
        }

        log.info("\n" + "=".repeat(80));
        log.info("ALL PAGES SUMMARY:");
        log.info("  Total Axe violations: {}", totalViolations);
        log.info("  Total Custom issues: {}", totalCustomIssues);
        log.info("=".repeat(80));

        skipAccessibilityCheck();
    }
}
