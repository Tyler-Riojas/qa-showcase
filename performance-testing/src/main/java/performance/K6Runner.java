package performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * K6 load testing runner for JavaScript-based performance tests.
 *
 * <p>Provides integration with k6 for:</p>
 * <ul>
 *   <li>Load testing with VUs (virtual users)</li>
 *   <li>Stress testing and soak testing</li>
 *   <li>Threshold-based assertions via pre-written scripts</li>
 *   <li>JSON metrics export</li>
 * </ul>
 *
 * <h2>Prerequisites:</h2>
 * <pre>
 * # macOS
 * brew install k6
 *
 * # Windows
 * choco install k6
 * </pre>
 *
 * <h2>Usage — pre-written scripts (preferred):</h2>
 * <pre>
 * K6Result result = new K6Runner()
 *     .withBaseUrl("http://localhost:3001")
 *     .runSimpleTest();
 *
 * // Point at a specific script
 * K6Result result = new K6Runner()
 *     .withScript("booking-spike-test.js")
 *     .withBaseUrl("https://staging-api.example.com")
 *     .runScript();
 * </pre>
 *
 * <h2>Usage — inline generated scripts (fallback):</h2>
 * <pre>
 * K6Result result = new K6Runner()
 *     .withVUs(10)
 *     .withDuration("30s")
 *     .runSimpleTest("https://api.example.com/endpoint");
 * </pre>
 */
public class K6Runner {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String REPORTS_DIR = System.getProperty("user.dir") + "/reports/k6";
    private static final String SCRIPTS_DIR = System.getProperty("user.dir") + "/src/test/resources/k6";

    // --- Configuration ---

    private int    vus        = 10;
    private String duration   = "30s";
    private int    iterations = 0;
    private String[] thresholds = new String[0];

    /** Explicit script file path (overrides the per-method default). */
    private String scriptPath;

    /**
     * Base URL injected into the k6 process via {@code --env BASE_URL=<url>}.
     * Pre-written scripts read this variable and append their own endpoint paths.
     */
    private String baseUrl;

    // ==================== Fluent configuration ====================

    /** Set number of virtual users (used by inline generated scripts only). */
    public K6Runner withVUs(int vus) {
        this.vus = vus;
        return this;
    }

    /** Set test duration, e.g. "30s", "1m", "5m" (generated scripts only). */
    public K6Runner withDuration(String duration) {
        this.duration = duration;
        return this;
    }

    /** Set fixed iteration count instead of duration (generated scripts only). */
    public K6Runner withIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    /** Override k6 thresholds (generated scripts only). */
    public K6Runner withThresholds(String... thresholds) {
        this.thresholds = thresholds;
        return this;
    }

    /**
     * Set an explicit script file to run.
     * Accepts a filename relative to {@code src/test/resources/k6/} or an absolute path.
     *
     * <pre>
     * new K6Runner().withScript("booking-spike-test.js").withBaseUrl(BASE_URL).runScript();
     * </pre>
     */
    public K6Runner withScript(String scriptPath) {
        this.scriptPath = scriptPath;
        return this;
    }

    /**
     * Set the API base URL injected into k6 scripts as the {@code BASE_URL} environment
     * variable ({@code --env BASE_URL=<url>}).
     *
     * <p>Pre-written scripts in {@code src/test/resources/k6/} read this variable:
     * <pre>const BASE_URL = __ENV.BASE_URL || 'http://localhost:3001';</pre>
     *
     * <p>Must be the bare origin (e.g. {@code http://localhost:3001}) — the scripts
     * append endpoint paths themselves.
     */
    public K6Runner withBaseUrl(String url) {
        this.baseUrl = url;
        return this;
    }

    // ==================== Run methods ====================

    /**
     * Run the script set via {@link #withScript(String)}.
     * Use this when you want full control over which pre-written script is executed.
     */
    public K6Result runScript() throws IOException, InterruptedException {
        if (scriptPath == null) {
            throw new IllegalStateException("No script set — call withScript(path) first.");
        }
        File script = resolveScriptFile(scriptPath);
        log.info("Running k6 script: {}", script.getAbsolutePath());
        return executeK6(script.getAbsolutePath(), baseUrl);
    }

    /**
     * Run a k6 script file by path.
     * Accepts a filename relative to {@code src/test/resources/k6/} or an absolute path.
     */
    public K6Result runScript(String path) throws IOException, InterruptedException {
        File script = resolveScriptFile(path);
        log.info("Running k6 script: {}", script.getAbsolutePath());
        return executeK6(script.getAbsolutePath(), baseUrl);
    }

    /**
     * Run a simple GET load test.
     *
     * <p>Resolution order for the script to execute:
     * <ol>
     *   <li>{@link #withScript(String)} override</li>
     *   <li>{@code booking-load-test.js} in {@code src/test/resources/k6/}</li>
     *   <li>Inline generated script (uses {@code url} as the full endpoint URL)</li>
     * </ol>
     *
     * <p>Base URL resolution for pre-written scripts:
     * <ol>
     *   <li>{@link #withBaseUrl(String)} — recommended; must be the bare origin</li>
     *   <li>{@code url} parameter — used as fallback; pass the bare origin when relying on this</li>
     * </ol>
     *
     * @param url Full endpoint URL for the inline generated-script fallback, or bare origin
     *            when no pre-written script is configured.
     */
    public K6Result runSimpleTest(String url) throws IOException, InterruptedException {
        String effectiveBaseUrl = baseUrl != null ? baseUrl : url;
        File script = resolveActiveScript("booking-load-test.js");
        if (script != null) {
            log.info("Running simple load test via {}", script.getName());
            return executeK6(script.getAbsolutePath(), effectiveBaseUrl);
        }
        log.info("booking-load-test.js not found — falling back to generated script for {}", url);
        return runGeneratedScript(generateSimpleScript(url, "GET"), "simple_get_test");
    }

    /**
     * Run a simple POST load test (always uses an inline generated script).
     */
    public K6Result runPostTest(String url, String body) throws IOException, InterruptedException {
        return runGeneratedScript(generatePostScript(url, body), "simple_post_test");
    }

    /**
     * Run a ramping stress test.
     *
     * <p>Script resolution order:
     * <ol>
     *   <li>{@link #withScript(String)} override</li>
     *   <li>{@code booking-stress-test.js} in {@code src/test/resources/k6/}</li>
     *   <li>Inline generated script (uses {@code url} + {@code maxVUs})</li>
     * </ol>
     *
     * @param url    Full endpoint URL for the generated-script fallback, or bare origin
     *               when relying on a pre-written script.
     * @param maxVUs Peak VU count for the inline generated-script fallback.
     */
    public K6Result runStressTest(String url, int maxVUs) throws IOException, InterruptedException {
        String effectiveBaseUrl = baseUrl != null ? baseUrl : url;
        File script = resolveActiveScript("booking-stress-test.js");
        if (script != null) {
            log.info("Running stress test via {}", script.getName());
            return executeK6(script.getAbsolutePath(), effectiveBaseUrl);
        }
        log.info("booking-stress-test.js not found — falling back to generated script for {}", url);
        return runGeneratedScript(generateStressTestScript(url, maxVUs), "stress_test");
    }

    /**
     * Run a spike test (always uses an inline generated script).
     */
    public K6Result runSpikeTest(String url, int baseVUs, int spikeVUs) throws IOException, InterruptedException {
        return runGeneratedScript(generateSpikeTestScript(url, baseVUs, spikeVUs), "spike_test");
    }

    // ==================== Internal helpers ====================

    /**
     * Resolve the script to use for a method call.
     *
     * <ol>
     *   <li>If {@link #scriptPath} was set via {@code withScript()}, resolve and return that.</li>
     *   <li>Otherwise look for {@code fallbackName} in {@code SCRIPTS_DIR}.</li>
     *   <li>Return {@code null} if neither exists (triggers generated-script fallback).</li>
     * </ol>
     */
    private File resolveActiveScript(String fallbackName) {
        if (scriptPath != null) {
            try { return resolveScriptFile(scriptPath); }
            catch (FileNotFoundException e) {
                log.warn("Configured script not found: {} — falling back to {}", scriptPath, fallbackName);
            }
        }
        File defaultScript = new File(SCRIPTS_DIR + "/" + fallbackName);
        return defaultScript.exists() ? defaultScript : null;
    }

    /**
     * Resolve a script file path to a {@link File} that exists.
     * Checks the literal path first, then {@code SCRIPTS_DIR/<path>}.
     */
    private File resolveScriptFile(String path) throws FileNotFoundException {
        File f = new File(path);
        if (f.exists()) return f;
        f = new File(SCRIPTS_DIR + "/" + path);
        if (f.exists()) return f;
        throw new FileNotFoundException("k6 script not found: " + path
                + " (also tried: " + SCRIPTS_DIR + "/" + path + ")");
    }

    /** Write an inline script to a temp file, run it, delete on exit. */
    private K6Result runGeneratedScript(String script, String testName) throws IOException, InterruptedException {
        File temp = File.createTempFile("k6_" + testName + "_", ".js");
        temp.deleteOnExit();
        Files.writeString(temp.toPath(), script);
        log.debug("Generated k6 script:\n{}", script);
        return executeK6(temp.getAbsolutePath(), baseUrl);
    }

    /**
     * Execute k6, stream output, wait for completion, and parse results.
     *
     * @param scriptPath      Absolute path to the script file.
     * @param effectiveBaseUrl Value injected as {@code --env BASE_URL=}; may be {@code null}.
     */
    private K6Result executeK6(String scriptPath, String effectiveBaseUrl) throws IOException, InterruptedException {
        new File(REPORTS_DIR).mkdirs();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String jsonPath  = REPORTS_DIR + "/k6_" + timestamp + ".json";

        // Build k6 command
        StringBuilder cmd = new StringBuilder("k6 run");
        cmd.append(" --out json=").append(jsonPath);

        if (effectiveBaseUrl != null && !effectiveBaseUrl.isEmpty()) {
            cmd.append(" --env BASE_URL=").append(effectiveBaseUrl);
        }

        // VUs / duration only apply to generated scripts; pre-written scripts
        // define their own options block and ignore CLI overrides.
        if (iterations > 0) {
            cmd.append(" --iterations ").append(iterations);
        } else {
            cmd.append(" --vus ").append(vus);
            cmd.append(" --duration ").append(duration);
        }

        for (String t : thresholds) {
            cmd.append(" --threshold ").append(t);
        }

        cmd.append(" ").append(scriptPath);

        log.info("Executing: {}", cmd);

        ProcessBuilder pb = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        pb.command(isWindows ? "cmd.exe" : "sh", isWindows ? "/c" : "-c", cmd.toString());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("k6: {}", line);
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("k6 test timed out after 10 minutes");
        }

        log.info("k6 finished with exit code: {}", process.exitValue());

        K6Result result = parseResults(output.toString());
        result.exitCode      = process.exitValue();
        result.passed        = process.exitValue() == 0;
        result.jsonReportPath = jsonPath;
        result.rawOutput     = output.toString();
        return result;
    }

    // ==================== Output parsing ====================

    private K6Result parseResults(String output) {
        K6Result result = new K6Result();

        for (String line : output.split("\n")) {
            line = line.trim();

            if (line.contains("http_reqs") && !line.contains("http_req_")) {
                long v = extractLongValue(line);
                result.httpReqs       = v;
                result.totalRequests  = (int) v;

            } else if (line.contains("http_req_duration") && line.contains("avg=")) {
                result.httpReqDurationAvg = extractDurationValue(line, "avg=");
                double p95 = extractDurationValue(line, "p(95)=");
                result.httpReqDurationP95 = p95;
                result.p95ResponseTime    = (long) p95;

            } else if (line.contains("http_req_failed")) {
                result.httpReqFailed = extractPercentValue(line);

            } else if (line.contains("iterations")) {
                result.iterations = extractLongValue(line);

            } else if (line.contains("vus_max")) {
                result.vusMax = (int) extractLongValue(line);
            }
        }

        return result;
    }

    private long extractLongValue(String line) {
        try {
            for (String part : line.split("\\s+")) {
                if (part.matches("\\d+")) {
                    return Long.parseLong(part);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse long from: {}", line);
        }
        return 0;
    }

    private double extractDurationValue(String line, String prefix) {
        try {
            int idx = line.indexOf(prefix);
            if (idx >= 0) {
                String value = line.substring(idx + prefix.length()).split("[,\\s]")[0];
                value = value.replaceAll("[^0-9.]", "");
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            log.debug("Failed to parse duration '{}' from: {}", prefix, line);
        }
        return 0;
    }

    private double extractPercentValue(String line) {
        try {
            if (line.contains("%")) {
                for (String part : line.split("\\s+")) {
                    if (part.contains("%")) {
                        return Double.parseDouble(part.replace("%", "")) / 100;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse percent from: {}", line);
        }
        return 0;
    }

    // ==================== Inline script generators (fallback) ====================

    private String generateSimpleScript(String url, String method) {
        return String.format("""
                import http from 'k6/http';
                import { check, sleep } from 'k6';

                export const options = {
                    vus: %d,
                    duration: '%s',
                    thresholds: {
                        'http_req_duration': ['p(95)<500'],
                        'http_req_failed': ['rate<0.01'],
                    },
                };

                export default function () {
                    const res = http.%s('%s');
                    check(res, {
                        'status is 200': (r) => r.status === 200,
                        'response time < 500ms': (r) => r.timings.duration < 500,
                    });
                    sleep(1);
                }
                """, vus, duration, method.toLowerCase(), url);
    }

    private String generatePostScript(String url, String body) {
        return String.format("""
                import http from 'k6/http';
                import { check, sleep } from 'k6';

                export const options = {
                    vus: %d,
                    duration: '%s',
                    thresholds: {
                        'http_req_duration': ['p(95)<500'],
                        'http_req_failed': ['rate<0.01'],
                    },
                };

                const payload = JSON.stringify(%s);
                const params = { headers: { 'Content-Type': 'application/json' } };

                export default function () {
                    const res = http.post('%s', payload, params);
                    check(res, {
                        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
                        'response time < 500ms': (r) => r.timings.duration < 500,
                    });
                    sleep(1);
                }
                """, vus, duration, body, url);
    }

    private String generateStressTestScript(String url, int maxVUs) {
        return String.format("""
                import http from 'k6/http';
                import { check, sleep } from 'k6';

                export const options = {
                    stages: [
                        { duration: '30s', target: %d },
                        { duration: '1m',  target: %d },
                        { duration: '30s', target: %d },
                        { duration: '1m',  target: %d },
                        { duration: '30s', target: 0  },
                    ],
                    thresholds: {
                        'http_req_duration': ['p(95)<1000'],
                        'http_req_failed': ['rate<0.05'],
                    },
                };

                export default function () {
                    const res = http.get('%s');
                    check(res, { 'status is 200': (r) => r.status === 200 });
                    sleep(1);
                }
                """, maxVUs / 4, maxVUs / 2, maxVUs / 2, maxVUs, url);
    }

    private String generateSpikeTestScript(String url, int baseVUs, int spikeVUs) {
        return String.format("""
                import http from 'k6/http';
                import { check, sleep } from 'k6';

                export const options = {
                    stages: [
                        { duration: '30s', target: %d },
                        { duration: '1m',  target: %d },
                        { duration: '10s', target: %d },
                        { duration: '1m',  target: %d },
                        { duration: '30s', target: %d },
                        { duration: '30s', target: 0  },
                    ],
                    thresholds: {
                        'http_req_duration': ['p(95)<2000'],
                        'http_req_failed': ['rate<0.1'],
                    },
                };

                export default function () {
                    const res = http.get('%s');
                    check(res, { 'status is 200': (r) => r.status === 200 });
                    sleep(1);
                }
                """, baseVUs, baseVUs, spikeVUs, spikeVUs, baseVUs, url);
    }

    // ==================== Installation check ====================

    /**
     * Returns {@code true} if the {@code k6} binary is available on {@code PATH}.
     */
    public static boolean isInstalled() {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(isWindows ? "cmd.exe" : "sh", isWindows ? "/c" : "-c", "k6 version");
            Process process = pb.start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Result ====================

    /**
     * Parsed results from a k6 test run.
     */
    public static class K6Result {
        /** {@code true} when k6 exited with code 0 (all thresholds passed). */
        public boolean passed;
        public int exitCode;

        /** Total number of HTTP requests made during the test. */
        public int totalRequests;
        /** P95 response time in milliseconds. */
        public long p95ResponseTime;
        /**
         * Fraction of failed HTTP requests (0–1 scale).
         * E.g. 0.005 = 0.5% failure rate.
         */
        public double httpReqFailed;

        // Additional metrics
        public long   httpReqs;            // same as totalRequests
        public double httpReqDurationAvg;
        public double httpReqDurationP95;  // same as p95ResponseTime, as double
        public long   iterations;
        public int    vusMax;

        public String jsonReportPath;
        public String rawOutput;

        public String getSummary() {
            return String.format(
                    "Requests: %d | Avg: %.2fms | P95: %dms | Failed: %.2f%%",
                    totalRequests, httpReqDurationAvg, p95ResponseTime, httpReqFailed * 100
            );
        }
    }
}
