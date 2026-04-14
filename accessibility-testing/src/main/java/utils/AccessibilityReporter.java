package utils;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
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

    // Memory guard — limits how many violations are rendered as full cards in the HTML report.
    // Console logging (logAxeViolations) is unaffected and always logs everything.
    private static final int MAX_VIOLATIONS_IN_REPORT = 10;

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
        attachCombinedReport(test, violations, null);
    }

    /**
     * Attaches a single Axe-core violation to the ExtentTest.
     */
    private static void attachSingleAxeViolation(ExtentTest test, AccessibilityViolation violation) {
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
    }

    /**
     * Attaches custom checker issues to an ExtentTest report.
     *
     * @param test   ExtentTest instance to attach issues to
     * @param issues list of custom checker issues
     */
    public static void attachCustomIssuesToReport(ExtentTest test, List<AccessibilityIssue> issues) {
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
        attachCombinedReport(test, null, issues);
    }

    /**
     * Attaches a single custom checker issue to the ExtentTest.
     */
    private static void attachSingleCustomIssue(ExtentTest test, AccessibilityIssue issue) {
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
    }

    /**
     * Attaches a combined accessibility report section to ExtentTest.
     * Features: Summary at top, separate sections for Axe-core and Custom checker,
     * collapsible severity groups within each section.
     *
     * @param test       ExtentTest instance
     * @param violations Axe-core violations (may be null)
     * @param issues     Custom checker issues (may be null)
     */
    public static void attachCombinedReport(ExtentTest test,
                                             List<AccessibilityViolation> violations,
                                             List<AccessibilityIssue> issues) {
        if (test == null) {
            log.warn("ExtentTest is null, skipping report attachment");
            return;
        }

        // Log summary and apply report cap before building any HTML
        int totalViolations = violations != null ? violations.size() : 0;
        log.info("Accessibility report: {} violations found, showing top {} in report, full list in console",
                totalViolations, Math.min(totalViolations, MAX_VIOLATIONS_IN_REPORT));

        // Truncate violations list for report rendering — console log is unchanged
        if (violations != null && violations.size() > MAX_VIOLATIONS_IN_REPORT) {
            int truncated = violations.size() - MAX_VIOLATIONS_IN_REPORT;
            log.info("Truncating report HTML to top {} violations ({} omitted — see console for full list)",
                    MAX_VIOLATIONS_IN_REPORT, truncated);
            violations = violations.subList(0, MAX_VIOLATIONS_IN_REPORT);
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
                    test.info(generateAxeCoreSection(violations));
                } catch (Exception e) {
                    log.warn("Failed to generate Axe-core section: {}", e.getMessage());
                    test.warning("Axe-core violations: " + violations.size());
                }
            }

            // 3. CUSTOM CHECKER ISSUES SECTION (separate category)
            if (!noIssues) {
                try {
                    test.info(generateCustomCheckerSection(issues));
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
    private static String generateAxeCoreSection(List<AccessibilityViolation> violations) {
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
            html.append(generateCollapsibleSection("🔴 CRITICAL", bySeverity.get("critical"), true));
        }
        // Serious
        if (bySeverity.containsKey("serious")) {
            html.append(generateCollapsibleSection("🟠 SERIOUS", bySeverity.get("serious"), true));
        }
        // Moderate
        if (bySeverity.containsKey("moderate")) {
            html.append(generateCollapsibleSection("🟡 MODERATE", bySeverity.get("moderate"), false));
        }
        // Minor
        if (bySeverity.containsKey("minor")) {
            html.append(generateCollapsibleSection("🟢 MINOR", bySeverity.get("minor"), false));
        }

        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Generate the Custom checker issues section with severity groupings.
     */
    private static String generateCustomCheckerSection(List<AccessibilityIssue> issues) {
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
            html.append(generateCollapsibleSection("🔴 CRITICAL", bySeverity.get("critical"), true));
        }
        // Moderate (custom checker uses moderate instead of serious)
        if (bySeverity.containsKey("moderate")) {
            html.append(generateCollapsibleSection("🟡 MODERATE", bySeverity.get("moderate"), false));
        }
        // Minor
        if (bySeverity.containsKey("minor")) {
            html.append(generateCollapsibleSection("🟢 MINOR", bySeverity.get("minor"), false));
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

        ViolationEntry(AccessibilityViolation v) {
            this.severity = v.getImpact() != null ? v.getImpact().toLowerCase() : "unknown";
            this.ruleName = v.getRuleId();
            this.description = v.getDescription();
            this.helpText = v.getHelp();
            this.helpUrl = v.getHelpUrl();
            this.selectors = v.getElementSelectors();
            this.tags = v.getTags();
            this.nodeCount = v.getNodeCount();
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
                                                      boolean expandedByDefault) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

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
            html.append(generateViolationCard(entry, i + 1));
        }

        html.append("</div>");
        html.append("</details>");

        return html.toString();
    }

    /**
     * Generate individual violation card with details and developer guidance.
     */
    private static String generateViolationCard(ViolationEntry entry, int index) {
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

            // Right column: Developer guidance (selectors, WCAG links, impact, effort, testing tip)
            html.append("<div style='flex: 0 0 280px; padding: 15px; background: #f9f9f9; ")
                .append("border-left: 1px solid #eee; font-size: 12px;'>");

            // All affected element selectors (up to 5)
            if (entry.selectors != null && !entry.selectors.isEmpty()) {
                int selectorCount = Math.min(entry.selectors.size(), 5);
                html.append("<p style='margin: 0 0 5px 0; color: #333;'><strong>🎯 Affected Elements (")
                    .append(entry.nodeCount).append(")</strong></p>");
                for (int s = 0; s < selectorCount; s++) {
                    String sel = entry.selectors.get(s);
                    if (sel != null) {
                        html.append("<code style='display: block; background: #efefef; padding: 2px 5px; ")
                            .append("border-radius: 3px; margin-bottom: 3px; word-break: break-all; font-size: 10px;'>")
                            .append(escapeHtml(truncateSelector(sel))).append("</code>");
                    }
                }
                if (entry.selectors.size() > 5) {
                    html.append("<span style='color: #888; font-style: italic; font-size: 11px;'>+ ")
                        .append(entry.selectors.size() - 5).append(" more</span>");
                }
            }

            // WCAG criteria links
            if (entry.tags != null) {
                List<String> wcagCriteria = entry.tags.stream()
                    .filter(t -> t != null && t.matches("wcag\\d{3,4}"))
                    .collect(Collectors.toList());
                if (!wcagCriteria.isEmpty()) {
                    html.append("<p style='margin: 10px 0 5px 0; color: #333;'><strong>📋 WCAG Criteria</strong></p>");
                    for (String criterion : wcagCriteria) {
                        html.append("<a href='").append(buildWcagUrl(criterion)).append("' target='_blank' ")
                            .append("style='display: block; color: #1565c0; margin-bottom: 3px; text-decoration: none;'>")
                            .append("SC ").append(formatWcagTag(criterion)).append(" ↗</a>");
                    }
                }
            }

            // User impact
            html.append("<p style='margin: 10px 0 5px 0; color: #333;'><strong>👥 User Impact</strong></p>");
            html.append("<p style='margin: 0 0 8px 0; color: #555; line-height: 1.4;'>")
                .append(escapeHtml(getUserImpact(entry.ruleName, entry.severity))).append("</p>");

            // Fix effort
            html.append("<p style='margin: 0 0 5px 0; color: #333;'><strong>🔧 Fix Effort</strong></p>");
            html.append("<span style='display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; ")
                .append("background: ").append(getFixEffortColor(entry.severity)).append("; color: white; margin-bottom: 8px;'>")
                .append(escapeHtml(getFixEffort(entry.severity))).append("</span>");

            // Manual testing tip
            html.append("<p style='margin: 8px 0 5px 0; color: #333;'><strong>🧪 How to Test</strong></p>");
            html.append("<p style='margin: 0; color: #555; line-height: 1.4; font-style: italic;'>")
                .append(escapeHtml(getManualTestTip(entry.ruleName))).append("</p>");

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

    /**
     * Truncates a CSS selector for display purposes.
     */
    private static String truncateSelector(String selector) {
        if (selector == null) return "";
        if (selector.length() <= 80) return selector;
        return selector.substring(0, 77) + "...";
    }

    /**
     * Returns a human-readable description of user impact for a given accessibility rule.
     */
    private static String getUserImpact(String ruleName, String severity) {
        if (ruleName == null) return "Users relying on assistive technology may be affected.";
        return switch (ruleName.toLowerCase()) {
            case "color-contrast", "color-contrast-enhanced" ->
                "Users with low vision or color blindness may be unable to read this text.";
            case "image-alt" ->
                "Screen reader users receive no information about this image.";
            case "label", "label-content-name-mismatch" ->
                "Screen reader users cannot identify this form field.";
            case "link-name" ->
                "Screen reader users cannot understand where this link leads.";
            case "button-name" ->
                "Screen reader users cannot determine the purpose of this button.";
            case "document-title" ->
                "Screen reader users cannot identify this page in browser history.";
            case "html-has-lang", "html-lang-valid" ->
                "Screen reader software cannot announce the correct language.";
            case "heading-order" ->
                "Screen reader users navigating by headings may miss content or be confused.";
            case "region" ->
                "Screen reader users navigating by landmark regions may miss this content.";
            case "list", "listitem", "definition-list", "dlitem" ->
                "Assistive technology cannot convey list structure to screen reader users.";
            case "tabindex" ->
                "Keyboard-only users may be unable to navigate the page in a logical order.";
            case "focus-order-semantics", "focus-visible" ->
                "Keyboard-only users cannot tell which element currently has focus.";
            case "aria-allowed-attr", "aria-required-attr", "aria-valid-attr", "aria-valid-attr-value" ->
                "Assistive technology receives incorrect or missing role/state information.";
            case "aria-hidden-body", "aria-hidden-focus" ->
                "Screen reader users may not be able to interact with this content.";
            case "scrollable-region-focusable" ->
                "Keyboard-only users cannot scroll this region.";
            case "keyboard", "frame-focusable-content" ->
                "Keyboard-only users cannot access this functionality.";
            default -> severity != null && (severity.equals("critical") || severity.equals("serious"))
                ? "Significant barrier for users relying on assistive technology."
                : "May cause difficulty for some users with disabilities.";
        };
    }

    /**
     * Returns an estimated fix effort label based on violation severity.
     */
    private static String getFixEffort(String severity) {
        if (severity == null) return "Unknown";
        return switch (severity.toLowerCase()) {
            case "critical" -> "Significant — blocking issue, fix immediately";
            case "serious"  -> "Significant — high priority, fix in current sprint";
            case "moderate" -> "Moderate — schedule for upcoming sprint";
            case "minor"    -> "Minor — schedule for future sprint";
            default         -> "Unknown";
        };
    }

    /**
     * Returns a badge background color for the fix effort indicator.
     */
    private static String getFixEffortColor(String severity) {
        if (severity == null) return "#9e9e9e";
        return switch (severity.toLowerCase()) {
            case "critical" -> "#c62828";
            case "serious"  -> "#ef6c00";
            case "moderate" -> "#f9a825";
            case "minor"    -> "#2e7d32";
            default         -> "#9e9e9e";
        };
    }

    /**
     * Returns a manual testing tip tailored to the given accessibility rule.
     */
    private static String getManualTestTip(String ruleName) {
        if (ruleName == null) return "Use the axe DevTools browser extension to inspect this element.";
        return switch (ruleName.toLowerCase()) {
            case "color-contrast", "color-contrast-enhanced" ->
                "Use Chrome DevTools > Accessibility panel or the Colour Contrast Analyser tool.";
            case "keyboard", "focus-order-semantics", "scrollable-region-focusable", "frame-focusable-content" ->
                "Tab through the page using only the keyboard and verify all interactive elements are reachable.";
            case "focus-visible" ->
                "Navigate using Tab key and confirm a visible focus indicator appears on every element.";
            case "image-alt" ->
                "Inspect the element in Chrome DevTools > Accessibility panel; verify alt text is descriptive.";
            case "document-title" ->
                "Check the <title> tag in page source; it should uniquely describe the page content.";
            case "html-has-lang", "html-lang-valid" ->
                "Check the lang attribute on the <html> element in page source (e.g. lang='en').";
            case "heading-order" ->
                "Use the axe DevTools extension or WAVE tool to visualize the heading structure.";
            case "label", "label-content-name-mismatch" ->
                "Navigate to the form field using Tab; verify your screen reader announces a meaningful label.";
            case "link-name" ->
                "Enable NVDA or VoiceOver and tab to this link — it should read a meaningful destination.";
            default ->
                "Use the axe DevTools browser extension or NVDA/VoiceOver with Chrome to verify.";
        };
    }

    /**
     * Formats a WCAG tag (e.g. "wcag143") as a success criterion number (e.g. "1.4.3").
     */
    private static String formatWcagTag(String tag) {
        if (tag == null || !tag.startsWith("wcag")) return tag != null ? tag : "";
        String digits = tag.substring(4);
        if (digits.length() == 3) {
            return digits.charAt(0) + "." + digits.charAt(1) + "." + digits.charAt(2);
        } else if (digits.length() == 4) {
            return digits.charAt(0) + "." + digits.charAt(1) + "." + digits.substring(2);
        }
        return digits;
    }

    /**
     * Builds a WCAG 2.1 quickref URL for the given tag (e.g. "wcag143" → SC 1.4.3 filter).
     */
    private static String buildWcagUrl(String tag) {
        if (tag == null || !tag.startsWith("wcag")) return "https://www.w3.org/WAI/WCAG21/quickref/";
        String digits = tag.substring(4);
        return "https://www.w3.org/WAI/WCAG21/quickref/?versions=2.1&showtechniques=" + digits;
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
