package performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Lighthouse runner for performance audits.
 *
 * <p>Provides programmatic access to Google Lighthouse for:</p>
 * <ul>
 *   <li>Performance scoring</li>
 *   <li>Core Web Vitals measurement</li>
 *   <li>Accessibility audits</li>
 *   <li>SEO analysis</li>
 * </ul>
 *
 * <h2>Prerequisites:</h2>
 * <pre>
 * npm install -g lighthouse
 * </pre>
 *
 * <h2>Usage:</h2>
 * <pre>
 * LighthouseRunner runner = new LighthouseRunner();
 *
 * // Run audit
 * LighthouseResult result = runner.runAudit("https://example.com");
 *
 * // Check scores
 * assertThat(result.performanceScore).isGreaterThanOrEqualTo(90);
 * assertThat(result.largestContentfulPaint).isLessThan(2500);
 * </pre>
 */
public class LighthouseRunner {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String REPORTS_DIR = System.getProperty("user.dir") + "/reports/lighthouse";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String chromePath;
    private boolean headless = true;
    private String formFactor = "desktop";
    private int throttlingMultiplier = 1;

    /**
     * Set Chrome executable path.
     */
    public LighthouseRunner withChromePath(String chromePath) {
        this.chromePath = chromePath;
        return this;
    }

    /**
     * Set headless mode.
     */
    public LighthouseRunner withHeadless(boolean headless) {
        this.headless = headless;
        return this;
    }

    /**
     * Set form factor (desktop or mobile).
     */
    public LighthouseRunner withFormFactor(String formFactor) {
        this.formFactor = formFactor;
        return this;
    }

    /**
     * Set mobile form factor.
     */
    public LighthouseRunner mobile() {
        this.formFactor = "mobile";
        return this;
    }

    /**
     * Set desktop form factor.
     */
    public LighthouseRunner desktop() {
        this.formFactor = "desktop";
        return this;
    }

    /**
     * Set CPU throttling multiplier.
     */
    public LighthouseRunner withThrottling(int multiplier) {
        this.throttlingMultiplier = multiplier;
        return this;
    }

    /**
     * Run Lighthouse audit on a URL.
     */
    public LighthouseResult runAudit(String url) throws IOException, InterruptedException {
        log.info("Running Lighthouse audit for: {} ({})", url, formFactor);

        File reportsDir = new File(REPORTS_DIR);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String reportName = "lighthouse_" + timestamp;
        String jsonPath = REPORTS_DIR + "/" + reportName + ".json";
        String htmlPath = REPORTS_DIR + "/" + reportName + ".html";

        // Build Lighthouse command
        StringBuilder command = new StringBuilder();
        command.append("lighthouse \"").append(url).append("\"");
        command.append(" --output=json,html");
        command.append(" --output-path=").append(REPORTS_DIR).append("/").append(reportName);
        command.append(" --form-factor=").append(formFactor);

        if (headless) {
            command.append(" --chrome-flags=\"--headless\"");
        }

        if (chromePath != null) {
            command.append(" --chrome-path=\"").append(chromePath).append("\"");
        }

        command.append(" --quiet");

        // Execute Lighthouse
        ProcessBuilder pb = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            pb.command("cmd.exe", "/c", command.toString());
        } else {
            pb.command("sh", "-c", command.toString());
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Lighthouse audit timed out after 5 minutes");
        }

        if (process.exitValue() != 0) {
            log.error("Lighthouse output: {}", output);
            throw new IOException("Lighthouse audit failed with exit code: " + process.exitValue());
        }

        // Parse results
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            // Try with report suffix
            jsonFile = new File(REPORTS_DIR + "/" + reportName + ".report.json");
        }

        LighthouseResult result = parseResults(jsonFile);
        result.url = url;
        result.formFactor = formFactor;
        result.jsonReportPath = jsonFile.getAbsolutePath();
        result.htmlReportPath = htmlPath;

        log.info("Lighthouse audit completed - Performance: {}, Accessibility: {}, SEO: {}",
                result.performanceScore, result.accessibilityScore, result.seoScore);

        return result;
    }

    /**
     * Parse Lighthouse JSON results.
     */
    private LighthouseResult parseResults(File jsonFile) throws IOException {
        LighthouseResult result = new LighthouseResult();

        if (!jsonFile.exists()) {
            log.warn("Lighthouse JSON report not found: {}", jsonFile.getPath());
            return result;
        }

        JsonNode root = objectMapper.readTree(jsonFile);

        // Parse category scores
        JsonNode categories = root.path("categories");
        result.performanceScore = getCategoryScore(categories, "performance");
        result.accessibilityScore = getCategoryScore(categories, "accessibility");
        result.bestPracticesScore = getCategoryScore(categories, "best-practices");
        result.seoScore = getCategoryScore(categories, "seo");

        // Parse Core Web Vitals
        JsonNode audits = root.path("audits");
        result.firstContentfulPaint = getAuditNumericValue(audits, "first-contentful-paint");
        result.largestContentfulPaint = getAuditNumericValue(audits, "largest-contentful-paint");
        result.totalBlockingTime = getAuditNumericValue(audits, "total-blocking-time");
        result.cumulativeLayoutShift = getAuditNumericValue(audits, "cumulative-layout-shift") / 100;
        result.speedIndex = getAuditNumericValue(audits, "speed-index");
        result.timeToInteractive = getAuditNumericValue(audits, "interactive");

        return result;
    }

    /**
     * Get category score from JSON.
     */
    private int getCategoryScore(JsonNode categories, String category) {
        JsonNode node = categories.path(category).path("score");
        return node.isMissingNode() ? 0 : (int) (node.asDouble() * 100);
    }

    /**
     * Get audit numeric value from JSON.
     */
    private long getAuditNumericValue(JsonNode audits, String audit) {
        JsonNode node = audits.path(audit).path("numericValue");
        return node.isMissingNode() ? 0 : node.asLong();
    }

    /**
     * Check if Lighthouse is installed.
     */
    public static boolean isInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", "lighthouse --version");
            } else {
                pb.command("sh", "-c", "lighthouse --version");
            }

            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lighthouse audit result.
     */
    public static class LighthouseResult {
        public String url;
        public String formFactor;

        // Category scores (0-100)
        public int performanceScore;
        public int accessibilityScore;
        public int bestPracticesScore;
        public int seoScore;

        // Core Web Vitals (milliseconds)
        public long firstContentfulPaint;
        public long largestContentfulPaint;
        public long totalBlockingTime;
        public double cumulativeLayoutShift;
        public long speedIndex;
        public long timeToInteractive;

        // Report paths
        public String jsonReportPath;
        public String htmlReportPath;

        /**
         * Check if performance meets threshold.
         */
        public boolean meetsPerformanceThreshold(int minScore) {
            return performanceScore >= minScore;
        }

        /**
         * Check if Core Web Vitals pass.
         */
        public boolean passesWebVitals() {
            return largestContentfulPaint <= 2500 &&
                    firstContentfulPaint <= 1800 &&
                    totalBlockingTime <= 200 &&
                    cumulativeLayoutShift <= 0.1;
        }

        /**
         * Get summary string.
         */
        public String getSummary() {
            return String.format(
                    "Performance: %d | LCP: %dms | FCP: %dms | TBT: %dms | CLS: %.3f",
                    performanceScore, largestContentfulPaint, firstContentfulPaint,
                    totalBlockingTime, cumulativeLayoutShift
            );
        }
    }
}
