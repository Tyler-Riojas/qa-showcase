package utils;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Device emulation configurations for responsive testing
 * Supports Chrome DevTools Protocol device emulation and browser window sizing
 */
public class DeviceEmulation {

    // Common device viewport sizes
    public enum Device {
        // Mobile devices
        IPHONE_SE(375, 667, "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15", 2.0),
        IPHONE_12(390, 844, "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15", 3.0),
        IPHONE_14_PRO_MAX(430, 932, "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15", 3.0),
        PIXEL_5(393, 851, "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36", 2.75),
        SAMSUNG_S21(360, 800, "Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36", 3.0),

        // Tablet devices
        IPAD(768, 1024, "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15", 2.0),
        IPAD_PRO_11(834, 1194, "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15", 2.0),
        IPAD_PRO_12(1024, 1366, "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15", 2.0),
        GALAXY_TAB_S7(800, 1280, "Mozilla/5.0 (Linux; Android 11; SM-T870) AppleWebKit/537.36", 2.0),

        // Desktop viewports
        DESKTOP_HD(1366, 768, null, 1.0),
        DESKTOP_FHD(1920, 1080, null, 1.0),
        DESKTOP_2K(2560, 1440, null, 1.0),
        LAPTOP(1280, 800, null, 1.0),
        LAPTOP_L(1440, 900, null, 1.0);

        private final int width;
        private final int height;
        private final String userAgent;
        private final double pixelRatio;

        Device(int width, int height, String userAgent, double pixelRatio) {
            this.width = width;
            this.height = height;
            this.userAgent = userAgent;
            this.pixelRatio = pixelRatio;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getUserAgent() { return userAgent; }
        public double getPixelRatio() { return pixelRatio; }
        public boolean isMobile() { return width < 768; }
        public boolean isTablet() { return width >= 768 && width < 1024; }
        public boolean isDesktop() { return width >= 1024; }
    }

    /**
     * Get Chrome options configured for device emulation
     */
    public static ChromeOptions getChromeOptionsForDevice(Device device) {
        ChromeOptions options = new ChromeOptions();

        Map<String, Object> deviceMetrics = new HashMap<>();
        deviceMetrics.put("width", device.getWidth());
        deviceMetrics.put("height", device.getHeight());
        deviceMetrics.put("pixelRatio", device.getPixelRatio());
        deviceMetrics.put("mobile", device.isMobile() || device.isTablet());

        Map<String, Object> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceMetrics", deviceMetrics);

        if (device.getUserAgent() != null) {
            mobileEmulation.put("userAgent", device.getUserAgent());
        }

        options.setExperimentalOption("mobileEmulation", mobileEmulation);

        return options;
    }

    /**
     * Get Chrome options for a named device (for TestNG parameters)
     */
    public static ChromeOptions getChromeOptionsForDevice(String deviceName) {
        Device device = getDeviceByName(deviceName);
        if (device != null) {
            return getChromeOptionsForDevice(device);
        }
        return new ChromeOptions();
    }

    /**
     * Get Firefox options with custom window size (Firefox doesn't support full device emulation)
     */
    public static FirefoxOptions getFirefoxOptionsForDevice(Device device) {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=" + device.getWidth());
        options.addArguments("--height=" + device.getHeight());

        if (device.getUserAgent() != null) {
            options.addPreference("general.useragent.override", device.getUserAgent());
        }

        return options;
    }

    /**
     * Get Edge options configured for device emulation
     */
    public static EdgeOptions getEdgeOptionsForDevice(Device device) {
        EdgeOptions options = new EdgeOptions();

        Map<String, Object> deviceMetrics = new HashMap<>();
        deviceMetrics.put("width", device.getWidth());
        deviceMetrics.put("height", device.getHeight());
        deviceMetrics.put("pixelRatio", device.getPixelRatio());
        deviceMetrics.put("mobile", device.isMobile() || device.isTablet());

        Map<String, Object> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceMetrics", deviceMetrics);

        if (device.getUserAgent() != null) {
            mobileEmulation.put("userAgent", device.getUserAgent());
        }

        options.setExperimentalOption("mobileEmulation", mobileEmulation);

        return options;
    }

    /**
     * Get Dimension object for setting window size
     */
    public static Dimension getDimension(Device device) {
        return new Dimension(device.getWidth(), device.getHeight());
    }

    /**
     * Get device by name string (case insensitive)
     */
    public static Device getDeviceByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        try {
            return Device.valueOf(name.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            // Try partial match
            String normalizedName = name.toUpperCase().replace(" ", "_").replace("-", "_");
            for (Device device : Device.values()) {
                if (device.name().contains(normalizedName) || normalizedName.contains(device.name())) {
                    return device;
                }
            }
            return null;
        }
    }

    /**
     * Get all mobile devices
     */
    public static Device[] getMobileDevices() {
        return new Device[] {
            Device.IPHONE_SE,
            Device.IPHONE_12,
            Device.IPHONE_14_PRO_MAX,
            Device.PIXEL_5,
            Device.SAMSUNG_S21
        };
    }

    /**
     * Get all tablet devices
     */
    public static Device[] getTabletDevices() {
        return new Device[] {
            Device.IPAD,
            Device.IPAD_PRO_11,
            Device.IPAD_PRO_12,
            Device.GALAXY_TAB_S7
        };
    }

    /**
     * Get all desktop viewports
     */
    public static Device[] getDesktopDevices() {
        return new Device[] {
            Device.LAPTOP,
            Device.LAPTOP_L,
            Device.DESKTOP_HD,
            Device.DESKTOP_FHD,
            Device.DESKTOP_2K
        };
    }
}
