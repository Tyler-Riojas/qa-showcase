package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feature toggle system for enabling/disabling framework capabilities.
 *
 * All toggles read from Configuration, which supports:
 * - System properties (-Dretry.enabled=false)
 * - Environment variables (RETRY_ENABLED=false)
 * - Properties files (retry.enabled=false)
 *
 * Usage:
 *   if (FeatureToggle.isRetryEnabled()) {
 *       // apply retry logic
 *   }
 */
public final class FeatureToggle {

    private static final Logger log = LoggerFactory.getLogger(FeatureToggle.class);

    // Property keys
    public static final String RETRY_ENABLED = "retry.enabled";

    // Prevent instantiation
    private FeatureToggle() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Check if Extent Reports HTML reporting is enabled
     */
    public static boolean isExtentEnabled() {
        return Configuration.getInstance().isExtentEnabled();
    }

    /**
     * Check if Allure Reports is enabled
     */
    public static boolean isAllureEnabled() {
        return Configuration.getInstance().isAllureEnabled();
    }

    /**
     * Check if screenshots should be captured on test failure
     */
    public static boolean isScreenshotOnFailureEnabled() {
        return Configuration.getInstance().isScreenshotOnFailure();
    }

    /**
     * Check if test retry on failure is enabled
     */
    public static boolean isRetryEnabled() {
        return Configuration.getInstance().getBoolean(RETRY_ENABLED, true);
    }

    /**
     * Get number of times to retry failed tests
     */
    public static int getRetryCount() {
        return Configuration.getInstance().getRetryCount();
    }

    /**
     * Check if headless mode is enabled
     */
    public static boolean isHeadlessEnabled() {
        return Configuration.getInstance().isHeadless();
    }

    /**
     * Log current status of all feature toggles
     */
    public static void logStatus() {
        log.info("=== Feature Toggle Status ===");
        log.info("Extent Reports: {}", isExtentEnabled() ? "ENABLED" : "DISABLED");
        log.info("Allure Reports: {}", isAllureEnabled() ? "ENABLED" : "DISABLED");
        log.info("Screenshot on Failure: {}", isScreenshotOnFailureEnabled() ? "ENABLED" : "DISABLED");
        log.info("Retry on Failure: {}", isRetryEnabled() ? "ENABLED" : "DISABLED");
        if (isRetryEnabled()) {
            log.info("  Retry Count: {}", getRetryCount());
        }
        log.info("Headless Mode: {}", isHeadlessEnabled() ? "ENABLED" : "DISABLED");
        log.info("==============================");
    }
}
