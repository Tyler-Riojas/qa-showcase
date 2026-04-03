package listeners;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import config.FeatureToggle;

public class RetryAnalyzer implements IRetryAnalyzer {

    static final Logger log = getLogger(lookup().lookupClass());

    // Thread-safe map to track retries per test method
    private static final ConcurrentHashMap<String, Integer> retryMap = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        // Check if retry is enabled via FeatureToggle
        if (!FeatureToggle.isRetryEnabled()) {
            log.debug("Retry is disabled via configuration - not retrying: " +
                      result.getMethod().getMethodName());
            return false;
        }

        int maxRetryCount = FeatureToggle.getRetryCount();
        String testMethodKey = getTestMethodKey(result);

        // Get current retry count for this specific test method
        int retryCount = retryMap.getOrDefault(testMethodKey, 0);

        if (retryCount < maxRetryCount) {
            retryCount++;
            retryMap.put(testMethodKey, retryCount);

            log.warn("⟳ Retrying test: " + result.getMethod().getMethodName() +
                     " (Attempt " + (retryCount + 1) + " of " + (maxRetryCount + 1) + ")");
            log.warn("Failure reason: " + result.getThrowable().getMessage());

            return true;  // Retry the test
        }

        // Max retries reached - cleanup the map entry
        retryMap.remove(testMethodKey);
        log.error("✗ Test failed after " + (maxRetryCount + 1) + " attempts: " +
                  result.getMethod().getMethodName());

        return false;  // Don't retry anymore
    }
    
    /**
     * Generate unique key for each test method (class + method name)
     * This handles parallel execution properly
     */
    private String getTestMethodKey(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }
    
    /**
     * Clear retry map (useful for cleanup between test suites)
     */
    public static void clearRetryMap() {
        retryMap.clear();
        log.debug("Retry map cleared");
    }
}