package utils;

/**
 * URL constants and builders for Kibeam.com test automation.
 *
 * <p>This class provides centralized URL management for all Kibeam pages.
 * It uses {@link UrlBuilder} to construct full URLs from the base URL
 * defined in configuration (test.properties).</p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Get full URL for navigation
 * driver.get(KibeamUrls.getEducatorsUrl());
 *
 * // Use path constant for URL matching
 * assertThat(currentUrl).contains(KibeamUrls.EDUCATORS);
 * }</pre>
 *
 * <h2>Configuration:</h2>
 * <p>Base URL is read from {@code test.properties}:</p>
 * <pre>
 * base.url=https://kibeam.com
 * </pre>
 *
 * @see UrlBuilder
 * @see config.Configuration
 */
public final class KibeamUrls {

    private KibeamUrls() {
        // Prevent instantiation - utility class
    }

    // ==================== Path Constants ====================

    /** Path to Educators page */
    public static final String EDUCATORS = "/pages/educators";

    /** Path to Contact Us page */
    public static final String CONTACT = "/pages/contact-us";

    /** Path to About Us page */
    public static final String ABOUT = "/pages/about-us";

    /** Path to Collections page (WIP) */
    public static final String COLLECTIONS = "/collections";

    // ==================== URL Builders ====================

    /**
     * Returns the full URL for the Educators page.
     *
     * @return full URL (e.g., "https://kibeam.com/pages/educators")
     */
    public static String getEducatorsUrl() {
        return UrlBuilder.path(EDUCATORS);
    }

    /**
     * Returns the full URL for the Contact Us page.
     *
     * @return full URL (e.g., "https://kibeam.com/pages/contact-us")
     */
    public static String getContactUrl() {
        return UrlBuilder.path(CONTACT);
    }

    /**
     * Returns the full URL for the About Us page.
     *
     * @return full URL (e.g., "https://kibeam.com/pages/about-us")
     */
    public static String getAboutUrl() {
        return UrlBuilder.path(ABOUT);
    }

    /**
     * Returns the full URL for the Collections page.
     *
     * @return full URL (e.g., "https://kibeam.com/collections")
     */
    public static String getCollectionsUrl() {
        return UrlBuilder.path(COLLECTIONS);
    }

    /**
     * Returns the base URL for Kibeam.
     *
     * @return base URL (e.g., "https://kibeam.com")
     */
    public static String getBaseUrl() {
        return UrlBuilder.baseUrl();
    }
}
