package utils;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.AccessibilityChecker.AccessibilityIssue;
import utils.AccessibilityChecker.IssueType;
import utils.AccessibilityChecker.Severity;
import utils.AccessibilityUtils.AccessibilityViolation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Accessibility Reporter for logging and reporting accessibility violations.
 *
 * <p>Integrates with both SLF4J logging and ExtentReports to provide
 * comprehensive accessibility violation reporting.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Console logging with clear formatting via SLF4J</li>
 *   <li>ExtentReports integration with severity-based coloring</li>
 *   <li>Summary statistics generation</li>
 *   <li>Support for both Axe-core violations and custom checker issues</li>
 *   <li>Thread-safe for parallel test execution</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Log Axe-core violations to console
 * List<AccessibilityViolation> violations = AccessibilityUtils.checkPage(driver);
 * AccessibilityReporter.logAxeViolations(violations);
 *
 * // Attach to ExtentReports
 * ExtentTest test = ExtentReportManager.getTest();
 * AccessibilityReporter.attachAxeViolationsToReport(test, violations);
 *
 * // Log custom checker issues
 * List<AccessibilityIssue> issues = AccessibilityChecker.checkAllTraps(driver);
 * AccessibilityReporter.logCustomIssues(issues);
 * AccessibilityReporter.attachCustomIssuesToReport(test, issues);
 *
 * // Generate summary
 * String summary = AccessibilityReporter.generateAxeSummary(violations);
 * }</pre>
 *
 * @see AccessibilityUtils
 * @see AccessibilityChecker
 * @see ExtentReportManager
 */
public final class AccessibilityReporter {

    private static final Logger log = LoggerFactory.getLogger(AccessibilityReporter.class);

    // Thread-safe counters for tracking across test runs
    private static final ThreadLocal<ReportStats> threadStats = ThreadLocal.withInitial(ReportStats::new);

    // Formatting constants
    private static final String SEPARATOR = "─".repeat(70);
    private static final String DOUBLE_SEPARATOR = "═".repeat(70);

    private AccessibilityReporter() {
        // Prevent instantiation - utility class
    }

    // ==================== AXE-CORE VIOLATION LOGGING ====================

    /**
     * Logs Axe-core violations to SLF4J with formatted output.
     *
     * @param violations list of Axe-core violations
     */
    public static void logAxeViolations(List<AccessibilityViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            log.info("✓ No Axe-core accessibility violations found");
            return;
        }

        log.warn(DOUBLE_SEPARATOR);
        log.warn("ACCESSIBILITY VIOLATIONS (Axe-core): {} found", violations.size());
        log.warn(DOUBLE_SEPARATOR);

        int index = 1;
        for (AccessibilityViolation violation : violations) {
            logSingleAxeViolation(violation, index++);
        }

        log.warn(SEPARATOR);
        log.warn("Total: {} violation(s)", violations.size());
        log.warn(DOUBLE_SEPARATOR);

        // Update stats
        threadStats.get().axeViolations.addAndGet(violations.size());
    }

    /**
     * Logs a single Axe-core violation with detailed formatting.
     */
    private static void logSingleAxeViolation(AccessibilityViolation violation, int index) {
        log.warn(SEPARATOR);
        log.warn("#{} [{}] {}", index, violation.getImpact().toUpperCase(), violation.getRuleId());
        log.warn(SEPARATOR);
        log.warn("  Severity:    {}", formatImpact(violation.getImpact()));
        log.warn("  Rule:        {}", violation.getRuleId());
        log.warn("  Description: {}", violation.getDescription());
        log.warn("  Help:        {}", violation.getHelp());
        log.warn("  Elements:    {} affected", violation.getNodeCount());
        log.warn("  WCAG Tags:   {}", String.join(", ", violation.getTags()));
        log.warn("  How to fix:  {}", violation.getHelpUrl());
    }

    // ==================== CUSTOM CHECKER ISSUE LOGGING ====================

    /**
     * Logs custom checker issues to SLF4J with formatted output.
     *
     * @param issues list of custom checker issues
     */
    public static void logCustomIssues(List<AccessibilityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            log.info("✓ No custom accessibility issues found");
            return;
        }

        log.warn(DOUBLE_SEPARATOR);
        log.warn("ACCESSIBILITY ISSUES (Custom Checker): {} found", issues.size());
        log.warn(DOUBLE_SEPARATOR);

        int index = 1;
        for (AccessibilityIssue issue : issues) {
            logSingleCustomIssue(issue, index++);
        }

        log.warn(SEPARATOR);
        log.warn("Total: {} issue(s)", issues.size());
        log.warn(DOUBLE_SEPARATOR);

        // Update stats
        threadStats.get().customIssues.addAndGet(issues.size());
    }

    /**
     * Logs a single custom checker issue with detailed formatting.
     */
    private static void logSingleCustomIssue(AccessibilityIssue issue, int index) {
        log.warn(SEPARATOR);
        log.warn("#{} [{}] {}", index, issue.getSeverity(), issue.getType());
        log.warn(SEPARATOR);
        log.warn("  Severity:    {}", formatSeverity(issue.getSeverity()));
        log.warn("  Rule:        {}", issue.getType().name());
        log.warn("  Element:     {}", issue.getElementSelector());
        log.warn("  Description: {}", issue.getDescription());
        log.warn("  How to fix:  {}", issue.getRecommendation());
    }

    // ==================== EXTENT REPORTS INTEGRATION ====================

    /**
     * Attaches Axe-core violations to an ExtentTest report with severity-based coloring.
     *
     * @param test       ExtentTest instance to attach violations to
     * @param violations list of Axe-core violations
     */
    public static void attachAxeViolationsToReport(ExtentTest test, List<AccessibilityViolation> violations) {
        attachAxeViolationsToReport(test, violations, null);
    }

    /**
     * Attaches Axe-core violations to an ExtentTest report with element screenshots.
     * Uses enhanced format with summary at top and collapsible severity groups.
     *
     * @param test       ExtentTest instance to attach violations to
     * @param violations list of Axe-core violations
     * @param driver     WebDriver for capturing element screenshots (can be null to skip screenshots)
     */
    public static void attachAxeViolationsToReport(ExtentTest test, List<AccessibilityViolation> violations, WebDriver driver) {
        if (test == null) {
            log.warn("ExtentTest is null, skipping report attachment");
            return;
        }

        if (violations == null || violations.isEmpty()) {
            test.log(Status.PASS, MarkupHelper.createLabel(
                    "✓ No Axe-core accessibility violations found", ExtentColor.GREEN));
            return;
        }

        // Use the combined report format with just Axe violations
        attachCombinedReport(test, violations, null, driver);
    }

    /**
     * Attaches a single Axe-core violation to the ExtentTest (without screenshot).
     */
    private static void attachSingleAxeViolation(ExtentTest test, AccessibilityViolation violation) {
        attachSingleAxeViolation(test, violation, null);
    }

    /**
     * Attaches a single Axe-core violation to the ExtentTest with optional element screenshot.
     */
    private static void attachSingleAxeViolation(ExtentTest test, AccessibilityViolation violation, WebDriver driver) {
        Status status = mapImpactToStatus(violation.getImpact());
        ExtentColor color = mapImpactToColor(violation.getImpact());

        // Create formatted violation entry
        String header = String.format("[%s] %s", violation.getImpact().toUpperCase(), violation.getRuleId());
        test.log(status, MarkupHelper.createLabel(header, color));

        // Build element info string
        String elementInfo = String.valueOf(violation.getNodeCount()) + " element(s)";
        if (violation.hasElementInfo()) {
            String firstSelector = violation.getFirstSelector();
            if (firstSelector != null && firstSelector.length() <= 100) {
                elementInfo += " - First: " + escapeHtml(firstSelector);
            }
        }

        // Create details table
        String[][] detailsData = {
                {"Severity", formatImpact(violation.getImpact())},
                {"Rule ID", violation.getRuleId()},
                {"Description", violation.getDescription()},
                {"Help", violation.getHelp()},
                {"Elements Affected", elementInfo},
                {"WCAG Tags", String.join(", ", violation.getTags())},
                {"How to Fix", createHyperlink(violation.getHelpUrl(), "View Documentation")}
        };
        test.log(Status.INFO, MarkupHelper.createTable(detailsData));

        // Capture element screenshots if driver is available
        if (driver != null && violation.hasElementInfo()) {
            captureAndAttachElementScreenshots(test, driver, violation.getElementSelectors(),
                    violation.getRuleId(), violation.getImpact());
        }
    }

    /**
     * Attaches custom checker issues to an ExtentTest report.
     *
     * @param test   ExtentTest instance to attach issues to
     * @param issues list of custom checker issues
     */
    public static void attachCustomIssuesToReport(ExtentTest test, List<AccessibilityIssue> issues) {
        attachCustomIssuesToReport(test, issues, null);
    }

    /**
     * Attaches custom checker issues to an ExtentTest report with element screenshots.
     * Uses enhanced format with summary at top and collapsible severity groups.
     *
     * @param test   ExtentTest instance to attach issues to
     * @param issues list of custom checker issues
     * @param driver WebDriver for capturing element screenshots (can be null to skip screenshots)
     */
    public static void attachCustomIssuesToReport(ExtentTest test, List<AccessibilityIssue> issues, WebDriver driver) {
        if (test == null) {
            log.warn("ExtentTest is null, skipping report attachment");
            return;
        }

        if (issues == null || issues.isEmpty()) {
            test.log(Status.PASS, MarkupHelper.createLabel(
                    "✓ No custom accessibility issues found", ExtentColor.GREEN));
            return;
        }

        // Use the combined report format with just custom issues
        attachCombinedReport(test, null, issues, driver);
    }

    /**
     * Attaches a single custom checker issue to the ExtentTest (without screenshot).
     */
    private static void attachSingleCustomIssue(ExtentTest test, AccessibilityIssue issue) {
        attachSingleCustomIssue(test, issue, null);
    }

    /**
     * Attaches a single custom checker issue to the ExtentTest with optional element screenshot.
     */
    private static void attachSingleCustomIssue(ExtentTest test, AccessibilityIssue issue, WebDriver driver) {
        Status status = mapSeverityToStatus(issue.getSeverity());
        ExtentColor color = mapSeverityToColor(issue.getSeverity());

        // Create formatted issue entry
        String header = String.format("[%s] %s", issue.getSeverity(), issue.getType());
        test.log(status, MarkupHelper.createLabel(header, color));

        // Create details table
        String[][] detailsData = {
                {"Severity", formatSeverity(issue.getSeverity())},
                {"Issue Type", issue.getType().name()},
                {"Element", escapeHtml(issue.getElementSelector())},
                {"Description", issue.getDescription()},
                {"How to Fix", issue.getRecommendation()}
        };
        test.log(Status.INFO, MarkupHelper.createTable(detailsData));

        // Capture element screenshot if driver is available
        if (driver != null && issue.getElementSelector() != null && !issue.getElementSelector().isEmpty()) {
            captureAndAttachElementScreenshot(test, driver, issue.getElementSelector(),
                    issue.getType().name(), issue.getSeverity().name());
        }
    }

    /**
     * Attaches a combined accessibility report section to ExtentTest.
     *
     * @param test       ExtentTest instance
     * @param violations Axe-core violations (may be null)
     * @param issues     Custom checker issues (may be null)
     */
    public static void attachCombinedReport(ExtentTest test,
                                             List<AccessibilityViolation> violations,
                                             List<AccessibilityIssue> issues) {
        attachCombinedReport(test, violations, issues, null);
    }

    /**
     * Attaches a combined accessibility report section to ExtentTest with element screenshots.
     * Features: Summary at top, separate sections for Axe-core and Custom checker,
     * collapsible severity groups within each section.
     *
     * @param test       ExtentTest instance
     * @param violations Axe-core violations (may be null)
     * @param issues     Custom checker issues (may be null)
     * @param driver     WebDriver for capturing element screenshots (can be null to skip screenshots)
     */
    public static void attachCombinedReport(ExtentTest test,
                                             List<AccessibilityViolation> violations,
                                             List<AccessibilityIssue> issues,
                                             WebDriver driver) {
        if (test == null) {
            log.warn("ExtentTest is null, skipping report attachment");
            return;
        }

        try {
            // Check if both are empty
            boolean noViolations = violations == null || violations.isEmpty();
            boolean noIssues = issues == null || issues.isEmpty();

            if (noViolations && noIssues) {
                test.log(Status.PASS, MarkupHelper.createLabel(
                        "✓ Page passed all accessibility checks", ExtentColor.GREEN));
                return;
            }

            // 1. ADD SUMMARY DASHBOARD AT THE TOP
            try {
                test.info(generateSummaryDashboard(violations, issues));
            } catch (Exception e) {
                log.warn("Failed to generate summary dashboard: {}", e.getMessage());
                test.info("Summary: " + (violations != null ? violations.size() : 0) + " Axe violations, " +
                         (issues != null ? issues.size() : 0) + " custom issues");
            }

            // 2. AXE-CORE VIOLATIONS SECTION (separate category)
            if (!noViolations) {
                try {
                    test.info(generateAxeCoreSection(violations, driver));
                } catch (Exception e) {
                    log.warn("Failed to generate Axe-core section: {}", e.getMessage());
                    test.warning("Axe-core violations: " + violations.size());
                }
            }

            // 3. CUSTOM CHECKER ISSUES SECTION (separate category)
            if (!noIssues) {
                try {
                    test.info(generateCustomCheckerSection(issues, driver));
                } catch (Exception e) {
                    log.warn("Failed to generate Custom checker section: {}", e.getMessage());
                    test.warning("Custom checker issues: " + issues.size());
                }
            }

        } catch (Exception e) {
            log.error("Failed to attach combined accessibility report: {}", e.getMessage(), e);
            // Fallback to simple text report
            test.warning("Accessibility report generation failed. " +
                        "Violations: " + (violations != null ? violations.size() : 0) + ", " +
                        "Issues: " + (issues != null ? issues.size() : 0));
        }
    }

    /**
     * Generate the Axe-core violations section with severity groupings.
     */
    private static String generateAxeCoreSection(List<AccessibilityViolation> violations, WebDriver driver) {
        if (violations == null || violations.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        // Section container
        html.append("<div style='margin: 20px 0; border: 2px solid #3498db; border-radius: 10px; overflow: hidden;'>");

        // Section header
        html.append("<div style='background: linear-gradient(135deg, #3498db 0%, #2980b9 100%); ")
            .append("padding: 15px 20px; color: white;'>");
        html.append("<h3 style='margin: 0; display: flex; align-items: center; justify-content: space-between;'>");
        html.append("<span>🔍 Axe-core Violations</span>");
        html.append("<span style='background: rgba(255,255,255,0.2); padding: 5px 15px; border-radius: 20px; font-size: 14px;'>")
            .append(violations.size()).append(" violation(s)</span>");
        html.append("</h3>");
        html.append("<p style='margin: 5px 0 0 0; opacity: 0.9; font-size: 13px;'>")
            .append("Automated WCAG compliance checks via axe-core engine</p>");
        html.append("</div>");

        // Section content
        html.append("<div style='padding: 15px; background: #f8f9fa;'>");

        // Group by severity
        List<ViolationEntry> entries = violations.stream()
                .map(ViolationEntry::new)
                .sorted(java.util.Comparator.comparingInt(ViolationEntry::getSeverityOrder))
                .collect(Collectors.toList());

        Map<String, List<ViolationEntry>> bySeverity = groupBySeverity(entries);

        // Critical
        if (bySeverity.containsKey("critical")) {
            html.append(generateCollapsibleSection("🔴 CRITICAL", bySeverity.get("critical"), driver, true));
        }
        // Serious
        if (bySeverity.containsKey("serious")) {
            html.append(generateCollapsibleSection("🟠 SERIOUS", bySeverity.get("serious"), driver, true));
        }
        // Moderate
        if (bySeverity.containsKey("moderate")) {
            html.append(generateCollapsibleSection("🟡 MODERATE", bySeverity.get("moderate"), driver, false));
        }
        // Minor
        if (bySeverity.containsKey("minor")) {
            html.append(generateCollapsibleSection("🟢 MINOR", bySeverity.get("minor"), driver, false));
        }

        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Generate the Custom checker issues section with severity groupings.
     */
    private static String generateCustomCheckerSection(List<AccessibilityIssue> issues, WebDriver driver) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        // Section container
        html.append("<div style='margin: 20px 0; border: 2px solid #9b59b6; border-radius: 10px; overflow: hidden;'>");

        // Section header
        html.append("<div style='background: linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%); ")
            .append("padding: 15px 20px; color: white;'>");
        html.append("<h3 style='margin: 0; display: flex; align-items: center; justify-content: space-between;'>");
        html.append("<span>🔎 Custom Checker Issues</span>");
        html.append("<span style='background: rgba(255,255,255,0.2); padding: 5px 15px; border-radius: 20px; font-size: 14px;'>")
            .append(issues.size()).append(" issue(s)</span>");
        html.append("</h3>");
        html.append("<p style='margin: 5px 0 0 0; opacity: 0.9; font-size: 13px;'>")
            .append("Additional accessibility checks (focus traps, keyboard navigation, etc.)</p>");
        html.append("</div>");

        // Section content
        html.append("<div style='padding: 15px; background: #faf8fc;'>");

        // Group by severity
        List<ViolationEntry> entries = issues.stream()
                .map(ViolationEntry::new)
                .sorted(java.util.Comparator.comparingInt(ViolationEntry::getSeverityOrder))
                .collect(Collectors.toList());

        Map<String, List<ViolationEntry>> bySeverity = groupBySeverity(entries);

        // Critical
        if (bySeverity.containsKey("critical")) {
            html.append(generateCollapsibleSection("🔴 CRITICAL", bySeverity.get("critical"), driver, true));
        }
        // Moderate (custom checker uses moderate instead of serious)
        if (bySeverity.containsKey("moderate")) {
            html.append(generateCollapsibleSection("🟡 MODERATE", bySeverity.get("moderate"), driver, false));
        }
        // Minor
        if (bySeverity.containsKey("minor")) {
            html.append(generateCollapsibleSection("🟢 MINOR", bySeverity.get("minor"), driver, false));
        }

        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Entry combining both Axe violations and custom issues for unified sorting.
     */
    private static class ViolationEntry {
        final String severity;
        final String ruleName;
        final String description;
        final String helpText;
        final String helpUrl;
        final List<String> selectors;
        final List<String> tags;
        final int nodeCount;
        final boolean isAxe;

        ViolationEntry(AccessibilityViolation v) {
            this.severity = v.getImpact() != null ? v.getImpact().toLowerCase() : "unknown";
            this.ruleName = v.getRuleId();
            this.description = v.getDescription();
            this.helpText = v.getHelp();
            this.helpUrl = v.getHelpUrl();
            this.selectors = v.getElementSelectors();
            this.tags = v.getTags();
            this.nodeCount = v.getNodeCount();
            this.isAxe = true;
        }

        ViolationEntry(AccessibilityIssue i) {
            this.severity = i.getSeverity().name().toLowerCase();
            this.ruleName = i.getType().name();
            this.description = i.getDescription();
            this.helpText = i.getRecommendation();
            this.helpUrl = null;
            this.selectors = i.getElementSelector() != null ?
                    List.of(i.getElementSelector()) : List.of();
            this.tags = List.of();
            this.nodeCount = 1;
            this.isAxe = false;
        }

        int getSeverityOrder() {
            return switch (severity) {
                case "critical" -> 0;
                case "serious" -> 1;
                case "moderate" -> 2;
                case "minor" -> 3;
                default -> 4;
            };
        }
    }

    /**
     * Combine Axe violations and custom issues, sort by severity.
     */
    private static List<ViolationEntry> combineAndSortBySeverity(
            List<AccessibilityViolation> violations, List<AccessibilityIssue> issues) {

        List<ViolationEntry> entries = new java.util.ArrayList<>();

        if (violations != null) {
            violations.forEach(v -> entries.add(new ViolationEntry(v)));
        }
        if (issues != null) {
            issues.forEach(i -> entries.add(new ViolationEntry(i)));
        }

        entries.sort(java.util.Comparator.comparingInt(ViolationEntry::getSeverityOrder));
        return entries;
    }

    /**
     * Group entries by severity level.
     */
    private static Map<String, List<ViolationEntry>> groupBySeverity(List<ViolationEntry> entries) {
        return entries.stream().collect(Collectors.groupingBy(
                e -> e.severity,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    /**
     * Generate summary dashboard HTML for top of report.
     */
    private static String generateSummaryDashboard(List<AccessibilityViolation> violations,
                                                    List<AccessibilityIssue> issues) {
        int axeCount = violations != null ? violations.size() : 0;
        int customCount = issues != null ? issues.size() : 0;
        int total = axeCount + customCount;

        // Count by severity for Axe-core
        long axeCritical = 0, axeSerious = 0, axeModerate = 0, axeMinor = 0;
        if (violations != null) {
            axeCritical = violations.stream().filter(v -> "critical".equalsIgnoreCase(v.getImpact())).count();
            axeSerious = violations.stream().filter(v -> "serious".equalsIgnoreCase(v.getImpact())).count();
            axeModerate = violations.stream().filter(v -> "moderate".equalsIgnoreCase(v.getImpact())).count();
            axeMinor = violations.stream().filter(v -> "minor".equalsIgnoreCase(v.getImpact())).count();
        }

        // Count by severity for Custom checker
        long customCritical = 0, customModerate = 0, customMinor = 0;
        if (issues != null) {
            customCritical = issues.stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();
            customModerate = issues.stream().filter(i -> i.getSeverity() == Severity.MODERATE).count();
            customMinor = issues.stream().filter(i -> i.getSeverity() == Severity.MINOR).count();
        }

        // Combined totals
        long totalCritical = axeCritical + customCritical;
        long totalSerious = axeSerious;
        long totalModerate = axeModerate + customModerate;
        long totalMinor = axeMinor + customMinor;

        StringBuilder html = new StringBuilder();
        html.append("<div style='background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); ")
            .append("padding: 20px; border-radius: 10px; margin-bottom: 20px; color: white;'>");

        // Title
        html.append("<h3 style='margin: 0 0 15px 0; color: #fff; border-bottom: 2px solid #e94560; ")
            .append("padding-bottom: 10px;'>📊 Accessibility Summary</h3>");

        // Total severity cards row
        html.append("<div style='display: flex; gap: 15px; flex-wrap: wrap; margin-bottom: 20px;'>");
        html.append(createStatCard("Total Issues", String.valueOf(total), "#6c5ce7"));
        html.append(createStatCard("🔴 Critical", String.valueOf(totalCritical), "#e74c3c"));
        html.append(createStatCard("🟠 Serious", String.valueOf(totalSerious), "#e67e22"));
        html.append(createStatCard("🟡 Moderate", String.valueOf(totalModerate), "#f39c12"));
        html.append(createStatCard("🟢 Minor", String.valueOf(totalMinor), "#27ae60"));
        html.append("</div>");

        // Category breakdown section
        html.append("<div style='display: flex; gap: 20px; flex-wrap: wrap; padding-top: 15px; border-top: 1px solid #444;'>");

        // Axe-core category card
        html.append("<div style='flex: 1; min-width: 200px; background: rgba(52, 152, 219, 0.2); ")
            .append("border: 1px solid #3498db; border-radius: 8px; padding: 15px;'>");
        html.append("<h4 style='margin: 0 0 10px 0; color: #3498db;'>🔍 Axe-core Violations</h4>");
        html.append("<div style='font-size: 28px; font-weight: bold; color: #fff;'>").append(axeCount).append("</div>");
        html.append("<div style='font-size: 11px; color: #aaa; margin-top: 8px;'>");
        html.append("Critical: ").append(axeCritical);
        html.append(" | Serious: ").append(axeSerious);
        html.append(" | Moderate: ").append(axeModerate);
        html.append(" | Minor: ").append(axeMinor);
        html.append("</div>");
        html.append("</div>");

        // Custom checker category card
        html.append("<div style='flex: 1; min-width: 200px; background: rgba(155, 89, 182, 0.2); ")
            .append("border: 1px solid #9b59b6; border-radius: 8px; padding: 15px;'>");
        html.append("<h4 style='margin: 0 0 10px 0; color: #9b59b6;'>🔎 Custom Checker Issues</h4>");
        html.append("<div style='font-size: 28px; font-weight: bold; color: #fff;'>").append(customCount).append("</div>");
        html.append("<div style='font-size: 11px; color: #aaa; margin-top: 8px;'>");
        html.append("Critical: ").append(customCritical);
        html.append(" | Moderate: ").append(customModerate);
        html.append(" | Minor: ").append(customMinor);
        html.append("</div>");
        html.append("</div>");

        html.append("</div>"); // End category breakdown

        html.append("</div>"); // End main container
        return html.toString();
    }

    /**
     * Create a stat card for the dashboard.
     */
    private static String createStatCard(String label, String value, String color) {
        return String.format(
            "<div style='background: %s; padding: 15px 20px; border-radius: 8px; text-align: center; min-width: 100px;'>" +
            "<div style='font-size: 24px; font-weight: bold;'>%s</div>" +
            "<div style='font-size: 12px; opacity: 0.9;'>%s</div>" +
            "</div>", color, value, label);
    }

    /**
     * Generate collapsible section HTML with violations grouped by severity.
     */
    private static String generateCollapsibleSection(String title, List<ViolationEntry> entries,
                                                      WebDriver driver, boolean expandedByDefault) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        String uniqueId = "section_" + System.nanoTime() + "_" + title.hashCode();
        String openAttr = expandedByDefault ? " open" : "";

        StringBuilder html = new StringBuilder();
        html.append("<details style='margin: 10px 0; border: 1px solid #ddd; border-radius: 8px; ")
            .append("overflow: hidden;'").append(openAttr).append(">");

        // Summary/header (clickable)
        html.append("<summary style='background: ").append(getSeverityBgColor(entries.get(0).severity))
            .append("; padding: 12px 15px; cursor: pointer; font-weight: bold; ")
            .append("display: flex; justify-content: space-between; align-items: center;'>");
        html.append("<span>").append(title).append("</span>");
        html.append("<span style='background: rgba(0,0,0,0.2); padding: 3px 10px; border-radius: 12px;'>")
            .append(entries.size()).append(" issue(s)</span>");
        html.append("</summary>");

        // Content
        html.append("<div style='padding: 15px; background: #fafafa;'>");

        for (int i = 0; i < entries.size(); i++) {
            ViolationEntry entry = entries.get(i);
            html.append(generateViolationCard(entry, i + 1, driver));
        }

        html.append("</div>");
        html.append("</details>");

        return html.toString();
    }

    /**
     * Generate individual violation card with screenshot and details.
     */
    private static String generateViolationCard(ViolationEntry entry, int index, WebDriver driver) {
        if (entry == null) {
            return "<div style='padding: 10px; color: #999;'>Invalid entry</div>";
        }

        StringBuilder html = new StringBuilder();

        try {
            String severity = entry.severity != null ? entry.severity : "unknown";
            String ruleName = entry.ruleName != null ? entry.ruleName : "Unknown Rule";
            String description = entry.description != null ? entry.description : "No description";

            html.append("<div style='background: white; border: 1px solid #e0e0e0; border-radius: 8px; ")
                .append("margin-bottom: 15px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

            // Card header
            html.append("<div style='background: ").append(getSeverityHeaderColor(severity))
                .append("; padding: 10px 15px; color: white;'>");
            html.append("<strong>#").append(index).append(" ")
                .append(escapeHtml(ruleName)).append("</strong>");
            html.append("<span style='float: right; background: rgba(255,255,255,0.2); ")
                .append("padding: 2px 8px; border-radius: 10px; font-size: 11px;'>")
                .append(severity.toUpperCase()).append("</span>");
            html.append("</div>");

            // Card body with two columns: details + screenshot
            html.append("<div style='display: flex; flex-wrap: wrap;'>");

            // Left column: Details
            html.append("<div style='flex: 1; min-width: 300px; padding: 15px;'>");

            // Severity badge
            html.append("<p><strong>Severity:</strong> ").append(formatImpact(severity)).append("</p>");

            // Description
            html.append("<p><strong>Description:</strong><br>")
                .append(escapeHtml(description)).append("</p>");

            // Help text
            if (entry.helpText != null && !entry.helpText.isEmpty()) {
                html.append("<p><strong>Recommendation:</strong><br>")
                    .append(escapeHtml(entry.helpText)).append("</p>");
            }

            // Elements affected
            html.append("<p><strong>Elements Affected:</strong> ").append(entry.nodeCount).append("</p>");

            // First selector
            if (entry.selectors != null && !entry.selectors.isEmpty() && entry.selectors.get(0) != null) {
                html.append("<p><strong>Element:</strong><br>")
                    .append("<code style='background: #f5f5f5; padding: 3px 6px; border-radius: 3px; ")
                    .append("font-size: 11px; word-break: break-all;'>")
                    .append(escapeHtml(truncateSelector(entry.selectors.get(0))))
                    .append("</code></p>");
            }

            // WCAG tags
            if (entry.tags != null && !entry.tags.isEmpty()) {
                html.append("<p><strong>WCAG:</strong> ");
                for (String tag : entry.tags) {
                    if (tag != null) {
                        html.append("<span style='background: #e3f2fd; color: #1565c0; padding: 2px 6px; ")
                            .append("border-radius: 3px; margin-right: 5px; font-size: 11px;'>")
                            .append(escapeHtml(tag)).append("</span>");
                    }
                }
                html.append("</p>");
            }

            // Help URL
            if (entry.helpUrl != null && !entry.helpUrl.isEmpty()) {
                html.append("<p><a href='").append(escapeHtml(entry.helpUrl))
                    .append("' target='_blank' style='color: #1976d2;'>📖 View Documentation</a></p>");
            }

            html.append("</div>");

            // Right column: Screenshot
            html.append("<div style='flex: 0 0 300px; padding: 15px; background: #f9f9f9; ")
                .append("border-left: 1px solid #eee;'>");
            html.append("<p style='color: #666; font-size: 12px; margin: 0 0 10px 0;'>")
                .append("<strong>📸 Element Screenshot</strong></p>");

            // Capture and embed screenshot if driver available
            if (driver != null && entry.selectors != null && !entry.selectors.isEmpty() && entry.selectors.get(0) != null) {
                try {
                    String screenshotHtml = captureScreenshotForCard(driver, entry.selectors.get(0));
                    if (screenshotHtml != null) {
                        html.append(screenshotHtml);
                    } else {
                        html.append("<p style='color: #999; font-style: italic;'>Screenshot unavailable</p>");
                    }
                } catch (Exception e) {
                    log.debug("Screenshot capture failed: {}", e.getMessage());
                    html.append("<p style='color: #999; font-style: italic;'>Screenshot failed</p>");
                }
            } else {
                html.append("<p style='color: #999; font-style: italic;'>No screenshot</p>");
            }

            html.append("</div>");
            html.append("</div>"); // End flex container

            html.append("</div>"); // End card

        } catch (Exception e) {
            log.warn("Failed to generate violation card: {}", e.getMessage());
            html.setLength(0);
            html.append("<div style='padding: 10px; border: 1px solid #ddd; margin: 5px 0;'>")
                .append("<strong>").append(entry.ruleName != null ? escapeHtml(entry.ruleName) : "Unknown").append("</strong>")
                .append(" - ").append(entry.description != null ? escapeHtml(entry.description) : "No description")
                .append("</div>");
        }

        return html.toString();
    }

    /**
     * Capture screenshot and return as embedded HTML with clickable lightbox.
     */
    private static String captureScreenshotForCard(WebDriver driver, String selector) {
        if (selector == null || selector.isEmpty() || driver == null) {
            return null;
        }

        try {
            WebElement element = driver.findElement(By.cssSelector(selector));
            String base64 = ScreenshotUtils.captureElementScreenshotAsBase64(driver, element);
            if (base64 != null) {
                return generateClickableScreenshot(base64, "Element Screenshot", false);
            }
        } catch (Exception e) {
            // Try full page screenshot as fallback
            try {
                String base64 = ScreenshotUtils.captureScreenshotAsBase64(driver);
                if (base64 != null) {
                    return generateClickableScreenshot(base64, "Full Page (element not found)", true);
                }
            } catch (Exception ex) {
                log.debug("Failed to capture fallback screenshot: {}", ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Generate screenshot HTML. Right-click to open in new tab.
     */
    private static String generateClickableScreenshot(String base64, String caption, boolean isFullPage) {
        String escapedCaption = escapeHtml(caption);
        String dataUri = "data:image/png;base64," + base64;

        StringBuilder html = new StringBuilder();

        // Screenshot image - right-click to open in new tab
        html.append("<img src='").append(dataUri).append("' ")
            .append("style='max-width: 100%; max-height: 200px; border: 2px solid #ddd; ")
            .append("border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);' ")
            .append("alt='").append(escapedCaption).append("' ")
            .append("title='Right-click to open in new tab'/>");

        // Caption for full page screenshots
        if (isFullPage) {
            html.append("<p style='font-size: 10px; color: #999; margin: 5px 0 0 0;'>(")
                .append(escapedCaption).append(")</p>");
        }

        return html.toString();
    }

    /**
     * Get background color for severity section header.
     */
    private static String getSeverityBgColor(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "#ffebee";
            case "serious" -> "#fff3e0";
            case "moderate" -> "#fffde7";
            case "minor" -> "#e8f5e9";
            default -> "#f5f5f5";
        };
    }

    /**
     * Get header color for violation cards.
     */
    private static String getSeverityHeaderColor(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "#c62828";
            case "serious" -> "#ef6c00";
            case "moderate" -> "#f9a825";
            case "minor" -> "#2e7d32";
            default -> "#757575";
        };
    }

    // ==================== SUMMARY GENERATION ====================

    /**
     * Generates a formatted summary string for Axe-core violations.
     *
     * @param violations list of violations
     * @return formatted summary string
     */
    public static String generateAxeSummary(List<AccessibilityViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            return "Axe-core Summary: No violations found ✓";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(DOUBLE_SEPARATOR).append("\n");
        sb.append("AXE-CORE ACCESSIBILITY SUMMARY\n");
        sb.append(DOUBLE_SEPARATOR).append("\n\n");

        // Group by impact
        Map<String, Long> byImpact = violations.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getImpact() != null ? v.getImpact() : "unknown",
                        LinkedHashMap::new,
                        Collectors.counting()));

        sb.append("By Severity:\n");
        sb.append(String.format("  %-12s %s\n", "Critical:", byImpact.getOrDefault("critical", 0L)));
        sb.append(String.format("  %-12s %s\n", "Serious:", byImpact.getOrDefault("serious", 0L)));
        sb.append(String.format("  %-12s %s\n", "Moderate:", byImpact.getOrDefault("moderate", 0L)));
        sb.append(String.format("  %-12s %s\n", "Minor:", byImpact.getOrDefault("minor", 0L)));

        // Group by rule
        sb.append("\nBy Rule:\n");
        Map<String, Long> byRule = violations.stream()
                .collect(Collectors.groupingBy(AccessibilityViolation::getRuleId, Collectors.counting()));
        byRule.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(String.format("  %-30s %d\n", e.getKey(), e.getValue())));

        // Total affected elements
        int totalNodes = violations.stream().mapToInt(AccessibilityViolation::getNodeCount).sum();
        sb.append(String.format("\nTotal Violations: %d\n", violations.size()));
        sb.append(String.format("Total Elements Affected: %d\n", totalNodes));

        sb.append(DOUBLE_SEPARATOR);
        return sb.toString();
    }

    /**
     * Generates a formatted summary string for custom checker issues.
     *
     * @param issues list of issues
     * @return formatted summary string
     */
    public static String generateCustomSummary(List<AccessibilityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "Custom Checker Summary: No issues found ✓";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(DOUBLE_SEPARATOR).append("\n");
        sb.append("CUSTOM CHECKER ACCESSIBILITY SUMMARY\n");
        sb.append(DOUBLE_SEPARATOR).append("\n\n");

        // Group by severity
        Map<Severity, Long> bySeverity = issues.stream()
                .collect(Collectors.groupingBy(AccessibilityIssue::getSeverity, Collectors.counting()));

        sb.append("By Severity:\n");
        sb.append(String.format("  %-12s %s\n", "Critical:", bySeverity.getOrDefault(Severity.CRITICAL, 0L)));
        sb.append(String.format("  %-12s %s\n", "Moderate:", bySeverity.getOrDefault(Severity.MODERATE, 0L)));
        sb.append(String.format("  %-12s %s\n", "Minor:", bySeverity.getOrDefault(Severity.MINOR, 0L)));

        // Group by type
        sb.append("\nBy Issue Type:\n");
        Map<IssueType, Long> byType = issues.stream()
                .collect(Collectors.groupingBy(AccessibilityIssue::getType, Collectors.counting()));
        byType.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(String.format("  %-35s %d\n", e.getKey(), e.getValue())));

        sb.append(String.format("\nTotal Issues: %d\n", issues.size()));
        sb.append(DOUBLE_SEPARATOR);
        return sb.toString();
    }

    /**
     * Generates a combined summary for both Axe-core and custom checker.
     *
     * @param violations Axe-core violations
     * @param issues     Custom checker issues
     * @return formatted combined summary
     */
    public static String generateCombinedSummary(List<AccessibilityViolation> violations,
                                                  List<AccessibilityIssue> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(DOUBLE_SEPARATOR).append("\n");
        sb.append("COMBINED ACCESSIBILITY SUMMARY\n");
        sb.append(DOUBLE_SEPARATOR).append("\n\n");

        int axeCount = violations != null ? violations.size() : 0;
        int customCount = issues != null ? issues.size() : 0;

        sb.append(String.format("Axe-core Violations:    %d\n", axeCount));
        sb.append(String.format("Custom Checker Issues:  %d\n", customCount));
        sb.append(String.format("─".repeat(30) + "\n"));
        sb.append(String.format("Total:                  %d\n", axeCount + customCount));

        if (axeCount == 0 && customCount == 0) {
            sb.append("\n✓ Page passed all accessibility checks!\n");
        }

        sb.append(DOUBLE_SEPARATOR);
        return sb.toString();
    }

    // ==================== STATISTICS ====================

    /**
     * Gets the current thread's accessibility statistics.
     *
     * @return ReportStats for current thread
     */
    public static ReportStats getStats() {
        return threadStats.get();
    }

    /**
     * Resets the current thread's statistics.
     */
    public static void resetStats() {
        threadStats.set(new ReportStats());
    }

    /**
     * Removes thread-local storage (call after test completion).
     */
    public static void cleanup() {
        threadStats.remove();
    }

    // ==================== ELEMENT SCREENSHOT METHODS ====================

    /**
     * Captures and attaches screenshots for multiple element selectors.
     *
     * @param test      ExtentTest to attach screenshots to
     * @param driver    WebDriver instance
     * @param selectors List of CSS selectors for elements
     * @param ruleId    Axe rule ID for naming
     * @param impact    Impact level for naming
     */
    private static void captureAndAttachElementScreenshots(ExtentTest test, WebDriver driver,
                                                            List<String> selectors, String ruleId, String impact) {
        if (selectors == null || selectors.isEmpty()) {
            return;
        }

        // Limit screenshots to avoid report bloat
        int maxScreenshots = Math.min(selectors.size(), 3);

        for (int i = 0; i < maxScreenshots; i++) {
            String selector = selectors.get(i);
            String screenshotName = sanitizeForFilename(ruleId) + "_" + (i + 1);
            captureAndAttachElementScreenshot(test, driver, selector, screenshotName, impact);
        }

        if (selectors.size() > maxScreenshots) {
            test.info(String.format("Note: %d additional element(s) not captured to limit report size",
                    selectors.size() - maxScreenshots));
        }
    }

    /**
     * Captures and attaches a screenshot for a single element.
     *
     * @param test       ExtentTest to attach screenshot to
     * @param driver     WebDriver instance
     * @param selector   CSS selector for the element
     * @param issueName  Name for the issue (used in filename)
     * @param severity   Severity level for naming
     */
    private static void captureAndAttachElementScreenshot(ExtentTest test, WebDriver driver,
                                                           String selector, String issueName, String severity) {
        if (selector == null || selector.isEmpty() || driver == null) {
            return;
        }

        try {
            // Try to find and screenshot the element
            WebElement element = driver.findElement(By.cssSelector(selector));
            String base64Screenshot = ScreenshotUtils.captureElementScreenshotAsBase64(driver, element);

            if (base64Screenshot != null) {
                // Attach to report with descriptive caption
                String caption = String.format("[%s] %s - %s",
                        severity.toUpperCase(), issueName, truncateSelector(selector));
                test.info(caption)
                    .addScreenCaptureFromBase64String(base64Screenshot, caption);
                log.debug("Captured element screenshot for: {}", truncateSelector(selector));
            }
        } catch (org.openqa.selenium.NoSuchElementException e) {
            log.debug("Element not found for screenshot: {}", truncateSelector(selector));
            // Try highlighted full-page screenshot as fallback
            captureFullPageWithAnnotation(test, driver, selector, issueName, severity);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            log.debug("Stale element, skipping screenshot: {}", truncateSelector(selector));
        } catch (Exception e) {
            log.warn("Failed to capture element screenshot: {}", e.getMessage());
        }
    }

    /**
     * Captures a full-page screenshot with annotation when element capture fails.
     */
    private static void captureFullPageWithAnnotation(ExtentTest test, WebDriver driver,
                                                       String selector, String issueName, String severity) {
        try {
            String base64Screenshot = ScreenshotUtils.captureScreenshotAsBase64(driver);
            if (base64Screenshot != null) {
                String caption = String.format("[%s] %s (full page) - Element: %s",
                        severity.toUpperCase(), issueName, truncateSelector(selector));
                test.info(caption)
                    .addScreenCaptureFromBase64String(base64Screenshot, caption);
            }
        } catch (Exception e) {
            log.debug("Failed to capture fallback full-page screenshot: {}", e.getMessage());
        }
    }

    /**
     * Truncates a CSS selector for display purposes.
     */
    private static String truncateSelector(String selector) {
        if (selector == null) return "";
        if (selector.length() <= 80) return selector;
        return selector.substring(0, 77) + "...";
    }

    /**
     * Sanitizes a string for use in filenames.
     */
    private static String sanitizeForFilename(String name) {
        if (name == null) return "element";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Maps Axe impact level to ExtentReports Status.
     */
    private static Status mapImpactToStatus(String impact) {
        if (impact == null) return Status.WARNING;
        return switch (impact.toLowerCase()) {
            case "critical" -> Status.FAIL;
            case "serious" -> Status.FAIL;
            case "moderate" -> Status.WARNING;
            case "minor" -> Status.INFO;
            default -> Status.WARNING;
        };
    }

    /**
     * Maps Axe impact level to ExtentReports color.
     */
    private static ExtentColor mapImpactToColor(String impact) {
        if (impact == null) return ExtentColor.ORANGE;
        return switch (impact.toLowerCase()) {
            case "critical" -> ExtentColor.RED;
            case "serious" -> ExtentColor.RED;
            case "moderate" -> ExtentColor.ORANGE;
            case "minor" -> ExtentColor.AMBER;
            default -> ExtentColor.ORANGE;
        };
    }

    /**
     * Maps custom severity to ExtentReports Status.
     */
    private static Status mapSeverityToStatus(Severity severity) {
        if (severity == null) return Status.WARNING;
        return switch (severity) {
            case CRITICAL -> Status.FAIL;
            case MODERATE -> Status.WARNING;
            case MINOR -> Status.INFO;
        };
    }

    /**
     * Maps custom severity to ExtentReports color.
     */
    private static ExtentColor mapSeverityToColor(Severity severity) {
        if (severity == null) return ExtentColor.ORANGE;
        return switch (severity) {
            case CRITICAL -> ExtentColor.RED;
            case MODERATE -> ExtentColor.ORANGE;
            case MINOR -> ExtentColor.AMBER;
        };
    }

    /**
     * Formats impact level with indicator.
     */
    private static String formatImpact(String impact) {
        if (impact == null) return "Unknown";
        return switch (impact.toLowerCase()) {
            case "critical" -> "🔴 CRITICAL - Must fix, blocks users";
            case "serious" -> "🟠 SERIOUS - Should fix, significant barrier";
            case "moderate" -> "🟡 MODERATE - Consider fixing";
            case "minor" -> "🟢 MINOR - Nice to fix";
            default -> impact;
        };
    }

    /**
     * Formats severity with indicator.
     */
    private static String formatSeverity(Severity severity) {
        if (severity == null) return "Unknown";
        return switch (severity) {
            case CRITICAL -> "🔴 CRITICAL - Must fix, blocks users";
            case MODERATE -> "🟡 MODERATE - Should fix";
            case MINOR -> "🟢 MINOR - Nice to fix";
        };
    }

    /**
     * Creates an HTML hyperlink.
     */
    private static String createHyperlink(String url, String text) {
        if (url == null || url.isEmpty()) return text;
        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", escapeHtml(url), escapeHtml(text));
    }

    /**
     * Escapes HTML special characters.
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Generates summary table data for Axe violations.
     */
    private static String[][] generateAxeSummaryTable(List<AccessibilityViolation> violations) {
        Map<String, Long> byImpact = violations.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getImpact() != null ? v.getImpact() : "unknown",
                        Collectors.counting()));

        return new String[][]{
                {"Impact Level", "Count"},
                {"Critical", String.valueOf(byImpact.getOrDefault("critical", 0L))},
                {"Serious", String.valueOf(byImpact.getOrDefault("serious", 0L))},
                {"Moderate", String.valueOf(byImpact.getOrDefault("moderate", 0L))},
                {"Minor", String.valueOf(byImpact.getOrDefault("minor", 0L))},
                {"Total", String.valueOf(violations.size())}
        };
    }

    /**
     * Generates summary table data for custom issues.
     */
    private static String[][] generateCustomSummaryTable(List<AccessibilityIssue> issues) {
        Map<Severity, Long> bySeverity = issues.stream()
                .collect(Collectors.groupingBy(AccessibilityIssue::getSeverity, Collectors.counting()));

        return new String[][]{
                {"Severity", "Count"},
                {"Critical", String.valueOf(bySeverity.getOrDefault(Severity.CRITICAL, 0L))},
                {"Moderate", String.valueOf(bySeverity.getOrDefault(Severity.MODERATE, 0L))},
                {"Minor", String.valueOf(bySeverity.getOrDefault(Severity.MINOR, 0L))},
                {"Total", String.valueOf(issues.size())}
        };
    }

    /**
     * Generates combined summary markup for ExtentReports.
     */
    private static String generateCombinedSummaryMarkup(List<AccessibilityViolation> violations,
                                                         List<AccessibilityIssue> issues) {
        int axeCount = violations != null ? violations.size() : 0;
        int customCount = issues != null ? issues.size() : 0;

        StringBuilder html = new StringBuilder();
        html.append("<div style='padding: 10px; background: #f5f5f5; border-radius: 5px;'>");
        html.append("<h4 style='margin: 0 0 10px 0;'>Accessibility Summary</h4>");
        html.append("<table style='width: 100%;'>");
        html.append("<tr><td><strong>Axe-core Violations:</strong></td><td>").append(axeCount).append("</td></tr>");
        html.append("<tr><td><strong>Custom Checker Issues:</strong></td><td>").append(customCount).append("</td></tr>");
        html.append("<tr style='border-top: 1px solid #ccc;'><td><strong>Total:</strong></td><td><strong>")
                .append(axeCount + customCount).append("</strong></td></tr>");
        html.append("</table>");

        if (axeCount == 0 && customCount == 0) {
            html.append("<p style='color: green; margin: 10px 0 0 0;'>✓ Page passed all accessibility checks!</p>");
        }

        html.append("</div>");
        return html.toString();
    }

    // ==================== STATISTICS CLASS ====================

    /**
     * Thread-safe statistics tracking for accessibility reports.
     */
    public static class ReportStats {
        private final AtomicInteger axeViolations = new AtomicInteger(0);
        private final AtomicInteger customIssues = new AtomicInteger(0);
        private final AtomicInteger pagesScanned = new AtomicInteger(0);

        public int getAxeViolations() {
            return axeViolations.get();
        }

        public int getCustomIssues() {
            return customIssues.get();
        }

        public int getPagesScanned() {
            return pagesScanned.get();
        }

        public int getTotalIssues() {
            return axeViolations.get() + customIssues.get();
        }

        public void incrementPagesScanned() {
            pagesScanned.incrementAndGet();
        }

        public void addAxeViolations(int count) {
            axeViolations.addAndGet(count);
        }

        public void addCustomIssues(int count) {
            customIssues.addAndGet(count);
        }

        @Override
        public String toString() {
            return String.format("ReportStats[axeViolations=%d, customIssues=%d, pagesScanned=%d]",
                    axeViolations.get(), customIssues.get(), pagesScanned.get());
        }
    }
}
