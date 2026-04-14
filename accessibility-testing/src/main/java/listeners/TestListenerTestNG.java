package listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.openqa.selenium.WebDriver;

import base.BaseTestTestNG;
import utils.ScreenshotUtils;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

public class TestListenerTestNG implements ITestListener {
    
    static final Logger log = getLogger(lookup().lookupClass());

    @Override
    public void onTestStart(ITestResult result) {
        log.info("========================================");
        log.info("Starting test: " + result.getMethod().getMethodName());
        log.info("========================================");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("✓ Test PASSED: " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("✗ Test FAILED: " + result.getMethod().getMethodName());
        log.error("Failure reason: " + result.getThrowable().getMessage());
        
        // Capture screenshot on failure
        try {
            Object testClass = result.getInstance();
            WebDriver driver = ((BaseTestTestNG) testClass).driver;
            
            String screenshotPath = ScreenshotUtils.captureScreenshot(
                driver, 
                result.getMethod().getMethodName() + "_FAILED"
            );
            
            log.info("Screenshot saved: " + screenshotPath);
        } catch (Exception e) {
            log.error("Failed to capture screenshot: " + e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("⊘ Test SKIPPED: " + result.getMethod().getMethodName());
    }

    @Override
    public void onStart(ITestContext context) {
        log.info("========================================");
        log.info("Test Suite Started: " + context.getName());
        log.info("========================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("========================================");
        log.info("Test Suite Finished: " + context.getName());
        log.info("Tests Passed: " + context.getPassedTests().size());
        log.info("Tests Failed: " + context.getFailedTests().size());
        log.info("Tests Skipped: " + context.getSkippedTests().size());
        log.info("========================================");
    }
}