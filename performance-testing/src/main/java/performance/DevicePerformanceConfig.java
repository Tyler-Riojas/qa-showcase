package performance;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Device and network performance profiles for realistic performance testing.
 *
 * <p>Provides pre-configured profiles simulating real-world conditions:</p>
 * <ul>
 *   <li>Device types: Mobile, Tablet, Desktop</li>
 *   <li>Network speeds: 2G, 3G, 4G, WiFi, Cable</li>
 *   <li>Combined profiles for realistic scenarios</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * // Get a specific profile
 * NetworkProfile slow3G = DevicePerformanceConfig.NetworkProfile.SLOW_3G;
 *
 * // Get all mobile devices
 * List&lt;DeviceProfile&gt; mobiles = DevicePerformanceConfig.getMobileDevices();
 *
 * // Apply throttling
 * proxy.setDownstreamKbps(slow3G.downloadKbps);
 * </pre>
 */
public class DevicePerformanceConfig {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private DevicePerformanceConfig() {
        // Utility class
    }

    // ==================== Network Profiles ====================

    /**
     * Network speed profiles simulating real-world connections.
     */
    public enum NetworkProfile {
        // Slow connections
        OFFLINE("Offline", 0, 0, 0),
        SLOW_2G("Slow 2G", 50, 25, 2000),
        REGULAR_2G("Regular 2G", 250, 50, 1400),

        // 3G connections
        SLOW_3G("Slow 3G", 400, 100, 800),
        REGULAR_3G("Regular 3G", 750, 250, 400),
        FAST_3G("Fast 3G", 1500, 750, 300),

        // 4G/LTE connections
        SLOW_4G("Slow 4G", 4000, 3000, 170),
        REGULAR_4G("Regular 4G", 9000, 9000, 100),
        FAST_4G("Fast 4G LTE", 18000, 9000, 70),

        // WiFi connections
        SLOW_WIFI("Slow WiFi", 5000, 2000, 80),
        REGULAR_WIFI("Regular WiFi", 25000, 5000, 40),
        FAST_WIFI("Fast WiFi", 50000, 25000, 20),

        // Wired connections
        CABLE("Cable", 50000, 10000, 10),
        FIBER("Fiber", 100000, 50000, 5),

        // No throttling
        NO_THROTTLE("No Throttle", -1, -1, 0);

        private final String displayName;
        private final long downloadKbps;
        private final long uploadKbps;
        private final int latencyMs;

        NetworkProfile(String displayName, long downloadKbps, long uploadKbps, int latencyMs) {
            this.displayName = displayName;
            this.downloadKbps = downloadKbps;
            this.uploadKbps = uploadKbps;
            this.latencyMs = latencyMs;
        }

        public String getDisplayName() { return displayName; }
        public long getDownloadKbps() { return downloadKbps; }
        public long getUploadKbps() { return uploadKbps; }
        public int getLatencyMs() { return latencyMs; }

        /**
         * Check if this profile enables throttling.
         */
        public boolean isThrottled() {
            return this != NO_THROTTLE && this != OFFLINE;
        }
    }

    // ==================== Device Profiles ====================

    /**
     * Device profiles with viewport and user agent configurations.
     */
    public enum DeviceProfile {
        // Mobile devices
        IPHONE_SE("iPhone SE", 375, 667, DeviceType.MOBILE,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15"),
        IPHONE_12("iPhone 12", 390, 844, DeviceType.MOBILE,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15"),
        IPHONE_14_PRO("iPhone 14 Pro", 393, 852, DeviceType.MOBILE,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"),
        PIXEL_5("Pixel 5", 393, 851, DeviceType.MOBILE,
                "Mozilla/5.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36"),
        SAMSUNG_S21("Samsung Galaxy S21", 360, 800, DeviceType.MOBILE,
                "Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36"),

        // Tablet devices
        IPAD("iPad", 768, 1024, DeviceType.TABLET,
                "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15"),
        IPAD_PRO_11("iPad Pro 11", 834, 1194, DeviceType.TABLET,
                "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15"),
        IPAD_PRO_12("iPad Pro 12.9", 1024, 1366, DeviceType.TABLET,
                "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15"),
        GALAXY_TAB_S7("Galaxy Tab S7", 800, 1280, DeviceType.TABLET,
                "Mozilla/5.0 (Linux; Android 11; SM-T870) AppleWebKit/537.36"),

        // Desktop viewports
        DESKTOP_HD("Desktop HD", 1366, 768, DeviceType.DESKTOP, null),
        DESKTOP_FHD("Desktop FHD", 1920, 1080, DeviceType.DESKTOP, null),
        DESKTOP_2K("Desktop 2K", 2560, 1440, DeviceType.DESKTOP, null),
        LAPTOP_13("Laptop 13\"", 1280, 800, DeviceType.DESKTOP, null),
        LAPTOP_15("Laptop 15\"", 1440, 900, DeviceType.DESKTOP, null);

        private final String displayName;
        private final int width;
        private final int height;
        private final DeviceType type;
        private final String userAgent;

        DeviceProfile(String displayName, int width, int height, DeviceType type, String userAgent) {
            this.displayName = displayName;
            this.width = width;
            this.height = height;
            this.type = type;
            this.userAgent = userAgent;
        }

        public String getDisplayName() { return displayName; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public DeviceType getType() { return type; }
        public String getUserAgent() { return userAgent; }
        public Dimension getDimension() { return new Dimension(width, height); }
    }

    /**
     * Device type categories.
     */
    public enum DeviceType {
        MOBILE, TABLET, DESKTOP
    }

    // ==================== Combined Test Profiles ====================

    /**
     * Pre-configured test scenarios combining device and network.
     */
    public enum TestScenario {
        // Mobile scenarios
        MOBILE_3G("Mobile on 3G", DeviceProfile.IPHONE_12, NetworkProfile.REGULAR_3G),
        MOBILE_4G("Mobile on 4G", DeviceProfile.IPHONE_14_PRO, NetworkProfile.REGULAR_4G),
        MOBILE_WIFI("Mobile on WiFi", DeviceProfile.PIXEL_5, NetworkProfile.REGULAR_WIFI),
        MOBILE_SLOW("Mobile Slow Network", DeviceProfile.IPHONE_SE, NetworkProfile.SLOW_3G),

        // Tablet scenarios
        TABLET_WIFI("Tablet on WiFi", DeviceProfile.IPAD_PRO_11, NetworkProfile.REGULAR_WIFI),
        TABLET_4G("Tablet on 4G", DeviceProfile.GALAXY_TAB_S7, NetworkProfile.REGULAR_4G),

        // Desktop scenarios
        DESKTOP_FAST("Desktop Fast", DeviceProfile.DESKTOP_FHD, NetworkProfile.FIBER),
        DESKTOP_CABLE("Desktop Cable", DeviceProfile.DESKTOP_HD, NetworkProfile.CABLE),
        DESKTOP_SLOW_WIFI("Desktop Slow WiFi", DeviceProfile.LAPTOP_13, NetworkProfile.SLOW_WIFI),

        // Worst case scenarios
        WORST_CASE_MOBILE("Worst Case Mobile", DeviceProfile.IPHONE_SE, NetworkProfile.SLOW_2G),
        WORST_CASE_DESKTOP("Worst Case Desktop", DeviceProfile.LAPTOP_13, NetworkProfile.SLOW_3G);

        private final String displayName;
        private final DeviceProfile device;
        private final NetworkProfile network;

        TestScenario(String displayName, DeviceProfile device, NetworkProfile network) {
            this.displayName = displayName;
            this.device = device;
            this.network = network;
        }

        public String getDisplayName() { return displayName; }
        public DeviceProfile getDevice() { return device; }
        public NetworkProfile getNetwork() { return network; }
    }

    // ==================== Helper Methods ====================

    /**
     * Get all mobile device profiles.
     */
    public static List<DeviceProfile> getMobileDevices() {
        return Arrays.stream(DeviceProfile.values())
                .filter(d -> d.getType() == DeviceType.MOBILE)
                .toList();
    }

    /**
     * Get all tablet device profiles.
     */
    public static List<DeviceProfile> getTabletDevices() {
        return Arrays.stream(DeviceProfile.values())
                .filter(d -> d.getType() == DeviceType.TABLET)
                .toList();
    }

    /**
     * Get all desktop device profiles.
     */
    public static List<DeviceProfile> getDesktopDevices() {
        return Arrays.stream(DeviceProfile.values())
                .filter(d -> d.getType() == DeviceType.DESKTOP)
                .toList();
    }

    /**
     * Get common test scenarios for quick testing.
     */
    public static List<TestScenario> getQuickTestScenarios() {
        return List.of(
                TestScenario.MOBILE_4G,
                TestScenario.TABLET_WIFI,
                TestScenario.DESKTOP_CABLE
        );
    }

    /**
     * Get comprehensive test scenarios for full coverage.
     */
    public static List<TestScenario> getComprehensiveScenarios() {
        return Arrays.asList(TestScenario.values());
    }

    /**
     * Get mobile-focused test scenarios.
     */
    public static List<TestScenario> getMobileScenarios() {
        return List.of(
                TestScenario.MOBILE_3G,
                TestScenario.MOBILE_4G,
                TestScenario.MOBILE_WIFI,
                TestScenario.MOBILE_SLOW
        );
    }
}
