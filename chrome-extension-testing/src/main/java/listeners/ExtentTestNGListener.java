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
 */
public class ExtentTestNGListener implements ITestListener, ISuiteListener {

    static final Logger log = getLogger(lookup().lookupClass());

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicInteger activeContexts = new AtomicInteger(0);

    // ==================== SUITE LISTENER ====================

    @Override
    public void onStart(ISuite suite) {
        log.info("SUITE STARTED: {}", suite.getName());
        if (initialized.compareAndSet(false, true)) {
            String reportName = suite.getParameter("reportName");
            String reportTitle = suite.getParameter("reportTitle");

            if (reportName != null && !reportName.isEmpty()) {
                ExtentReportManager.setReportName(reportName);
            } else {
                ExtentReportManager.setReportName(suite.getName().replace(" ", "_") + "_Report");
            }

            if (reportTitle != null && !reportTitle.isEmpty()) {
                ExtentReportManager.setReportTitle(reportTitle);
            } else {
                ExtentReportManager.setReportTitle(suite.getName() + " Test Report");
            }

            ExtentReportManager.getInstance();
            log.info("ExtentReports initialised: {}", ExtentReportManager.getReportName());
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("SUITE FINISHED: {}", suite.getName());
        ExtentReportManager.flushReports();
        log.info("Report saved to: {}", ExtentReportManager.getReportPath());
        initialized.set(false);
    }

    // ==================== TEST CONTEXT LISTENER ====================

    @Override
    public void onStart(ITestContext context) {
        int count = activeContexts.incrementAndGet();
        log.info("Test Context Started: {} (active: {})", context.getName(), count);

        if (initialized.compareAndSet(false, true)) {
            ExtentReportManager.setReportName(context.getName().replace(" ", "_") + "_Report");
            ExtentReportManager.setReportTitle(context.getName() + " Test Report");
            ExtentReportManager.getInstance();
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();

        log.info("Starting test: {}", testName);

        ExtentTest test;
        if (description != null && !description.isEmpty()) {
            test = ExtentReportManager.createTest(testName, description);
        } else {
            test = ExtentReportManager.createTest(testName);
        }
        test.assignCategory(result.getTestClass().getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("Test PASSED: {}", result.getMethod().getMethodName());
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.pass(MarkupHelper.createLabel("TEST PASSED", ExtentColor.GREEN));
            test.info("Execution time: " + getExecutionTime(result) + " seconds");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("Test FAILED: {}", result.getMethod().getMethodName());
        Throwable throwable = result.getThrowable();

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
        log.warn("Test SKIPPED: {}", result.getMethod().getMethodName());
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.skip(MarkupHelper.createLabel("TEST SKIPPED", ExtentColor.ORANGE));
            Throwable throwable = result.getThrowable();
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
        log.info("Test Context Finished: {} (remaining: {})", context.getName(), remaining);
        log.info("Passed: {} | Failed: {} | Skipped: {}",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());

        if (remaining <= 0) {
            ExtentReportManager.flushReports();
            log.info("Report saved to: {}", ExtentReportManager.getReportPath());
            initialized.set(false);
            activeContexts.set(0);
        }
    }

    private void attachScreenshot(ITestResult result, ExtentTest test) {
        try {
            Object testClass = result.getInstance();
            if (testClass instanceof BaseTestTestNG) {
                WebDriver driver = ((BaseTestTestNG) testClass).getDriver();
                if (driver != null) {
                    String base64Screenshot = ScreenshotUtils.captureScreenshotAsBase64(driver);
                    if (base64Screenshot != null) {
                        test.addScreenCaptureFromBase64String(base64Screenshot, "Failure Screenshot");
                    }
                    ScreenshotUtils.captureScreenshot(driver,
                            result.getMethod().getMethodName() + "_FAILED");
                }
            }
        } catch (Exception e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
            test.warning("Could not capture screenshot: " + e.getMessage());
        }
    }

    private String getExecutionTime(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        return String.format("%.2f", duration / 1000.0);
    }
}
