package listeners;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;

import base.BaseTestTestNG;
import utils.ExtentReportManager;
import utils.ScreenshotUtils;

/**
 * TestNG Listener for Extent Reports integration.
 * Captures test results and generates rich HTML reports.
 *
 * <p>Implements both {@link ITestListener} and {@link ISuiteListener} for proper
 * handling of parallel execution scenarios.</p>
 *
 * <p><b>Thread Safety:</b> Uses atomic counters to track test contexts and ensure
 * reports are flushed only once when all tests complete.</p>
 *
 * <p><b>Dynamic Report Naming:</b> Supports -Dtest and -Ddevice system properties
 * for meaningful report names when running individual tests.</p>
 */
public class ExtentTestNGListener implements ITestListener, ISuiteListener {

    static final Logger log = getLogger(lookup().lookupClass());

    // Track initialization and active test contexts for thread safety
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicInteger activeContexts = new AtomicInteger(0);

    // ==================== SUITE LISTENER ====================

    @Override
    public void onStart(ISuite suite) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║ SUITE STARTED: {}", suite.getName());
        log.info("╚══════════════════════════════════════════════════════════╝");

        // Initialize ExtentReports once per suite
        if (initialized.compareAndSet(false, true)) {
            String reportName = suite.getParameter("reportName");
            String reportTitle = suite.getParameter("reportTitle");
            String suiteName = suite.getName();
            String testProperty = System.getProperty("test");
            boolean hasTestProperty = testProperty != null && !testProperty.isEmpty()
                    && !testProperty.startsWith("${");

            if (hasTestProperty) {
                // -Dtest overrides XML param — name the report after the specific test being run
                ExtentReportManager.setReportName(buildReportNameFromProperties(suiteName));
                ExtentReportManager.setReportTitle(buildReportTitleFromProperties(suiteName));
            } else if (reportName != null && !reportName.isEmpty()) {
                ExtentReportManager.setReportName(reportName);
                if (reportTitle != null && !reportTitle.isEmpty()) {
                    ExtentReportManager.setReportTitle(reportTitle);
                } else {
                    ExtentReportManager.setReportTitle(buildReportTitleFromProperties(suiteName));
                }
            } else {
                // Use smart naming based on -Ddevice properties
                ExtentReportManager.setReportName(buildReportNameFromProperties(suiteName));
                ExtentReportManager.setReportTitle(buildReportTitleFromProperties(suiteName));
            }

            // Set device info if specified
            String deviceProperty = System.getProperty("device");
            if (deviceProperty != null && !deviceProperty.isEmpty()) {
                ExtentReportManager.getInstance();
                ExtentReportManager.setDeviceInfo(deviceProperty);
            } else {
                ExtentReportManager.getInstance();
            }

            log.info("ExtentReports initialized: {}", ExtentReportManager.getReportName());
        }
    }

    /**
     * Build report name from system properties (-Dtest, -Ddevice).
     * Used when no explicit reportName parameter is set.
     */
    private String buildReportNameFromProperties(String suiteName) {
        StringBuilder name = new StringBuilder();

        String testProperty = System.getProperty("test");
        String deviceProperty = System.getProperty("device");

        if (testProperty != null && !testProperty.isEmpty()) {
            // Extract page name from test class (e.g., "HomePageAccessibilityTest" -> "HomePage")
            String pageName = extractPageName(testProperty);
            name.append(pageName);
        } else if (isValidSuiteName(suiteName)) {
            name.append(suiteName.replace(" ", "_"));
        } else {
            name.append("Test");
        }

        // Add device if specified
        if (deviceProperty != null && !deviceProperty.isEmpty()) {
            name.append("_").append(deviceProperty.toUpperCase());
        }

        name.append("_Report");
        return name.toString();
    }

    /**
     * Build report title from system properties (-Dtest, -Ddevice).
     */
    private String buildReportTitleFromProperties(String suiteName) {
        StringBuilder title = new StringBuilder();

        String testProperty = System.getProperty("test");
        String deviceProperty = System.getProperty("device");

        if (testProperty != null && !testProperty.isEmpty()) {
            String pageName = extractPageName(testProperty);
            title.append(pageName).append(" ");
        } else if (isValidSuiteName(suiteName)) {
            title.append(suiteName).append(" ");
        }

        if (deviceProperty != null && !deviceProperty.isEmpty()) {
            title.append("(").append(deviceProperty).append(") ");
        }

        title.append("Test Report");
        return title.toString();
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║ SUITE FINISHED: {}", suite.getName());
        log.info("╚══════════════════════════════════════════════════════════╝");

        // Flush reports when suite completes
        ExtentReportManager.flushReports();
        log.info("Report saved to: {}", ExtentReportManager.getReportPath());

        // Reset for potential next suite run
        initialized.set(false);
    }

    // ==================== TEST CONTEXT LISTENER ====================

    @Override
    public void onStart(ITestContext context) {
        int count = activeContexts.incrementAndGet();
        log.info("========================================");
        log.info("Test Context Started: {} (active contexts: {})", context.getName(), count);
        log.info("========================================");

        // Initialize if not already done (fallback for -Dgroups execution without suite)
        if (initialized.compareAndSet(false, true)) {
            String suiteName = context.getSuite().getName();
            String reportName = context.getSuite().getParameter("reportName");
            String reportTitle = context.getSuite().getParameter("reportTitle");
            String testProperty = System.getProperty("test");
            boolean hasTestProperty = testProperty != null && !testProperty.isEmpty()
                    && !testProperty.startsWith("${");

            if (hasTestProperty) {
                // -Dtest overrides XML param — name the report after the specific test being run
                ExtentReportManager.setReportName(buildReportName(context, suiteName));
                ExtentReportManager.setReportTitle(buildReportTitle(context, suiteName));
            } else if (reportName != null && !reportName.isEmpty()) {
                ExtentReportManager.setReportName(reportName);
                if (reportTitle != null && !reportTitle.isEmpty()) {
                    ExtentReportManager.setReportTitle(reportTitle);
                } else {
                    ExtentReportManager.setReportTitle(buildReportTitle(context, suiteName));
                }
            } else {
                // Build meaningful report name from test class and device
                ExtentReportManager.setReportName(buildReportName(context, suiteName));
                ExtentReportManager.setReportTitle(buildReportTitle(context, suiteName));
            }

            ExtentReportManager.getInstance();

            // Set device info in system info if specified
            String deviceProperty = System.getProperty("device");
            if (deviceProperty != null && !deviceProperty.isEmpty()) {
                ExtentReportManager.setDeviceInfo(deviceProperty);
            }

            log.info("ExtentReports initialized (via context): {}", ExtentReportManager.getReportName());
        }
    }

    /**
     * Build a meaningful report name from context, system properties, and test classes.
     * Priority: -Dtest class name > context test methods > suite name (if meaningful) > default
     */
    private String buildReportName(ITestContext context, String suiteName) {
        StringBuilder name = new StringBuilder();

        String testProperty = System.getProperty("test");
        String deviceProperty = System.getProperty("device");

        if (testProperty != null && !testProperty.isEmpty()) {
            String pageName = extractPageName(testProperty);
            name.append(pageName);
        } else {
            var testMethods = context.getAllTestMethods();
            if (testMethods != null && testMethods.length > 0) {
                String className = testMethods[0].getTestClass().getRealClass().getSimpleName();
                String pageName = extractPageName(className);
                name.append(pageName);
            } else if (isValidSuiteName(suiteName)) {
                name.append(suiteName.replace(" ", "_"));
            } else {
                name.append("Test");
            }
        }

        if (deviceProperty != null && !deviceProperty.isEmpty()) {
            name.append("_").append(deviceProperty.toUpperCase());
        }

        name.append("_Report");
        return name.toString();
    }

    /**
     * Check if suite name is a meaningful custom name (not a default/auto-generated one).
     */
    private boolean isValidSuiteName(String suiteName) {
        if (suiteName == null || suiteName.isEmpty()) {
            return false;
        }
        String lowerName = suiteName.toLowerCase();
        return !lowerName.contains("default") &&
               !lowerName.contains("surefire") &&
               !lowerName.contains("command line") &&
               !lowerName.equals("suite") &&
               !lowerName.equals("test");
    }

    /**
     * Build a meaningful report title.
     */
    private String buildReportTitle(ITestContext context, String suiteName) {
        StringBuilder title = new StringBuilder();

        String testProperty = System.getProperty("test");
        String deviceProperty = System.getProperty("device");

        if (testProperty != null && !testProperty.isEmpty()) {
            String pageName = extractPageName(testProperty);
            title.append(pageName).append(" ");
        } else {
            var testMethods = context.getAllTestMethods();
            if (testMethods != null && testMethods.length > 0) {
                String className = testMethods[0].getTestClass().getRealClass().getSimpleName();
                String pageName = extractPageName(className);
                title.append(pageName).append(" ");
            } else if (isValidSuiteName(suiteName)) {
                title.append(suiteName).append(" ");
            }
        }

        if (deviceProperty != null && !deviceProperty.isEmpty()) {
            title.append("(").append(deviceProperty).append(") ");
        }

        title.append("Test Report");
        return title.toString();
    }

    /**
     * Extract a clean page name from test class name.
     * Examples:
     *   "ContactPageAccessibilityTest" -> "ContactPage"
     *   "HomePageAccessibilityTest" -> "HomePage"
     *   "LoginTestTestNG" -> "Login"
     */
    private String extractPageName(String className) {
        if (className == null || className.isEmpty()) {
            return "Test";
        }

        String name = className
                .replace("AccessibilityTest", "")
                .replace("TestTestNG", "")
                .replace("Test", "")
                .replace("_", "");

        if (name.isEmpty()) {
            return className;
        }

        return name;
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();

        // Build enhanced test name with device/page info
        String enhancedTestName = buildEnhancedTestName(result, testName);

        log.info("Starting test: " + enhancedTestName);

        // Create test in Extent Report
        ExtentTest test;
        if (description != null && !description.isEmpty()) {
            test = ExtentReportManager.createTest(enhancedTestName, description);
        } else {
            test = ExtentReportManager.createTest(enhancedTestName);
        }

        // Add test class info
        test.assignCategory(result.getTestClass().getName());

        // Add device category if available
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            for (Object param : parameters) {
                if (param != null && param.getClass().getSimpleName().equals("Device")) {
                    test.assignCategory(param.toString());
                }
            }
        }
    }

    /**
     * Build enhanced test name including device and page info.
     */
    private String buildEnhancedTestName(ITestResult result, String baseTestName) {
        Object[] parameters = result.getParameters();
        String className = result.getTestClass().getRealClass().getSimpleName();

        // Extract page name from class name
        String pageName = "";
        if (className.contains("Page")) {
            pageName = className.replace("PageAccessibilityTest", "")
                               .replace("AccessibilityTest", "");
        }

        // Extract device name from parameters
        String deviceName = "";
        if (parameters != null && parameters.length > 0) {
            for (Object param : parameters) {
                if (param != null && param.getClass().getSimpleName().equals("Device")) {
                    deviceName = param.toString();
                    break;
                }
            }
        }

        // Build enhanced name
        if (!deviceName.isEmpty() && !pageName.isEmpty()) {
            return pageName + " Page - " + deviceName;
        } else if (!deviceName.isEmpty()) {
            return baseTestName + " - " + deviceName;
        } else if (!pageName.isEmpty()) {
            return pageName + " Page - " + baseTestName;
        }

        return baseTestName;
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("✓ Test PASSED: " + testName);

        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.pass(MarkupHelper.createLabel("TEST PASSED", ExtentColor.GREEN));
            test.info("Execution time: " + getExecutionTime(result) + " seconds");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        Throwable throwable = result.getThrowable();

        log.error("✗ Test FAILED: " + testName);
        log.error("Failure reason: " + (throwable != null ? throwable.getMessage() : "Unknown"));

        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.fail(MarkupHelper.createLabel("TEST FAILED", ExtentColor.RED));
            test.info("Execution time: " + getExecutionTime(result) + " seconds");

            if (throwable != null) {
                test.fail(throwable);
            }

            attachScreenshot(result, test);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        Throwable throwable = result.getThrowable();

        log.warn("⊘ Test SKIPPED: " + testName);

        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.skip(MarkupHelper.createLabel("TEST SKIPPED", ExtentColor.ORANGE));

            if (throwable != null) {
                test.skip(throwable);
            }
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.warning("Test failed but within success percentage");
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        int remaining = activeContexts.decrementAndGet();

        log.info("========================================");
        log.info("Test Context Finished: {} (remaining contexts: {})", context.getName(), remaining);
        log.info("Tests Passed: {}", context.getPassedTests().size());
        log.info("Tests Failed: {}", context.getFailedTests().size());
        log.info("Tests Skipped: {}", context.getSkippedTests().size());
        log.info("========================================");

        // Flush reports when all contexts are done
        if (remaining <= 0) {
            ExtentReportManager.flushReports();
            log.info("Report saved to: {}", ExtentReportManager.getReportPath());

            initialized.set(false);
            activeContexts.set(0);
        }
    }

    /**
     * Capture screenshot and attach to Extent Report
     */
    private void attachScreenshot(ITestResult result, ExtentTest test) {
        try {
            Object testClass = result.getInstance();
            if (testClass instanceof BaseTestTestNG) {
                WebDriver driver = ((BaseTestTestNG) testClass).getDriver();
                if (driver != null) {
                    String base64Screenshot = ScreenshotUtils.captureScreenshotAsBase64(driver);
                    if (base64Screenshot != null) {
                        test.addScreenCaptureFromBase64String(base64Screenshot, "Failure Screenshot");
                        log.info("Screenshot attached to report");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to capture screenshot: " + e.getMessage());
            test.warning("Could not capture screenshot: " + e.getMessage());
        }
    }

    /**
     * Calculate test execution time in seconds
     */
    private String getExecutionTime(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        return String.format("%.2f", duration / 1000.0);
    }
}
