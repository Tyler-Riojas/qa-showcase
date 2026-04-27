package enums;

/**
 * Supported browser types for test execution.
 * Use fromString() to parse from configuration properties.
 */
public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE,
    SAFARI;

    /**
     * Parse browser type from string (case-insensitive).
     * Defaults to CHROME if input is null or empty.
     *
     * @param browserName Browser name from properties/parameters
     * @return Corresponding BrowserType enum value
     * @throws IllegalArgumentException if browser is not supported
     */
    public static BrowserType fromString(String browserName) {
        if (browserName == null || browserName.trim().isEmpty()) {
            return CHROME; // default browser
        }

        String normalized = browserName.trim().toUpperCase();

        return switch (normalized) {
            case "CHROME" -> CHROME;
            case "FIREFOX", "FF" -> FIREFOX;
            case "EDGE", "MSEDGE", "MS_EDGE" -> EDGE;
            case "SAFARI" -> SAFARI;
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: '" + browserName + "'. " +
                    "Supported browsers: chrome, firefox, edge, safari"
            );
        };
    }

    /**
     * Get browser name in lowercase (for logging/display)
     */
    public String getName() {
        return name().toLowerCase();
    }
}
