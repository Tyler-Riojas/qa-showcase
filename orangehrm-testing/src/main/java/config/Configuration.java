package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Thread-safe singleton configuration manager for test framework.
 *
 * Property Resolution Order (highest to lowest priority):
 * 1. System properties (-Dkey=value)
 * 2. Environment variables (KEY_NAME with underscores)
 * 3. Environment-specific properties file (e.g., staging.properties)
 * 4. Default properties file (test.properties)
 *
 * Usage:
 *   Configuration config = Configuration.getInstance();
 *   String url = config.getBaseUrl();
 *   int timeout = config.getTimeout();
 *
 * Switch environments:
 *   mvn test -Denv=staging
 *   mvn test -Denv=prod
 */
public class Configuration {

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    // Singleton instance with volatile for thread safety
    private static volatile Configuration instance;

    // Properties storage
    private final Properties properties;

    // Configuration file paths
    private static final String CONFIG_DIR = "config/";
    private static final String DEFAULT_CONFIG = "test.properties";

    // Required property keys - fail fast if missing
    private static final Set<String> REQUIRED_PROPERTIES = Set.of(
            "base.url",
            "browser",
            "timeout"
    );

    // Sensitive keys to mask in logs
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "api.key",
            "secret",
            "token",
            "credentials"
    );

    // Property keys as constants
    public static final String BASE_URL = "base.url";
    public static final String BROWSER = "browser";
    public static final String TIMEOUT = "timeout";
    public static final String HEADLESS = "headless";
    public static final String SCREENSHOT_ON_FAILURE = "screenshot.on.failure";
    public static final String RETRY_COUNT = "retry.count";
    public static final String EXTENT_ENABLED = "extent.enabled";
    public static final String ALLURE_ENABLED = "allure.enabled";
    public static final String ENVIRONMENT = "env";
    public static final String GRID_ENABLED = "grid.enabled";
    public static final String GRID_URL = "grid.url";
    public static final String GRID_PLATFORM = "grid.platform";
    public static final String GRID_BROWSER_VERSION = "grid.browser.version";
    public static final String GRID_TIMEOUT = "grid.timeout";

    /**
     * Private constructor - loads properties on instantiation
     */
    private Configuration() {
        this.properties = new Properties();
        loadConfiguration();
        validateRequiredProperties();
        logConfiguration();
    }

    /**
     * Get singleton instance using double-checked locking
     * Thread-safe and lazy initialization
     */
    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    /**
     * Reset configuration (useful for testing)
     * Forces reload on next getInstance() call
     */
    public static synchronized void reset() {
        instance = null;
        LoggerFactory.getLogger(Configuration.class).info("Configuration reset - will reload on next access");
    }

    /**
     * Load configuration from properties files
     * Order: default -> environment-specific (overwrites defaults)
     */
    private void loadConfiguration() {
        // 1. Load default configuration
        loadPropertiesFile(DEFAULT_CONFIG);

        // 2. Load environment-specific configuration (overwrites defaults)
        String env = getEnvironment();
        if (env != null && !env.isEmpty() && !env.equals("test")) {
            String envConfig = env + ".properties";
            loadPropertiesFile(envConfig);
        }

        log.info("Configuration loaded for environment: {}", env != null ? env : "default");
    }

    /**
     * Load a properties file from classpath
     */
    private void loadPropertiesFile(String filename) {
        String path = CONFIG_DIR + filename;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input != null) {
                properties.load(input);
                log.debug("Loaded configuration file: {}", path);
            } else {
                log.warn("Configuration file not found: {} (this may be expected)", path);
            }
        } catch (IOException e) {
            log.error("Failed to load configuration file: {}", path, e);
            throw new ConfigurationException("Failed to load configuration: " + path, e);
        }
    }

    /**
     * Validate all required properties are present
     * Fails fast if any required property is missing
     */
    private void validateRequiredProperties() {
        for (String key : REQUIRED_PROPERTIES) {
            String value = getString(key);
            if (value == null || value.trim().isEmpty()) {
                String message = String.format(
                        "Required configuration property '%s' is missing. " +
                        "Set it in test.properties or via -D%s=value",
                        key, key
                );
                log.error(message);
                throw new ConfigurationException(message);
            }
        }
        log.debug("All required properties validated successfully");
    }

    /**
     * Log current configuration (masking sensitive values)
     */
    private void logConfiguration() {
        log.info("=== Configuration Summary ===");
        log.info("Environment: {}", getEnvironment());
        log.info("Base URL: {}", getBaseUrl());
        log.info("Browser: {}", getBrowser());
        log.info("Timeout: {} seconds", getTimeout());
        log.info("Headless: {}", isHeadless());
        log.info("Screenshot on Failure: {}", isScreenshotOnFailure());
        log.info("Retry Count: {}", getRetryCount());
        log.info("Extent Reports Enabled: {}", isExtentEnabled());
        log.info("Allure Reports Enabled: {}", isAllureEnabled());
        log.info("Grid Enabled: {}", isGridEnabled());
        if (isGridEnabled()) {
            log.info("Grid URL: {}", getGridUrl());
            log.info("Grid Platform: {}", getGridPlatform());
        }
        log.info("=============================");
    }

    // ==================== Property Resolution ====================

    /**
     * Get property value with priority resolution:
     * System property > Environment variable > Properties file
     */
    private String resolveProperty(String key) {
        // 1. Check System property (highest priority)
        String value = System.getProperty(key);
        if (value != null) {
            log.trace("Property '{}' resolved from System property", key);
            return value;
        }

        // 2. Check Environment variable (convert key.name to KEY_NAME)
        String envKey = key.toUpperCase().replace(".", "_");
        value = System.getenv(envKey);
        if (value != null) {
            log.trace("Property '{}' resolved from environment variable '{}'", key, envKey);
            return value;
        }

        // 3. Check properties file
        value = properties.getProperty(key);
        if (value != null) {
            log.trace("Property '{}' resolved from properties file", key);
        }

        return value;
    }

    // ==================== Type-Safe Getters ====================

    /**
     * Get string property value
     * @param key Property key
     * @return Property value or null if not found
     */
    public String getString(String key) {
        return resolveProperty(key);
    }

    /**
     * Get string property with default value
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value or default
     */
    public String getString(String key, String defaultValue) {
        String value = resolveProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get required string property (throws if missing)
     * @param key Property key
     * @return Property value
     * @throws ConfigurationException if property is missing
     */
    public String getRequiredString(String key) {
        String value = resolveProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigurationException("Required property not found: " + key);
        }
        return value;
    }

    /**
     * Get integer property value
     * @param key Property key
     * @return Integer value or null if not found
     * @throws ConfigurationException if value cannot be parsed as integer
     */
    public Integer getInt(String key) {
        String value = resolveProperty(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    String.format("Property '%s' value '%s' is not a valid integer", key, value), e
            );
        }
    }

    /**
     * Get integer property with default value
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Integer value or default
     */
    public int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get long property value
     * @param key Property key
     * @return Long value or null if not found
     */
    public Long getLong(String key) {
        String value = resolveProperty(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    String.format("Property '%s' value '%s' is not a valid long", key, value), e
            );
        }
    }

    /**
     * Get long property with default value
     */
    public long getLong(String key, long defaultValue) {
        Long value = getLong(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get double property value
     * @param key Property key
     * @return Double value or null if not found
     */
    public Double getDouble(String key) {
        String value = resolveProperty(key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    String.format("Property '%s' value '%s' is not a valid double", key, value), e
            );
        }
    }

    /**
     * Get double property with default value
     */
    public double getDouble(String key, double defaultValue) {
        Double value = getDouble(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get boolean property value
     * Accepts: true, false, yes, no, 1, 0 (case-insensitive)
     * @param key Property key
     * @return Boolean value or null if not found
     */
    public Boolean getBoolean(String key) {
        String value = resolveProperty(key);
        if (value == null) {
            return null;
        }
        value = value.trim().toLowerCase();
        if (value.equals("true") || value.equals("yes") || value.equals("1")) {
            return true;
        }
        if (value.equals("false") || value.equals("no") || value.equals("0")) {
            return false;
        }
        throw new ConfigurationException(
                String.format("Property '%s' value '%s' is not a valid boolean (use true/false/yes/no/1/0)", key, value)
        );
    }

    /**
     * Get boolean property with default value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get list property (comma-separated values)
     * @param key Property key
     * @return List of trimmed string values, or empty list if not found
     */
    public List<String> getList(String key) {
        String value = resolveProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Get list property with default value
     */
    public List<String> getList(String key, List<String> defaultValue) {
        List<String> value = getList(key);
        return value.isEmpty() ? defaultValue : value;
    }

    // ==================== Convenience Getters ====================

    /**
     * Get current environment (dev, staging, prod, or test as default)
     */
    public String getEnvironment() {
        return getString(ENVIRONMENT, "test");
    }

    /**
     * Get base URL for the application under test
     */
    public String getBaseUrl() {
        return getRequiredString(BASE_URL);
    }

    /**
     * Get browser name (chrome, firefox, edge, safari)
     */
    public String getBrowser() {
        return getString(BROWSER, "chrome").toLowerCase();
    }

    /**
     * Get default wait timeout in seconds
     */
    public int getTimeout() {
        return getInt(TIMEOUT, 10);
    }

    /**
     * Get timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return getTimeout() * 1000L;
    }

    /**
     * Check if browser should run in headless mode
     */
    public boolean isHeadless() {
        return getBoolean(HEADLESS, false);
    }

    /**
     * Check if screenshots should be captured on test failure
     */
    public boolean isScreenshotOnFailure() {
        return getBoolean(SCREENSHOT_ON_FAILURE, true);
    }

    /**
     * Get number of times to retry failed tests
     */
    public int getRetryCount() {
        return getInt(RETRY_COUNT, 2);
    }

    /**
     * Check if Extent Reports is enabled
     */
    public boolean isExtentEnabled() {
        return getBoolean(EXTENT_ENABLED, true);
    }

    /**
     * Check if Allure Reports is enabled
     */
    public boolean isAllureEnabled() {
        return getBoolean(ALLURE_ENABLED, false);
    }

    // ==================== Selenium Grid ====================

    /**
     * Check if WebDriver sessions should be routed through Selenium Grid
     */
    public boolean isGridEnabled() {
        return getBoolean(GRID_ENABLED, false);
    }

    /**
     * Get Selenium Grid hub URL
     */
    public String getGridUrl() {
        return getString(GRID_URL, "http://localhost:4444");
    }

    /**
     * Get platform to request from Grid (linux, windows, mac)
     */
    public String getGridPlatform() {
        return getString(GRID_PLATFORM, "linux");
    }

    /**
     * Get browser version to request from Grid (empty string = latest)
     */
    public String getGridBrowserVersion() {
        return getString(GRID_BROWSER_VERSION, "");
    }

    /**
     * Get Grid session timeout in seconds
     */
    public int getGridTimeout() {
        return getInt(GRID_TIMEOUT, 300);
    }

    // ==================== Utility Methods ====================

    /**
     * Check if a property exists (in any source)
     */
    public boolean hasProperty(String key) {
        return resolveProperty(key) != null;
    }

    /**
     * Get masked value for logging (hides sensitive data)
     */
    public String getMaskedValue(String key) {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        for (String sensitive : SENSITIVE_KEYS) {
            if (key.toLowerCase().contains(sensitive)) {
                return "********";
            }
        }
        return value;
    }

    /**
     * Custom exception for configuration errors
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
