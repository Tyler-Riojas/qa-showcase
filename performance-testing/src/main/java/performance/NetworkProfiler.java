package performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Network profiler using BrowserMob Proxy for HAR capture and analysis.
 *
 * <p>Provides capabilities for:</p>
 * <ul>
 *   <li>HAR (HTTP Archive) file capture</li>
 *   <li>Network throttling simulation</li>
 *   <li>Response time analysis</li>
 *   <li>Resource size tracking</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * NetworkProfiler profiler = new NetworkProfiler();
 * profiler.start();
 *
 * // Configure WebDriver with proxy
 * ChromeOptions options = profiler.configureChromeOptions(new ChromeOptions());
 *
 * // Start capture
 * profiler.startCapture("PageLoad");
 *
 * // ... navigate and interact ...
 *
 * // Get results
 * NetworkMetrics metrics = profiler.stopCapture();
 * profiler.saveHar("page-load.har");
 *
 * profiler.stop();
 * </pre>
 */
public class NetworkProfiler {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String HAR_DIR = System.getProperty("user.dir") + "/reports/har";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private BrowserMobProxy proxy;
    private DevicePerformanceConfig.NetworkProfile currentThrottle;
    private long captureStartTime;

    /**
     * Start the proxy server.
     */
    public void start() {
        proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start(0);

        log.info("NetworkProfiler started on port {}", proxy.getPort());
    }

    /**
     * Start with network throttling.
     */
    public void start(DevicePerformanceConfig.NetworkProfile networkProfile) {
        start();
        applyThrottling(networkProfile);
    }

    /**
     * Stop the proxy server.
     */
    public void stop() {
        if (proxy != null && proxy.isStarted()) {
            proxy.stop();
            log.info("NetworkProfiler stopped");
        }
    }

    /**
     * Apply network throttling profile.
     */
    public void applyThrottling(DevicePerformanceConfig.NetworkProfile profile) {
        if (proxy == null || !proxy.isStarted()) {
            throw new IllegalStateException("Proxy not started");
        }

        this.currentThrottle = profile;

        if (profile == DevicePerformanceConfig.NetworkProfile.NO_THROTTLE) {
            proxy.setReadBandwidthLimit(0);
            proxy.setWriteBandwidthLimit(0);
            log.info("Network throttling disabled");
            return;
        }

        if (profile == DevicePerformanceConfig.NetworkProfile.OFFLINE) {
            proxy.setReadBandwidthLimit(1);
            proxy.setWriteBandwidthLimit(1);
            log.info("Network set to offline (minimal bandwidth)");
            return;
        }

        // Convert Kbps to bytes per second
        long downloadBps = profile.getDownloadKbps() * 1024 / 8;
        long uploadBps = profile.getUploadKbps() * 1024 / 8;

        proxy.setReadBandwidthLimit(downloadBps);
        proxy.setWriteBandwidthLimit(uploadBps);
        proxy.setLatency(profile.getLatencyMs(), TimeUnit.MILLISECONDS);

        log.info("Network throttling applied: {} ({}Kbps down, {}Kbps up, {}ms latency)",
                profile.getDisplayName(),
                profile.getDownloadKbps(),
                profile.getUploadKbps(),
                profile.getLatencyMs());
    }

    /**
     * Remove network throttling.
     */
    public void removeThrottling() {
        applyThrottling(DevicePerformanceConfig.NetworkProfile.NO_THROTTLE);
    }

    /**
     * Start HAR capture for a page.
     */
    public void startCapture(String pageName) {
        if (proxy == null || !proxy.isStarted()) {
            throw new IllegalStateException("Proxy not started");
        }

        proxy.enableHarCaptureTypes(
                CaptureType.REQUEST_HEADERS,
                CaptureType.REQUEST_CONTENT,
                CaptureType.RESPONSE_HEADERS,
                CaptureType.RESPONSE_CONTENT
        );

        proxy.newHar(pageName);
        captureStartTime = System.currentTimeMillis();

        log.info("HAR capture started: {}", pageName);
    }

    /**
     * Stop capture and return metrics.
     */
    public NetworkMetrics stopCapture() {
        if (proxy == null || proxy.getHar() == null) {
            throw new IllegalStateException("No active capture");
        }

        long captureEndTime = System.currentTimeMillis();
        Har har = proxy.getHar();

        NetworkMetrics metrics = analyzeHar(har);
        metrics.totalCaptureTime = captureEndTime - captureStartTime;
        metrics.networkProfile = currentThrottle != null ? currentThrottle.getDisplayName() : "None";

        log.info("HAR capture stopped. {} requests, {} total size, {} total time",
                metrics.totalRequests, formatBytes(metrics.totalSize), metrics.totalCaptureTime + "ms");

        return metrics;
    }

    /**
     * Get current HAR without stopping capture.
     */
    public Har getCurrentHar() {
        if (proxy == null) return null;
        return proxy.getHar();
    }

    /**
     * Save HAR to file.
     */
    public String saveHar(String filename) throws IOException {
        Har har = proxy.getHar();
        if (har == null) {
            throw new IllegalStateException("No HAR to save");
        }

        File harDir = new File(HAR_DIR);
        if (!harDir.exists()) {
            harDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String harPath = HAR_DIR + "/" + filename.replace(".har", "") + "_" + timestamp + ".har";

        har.writeTo(new File(harPath));
        log.info("HAR saved: {}", harPath);

        return harPath;
    }

    /**
     * Configure ChromeOptions to use the proxy.
     */
    public ChromeOptions configureChromeOptions(ChromeOptions options) {
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        options.setProxy(seleniumProxy);
        options.setAcceptInsecureCerts(true);
        return options;
    }

    /**
     * Get Selenium proxy configuration.
     */
    public Proxy getSeleniumProxy() {
        return ClientUtil.createSeleniumProxy(proxy);
    }

    /**
     * Get proxy port.
     */
    public int getPort() {
        return proxy != null ? proxy.getPort() : -1;
    }

    /**
     * Check if proxy is running.
     */
    public boolean isRunning() {
        return proxy != null && proxy.isStarted();
    }

    // ==================== HAR Analysis ====================

    /**
     * Analyze HAR and extract metrics.
     */
    private NetworkMetrics analyzeHar(Har har) {
        NetworkMetrics metrics = new NetworkMetrics();

        if (har == null || har.getLog() == null || har.getLog().getEntries() == null) {
            return metrics;
        }

        List<HarEntry> entries = har.getLog().getEntries();
        metrics.totalRequests = entries.size();

        Map<String, Long> sizeByType = new HashMap<>();
        Map<String, Integer> countByType = new HashMap<>();
        List<Long> responseTimes = new ArrayList<>();

        for (HarEntry entry : entries) {
            // Response time
            long time = entry.getTime();
            responseTimes.add(time);
            metrics.totalTime += time;

            // Response size
            long size = entry.getResponse().getBodySize();
            if (size < 0) size = 0;
            metrics.totalSize += size;

            // Categorize by content type
            String contentType = getContentCategory(entry.getResponse().getContent().getMimeType());
            sizeByType.merge(contentType, size, Long::sum);
            countByType.merge(contentType, 1, Integer::sum);

            // Track status codes
            int status = entry.getResponse().getStatus();
            metrics.statusCodes.merge(status, 1, Integer::sum);

            // Track slowest requests
            if (metrics.slowestRequests.size() < 5 || time > metrics.slowestRequests.get(4).time) {
                metrics.slowestRequests.add(new RequestTiming(entry.getRequest().getUrl(), time, size));
                metrics.slowestRequests.sort((a, b) -> Long.compare(b.time, a.time));
                if (metrics.slowestRequests.size() > 5) {
                    metrics.slowestRequests.remove(5);
                }
            }

            // Track largest resources
            if (metrics.largestResources.size() < 5 || size > metrics.largestResources.get(4).size) {
                metrics.largestResources.add(new RequestTiming(entry.getRequest().getUrl(), time, size));
                metrics.largestResources.sort((a, b) -> Long.compare(b.size, a.size));
                if (metrics.largestResources.size() > 5) {
                    metrics.largestResources.remove(5);
                }
            }
        }

        metrics.sizeByType = sizeByType;
        metrics.countByType = countByType;

        // Calculate statistics
        if (!responseTimes.isEmpty()) {
            responseTimes.sort(Long::compare);
            metrics.minResponseTime = responseTimes.get(0);
            metrics.maxResponseTime = responseTimes.get(responseTimes.size() - 1);
            metrics.avgResponseTime = metrics.totalTime / metrics.totalRequests;
            metrics.medianResponseTime = responseTimes.get(responseTimes.size() / 2);

            // 95th percentile
            int p95Index = (int) Math.ceil(responseTimes.size() * 0.95) - 1;
            metrics.p95ResponseTime = responseTimes.get(Math.max(0, p95Index));
        }

        return metrics;
    }

    /**
     * Categorize content type into simple categories.
     */
    private String getContentCategory(String mimeType) {
        if (mimeType == null) return "other";

        mimeType = mimeType.toLowerCase();

        if (mimeType.contains("html")) return "html";
        if (mimeType.contains("css")) return "css";
        if (mimeType.contains("javascript") || mimeType.contains("js")) return "javascript";
        if (mimeType.contains("image")) return "images";
        if (mimeType.contains("font")) return "fonts";
        if (mimeType.contains("json")) return "json";
        if (mimeType.contains("xml")) return "xml";
        if (mimeType.contains("video")) return "video";
        if (mimeType.contains("audio")) return "audio";

        return "other";
    }

    /**
     * Format bytes to human readable string.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== Data Classes ====================

    /**
     * Network performance metrics from HAR analysis.
     */
    public static class NetworkMetrics {
        public int totalRequests;
        public long totalSize;
        public long totalTime;
        public long totalCaptureTime;
        public long minResponseTime;
        public long maxResponseTime;
        public long avgResponseTime;
        public long medianResponseTime;
        public long p95ResponseTime;
        public String networkProfile;
        public Map<String, Long> sizeByType = new HashMap<>();
        public Map<String, Integer> countByType = new HashMap<>();
        public Map<Integer, Integer> statusCodes = new HashMap<>();
        public List<RequestTiming> slowestRequests = new ArrayList<>();
        public List<RequestTiming> largestResources = new ArrayList<>();

        public String getTotalSizeFormatted() {
            return formatBytes(totalSize);
        }
    }

    /**
     * Individual request timing data.
     */
    public static class RequestTiming {
        public final String url;
        public final long time;
        public final long size;

        public RequestTiming(String url, long time, long size) {
            this.url = url;
            this.time = time;
            this.size = size;
        }

        public String getShortUrl() {
            if (url.length() > 80) {
                return url.substring(0, 40) + "..." + url.substring(url.length() - 35);
            }
            return url;
        }
    }
}
