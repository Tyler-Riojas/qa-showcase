package base;

import com.aventstack.extentreports.ExtentTest;
import config.Configuration;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;

import utils.AccessibilityChecker;
import utils.AccessibilityChecker.AccessibilityIssue;
import utils.AccessibilityChecker.Severity;
import utils.AccessibilityReporter;
import utils.AccessibilityUtils;
import utils.AccessibilityUtils.AccessibilityViolation;
import utils.ExtentReportManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base test class with automatic accessibility checking capabilities.
 *
 * <p>Extends {@link BaseTestTestNG} to provide automatic accessibility testing
 * after each test method. Tests extending this class automatically get accessibility
 * checks without any additional code.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Automatic accessibility scan after each test method</li>
 *   <li>Configurable via test.properties</li>
 *   <li>Integrates with ExtentReports for violation reporting</li>
 *   <li>Option to fail tests on critical violations</li>
 *   <li>Skip mechanism for specific tests</li>
 *   <li>Manual check method for custom timing</li>
 * </ul>
 *
 * <h2>Configuration (test.properties):</h2>
 * <pre>
 * # Enable/disable automatic accessibility checks
 * accessibility.enabled=true
 *
 * # Fail test if critical violations found
 * accessibility.fail.on.critical=false
 *
 * # Use Axe-core (comprehensive) or custom checker (fast)
 * accessibility.use.axe=true
 * accessibility.use.custom=true
 *
 * # WCAG tags to check (for Axe-core)
 * accessibility.tags=wcag2a,wcag2aa
 * </pre>
 *
 * <h2>Usage - Automatic Checks:</h2>
 * <pre>{@code
 * public class MyTest extends BaseAccessibilityTest {
 *
 *     @Test
 *     public void testLoginPage() {
 *         driver.get("https://example.com/login");
 *         // ... test logic ...
 *         // Accessibility check runs automatically after test
 *     }
 *
 *     @Test
 *     public void testWithoutA11yCheck() {
 *         skipAccessibilityCheck(); // Skip for this test only
 *         driver.get("https://example.com/admin");
 *         // No accessibility check
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage - Manual Checks:</h2>
 * <pre>{@code
 * @Test
 * public void testMultiplePages() {
 *     driver.get("https://example.com/page1");
 *     checkAccessibilityNow(); // Check page 1
 *
 *     driver.get("https://example.com/page2");
 *     checkAccessibilityNow(); // Check page 2
 *
 *     // Automatic check also runs at end
 * }
 * }</pre>
 *
 * @see BaseTestTestNG
 * @see AccessibilityUtils
 * @see AccessibilityChecker
 * @see AccessibilityReporter
 */
public class BaseAccessibilityTest extends BaseTestTestNG {

    private static final Logger log = LoggerFactory.getLogger(BaseAccessibilityTest.class);

    // Configuration property keys
    private static final String PROP_ENABLED = "accessibility.enabled";
    private static final String PROP_FAIL_ON_CRITICAL = "accessibility.fail.on.critical";
    private static final String PROP_USE_AXE = "accessibility.use.axe";
    private static final String PROP_USE_CUSTOM = "accessibility.use.custom";

    // ThreadLocal for skip flag (supports parallel execution)
    private static final ThreadLocal<Boolean> skipCheck = ThreadLocal.withInitial(() -> false);

    // ThreadLocal to store violations found during the test
    private static final ThreadLocal<List<AccessibilityViolation>> axeViolations =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<AccessibilityIssue>> customIssues =
            ThreadLocal.withInitial(ArrayList::new);

    // ThreadLocal to track if manual check was already performed
    private static final ThreadLocal<Boolean> manualCheckPerformed = ThreadLocal.withInitial(() -> false);

    // ==================== Configuration Methods ====================

    /**
     * Checks if accessibility testing is enabled via configuration.
     *
     * <p>Reads from test.properties: {@code accessibility.enabled}</p>
     *
     * @return true if accessibility checks should run (default: true)
     */
    protected boolean isAccessibilityEnabled() {
        return Configuration.getInstance().getBoolean(PROP_ENABLED, true);
    }

    /**
     * Checks if tests should fail when critical violations are found.
     *
     * <p>Reads from test.properties: {@code accessibility.fail.on.critical}</p>
     *
     * @return true if tests should fail on critical violations (default: false)
     */
    protected boolean shouldFailOnCritical() {
        return Configuration.getInstance().getBoolean(PROP_FAIL_ON_CRITICAL, false);
    }

    /**
     * Checks if Axe-core should be used for accessibility scanning.
     *
     * <p>Reads from test.properties: {@code accessibility.use.axe}</p>
     *
     * @return true if Axe-core should be used (default: true)
     */
    protected boolean useAxeCore() {
        return Configuration.getInstance().getBoolean(PROP_USE_AXE, true);
    }

    /**
     * Checks if custom checker should be used for accessibility scanning.
     *
     * <p>Reads from test.properties: {@code accessibility.use.custom}</p>
     *
     * @return true if custom checker should be used (default: true)
     */
    protected boolean useCustomChecker() {
        return Configuration.getInstance().getBoolean(PROP_USE_CUSTOM, true);
    }

    // ==================== Skip Control ====================

    /**
     * Skips the automatic accessibility check for the current test method.
     *
     * <p>Call this method at the beginning of a test to prevent the automatic
     * accessibility scan from running after that specific test. This is useful
     * for tests that intentionally test accessibility issues or where scanning
     * is not applicable (e.g., error pages, redirects).</p>
     *
     * <p>The skip flag is reset automatically after each test method.</p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * @Test
     * public void testAdminPageWithKnownIssues() {
     *     skipAccessibilityCheck(); // Skip for this test only
     *     driver.get(adminUrl);
     *     // ... test admin functionality ...
     * }
     * }</pre>
     */
    public void skipAccessibilityCheck() {
        log.debug("Accessibility check will be skipped for this test");
        skipCheck.set(true);
    }

    /**
     * Checks if the accessibility check should be skipped for the current test.
     *
     * @return true if check should be skipped
     */
    protected boolean shouldSkipCheck() {
        return skipCheck.get();
    }

    // ==================== Manual Check Method ====================

    /**
     * Manually triggers an accessibility check at the current point in the test.
     *
     * <p>Use this method when you need to check accessibility at specific points
     * during a test, such as after navigating to different pages or after
     * dynamic content changes.</p>
     *
     * <p>Results are accumulated and included in the final report. The automatic
     * end-of-test check will still run unless {@link #skipAccessibilityCheck()}
     * is called.</p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * @Test
     * public void testCheckoutFlow() {
     *     // Step 1: Cart page
     *     driver.get(cartUrl);
     *     checkAccessibilityNow();
     *
     *     // Step 2: Shipping page
     *     clickCheckout();
     *     checkAccessibilityNow();
     *
     *     // Step 3: Payment page
     *     fillShippingInfo();
     *     clickContinue();
     *     checkAccessibilityNow();
     * }
     * }</pre>
     *
     * @return {@link AccessibilityCheckResult} containing all violations found
     */
    public AccessibilityCheckResult checkAccessibilityNow() {
        if (!isAccessibilityEnabled()) {
            log.debug("Accessibility checks are disabled, skipping manual check");
            return new AccessibilityCheckResult(List.of(), List.of());
        }

        log.info("Running manual accessibility check on: {}", getDriver().getCurrentUrl());
        manualCheckPerformed.set(true);

        AccessibilityCheckResult result = performAccessibilityCheck(getDriver());

        // Accumulate results
        axeViolations.get().addAll(result.getAxeViolations());
        customIssues.get().addAll(result.getCustomIssues());

        // Log to console
        if (!result.getAxeViolations().isEmpty()) {
            AccessibilityReporter.logAxeViolations(result.getAxeViolations());
        }
        if (!result.getCustomIssues().isEmpty()) {
            AccessibilityReporter.logCustomIssues(result.getCustomIssues());
        }

        return result;
    }

    // ==================== Automatic Check Hook ====================

    /**
     * AfterMethod hook that automatically runs accessibility checks.
     *
     * <p>This method runs after each test method but before the parent class
     * teardown. It performs accessibility scanning based on configuration and
     * attaches results to ExtentReports.</p>
     *
     * <p>The check is skipped if:</p>
     * <ul>
     *   <li>Accessibility is disabled in configuration</li>
     *   <li>{@link #skipAccessibilityCheck()} was called</li>
     *   <li>The WebDriver is null (browser already closed)</li>
     * </ul>
     *
     * @param result TestNG test result (used to fail test if critical violations found)
     */
    @AfterMethod(alwaysRun = true)
    public void runAccessibilityCheck(ITestResult result) {
        try {
            // Check if we should run
            if (!isAccessibilityEnabled()) {
                log.debug("Accessibility checks disabled via configuration");
                return;
            }

            if (shouldSkipCheck()) {
                log.info("Accessibility check skipped for test: {}", result.getName());
                return;
            }

            if (getDriver() == null) {
                log.warn("WebDriver is null, cannot perform accessibility check");
                return;
            }

            // Perform the check (unless only manual checks were requested)
            if (!manualCheckPerformed.get()) {
                log.info("Running automatic accessibility check for: {}", result.getName());
                AccessibilityCheckResult checkResult = performAccessibilityCheck(getDriver());
                axeViolations.get().addAll(checkResult.getAxeViolations());
                customIssues.get().addAll(checkResult.getCustomIssues());
            }

            // Get accumulated results
            List<AccessibilityViolation> allAxeViolations = axeViolations.get();
            List<AccessibilityIssue> allCustomIssues = customIssues.get();

            // Log to console
            AccessibilityReporter.logAxeViolations(allAxeViolations);
            AccessibilityReporter.logCustomIssues(allCustomIssues);

            // Attach to ExtentReports
            attachToExtentReport(allAxeViolations, allCustomIssues);

            // Check for critical violations if configured to fail
            if (shouldFailOnCritical()) {
                checkForCriticalViolations(result, allAxeViolations, allCustomIssues);
            }

            // Log summary
            log.info("Accessibility check complete: {} Axe violations, {} custom issues",
                    allAxeViolations.size(), allCustomIssues.size());

        } catch (Exception e) {
            log.error("Error during accessibility check: {}", e.getMessage(), e);
            // Don't fail the test due to accessibility check errors
        } finally {
            // Reset ThreadLocal state for next test
            resetThreadLocalState();
        }
    }

    // ==================== Internal Methods ====================

    /**
     * Performs the actual accessibility check using configured scanners.
     *
     * @param webDriver WebDriver instance to scan
     * @return AccessibilityCheckResult with all violations found
     */
    protected AccessibilityCheckResult performAccessibilityCheck(WebDriver webDriver) {
        List<AccessibilityViolation> axe = new ArrayList<>();
        List<AccessibilityIssue> custom = new ArrayList<>();

        // Run Axe-core if enabled
        if (useAxeCore()) {
            try {
                axe = AccessibilityUtils.checkPage(webDriver);
                log.debug("Axe-core scan found {} violations", axe.size());
            } catch (Exception e) {
                log.warn("Axe-core scan failed: {}", e.getMessage());
            }
        }

        // Run custom checker if enabled
        if (useCustomChecker()) {
            try {
                custom = AccessibilityChecker.checkAllTraps(webDriver);
                log.debug("Custom checker found {} issues", custom.size());
            } catch (Exception e) {
                log.warn("Custom checker scan failed: {}", e.getMessage());
            }
        }

        return new AccessibilityCheckResult(axe, custom);
    }

    /**
     * Attaches accessibility results to ExtentReports with element screenshots.
     */
    private void attachToExtentReport(List<AccessibilityViolation> violations,
                                       List<AccessibilityIssue> issues) {
        ExtentTest extentTest = ExtentReportManager.getTest();
        if (extentTest != null) {
            // Pass driver for element screenshots on failures
            AccessibilityReporter.attachCombinedReport(extentTest, violations, issues, getDriver());
        } else {
            log.debug("ExtentTest not available, skipping report attachment");
        }
    }

    /**
     * Checks for critical violations and fails the test if configured.
     */
    private void checkForCriticalViolations(ITestResult result,
                                             List<AccessibilityViolation> violations,
                                             List<AccessibilityIssue> issues) {
        // Check Axe-core critical/serious violations
        List<AccessibilityViolation> severeAxe = violations.stream()
                .filter(AccessibilityViolation::isSevere)
                .collect(Collectors.toList());

        // Check custom checker critical issues
        List<AccessibilityIssue> criticalCustom = issues.stream()
                .filter(i -> i.getSeverity() == Severity.CRITICAL)
                .collect(Collectors.toList());

        if (!severeAxe.isEmpty() || !criticalCustom.isEmpty()) {
            String message = buildCriticalViolationMessage(severeAxe, criticalCustom);
            log.error("Critical accessibility violations found: {}", message);

            // Mark test as failed
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(new AccessibilityAssertionError(message));
        }
    }

    /**
     * Builds error message for critical violations.
     */
    private String buildCriticalViolationMessage(List<AccessibilityViolation> axeViolations,
                                                  List<AccessibilityIssue> customIssues) {
        StringBuilder sb = new StringBuilder();
        sb.append("Critical accessibility violations found:\n");

        if (!axeViolations.isEmpty()) {
            sb.append("Axe-core (").append(axeViolations.size()).append("):\n");
            axeViolations.forEach(v ->
                    sb.append("  - [").append(v.getImpact()).append("] ")
                            .append(v.getRuleId()).append(": ")
                            .append(v.getDescription()).append("\n"));
        }

        if (!customIssues.isEmpty()) {
            sb.append("Custom checker (").append(customIssues.size()).append("):\n");
            customIssues.forEach(i ->
                    sb.append("  - [").append(i.getSeverity()).append("] ")
                            .append(i.getType()).append(": ")
                            .append(i.getDescription()).append("\n"));
        }

        return sb.toString();
    }

    /**
     * Resets ThreadLocal state after each test.
     */
    private void resetThreadLocalState() {
        skipCheck.set(false);
        axeViolations.get().clear();
        customIssues.get().clear();
        manualCheckPerformed.set(false);
        AccessibilityReporter.cleanup();
    }

    // ==================== Result Classes ====================

    /**
     * Container for accessibility check results.
     *
     * <p>Holds both Axe-core violations and custom checker issues from a scan.</p>
     */
    public static class AccessibilityCheckResult {
        private final List<AccessibilityViolation> axeViolations;
        private final List<AccessibilityIssue> customIssues;

        /**
         * Creates a new result container.
         *
         * @param axeViolations Axe-core violations (may be empty, not null)
         * @param customIssues  Custom checker issues (may be empty, not null)
         */
        public AccessibilityCheckResult(List<AccessibilityViolation> axeViolations,
                                         List<AccessibilityIssue> customIssues) {
            this.axeViolations = axeViolations != null ? axeViolations : List.of();
            this.customIssues = customIssues != null ? customIssues : List.of();
        }

        /**
         * Gets Axe-core violations from the scan.
         *
         * @return list of Axe-core violations
         */
        public List<AccessibilityViolation> getAxeViolations() {
            return axeViolations;
        }

        /**
         * Gets custom checker issues from the scan.
         *
         * @return list of custom checker issues
         */
        public List<AccessibilityIssue> getCustomIssues() {
            return customIssues;
        }

        /**
         * Checks if any violations were found.
         *
         * @return true if there are any violations or issues
         */
        public boolean hasViolations() {
            return !axeViolations.isEmpty() || !customIssues.isEmpty();
        }

        /**
         * Checks if any critical/severe violations were found.
         *
         * @return true if there are critical Axe violations or critical custom issues
         */
        public boolean hasCriticalViolations() {
            boolean severeAxe = axeViolations.stream().anyMatch(AccessibilityViolation::isSevere);
            boolean criticalCustom = customIssues.stream()
                    .anyMatch(i -> i.getSeverity() == Severity.CRITICAL);
            return severeAxe || criticalCustom;
        }

        /**
         * Gets total violation count.
         *
         * @return total number of violations and issues
         */
        public int getTotalCount() {
            return axeViolations.size() + customIssues.size();
        }

        @Override
        public String toString() {
            return String.format("AccessibilityCheckResult[axe=%d, custom=%d, critical=%s]",
                    axeViolations.size(), customIssues.size(), hasCriticalViolations());
        }
    }

    /**
     * Custom assertion error for accessibility failures.
     *
     * <p>Thrown when critical accessibility violations are found and
     * {@code accessibility.fail.on.critical=true} is configured.</p>
     */
    public static class AccessibilityAssertionError extends AssertionError {

        /**
         * Creates a new accessibility assertion error.
         *
         * @param message description of the violations found
         */
        public AccessibilityAssertionError(String message) {
            super(message);
        }
    }
}
