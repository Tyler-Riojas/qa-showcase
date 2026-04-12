package utils;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import config.Configuration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for accessibility testing using Axe-core.
 *
 * <p>Provides methods to scan pages and elements for WCAG accessibility violations.
 * Integrates with the Configuration system for feature toggles and rule configuration.</p>
 *
 * <h2>Configuration (test.properties):</h2>
 * <pre>
 * # Enable/disable accessibility checks
 * accessibility.enabled=true
 *
 * # Comma-separated WCAG rules to check (empty = all rules)
 * accessibility.rules=wcag2a,wcag2aa,wcag21a,wcag21aa
 *
 * # Tags to include (e.g., wcag2a, wcag2aa, wcag21aa, best-practice)
 * accessibility.tags=wcag2a,wcag2aa
 * </pre>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Check entire page
 * List<AccessibilityViolation> violations = AccessibilityUtils.checkPage(driver);
 * if (!violations.isEmpty()) {
 *     violations.forEach(v -> log.warn("Violation: {}", v));
 * }
 *
 * // Check specific element
 * List<AccessibilityViolation> violations = AccessibilityUtils.checkElement(driver, By.id("form"));
 *
 * // Check with specific WCAG rules
 * List<AccessibilityViolation> violations = AccessibilityUtils.checkPageWithTags(
 *     driver, Arrays.asList("wcag2a", "wcag2aa")
 * );
 *
 * // Assert no violations in test
 * List<AccessibilityViolation> violations = AccessibilityUtils.checkPage(driver);
 * assertTrue(violations.isEmpty(), "Accessibility violations found: " + violations);
 * }</pre>
 *
 * <h2>Maven Dependency:</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.deque.html.axe-core</groupId>
 *     <artifactId>selenium</artifactId>
 *     <version>4.8.0</version>
 * </dependency>
 * }</pre>
 *
 * @see <a href="https://www.deque.com/axe/">Axe Accessibility Testing</a>
 * @see <a href="https://www.w3.org/WAI/WCAG21/quickref/">WCAG Quick Reference</a>
 */
public final class AccessibilityUtils {

    private static final Logger log = LoggerFactory.getLogger(AccessibilityUtils.class);

    // Configuration property keys
    private static final String PROP_ENABLED = "accessibility.enabled";
    private static final String PROP_TAGS = "accessibility.tags";
    private static final String PROP_RULES = "accessibility.rules";

    // ThreadLocal storage for last scan results (for parallel execution)
    private static final ThreadLocal<List<AccessibilityViolation>> lastViolations =
            ThreadLocal.withInitial(ArrayList::new);

    private AccessibilityUtils() {
        // Prevent instantiation - utility class
    }

    // ==================== Main Check Methods ====================

    /**
     * Scans the entire page for accessibility violations.
     *
     * <p>Uses rules/tags from Configuration if specified, otherwise scans with all rules.</p>
     *
     * @param driver WebDriver instance
     * @return list of accessibility violations found (empty if none or disabled)
     */
    public static List<AccessibilityViolation> checkPage(WebDriver driver) {
        if (!isEnabled()) {
            log.debug("Accessibility checks disabled via configuration");
            return Collections.emptyList();
        }

        log.info("Running accessibility scan on page: {}", driver.getCurrentUrl());

        // Get configuration
        List<String> tags = getConfiguredTags();
        List<String> rules = getConfiguredRules();

        // Log configuration being used
        if (tags.isEmpty() && rules.isEmpty()) {
            log.warn("⚠️ WARNING: No tags or rules configured - using axe-core defaults which may be minimal!");
            log.warn("Consider adding 'accessibility.tags=wcag2a,wcag2aa,wcag21a,wcag21aa' to test.properties");
        } else {
            if (!tags.isEmpty()) {
                log.info("✓ Scanning with configured tags: {}", tags);
            }
            if (!rules.isEmpty()) {
                log.info("✓ Scanning with configured rules: {}", rules);
            }
        }

        try {
            AxeBuilder axeBuilder = new AxeBuilder();

            // Apply configured tags if specified
            if (!tags.isEmpty()) {
                axeBuilder.withTags(tags);
            }

            // Apply configured rules if specified
            if (!rules.isEmpty()) {
                axeBuilder.withRules(rules);
            }

            Results results = axeBuilder.analyze(driver);
            return processResults(results);

        } catch (Exception e) {
            log.error("⚠️ CRITICAL: Accessibility scan failed: {}", e.getMessage(), e);
            log.error("This may indicate axe-core script injection failed or CSP issues");
            throw new AccessibilityScanException("Accessibility scan failed - cannot determine compliance", e);
        }
    }

    /**
     * Scans a specific element for accessibility violations.
     *
     * @param driver  WebDriver instance
     * @param locator By locator for the element to scan
     * @return list of accessibility violations found
     */
    public static List<AccessibilityViolation> checkElement(WebDriver driver, By locator) {
        if (!isEnabled()) {
            log.debug("Accessibility checks disabled via configuration");
            return Collections.emptyList();
        }

        WebElement element = driver.findElement(locator);
        log.info("Running accessibility scan on element: {}", locator);

        try {
            AxeBuilder axeBuilder = new AxeBuilder();

            // Apply configured tags
            List<String> tags = getConfiguredTags();
            if (!tags.isEmpty()) {
                axeBuilder.withTags(tags);
            }

            Results results = axeBuilder.analyze(driver, element);
            return processResults(results);

        } catch (Exception e) {
            log.error("⚠️ CRITICAL: Accessibility scan failed for element {}: {}", locator, e.getMessage(), e);
            log.error("This may indicate axe-core script injection failed or CSP issues");
            throw new AccessibilityScanException("Accessibility scan failed for element " + locator, e);
        }
    }

    /**
     * Scans the page with specific WCAG tags.
     *
     * <p>Common tags include:
     * <ul>
     *   <li>wcag2a - WCAG 2.0 Level A</li>
     *   <li>wcag2aa - WCAG 2.0 Level AA</li>
     *   <li>wcag21a - WCAG 2.1 Level A</li>
     *   <li>wcag21aa - WCAG 2.1 Level AA</li>
     *   <li>best-practice - Best practices (not WCAG)</li>
     *   <li>section508 - Section 508 compliance</li>
     * </ul>
     *
     * @param driver WebDriver instance
     * @param tags   list of WCAG tags to check
     * @return list of accessibility violations found
     */
    public static List<AccessibilityViolation> checkPageWithTags(WebDriver driver, List<String> tags) {
        if (!isEnabled()) {
            log.debug("Accessibility checks disabled via configuration");
            return Collections.emptyList();
        }

        log.info("Running accessibility scan with tags: {}", tags);

        try {
            AxeBuilder axeBuilder = new AxeBuilder().withTags(tags);
            Results results = axeBuilder.analyze(driver);
            return processResults(results);

        } catch (Exception e) {
            log.error("⚠️ CRITICAL: Accessibility scan failed with tags {}: {}", tags, e.getMessage(), e);
            log.error("This may indicate axe-core script injection failed or CSP issues");
            throw new AccessibilityScanException("Accessibility scan failed with tags " + tags, e);
        }
    }

    /**
     * Scans the page with specific rule IDs.
     *
     * <p>Example rule IDs: "color-contrast", "image-alt", "label", "link-name"</p>
     *
     * @param driver WebDriver instance
     * @param rules  list of rule IDs to check
     * @return list of accessibility violations found
     * @see <a href="https://dequeuniversity.com/rules/axe/">Axe Rules Reference</a>
     */
    public static List<AccessibilityViolation> checkPageWithRules(WebDriver driver, List<String> rules) {
        if (!isEnabled()) {
            log.debug("Accessibility checks disabled via configuration");
            return Collections.emptyList();
        }

        log.info("Running accessibility scan with rules: {}", rules);

        try {
            AxeBuilder axeBuilder = new AxeBuilder().withRules(rules);
            Results results = axeBuilder.analyze(driver);
            return processResults(results);

        } catch (Exception e) {
            log.error("⚠️ CRITICAL: Accessibility scan failed with rules {}: {}", rules, e.getMessage(), e);
            log.error("This may indicate axe-core script injection failed or CSP issues");
            throw new AccessibilityScanException("Accessibility scan failed with rules " + rules, e);
        }
    }

    // ==================== Results Methods ====================

    /**
     * Returns violations from the last scan for the current thread.
     *
     * @return list of violations from last scan
     */
    public static List<AccessibilityViolation> getViolations() {
        return new ArrayList<>(lastViolations.get());
    }

    /**
     * Clears stored violations for the current thread.
     */
    public static void clearViolations() {
        lastViolations.get().clear();
    }

    /**
     * Checks if any violations were found in the last scan.
     *
     * @return true if violations exist
     */
    public static boolean hasViolations() {
        return !lastViolations.get().isEmpty();
    }

    /**
     * Returns count of violations from last scan.
     *
     * @return number of violations
     */
    public static int getViolationCount() {
        return lastViolations.get().size();
    }

    // ==================== Configuration Methods ====================

    /**
     * Checks if accessibility testing is enabled via configuration.
     *
     * @return true if enabled (default: true)
     */
    public static boolean isEnabled() {
        return Configuration.getInstance().getBoolean(PROP_ENABLED, true);
    }

    /**
     * Gets configured WCAG tags from properties.
     *
     * @return list of tags, or empty list if not configured
     */
    private static List<String> getConfiguredTags() {
        return Configuration.getInstance().getList(PROP_TAGS);
    }

    /**
     * Gets configured rule IDs from properties.
     *
     * @return list of rule IDs, or empty list if not configured
     */
    private static List<String> getConfiguredRules() {
        return Configuration.getInstance().getList(PROP_RULES);
    }

    // ==================== Internal Methods ====================

    /**
     * Processes Axe results into AccessibilityViolation objects.
     * Includes verification that the scan actually ran successfully.
     */
    private static List<AccessibilityViolation> processResults(Results results) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        // Verify scan actually ran
        if (results == null) {
            log.error("⚠️ Axe-core returned null results - scan did not run!");
            throw new AccessibilityScanException("Accessibility scan returned null results");
        }

        // Check that axe-core actually processed rules
        if (results.getPasses() == null || results.getPasses().isEmpty()) {
            log.warn("⚠️ WARNING: Axe-core returned no passing rules - scan may not have run properly!");
            log.warn("Check for CSP issues, script injection failures, or configuration problems");
        } else {
            log.info("✓ Axe-core scan completed successfully - checked {} rules ({} passed)",
                    results.getPasses().size() + (results.getViolations() != null ? results.getViolations().size() : 0),
                    results.getPasses().size());
        }

        if (results.getViolations() == null || results.getViolations().isEmpty()) {
            log.info("✓ No accessibility violations found");
            lastViolations.set(violations);
            return violations;
        }

        for (Rule rule : results.getViolations()) {
            // Extract element selectors and HTML from nodes
            List<String> elementSelectors = new ArrayList<>();
            List<String> elementHtml = new ArrayList<>();

            if (rule.getNodes() != null) {
                for (CheckedNode node : rule.getNodes()) {
                    // Get CSS selector - nodes have target which is a list of selectors
                    Object targetObj = node.getTarget();
                    if (targetObj instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<String> targetList = (List<String>) targetObj;
                        if (!targetList.isEmpty()) {
                            String selector = String.join(" ", targetList);
                            elementSelectors.add(selector);
                        }
                    }
                    // Get HTML snippet
                    if (node.getHtml() != null) {
                        elementHtml.add(node.getHtml());
                    }
                }
            }

            AccessibilityViolation violation = new AccessibilityViolation(
                    rule.getId(),
                    rule.getDescription(),
                    rule.getHelp(),
                    rule.getHelpUrl(),
                    rule.getImpact(),
                    rule.getTags(),
                    rule.getNodes().size(),
                    elementSelectors,
                    elementHtml
            );
            violations.add(violation);

            // Log each violation
            log.warn("Accessibility violation: [{}] {} - {} ({} occurrences)",
                    violation.getImpact().toUpperCase(),
                    violation.getRuleId(),
                    violation.getDescription(),
                    violation.getNodeCount());
        }

        log.warn("Found {} accessibility violation(s)", violations.size());
        lastViolations.set(violations);
        return violations;
    }

    // ==================== Violation Data Class ====================

    /**
     * Represents an accessibility violation found during scanning.
     */
    public static class AccessibilityViolation {
        private final String ruleId;
        private final String description;
        private final String help;
        private final String helpUrl;
        private final String impact;
        private final List<String> tags;
        private final int nodeCount;
        private final List<String> elementSelectors;
        private final List<String> elementHtml;

        public AccessibilityViolation(String ruleId, String description, String help,
                                       String helpUrl, String impact, List<String> tags, int nodeCount) {
            this(ruleId, description, help, helpUrl, impact, tags, nodeCount,
                 Collections.emptyList(), Collections.emptyList());
        }

        public AccessibilityViolation(String ruleId, String description, String help,
                                       String helpUrl, String impact, List<String> tags, int nodeCount,
                                       List<String> elementSelectors, List<String> elementHtml) {
            this.ruleId = ruleId;
            this.description = description;
            this.help = help;
            this.helpUrl = helpUrl;
            this.impact = impact != null ? impact : "unknown";
            this.tags = tags != null ? new ArrayList<>(tags) : Collections.emptyList();
            this.nodeCount = nodeCount;
            this.elementSelectors = elementSelectors != null ? new ArrayList<>(elementSelectors) : Collections.emptyList();
            this.elementHtml = elementHtml != null ? new ArrayList<>(elementHtml) : Collections.emptyList();
        }

        /** Rule identifier (e.g., "color-contrast", "image-alt") */
        public String getRuleId() { return ruleId; }

        /** Full description of the violation */
        public String getDescription() { return description; }

        /** Short help text */
        public String getHelp() { return help; }

        /** URL to detailed documentation */
        public String getHelpUrl() { return helpUrl; }

        /** Impact level: "minor", "moderate", "serious", "critical" */
        public String getImpact() { return impact; }

        /** WCAG tags this rule belongs to */
        public List<String> getTags() { return Collections.unmodifiableList(tags); }

        /** Number of DOM nodes affected */
        public int getNodeCount() { return nodeCount; }

        /** CSS selectors for affected elements */
        public List<String> getElementSelectors() { return Collections.unmodifiableList(elementSelectors); }

        /** HTML snippets of affected elements */
        public List<String> getElementHtml() { return Collections.unmodifiableList(elementHtml); }

        /** Get the first element selector (for single-element issues) */
        public String getFirstSelector() {
            return elementSelectors.isEmpty() ? null : elementSelectors.get(0);
        }

        /** Get the first HTML snippet */
        public String getFirstHtml() {
            return elementHtml.isEmpty() ? null : elementHtml.get(0);
        }

        /** Check if this violation has element information */
        public boolean hasElementInfo() {
            return !elementSelectors.isEmpty();
        }

        /** Check if this is a critical or serious violation */
        public boolean isSevere() {
            return "critical".equalsIgnoreCase(impact) || "serious".equalsIgnoreCase(impact);
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (%d nodes) - %s",
                    impact.toUpperCase(), ruleId, description, nodeCount, helpUrl);
        }
    }

    // ==================== Exception Class ====================

    /**
     * Exception thrown when accessibility scanning fails.
     *
     * <p>This exception indicates that the scan could not complete successfully,
     * meaning accessibility compliance cannot be determined. This is different
     * from finding accessibility violations - it means the scan itself failed.</p>
     */
    public static class AccessibilityScanException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a new accessibility scan exception.
         *
         * @param message description of the failure
         */
        public AccessibilityScanException(String message) {
            super(message);
        }

        /**
         * Creates a new accessibility scan exception with cause.
         *
         * @param message description of the failure
         * @param cause   the underlying exception
         */
        public AccessibilityScanException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ==================== Retry Logic ====================

    /** Default number of retry attempts for scan failures */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * Analyzes the page with retry logic for resilience against injection failures.
     *
     * <p>Uses exponential backoff between retries to handle transient issues
     * like slow page loads or CSP initialization delays.</p>
     *
     * @param axeBuilder configured AxeBuilder instance
     * @param driver     WebDriver instance
     * @param maxRetries maximum number of attempts
     * @return Results from axe-core analysis
     * @throws AccessibilityScanException if all retry attempts fail
     */
    private static Results analyzeWithRetry(AxeBuilder axeBuilder, WebDriver driver, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Axe-core scan attempt {} of {}", attempt, maxRetries);
                return axeBuilder.analyze(driver);
            } catch (Exception e) {
                lastException = e;
                log.warn("Axe-core scan attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        long sleepMs = 1000L * attempt; // Exponential backoff
                        log.debug("Waiting {}ms before retry...", sleepMs);
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AccessibilityScanException("Scan interrupted during retry", ie);
                    }
                }
            }
        }

        throw new AccessibilityScanException(
                "Axe-core scan failed after " + maxRetries + " attempts", lastException);
    }

    /**
     * Scans the page with retry logic for better resilience.
     *
     * <p>This is an alternative to {@link #checkPage(WebDriver)} that automatically
     * retries on transient failures.</p>
     *
     * @param driver     WebDriver instance
     * @param maxRetries maximum number of attempts (default: 3)
     * @return list of accessibility violations found
     * @throws AccessibilityScanException if all retries fail
     */
    public static List<AccessibilityViolation> checkPageWithRetry(WebDriver driver, int maxRetries) {
        if (!isEnabled()) {
            log.debug("Accessibility checks disabled via configuration");
            return Collections.emptyList();
        }

        log.info("Running accessibility scan with retry on page: {}", driver.getCurrentUrl());

        List<String> tags = getConfiguredTags();
        List<String> rules = getConfiguredRules();

        AxeBuilder axeBuilder = new AxeBuilder();
        if (!tags.isEmpty()) {
            axeBuilder.withTags(tags);
        }
        if (!rules.isEmpty()) {
            axeBuilder.withRules(rules);
        }

        Results results = analyzeWithRetry(axeBuilder, driver, maxRetries);
        return processResults(results);
    }

    /**
     * Scans the page with default retry logic (3 attempts).
     *
     * @param driver WebDriver instance
     * @return list of accessibility violations found
     */
    public static List<AccessibilityViolation> checkPageWithRetry(WebDriver driver) {
        return checkPageWithRetry(driver, DEFAULT_MAX_RETRIES);
    }
}
