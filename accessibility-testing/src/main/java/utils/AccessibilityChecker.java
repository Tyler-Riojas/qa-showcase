package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom accessibility checker for common accessibility traps.
 *
 * <p>Performs accessibility checks without external dependencies using Selenium.
 * Use this for quick, lightweight checks or in combination with {@link AccessibilityUtils}
 * for comprehensive testing.</p>
 *
 * <h2>Checks Performed:</h2>
 * <ul>
 *   <li><b>Images:</b> Missing or empty alt attributes</li>
 *   <li><b>Form Labels:</b> Inputs without associated labels</li>
 *   <li><b>Headings:</b> Broken hierarchy (e.g., h1 → h3 skipping h2)</li>
 *   <li><b>ARIA Labels:</b> Interactive elements without accessible names</li>
 *   <li><b>Color Contrast:</b> Basic contrast ratio estimation</li>
 *   <li><b>Keyboard Navigation:</b> Tab order and focusable elements</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Run all checks
 * List<AccessibilityIssue> issues = AccessibilityChecker.checkAllTraps(driver);
 * issues.forEach(issue -> log.warn("{}", issue));
 *
 * // Run specific check
 * List<AccessibilityIssue> imageIssues = AccessibilityChecker.checkImages(driver);
 *
 * // Assert no critical issues
 * List<AccessibilityIssue> critical = issues.stream()
 *     .filter(i -> i.getSeverity() == Severity.CRITICAL)
 *     .toList();
 * assertTrue(critical.isEmpty(), "Critical a11y issues: " + critical);
 * }</pre>
 *
 * @see AccessibilityUtils For Axe-core based comprehensive testing
 */
public final class AccessibilityChecker {

    private static final Logger log = LoggerFactory.getLogger(AccessibilityChecker.class);

    private AccessibilityChecker() {
        // Prevent instantiation - utility class
    }

    // ==================== Main Check Methods ====================

    /**
     * Runs all accessibility checks on the current page.
     *
     * @param driver WebDriver instance
     * @return list of all accessibility issues found
     */
    public static List<AccessibilityIssue> checkAllTraps(WebDriver driver) {
        log.info("Running all accessibility checks on: {}", driver.getCurrentUrl());

        List<AccessibilityIssue> allIssues = new ArrayList<>();

        allIssues.addAll(checkImages(driver));
        allIssues.addAll(checkFormLabels(driver));
        allIssues.addAll(checkHeadings(driver));
        allIssues.addAll(checkAriaLabels(driver));
        allIssues.addAll(checkColorContrast(driver));
        allIssues.addAll(checkKeyboardNavigation(driver));

        log.info("Found {} accessibility issue(s)", allIssues.size());
        return allIssues;
    }

    // ==================== Image Checks ====================

    /**
     * Checks for images without alt text or with empty alt attributes.
     *
     * <p>WCAG 1.1.1 - Non-text Content (Level A)</p>
     *
     * @param driver WebDriver instance
     * @return list of image accessibility issues
     */
    public static List<AccessibilityIssue> checkImages(WebDriver driver) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        List<WebElement> images = driver.findElements(By.tagName("img"));

        for (WebElement img : images) {
            String alt = img.getAttribute("alt");
            String src = img.getAttribute("src");
            String selector = getElementSelector(img);

            // Check for missing alt attribute
            if (alt == null) {
                issues.add(new AccessibilityIssue(
                        IssueType.MISSING_ALT_TEXT,
                        Severity.CRITICAL,
                        selector,
                        "Image missing alt attribute",
                        "Add alt attribute: <img alt=\"Description of image\" src=\"" +
                                truncate(src, 50) + "\">"
                ));
            }
            // Check for empty alt on non-decorative images
            else if (alt.trim().isEmpty()) {
                // Empty alt is valid for decorative images, but flag for review
                issues.add(new AccessibilityIssue(
                        IssueType.EMPTY_ALT_TEXT,
                        Severity.MODERATE,
                        selector,
                        "Image has empty alt text (valid only for decorative images)",
                        "If image conveys meaning, add descriptive alt text. " +
                                "If decorative, add role=\"presentation\""
                ));
            }
            // Check for placeholder alt text
            else if (isPlaceholderAlt(alt)) {
                issues.add(new AccessibilityIssue(
                        IssueType.PLACEHOLDER_ALT_TEXT,
                        Severity.MODERATE,
                        selector,
                        "Image has placeholder alt text: \"" + alt + "\"",
                        "Replace with meaningful description of image content"
                ));
            }
        }

        log.debug("Image check: found {} issues in {} images", issues.size(), images.size());
        return issues;
    }

    // ==================== Form Label Checks ====================

    /**
     * Checks for form inputs without associated labels.
     *
     * <p>WCAG 1.3.1 - Info and Relationships (Level A)</p>
     * <p>WCAG 3.3.2 - Labels or Instructions (Level A)</p>
     *
     * @param driver WebDriver instance
     * @return list of form label issues
     */
    public static List<AccessibilityIssue> checkFormLabels(WebDriver driver) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // Check input, select, and textarea elements
        List<WebElement> formElements = new ArrayList<>();
        formElements.addAll(driver.findElements(By.tagName("input")));
        formElements.addAll(driver.findElements(By.tagName("select")));
        formElements.addAll(driver.findElements(By.tagName("textarea")));

        for (WebElement element : formElements) {
            String type = element.getAttribute("type");
            String id = element.getAttribute("id");
            String ariaLabel = element.getAttribute("aria-label");
            String ariaLabelledBy = element.getAttribute("aria-labelledby");
            String placeholder = element.getAttribute("placeholder");
            String title = element.getAttribute("title");

            // Skip hidden, submit, button, image, and reset inputs
            if (type != null && (type.equals("hidden") || type.equals("submit") ||
                    type.equals("button") || type.equals("image") || type.equals("reset"))) {
                continue;
            }

            String selector = getElementSelector(element);
            boolean hasLabel = false;

            // Check for associated label via 'for' attribute
            if (id != null && !id.isEmpty()) {
                List<WebElement> labels = driver.findElements(By.cssSelector("label[for='" + id + "']"));
                if (!labels.isEmpty()) {
                    hasLabel = true;
                }
            }

            // Check for wrapping label
            if (!hasLabel) {
                try {
                    WebElement parent = element.findElement(By.xpath("./ancestor::label"));
                    if (parent != null) {
                        hasLabel = true;
                    }
                } catch (Exception ignored) {
                    // No wrapping label
                }
            }

            // Check for aria-label or aria-labelledby
            if (!hasLabel && (ariaLabel != null && !ariaLabel.isEmpty()) ||
                    (ariaLabelledBy != null && !ariaLabelledBy.isEmpty())) {
                hasLabel = true;
            }

            // Check for title attribute (less accessible but valid)
            if (!hasLabel && title != null && !title.isEmpty()) {
                hasLabel = true;
                issues.add(new AccessibilityIssue(
                        IssueType.TITLE_ONLY_LABEL,
                        Severity.MINOR,
                        selector,
                        "Form element uses title attribute instead of label",
                        "Consider using a visible <label> element for better accessibility"
                ));
            }

            if (!hasLabel) {
                // Placeholder alone is not sufficient
                String message = placeholder != null && !placeholder.isEmpty()
                        ? "Form element has only placeholder (not accessible)"
                        : "Form element missing label";

                issues.add(new AccessibilityIssue(
                        IssueType.MISSING_FORM_LABEL,
                        Severity.CRITICAL,
                        selector,
                        message,
                        "Add <label for=\"" + (id != null ? id : "elementId") + "\">Label text</label>"
                ));
            }
        }

        log.debug("Form label check: found {} issues", issues.size());
        return issues;
    }

    // ==================== Heading Hierarchy Checks ====================

    /**
     * Checks for broken heading hierarchy (e.g., h1 → h3 skipping h2).
     *
     * <p>WCAG 1.3.1 - Info and Relationships (Level A)</p>
     *
     * @param driver WebDriver instance
     * @return list of heading hierarchy issues
     */
    public static List<AccessibilityIssue> checkHeadings(WebDriver driver) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // Find all headings
        List<WebElement> headings = driver.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));

        if (headings.isEmpty()) {
            issues.add(new AccessibilityIssue(
                    IssueType.NO_HEADINGS,
                    Severity.MODERATE,
                    "page",
                    "Page has no heading elements",
                    "Add at least one <h1> element to identify the main content"
            ));
            return issues;
        }

        // Check for h1 presence
        List<WebElement> h1s = driver.findElements(By.tagName("h1"));
        if (h1s.isEmpty()) {
            issues.add(new AccessibilityIssue(
                    IssueType.MISSING_H1,
                    Severity.MODERATE,
                    "page",
                    "Page is missing an <h1> heading",
                    "Add an <h1> element for the main page title"
            ));
        } else if (h1s.size() > 1) {
            issues.add(new AccessibilityIssue(
                    IssueType.MULTIPLE_H1,
                    Severity.MINOR,
                    "page",
                    "Page has " + h1s.size() + " <h1> elements (typically only one recommended)",
                    "Use a single <h1> for the main page title, use <h2>-<h6> for subsections"
            ));
        }

        // Check heading hierarchy
        int lastLevel = 0;
        for (WebElement heading : headings) {
            String tagName = heading.getTagName().toLowerCase();
            int currentLevel = Integer.parseInt(tagName.substring(1));
            String selector = getElementSelector(heading);
            String text = truncate(heading.getText(), 50);

            // Check for skipped levels
            if (lastLevel > 0 && currentLevel > lastLevel + 1) {
                issues.add(new AccessibilityIssue(
                        IssueType.SKIPPED_HEADING_LEVEL,
                        Severity.MODERATE,
                        selector,
                        "Heading level skipped: h" + lastLevel + " → h" + currentLevel +
                                " (\"" + text + "\")",
                        "Use h" + (lastLevel + 1) + " instead of h" + currentLevel
                ));
            }

            // Check for empty headings
            if (heading.getText().trim().isEmpty()) {
                issues.add(new AccessibilityIssue(
                        IssueType.EMPTY_HEADING,
                        Severity.CRITICAL,
                        selector,
                        "Empty heading element <" + tagName + ">",
                        "Add text content or remove the heading element"
                ));
            }

            lastLevel = currentLevel;
        }

        log.debug("Heading check: found {} issues in {} headings", issues.size(), headings.size());
        return issues;
    }

    // ==================== ARIA Label Checks ====================

    /**
     * Checks for interactive elements without accessible names.
     *
     * <p>WCAG 4.1.2 - Name, Role, Value (Level A)</p>
     *
     * @param driver WebDriver instance
     * @return list of ARIA label issues
     */
    public static List<AccessibilityIssue> checkAriaLabels(WebDriver driver) {
        List<AccessibilityIssue> issues = new ArrayList<>();

        // Check buttons
        List<WebElement> buttons = driver.findElements(By.tagName("button"));
        for (WebElement button : buttons) {
            if (!hasAccessibleName(button)) {
                issues.add(new AccessibilityIssue(
                        IssueType.MISSING_ARIA_LABEL,
                        Severity.CRITICAL,
                        getElementSelector(button),
                        "Button has no accessible name",
                        "Add text content, aria-label, or aria-labelledby"
                ));
            }
        }

        // Check links
        List<WebElement> links = driver.findElements(By.tagName("a"));
        for (WebElement link : links) {
            String href = link.getAttribute("href");
            if (href != null && !href.isEmpty() && !hasAccessibleName(link)) {
                issues.add(new AccessibilityIssue(
                        IssueType.MISSING_LINK_TEXT,
                        Severity.CRITICAL,
                        getElementSelector(link),
                        "Link has no accessible name",
                        "Add link text or aria-label"
                ));
            }
        }

        // Check elements with click handlers (role=button without label)
        List<WebElement> clickables = driver.findElements(
                By.cssSelector("[role='button'], [onclick], [tabindex='0']"));
        for (WebElement element : clickables) {
            String tagName = element.getTagName().toLowerCase();
            if (!tagName.equals("button") && !tagName.equals("a") && !hasAccessibleName(element)) {
                issues.add(new AccessibilityIssue(
                        IssueType.MISSING_ARIA_LABEL,
                        Severity.CRITICAL,
                        getElementSelector(element),
                        "Interactive element has no accessible name",
                        "Add aria-label or aria-labelledby attribute"
                ));
            }
        }

        // Check icon-only buttons (buttons containing only icons)
        List<WebElement> iconButtons = driver.findElements(
                By.cssSelector("button > svg, button > i, button > span[class*='icon']"));
        for (WebElement icon : iconButtons) {
            try {
                WebElement button = icon.findElement(By.xpath("./parent::button"));
                String text = button.getText().trim();
                String ariaLabel = button.getAttribute("aria-label");

                if (text.isEmpty() && (ariaLabel == null || ariaLabel.isEmpty())) {
                    issues.add(new AccessibilityIssue(
                            IssueType.ICON_ONLY_BUTTON,
                            Severity.CRITICAL,
                            getElementSelector(button),
                            "Icon-only button has no accessible name",
                            "Add aria-label describing the button's action"
                    ));
                }
            } catch (Exception ignored) {
                // Not a direct child of button
            }
        }

        log.debug("ARIA label check: found {} issues", issues.size());
        return issues;
    }

    // ==================== Color Contrast Checks ====================

    /**
     * Performs basic color contrast estimation for text elements.
     *
     * <p>WCAG 1.4.3 - Contrast (Minimum) (Level AA)</p>
     *
     * <p>Note: This is a basic check. For comprehensive contrast testing,
     * use Axe-core via {@link AccessibilityUtils}.</p>
     *
     * @param driver WebDriver instance
     * @return list of potential contrast issues
     */
    public static List<AccessibilityIssue> checkColorContrast(WebDriver driver) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Check common text elements
        List<WebElement> textElements = driver.findElements(
                By.cssSelector("p, span, div, li, td, th, label, a"));

        // Limit check to visible elements with text
        int checked = 0;
        int maxChecks = 50; // Limit for performance

        for (WebElement element : textElements) {
            if (checked >= maxChecks) break;

            try {
                String text = element.getText().trim();
                if (text.isEmpty() || !element.isDisplayed()) continue;

                // Get computed styles
                String color = (String) js.executeScript(
                        "return window.getComputedStyle(arguments[0]).color", element);
                String bgColor = (String) js.executeScript(
                        "return window.getComputedStyle(arguments[0]).backgroundColor", element);
                String fontSize = (String) js.executeScript(
                        "return window.getComputedStyle(arguments[0]).fontSize", element);

                // Parse colors and calculate contrast
                int[] fg = parseRgbColor(color);
                int[] bg = parseRgbColor(bgColor);

                if (fg != null && bg != null) {
                    double ratio = calculateContrastRatio(fg, bg);
                    double fontSizePx = parseFontSize(fontSize);
                    boolean isLargeText = fontSizePx >= 18 || (fontSizePx >= 14 && isBold(element, js));

                    // WCAG AA: 4.5:1 for normal text, 3:1 for large text
                    double requiredRatio = isLargeText ? 3.0 : 4.5;

                    if (ratio < requiredRatio && ratio > 1.0) { // Ignore 1:1 (likely transparent bg)
                        issues.add(new AccessibilityIssue(
                                IssueType.LOW_COLOR_CONTRAST,
                                Severity.MODERATE,
                                getElementSelector(element),
                                String.format("Low color contrast ratio: %.2f:1 (requires %.1f:1)",
                                        ratio, requiredRatio),
                                "Increase contrast between text color (" + color +
                                        ") and background (" + bgColor + ")"
                        ));
                    }
                }

                checked++;
            } catch (Exception ignored) {
                // Skip elements that can't be analyzed
            }
        }

        log.debug("Color contrast check: analyzed {} elements, found {} issues", checked, issues.size());
        return issues;
    }

    // ==================== Keyboard Navigation Checks ====================

    /**
     * Checks for keyboard navigation issues.
     *
     * <p>WCAG 2.1.1 - Keyboard (Level A)</p>
     * <p>WCAG 2.4.3 - Focus Order (Level A)</p>
     *
     * @param driver WebDriver instance
     * @return list of keyboard navigation issues
     */
    public static List<AccessibilityIssue> checkKeyboardNavigation(WebDriver driver) {
        List<AccessibilityIssue> issues = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Check for positive tabindex (disrupts natural tab order)
        List<WebElement> positiveTabindex = driver.findElements(
                By.cssSelector("[tabindex]:not([tabindex='0']):not([tabindex='-1'])"));

        for (WebElement element : positiveTabindex) {
            String tabindex = element.getAttribute("tabindex");
            try {
                int value = Integer.parseInt(tabindex);
                if (value > 0) {
                    issues.add(new AccessibilityIssue(
                            IssueType.POSITIVE_TABINDEX,
                            Severity.MODERATE,
                            getElementSelector(element),
                            "Element has positive tabindex=" + value + " (disrupts tab order)",
                            "Use tabindex=\"0\" for natural tab order or tabindex=\"-1\" for programmatic focus"
                    ));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for keyboard traps (elements that might trap focus)
        List<WebElement> modals = driver.findElements(
                By.cssSelector("[role='dialog'], [role='alertdialog'], .modal, [class*='modal']"));

        for (WebElement modal : modals) {
            if (modal.isDisplayed()) {
                // Check if modal has focusable elements
                List<WebElement> focusable = modal.findElements(
                        By.cssSelector("button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])"));

                if (focusable.isEmpty()) {
                    issues.add(new AccessibilityIssue(
                            IssueType.NO_FOCUSABLE_ELEMENTS,
                            Severity.CRITICAL,
                            getElementSelector(modal),
                            "Modal/dialog has no focusable elements",
                            "Ensure modal has at least one focusable element (e.g., close button)"
                    ));
                }
            }
        }

        // Check for elements with click handlers but not keyboard accessible
        List<WebElement> clickOnly = driver.findElements(
                By.cssSelector("[onclick]:not(button):not(a):not([tabindex])"));

        for (WebElement element : clickOnly) {
            String tagName = element.getTagName().toLowerCase();
            if (!tagName.equals("button") && !tagName.equals("a") &&
                    !tagName.equals("input") && !tagName.equals("select")) {
                issues.add(new AccessibilityIssue(
                        IssueType.NOT_KEYBOARD_ACCESSIBLE,
                        Severity.CRITICAL,
                        getElementSelector(element),
                        "Element has onclick but is not keyboard accessible",
                        "Add tabindex=\"0\" and keydown/keypress handler, or use <button>"
                ));
            }
        }

        // Check focus visibility
        String outlineStyle = (String) js.executeScript(
                "return document.body.style.outline || window.getComputedStyle(document.body).outline");

        if (outlineStyle != null && outlineStyle.contains("none")) {
            issues.add(new AccessibilityIssue(
                    IssueType.FOCUS_NOT_VISIBLE,
                    Severity.MODERATE,
                    "body",
                    "CSS may hide focus indicators (outline: none)",
                    "Ensure :focus styles are visible for keyboard users"
            ));
        }

        log.debug("Keyboard navigation check: found {} issues", issues.size());
        return issues;
    }

    // ==================== Helper Methods ====================

    /**
     * Checks if an element has an accessible name.
     */
    private static boolean hasAccessibleName(WebElement element) {
        // Check text content
        String text = element.getText().trim();
        if (!text.isEmpty()) return true;

        // Check aria-label
        String ariaLabel = element.getAttribute("aria-label");
        if (ariaLabel != null && !ariaLabel.trim().isEmpty()) return true;

        // Check aria-labelledby
        String ariaLabelledBy = element.getAttribute("aria-labelledby");
        if (ariaLabelledBy != null && !ariaLabelledBy.trim().isEmpty()) return true;

        // Check title
        String title = element.getAttribute("title");
        if (title != null && !title.trim().isEmpty()) return true;

        // Check for img with alt inside
        List<WebElement> imgs = element.findElements(By.tagName("img"));
        for (WebElement img : imgs) {
            String alt = img.getAttribute("alt");
            if (alt != null && !alt.trim().isEmpty()) return true;
        }

        return false;
    }

    /**
     * Generates a CSS selector for an element.
     */
    private static String getElementSelector(WebElement element) {
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            return "#" + id;
        }

        String className = element.getAttribute("class");
        String tagName = element.getTagName().toLowerCase();

        if (className != null && !className.isEmpty()) {
            String firstClass = className.split("\\s+")[0];
            return tagName + "." + firstClass;
        }

        return tagName;
    }

    /**
     * Checks if alt text is a placeholder.
     */
    private static boolean isPlaceholderAlt(String alt) {
        if (alt == null) return false;
        String lower = alt.toLowerCase().trim();
        return lower.equals("image") || lower.equals("photo") || lower.equals("picture") ||
                lower.equals("img") || lower.equals("icon") || lower.matches("image\\d*") ||
                lower.matches("img\\d*") || lower.equals("untitled") || lower.equals("placeholder");
    }

    /**
     * Parses RGB color string to int array.
     */
    private static int[] parseRgbColor(String color) {
        if (color == null) return null;

        Pattern pattern = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)");
        Matcher matcher = pattern.matcher(color);

        if (matcher.find()) {
            return new int[]{
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            };
        }
        return null;
    }

    /**
     * Calculates contrast ratio between two colors.
     */
    private static double calculateContrastRatio(int[] fg, int[] bg) {
        double fgLuminance = getRelativeLuminance(fg);
        double bgLuminance = getRelativeLuminance(bg);

        double lighter = Math.max(fgLuminance, bgLuminance);
        double darker = Math.min(fgLuminance, bgLuminance);

        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Calculates relative luminance of a color.
     */
    private static double getRelativeLuminance(int[] rgb) {
        double[] normalized = new double[3];
        for (int i = 0; i < 3; i++) {
            double val = rgb[i] / 255.0;
            normalized[i] = val <= 0.03928 ? val / 12.92 : Math.pow((val + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * normalized[0] + 0.7152 * normalized[1] + 0.0722 * normalized[2];
    }

    /**
     * Parses font size from CSS value.
     */
    private static double parseFontSize(String fontSize) {
        if (fontSize == null) return 16.0;
        try {
            return Double.parseDouble(fontSize.replace("px", "").trim());
        } catch (NumberFormatException e) {
            return 16.0;
        }
    }

    /**
     * Checks if element has bold font weight.
     */
    private static boolean isBold(WebElement element, JavascriptExecutor js) {
        try {
            String fontWeight = (String) js.executeScript(
                    "return window.getComputedStyle(arguments[0]).fontWeight", element);
            int weight = Integer.parseInt(fontWeight);
            return weight >= 700;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Truncates string to specified length.
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    // ==================== Data Classes ====================

    /**
     * Types of accessibility issues that can be detected.
     */
    public enum IssueType {
        // Image issues
        MISSING_ALT_TEXT,
        EMPTY_ALT_TEXT,
        PLACEHOLDER_ALT_TEXT,

        // Form issues
        MISSING_FORM_LABEL,
        TITLE_ONLY_LABEL,

        // Heading issues
        NO_HEADINGS,
        MISSING_H1,
        MULTIPLE_H1,
        SKIPPED_HEADING_LEVEL,
        EMPTY_HEADING,

        // ARIA issues
        MISSING_ARIA_LABEL,
        MISSING_LINK_TEXT,
        ICON_ONLY_BUTTON,

        // Color issues
        LOW_COLOR_CONTRAST,

        // Keyboard issues
        POSITIVE_TABINDEX,
        NO_FOCUSABLE_ELEMENTS,
        NOT_KEYBOARD_ACCESSIBLE,
        FOCUS_NOT_VISIBLE
    }

    /**
     * Severity levels for accessibility issues.
     *
     * <p>Severity scale:</p>
     * <ul>
     *   <li><b>CRITICAL</b> – Completely blocks access for users with disabilities.
     *       Examples: missing form labels (screen reader cannot identify fields),
     *       keyboard traps (keyboard-only users cannot navigate past the element),
     *       missing image alt text on functional images (e.g. linked images with no label).</li>
     *   <li><b>MODERATE</b> – Significant barrier but a workaround exists.
     *       Examples: low color contrast (users can zoom or use OS high-contrast mode),
     *       decorative images missing empty alt="" (screen reader reads filename noise),
     *       missing heading hierarchy (impairs navigation but content is still reachable).</li>
     *   <li><b>MINOR</b> – Minor inconvenience; best practice to fix but low user impact.
     *       Examples: redundant title attributes, minor landmark region issues,
     *       non-critical ARIA attribute warnings.</li>
     * </ul>
     *
     * <p>Color contrast (WCAG 1.4.3 Level AA) is classified as MODERATE because
     * users can compensate via OS accessibility settings, browser zoom, or extensions.
     * It is never a complete blocker.</p>
     */
    public enum Severity {
        /** Must be fixed - completely blocks access for users with disabilities */
        CRITICAL,
        /** Should be fixed - significant barrier but a workaround exists */
        MODERATE,
        /** Nice to fix - minor inconvenience, low user impact */
        MINOR
    }

    /**
     * Represents an accessibility issue found during checking.
     */
    public static class AccessibilityIssue {
        private final IssueType type;
        private final Severity severity;
        private final String elementSelector;
        private final String description;
        private final String recommendation;

        public AccessibilityIssue(IssueType type, Severity severity, String elementSelector,
                                   String description, String recommendation) {
            this.type = type;
            this.severity = severity;
            this.elementSelector = elementSelector;
            this.description = description;
            this.recommendation = recommendation;
        }

        public IssueType getType() { return type; }
        public Severity getSeverity() { return severity; }
        public String getElementSelector() { return elementSelector; }
        public String getDescription() { return description; }
        public String getRecommendation() { return recommendation; }

        @Override
        public String toString() {
            return String.format("[%s] %s @ %s: %s",
                    severity, type, elementSelector, description);
        }
    }
}
