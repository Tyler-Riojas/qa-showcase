package performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.stats.StatsSummary;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

/**
 * JMeter Java DSL runner for programmatic load testing.
 *
 * <p>Provides fluent API for creating and executing load tests:</p>
 * <ul>
 *   <li>HTTP request configuration</li>
 *   <li>Thread group management</li>
 *   <li>Response time assertions</li>
 *   <li>Results export to HTML/JSON</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * JMeterRunner runner = new JMeterRunner();
 *
 * // Simple load test
 * LoadTestResult result = runner
 *     .withBaseUrl("https://api.example.com")
 *     .withThreads(10)
 *     .withDuration(Duration.ofMinutes(1))
 *     .runGetTest("/endpoint");
 *
 * // Assert results
 * assertThat(result.getP99ResponseTime()).isLessThan(2000);
 * </pre>
 */
public class JMeterRunner {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String REPORTS_DIR = System.getProperty("user.dir") + "/reports/jmeter";

    private String baseUrl;
    private int threads = 10;
    private Duration rampUp = Duration.ofSeconds(5);
    private Duration duration = Duration.ofSeconds(30);
    private Duration holdFor = Duration.ZERO;
    private Map<String, String> headers = new HashMap<>();

    /**
     * Set the base URL for all requests.
     */
    public JMeterRunner withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Set the number of concurrent threads (virtual users).
     */
    public JMeterRunner withThreads(int threads) {
        this.threads = threads;
        return this;
    }

    /**
     * Set the ramp-up duration.
     */
    public JMeterRunner withRampUp(Duration rampUp) {
        this.rampUp = rampUp;
        return this;
    }

    /**
     * Set the test duration.
     */
    public JMeterRunner withDuration(Duration duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Set the hold duration after ramp-up.
     */
    public JMeterRunner withHoldFor(Duration holdFor) {
        this.holdFor = holdFor;
        return this;
    }

    /**
     * Add a header to all requests.
     */
    public JMeterRunner withHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /**
     * Add multiple headers to all requests.
     */
    public JMeterRunner withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Build HTTP defaults from baseUrl using individual protocol/host/port setters.
     *
     * Using .url(baseUrl) would parse the URL and set path="/" on the config element.
     * JMeter 5.5's config merging leaves that "/" in play alongside the sampler's path,
     * which can cause the sampler's DOMAIN to remain empty on certain path patterns (e.g.
     * /booking/11). Setting protocol/host/port individually leaves PATH unset on the
     * config element, so the sampler's own path is the only PATH in the tree — the
     * intended behavior.
     */
    private us.abstracta.jmeter.javadsl.http.DslHttpDefaults buildHttpDefaults() {
        URI uri = URI.create(baseUrl);
        us.abstracta.jmeter.javadsl.http.DslHttpDefaults defaults = httpDefaults()
                .protocol(uri.getScheme())
                .host(uri.getHost());
        if (uri.getPort() != -1) {
            defaults.port(uri.getPort());
        }
        return defaults;
    }

    /**
     * Run a GET request load test.
     */
    public LoadTestResult runGetTest(String endpoint) throws IOException {
        return runGetTest(endpoint, "GET " + endpoint);
    }

    /**
     * Run a GET request load test with custom name.
     */
    public LoadTestResult runGetTest(String endpoint, String testName) throws IOException {
        log.info("Starting JMeter load test: {} with {} threads for {}",
                testName, threads, duration);

        TestPlanStats stats = testPlan(
                buildHttpDefaults(),
                httpHeaders().header("Accept", "application/json"),
                threadGroup()
                        .rampTo(threads, rampUp)
                        .holdFor(duration)
                        .children(
                                httpSampler(testName, endpoint)
                        )
        ).run();

        return extractResults(stats, testName);
    }

    /**
     * Run a POST request load test.
     */
    public LoadTestResult runPostTest(String endpoint, String body) throws IOException {
        return runPostTest(endpoint, body, "POST " + endpoint);
    }

    /**
     * Run a POST request load test with custom name.
     */
    public LoadTestResult runPostTest(String endpoint, String body, String testName) throws IOException {
        log.info("Starting JMeter POST load test: {} with {} threads for {}",
                testName, threads, duration);

        TestPlanStats stats = testPlan(
                buildHttpDefaults(),
                httpHeaders()
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json"),
                threadGroup()
                        .rampTo(threads, rampUp)
                        .holdFor(duration)
                        .children(
                                httpSampler(testName, endpoint)
                                        .post(body, org.apache.http.entity.ContentType.APPLICATION_JSON)
                        )
        ).run();

        return extractResults(stats, testName);
    }

    /**
     * Run a custom test plan with multiple endpoints.
     */
    public LoadTestResult runMultiEndpointTest(String testName, Map<String, String> endpoints) throws IOException {
        log.info("Starting JMeter multi-endpoint test: {} with {} threads",
                testName, threads);

        var samplers = endpoints.entrySet().stream()
                .map(entry -> httpSampler(entry.getKey(), entry.getValue()))
                .toArray(us.abstracta.jmeter.javadsl.http.DslHttpSampler[]::new);

        TestPlanStats stats = testPlan(
                buildHttpDefaults(),
                httpHeaders().header("Accept", "application/json"),
                threadGroup()
                        .rampTo(threads, rampUp)
                        .holdFor(duration)
                        .children(samplers)
        ).run();

        return extractResults(stats, testName);
    }

    /**
     * Run a stress test with ramping threads.
     */
    public LoadTestResult runStressTest(String endpoint, int maxThreads, Duration rampDuration) throws IOException {
        log.info("Starting stress test: {} ramping to {} threads over {}",
                endpoint, maxThreads, rampDuration);

        TestPlanStats stats = testPlan(
                buildHttpDefaults(),
                httpHeaders().header("Accept", "application/json"),
                threadGroup()
                        .rampToAndHold(maxThreads, rampDuration, Duration.ofSeconds(30))
                        .children(
                                httpSampler("Stress Test", endpoint)
                        )
        ).run();

        return extractResults(stats, "Stress Test: " + endpoint);
    }

    /**
     * Run a spike test with sudden load increase.
     */
    public LoadTestResult runSpikeTest(String endpoint, int baseThreads, int spikeThreads) throws IOException {
        log.info("Starting spike test: {} from {} to {} threads",
                endpoint, baseThreads, spikeThreads);

        TestPlanStats stats = testPlan(
                buildHttpDefaults(),
                httpHeaders().header("Accept", "application/json"),
                threadGroup()
                        .rampTo(baseThreads, Duration.ofSeconds(10))
                        .holdFor(Duration.ofSeconds(30))
                        .rampTo(spikeThreads, Duration.ofSeconds(5))
                        .holdFor(Duration.ofSeconds(30))
                        .rampTo(baseThreads, Duration.ofSeconds(10))
                        .holdFor(Duration.ofSeconds(30))
                        .children(
                                httpSampler("Spike Test", endpoint)
                        )
        ).run();

        return extractResults(stats, "Spike Test: " + endpoint);
    }

    /**
     * Extract results from JMeter stats.
     */
    private LoadTestResult extractResults(TestPlanStats stats, String testName) {
        LoadTestResult result = new LoadTestResult();
        result.testName = testName;
        result.threads = threads;
        result.duration = duration;

        StatsSummary overall = stats.overall();

        result.totalRequests = overall.samplesCount();
        result.errorCount = overall.errorsCount();
        result.errorRate = overall.errorsCount() * 100.0 / overall.samplesCount();

        result.minResponseTime = overall.sampleTime().min().toMillis();
        result.maxResponseTime = overall.sampleTime().max().toMillis();
        result.meanResponseTime = overall.sampleTime().mean().toMillis();
        result.p50ResponseTime = overall.sampleTime().median().toMillis();
        result.p90ResponseTime = overall.sampleTime().perc90().toMillis();
        result.p95ResponseTime = overall.sampleTime().perc95().toMillis();
        result.p99ResponseTime = overall.sampleTime().perc99().toMillis();

        result.throughput = overall.samplesCount() / duration.toSeconds();

        result.receivedBytes = overall.receivedBytes().total();
        result.sentBytes = overall.sentBytes().total();

        log.info("Load test completed: {} requests, {}% error rate, {} avg response time",
                result.totalRequests, String.format("%.2f", result.errorRate), result.meanResponseTime + "ms");

        return result;
    }

    /**
     * Save results to HTML report.
     */
    public String saveHtmlReport(LoadTestResult result, String filename) throws IOException {
        File reportsDir = new File(REPORTS_DIR);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String reportPath = REPORTS_DIR + "/" + filename + ".html";
        PerformanceReporter.generateHtmlReport(result, reportPath);

        log.info("JMeter HTML report saved: {}", reportPath);
        return reportPath;
    }

    /**
     * Load test result data.
     */
    public static class LoadTestResult {
        public String testName;
        public int threads;
        public Duration duration;

        public long totalRequests;
        public long errorCount;
        public double errorRate;

        public long minResponseTime;
        public long maxResponseTime;
        public long meanResponseTime;
        public long p50ResponseTime;
        public long p90ResponseTime;
        public long p95ResponseTime;
        public long p99ResponseTime;

        public double throughput;
        public long receivedBytes;
        public long sentBytes;

        public boolean meetsThreshold(long maxP95ms, double maxErrorRate) {
            return p95ResponseTime <= maxP95ms && errorRate <= maxErrorRate;
        }

        public String getSummary() {
            return String.format(
                    "Test: %s | Requests: %d | Errors: %.2f%% | P95: %dms | Throughput: %.1f/s",
                    testName, totalRequests, errorRate, p95ResponseTime, throughput
            );
        }
    }
}
