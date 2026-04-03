package utils;

import config.Configuration;

/**
 * Utility class for building URLs from base URL and paths.
 *
 * <p>Uses the base URL from Configuration (test.properties) to construct
 * full URLs. This centralizes URL construction and ensures consistency
 * across the test framework.</p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Build URL from path
 * String url = UrlBuilder.path("/pages/educators");
 * // Returns: https://kibeam.com/pages/educators
 *
 * // Get base URL
 * String base = UrlBuilder.baseUrl();
 * // Returns: https://kibeam.com
 * }</pre>
 *
 * @see Configuration#getBaseUrl()
 */
public final class UrlBuilder {

    private UrlBuilder() {
        // Prevent instantiation - utility class
    }

    /**
     * Returns the base URL from configuration.
     *
     * @return the base URL (e.g., "https://kibeam.com")
     */
    public static String baseUrl() {
        return Configuration.getInstance().getBaseUrl();
    }

    /**
     * Builds a full URL by appending the given path to the base URL.
     *
     * <p>Handles path normalization:</p>
     * <ul>
     *   <li>Adds leading slash if missing</li>
     *   <li>Removes trailing slash from base URL if present</li>
     * </ul>
     *
     * @param path the path to append (e.g., "/pages/educators" or "pages/educators")
     * @return the full URL (e.g., "https://kibeam.com/pages/educators")
     * @throws IllegalArgumentException if path is null
     */
    public static String path(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        String base = baseUrl();

        // Remove trailing slash from base URL if present
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        // Add leading slash to path if missing
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }

        return base + path;
    }

    /**
     * Builds a full URL with query parameters.
     *
     * @param path the path to append
     * @param queryParams the query string (without leading "?")
     * @return the full URL with query parameters
     */
    public static String pathWithQuery(String path, String queryParams) {
        String url = path(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            String separator = queryParams.startsWith("?") ? "" : "?";
            return url + separator + queryParams;
        }
        return url;
    }
}
