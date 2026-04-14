package TestNG.tests.performance;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import performance.JMeterRunner;
import performance.JMeterRunner.LoadTestResult;
import performance.K6Runner;
import performance.PerformanceReporter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API load testing examples using JMeter DSL and k6.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Simple load tests with configurable VUs</li>
 *   <li>Stress testing with ramping load</li>
 *   <li>Spike testing for sudden traffic bursts</li>
 *   <li>Performance threshold assertions</li>
 * </ul>
 *
 * <h2>Run:</h2>
 * <pre>
 * mvn test -P load
 * </pre>
 */
@Feature("API Load Testing")
@Test(groups = {"performance", "load"})
public class ApiLoadTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    // Base URL resolved at runtime from the system property "api.base.url".
    //
    // DEFAULT — localhost:3001 (Docker container):
    //   A controlled, isolated environment. No shared traffic, no rate limiting,
    //   consistent response times. All three thresholds (error rate, p95, throughput)
    //   are meaningful and reliable here. Use this for CI and real assertions.
    //
    //   Start the server:  docker run -d -p 3001:3001 mwinteringham/restfulbooker
    //   Run tests:         mvn test -Pload
    //
    // OVERRIDE — restful-booker.herokuapp.com (shared public server):
    //   A free demo API shared by everyone running tutorials and exercises. Response
    //   times are unpredictable, throughput assertions will flake, and the server
    //   cold-starts on Heroku after inactivity. Useful for demonstrating why
    //   environment control matters — the noise here is the lesson, not a bug.
    //
    //   Run tests:  mvn test -Pload -Dapi.base.url=https://restful-booker.herokuapp.com
    //
    // Any other environment:
    //   mvn test -Pload -Dapi.base.url=https://your-api.example.com
    private static final String BASE_URL = System.getProperty(
            "api.base.url", "http://localhost:3001");

    private static final boolean IS_CI = Boolean.parseBoolean(System.getProperty("ci", "false"));

    // P95 threshold: 500 ms on a local machine, 2000 ms on shared CI runners.
    // Pass -Dci=true when running in GitHub Actions (added to all mvn commands in ci.yml).
    private static final long P95_THRESHOLD_MS = IS_CI ? 2000L : 500L;

    // Throughput threshold: shared CI runners compete with Docker for CPU,
    // so 50 req/s is unachievable there. 20 req/s is a realistic CI floor.
    private static final double THROUGHPUT_THRESHOLD_PER_S = IS_CI ? 20.0 : 50.0;

    private static final int DEFAULT_THREADS = 3;
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(15);

    // Booking ID created in @BeforeClass so GET /booking/{id} tests use a known-good ID.
    // Restful-booker on a fresh Docker container has no pre-seeded bookings — ID 1 is not guaranteed.
    private static int createdBookingId = -1;

    private JMeterRunner jmeterRunner;

    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception {
        log.info("ApiLoadTest target: {} (override with -Dapi.base.url=<url>)", BASE_URL);
        jmeterRunner = new JMeterRunner()
                .withBaseUrl(BASE_URL)
                .withThreads(DEFAULT_THREADS)
                .withDuration(DEFAULT_DURATION)
                .withRampUp(Duration.ofSeconds(5));

        // Create one booking to get a valid ID for GET /booking/{id} load tests.
        // Restful-booker on a fresh Docker container has no pre-seeded data —
        // /booking/1 is not guaranteed to exist. Log status + body so CI failures are visible.
        String bookingJson = "{\"firstname\":\"CI\",\"lastname\":\"Test\",\"totalprice\":100," +
                "\"depositpaid\":true,\"bookingdates\":{\"checkin\":\"2025-01-01\",\"checkout\":\"2025-01-10\"}," +
                "\"additionalneeds\":\"None\"}";
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(BASE_URL + "/booking"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bookingJson))
                .build();
        java.net.http.HttpResponse<String> resp =
                http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        log.info("POST /booking → HTTP {} : {}", resp.statusCode(), resp.body());
        if (resp.statusCode() == 200 || resp.statusCode() == 201) {
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("\"bookingid\":(\\d+)").matcher(resp.body());
            if (m.find()) {
                createdBookingId = Integer.parseInt(m.group(1));
                log.info("Created test booking with ID: {}", createdBookingId);
            } else {
                throw new RuntimeException("POST /booking succeeded but response contained no bookingid: " + resp.body());
            }
        } else {
            throw new RuntimeException("POST /booking failed — HTTP " + resp.statusCode() + ": " + resp.body());
        }

        // Confirm the booking is actually accessible before running any load tests.
        // A successful POST does not guarantee the server can serve the booking under load —
        // if the server is in a degraded state this will fail early with a clear message
        // rather than silently producing 100% error rates in testGetBookingByIdLoad.
        java.net.http.HttpRequest verifyReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(BASE_URL + "/booking/" + createdBookingId))
                .header("Accept", "application/json")
                .GET()
                .build();
        java.net.http.HttpResponse<String> verifyResp =
                http.send(verifyReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (verifyResp.statusCode() != 200) {
            throw new RuntimeException("Pre-flight GET /booking/" + createdBookingId +
                    " returned HTTP " + verifyResp.statusCode() +
                    " — server cannot serve the booking, aborting suite.");
        }
        log.info("Pre-flight check passed: GET /booking/{} → HTTP 200", createdBookingId);

        // Warn early if the database has accumulated too many bookings —
        // a signal that @AfterClass cleanup has not been running after previous suites.
        try {
            java.net.http.HttpRequest countReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/booking"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> countResp =
                    http.send(countReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            java.util.regex.Matcher countMatcher =
                    java.util.regex.Pattern.compile("\"bookingid\"").matcher(countResp.body());
            int count = 0;
            while (countMatcher.find()) count++;
            if (count > 500) {
                log.warn("Pre-run booking count is {} — the container must be reset before running " +
                        "write tests (testCreateBookingLoad). " +
                        "Run: docker restart <container> or docker stop/rm + docker run.", count);
            } else if (count > 100) {
                log.warn("Pre-run booking count is {} — @AfterClass cleanup may not have run after " +
                        "a previous suite. Consider restarting the Docker container.", count);
            } else {
                log.info("Pre-run booking count: {}", count);
            }
        } catch (Exception e) {
            log.warn("Could not check pre-run booking count: {}", e.getMessage());
        }
    }

    /**
     * Delete all bookings from the local test server after the suite completes.
     *
     * <p>{@code testCreateBookingLoad} creates hundreds of bookings per run. Without cleanup
     * these accumulate across runs and slow {@code GET /booking} to unusable speeds.
     *
     * <p>Only runs when {@code BASE_URL} contains {@code localhost} — shared environments
     * (Heroku, staging) are never wiped.
     */
    @AfterClass(alwaysRun = true)
    public void cleanup() {
        if (!BASE_URL.contains("localhost")) {
            log.info("Skipping cleanup — not a localhost environment ({})", BASE_URL);
            return;
        }

        try {
            java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();

            // Step 1: Obtain an auth token (required for DELETE /booking/{id})
            java.net.http.HttpRequest authReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/auth"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            "{\"username\":\"admin\",\"password\":\"password123\"}"))
                    .build();
            java.net.http.HttpResponse<String> authResp =
                    http.send(authReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            java.util.regex.Matcher tokenMatcher =
                    java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(authResp.body());
            if (!tokenMatcher.find()) {
                log.warn("Cleanup skipped — could not obtain auth token (HTTP {}): {}",
                        authResp.statusCode(), authResp.body());
                return;
            }
            String token = tokenMatcher.group(1);

            // Step 2: Retrieve all current booking IDs
            java.net.http.HttpRequest listReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/booking"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> listResp =
                    http.send(listReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            java.util.regex.Matcher idMatcher =
                    java.util.regex.Pattern.compile("\"bookingid\":(\\d+)").matcher(listResp.body());
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            while (idMatcher.find()) {
                ids.add(Integer.parseInt(idMatcher.group(1)));
            }
            log.info("Cleanup: found {} bookings to delete", ids.size());

            // Step 3: Delete every booking.
            // restful-booker returns 201 (not 200) on a successful DELETE — known API quirk.
            int deleted = 0;
            for (int id : ids) {
                java.net.http.HttpRequest deleteReq = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(BASE_URL + "/booking/" + id))
                        .header("Cookie", "token=" + token)
                        .DELETE()
                        .build();
                java.net.http.HttpResponse<Void> deleteResp =
                        http.send(deleteReq, java.net.http.HttpResponse.BodyHandlers.discarding());
                if (deleteResp.statusCode() == 201) {
                    deleted++;
                } else {
                    log.warn("DELETE /booking/{} → HTTP {} (expected 201)", id, deleteResp.statusCode());
                }
            }
            log.info("Cleanup complete: deleted {}/{} bookings", deleted, ids.size());

        } catch (Exception e) {
            log.warn("Cleanup failed and will not block test results: {}", e.getMessage());
        }
    }

    @Story("JMeter Load Tests")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Load test GET /booking endpoint")
    public void testGetBookingsLoad() throws IOException {
        Allure.step("Run JMeter load test against GET /booking with " + DEFAULT_THREADS + " threads for " + DEFAULT_DURATION.getSeconds() + "s");
        LoadTestResult result = jmeterRunner.runGetTest("/booking", "Get All Bookings");

        Allure.step("Log load test results and assert error rate, P95, and throughput thresholds");
        // Log to Extent Report
        PerformanceReporter.logLoadTestResults(result);

        assertThat(result.errorRate)
                .as("Error rate must be under 1%% — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under %d ms — got %d ms", P95_THRESHOLD_MS, result.p95ResponseTime)
                .isLessThan(P95_THRESHOLD_MS);

        assertThat(result.throughput)
                .as("Throughput must exceed %.0f req/s — got %.1f req/s", THROUGHPUT_THRESHOLD_PER_S, result.throughput)
                .isGreaterThan(THROUGHPUT_THRESHOLD_PER_S);

        log.info("Load test completed: {}", result.getSummary());
    }

    @Story("JMeter Load Tests")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Load test GET /booking/{id} endpoint")
    public void testGetBookingByIdLoad() throws IOException {
        Allure.step("Run JMeter load test against GET /booking/" + createdBookingId + " with " + DEFAULT_THREADS + " threads");
        LoadTestResult result = jmeterRunner.runGetTest("/booking/" + createdBookingId, "Get Booking By ID");

        Allure.step("Log load test results and assert error rate, P95, and throughput thresholds");
        PerformanceReporter.logLoadTestResults(result);

        assertThat(result.errorRate)
                .as("Error rate must be under 1%% — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under %d ms — got %d ms", P95_THRESHOLD_MS, result.p95ResponseTime)
                .isLessThan(P95_THRESHOLD_MS);

        assertThat(result.throughput)
                .as("Throughput must exceed %.0f req/s — got %.1f req/s", THROUGHPUT_THRESHOLD_PER_S, result.throughput)
                .isGreaterThan(THROUGHPUT_THRESHOLD_PER_S);

        log.info("Load test completed: {}", result.getSummary());
    }

    // POST load tests create hundreds of persistent bookings per run. They should only be
    // executed against a freshly reset Docker container — never as part of regular CI runs
    // or back-to-back with other load tests without a container restart in between.
    // Excluded from -Pload via testngLoadTest.xml (<exclude name="load-write"/>).
    // To run explicitly: mvn test -Dtest=ApiLoadTest#testCreateBookingLoad
    @Story("JMeter Load Tests")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Load test POST /booking endpoint", groups = {"performance", "load-write"})
    public void testCreateBookingLoad() throws IOException {
        String bookingBody = """
                {
                    "firstname": "LoadTest",
                    "lastname": "User",
                    "totalprice": 100,
                    "depositpaid": true,
                    "bookingdates": {
                        "checkin": "2024-01-01",
                        "checkout": "2024-01-10"
                    },
                    "additionalneeds": "Breakfast"
                }
                """;

        Allure.step("Run JMeter POST load test against /booking with 3 threads");
        LoadTestResult result = jmeterRunner
                .withThreads(3)
                .runPostTest("/booking", bookingBody, "Create Booking");

        Allure.step("Log load test results and assert error rate and P95 thresholds (no throughput for write endpoint)");
        PerformanceReporter.logLoadTestResults(result);

        // POST (write) endpoints are slower than reads — no throughput assertion.
        // Error rate and p95 still apply: writes must be reliable and responsive
        // even if they cannot match the raw req/s of a GET endpoint.
        assertThat(result.errorRate)
                .as("Error rate must be under 1%% — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under %d ms — got %d ms", P95_THRESHOLD_MS, result.p95ResponseTime)
                .isLessThan(P95_THRESHOLD_MS);

        log.info("Load test completed: {}", result.getSummary());
    }

    @Story("JMeter Load Tests")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Multi-endpoint load test")
    public void testMultiEndpointLoad() throws IOException {
        Map<String, String> endpoints = Map.of(
                "Get Bookings", "/booking",
                "Get Booking By ID", "/booking/" + createdBookingId,
                "Health Check", "/ping"
        );

        // CI shared runners compete with Docker for CPU — 2 threads keeps the
        // test meaningful without exhausting the runner. Local uses 5 threads.
        int threads = IS_CI ? 2 : 5;
        Allure.step("Run multi-endpoint JMeter load test across /booking, /booking/{id}, /ping with " + threads + " threads");
        LoadTestResult result = jmeterRunner
                .withThreads(threads)
                .withDuration(Duration.ofSeconds(20))
                .runMultiEndpointTest("Multi-Endpoint Test", endpoints);

        Allure.step("Log multi-endpoint results and assert error rate, P95, and throughput thresholds");
        PerformanceReporter.logLoadTestResults(result);

        assertThat(result.errorRate)
                .as("Error rate must be under 1%% — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under %d ms — got %d ms", P95_THRESHOLD_MS, result.p95ResponseTime)
                .isLessThan(P95_THRESHOLD_MS);

        assertThat(result.throughput)
                .as("Throughput must exceed %.0f req/s — got %.1f req/s", THROUGHPUT_THRESHOLD_PER_S, result.throughput)
                .isGreaterThan(THROUGHPUT_THRESHOLD_PER_S);

        log.info("Multi-endpoint test completed: {}", result.getSummary());
    }

    @Story("JMeter Stress Tests")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Stress test with increasing load")
    public void testStressTest() throws IOException {
        Allure.step("Run JMeter stress test against /booking ramping up to 10 threads over 20s");
        LoadTestResult result = jmeterRunner.runStressTest(
                "/booking",
                10, // max threads (reduced for demo API)
                Duration.ofSeconds(20)
        );

        Allure.step("Log stress test results and assert error rate stays under 1% at peak load");
        PerformanceReporter.logLoadTestResults(result);

        // Stress tests intentionally push beyond steady-state capacity — p95 and
        // throughput will degrade under peak load. Only error rate is asserted here:
        // even at maximum thread count the API must not return errors at scale,
        // it should slow down (high p95) rather than fail (high error rate).
        assertThat(result.errorRate)
                .as("Error rate must stay under 1%% even under stress — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        log.info("Stress test completed: {} requests at peak load", result.totalRequests);
    }

    @Story("JMeter Stress Tests")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Spike test with sudden load increase")
    public void testSpikeTest() throws IOException {
        Allure.step("Run JMeter spike test against /booking: 3 base threads spiking to 10 threads");
        LoadTestResult result = jmeterRunner.runSpikeTest(
                "/booking",
                3,  // base threads (reduced for demo API)
                10  // spike threads (reduced for demo API)
        );

        Allure.step("Log spike test results and assert error rate stays under 1% during traffic burst");
        PerformanceReporter.logLoadTestResults(result);

        // Spike tests introduce sudden traffic bursts — transient p95 spikes are
        // expected and acceptable. Error rate is still asserted: the API must absorb
        // the spike without returning errors, even if response times increase sharply.
        assertThat(result.errorRate)
                .as("Error rate must stay under 1%% during spike — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        log.info("Spike test completed: {} requests", result.totalRequests);
    }

    @Story("K6 Load Tests")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "K6 simple load test — booking-load-test.js", groups = {"performance", "k6"})
    public void testK6SimpleLoad() throws IOException, InterruptedException {
        Allure.step("Run K6 simple load test using booking-load-test.js script");
        if (!K6Runner.isInstalled()) {
            log.warn("K6 not installed, skipping test");
            return;
        }

        K6Runner.K6Result result = new K6Runner()
                .withScript("booking-load-test.js")
                .withBaseUrl(BASE_URL)
                .runScript();

        Allure.step("Log K6 results and assert all thresholds pass: error rate <1%, P95 <500ms");
        PerformanceReporter.logK6Results(result);

        assertThat(result.passed)
                .as("k6 must exit 0 — all thresholds must pass")
                .isTrue();

        assertThat(result.httpReqFailed)
                .as("Error rate must be under 1%% — got %.4f", result.httpReqFailed)
                .isLessThan(0.01);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under 500 ms — got %d ms", result.p95ResponseTime)
                .isLessThan(500);

        log.info("K6 simple load test completed: {}", result.getSummary());
    }

    @Story("K6 Stress Tests")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "K6 stress test — booking-stress-test.js", groups = {"performance", "k6"})
    public void testK6StressTest() throws IOException, InterruptedException {
        Allure.step("Run K6 stress test using booking-stress-test.js script");
        if (!K6Runner.isInstalled()) {
            log.warn("K6 not installed, skipping test");
            return;
        }

        K6Runner.K6Result result = new K6Runner()
                .withScript("booking-stress-test.js")
                .withBaseUrl(BASE_URL)
                .runScript();

        Allure.step("Log K6 stress test results and assert error rate <1%, P95 <500ms under stress");
        PerformanceReporter.logK6Results(result);

        assertThat(result.passed)
                .as("k6 must exit 0 — all thresholds must pass")
                .isTrue();

        assertThat(result.httpReqFailed)
                .as("Error rate must stay under 1%% even under stress — got %.4f", result.httpReqFailed)
                .isLessThan(0.01);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under 500 ms — got %d ms", result.p95ResponseTime)
                .isLessThan(500);

        log.info("K6 stress test completed: {}", result.getSummary());
    }

    @Story("K6 Spike Tests")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "K6 spike test — booking-spike-test.js", groups = {"performance", "k6"})
    public void testK6SpikeTest() throws IOException, InterruptedException {
        Allure.step("Run K6 spike test using booking-spike-test.js script");
        if (!K6Runner.isInstalled()) {
            log.warn("K6 not installed, skipping test");
            return;
        }

        K6Runner.K6Result result = new K6Runner()
                .withScript("booking-spike-test.js")
                .withBaseUrl(BASE_URL)
                .runScript();

        Allure.step("Log K6 spike test results and assert error rate <1%, P95 <1500ms (spike tolerance)");
        PerformanceReporter.logK6Results(result);

        // The spike script's own threshold is p(95)<1500 — transient latency during
        // the burst window is expected and acceptable. The Java-level assertion
        // mirrors this: if the overall P95 exceeds 1500 ms the server has not
        // recovered and the test should fail.
        // Error rate must stay under 1% throughout: the server should slow down
        // under the spike, not return errors.
        assertThat(result.passed)
                .as("k6 must exit 0 — all thresholds must pass")
                .isTrue();

        assertThat(result.httpReqFailed)
                .as("Error rate must stay under 1%% during spike — got %.4f", result.httpReqFailed)
                .isLessThan(0.01);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must recover to under 1500 ms — got %d ms", result.p95ResponseTime)
                .isLessThan(1500);

        log.info("K6 spike test completed: {}", result.getSummary());
    }

    @Story("JMeter Load Tests")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Verify performance thresholds")
    public void testPerformanceThresholds() throws IOException {
        Allure.step("Run JMeter load test against /ping health-check endpoint with 3 threads for 10s");
        LoadTestResult result = jmeterRunner
                .withThreads(3)
                .withDuration(Duration.ofSeconds(10))
                .runGetTest("/ping", "Health Check Load");

        Allure.step("Log health-check results and assert all three performance thresholds");
        PerformanceReporter.logLoadTestResults(result);

        // /ping is a lightweight health-check endpoint — it should easily meet all
        // three thresholds and is the clearest signal that the API is healthy under load.
        assertThat(result.errorRate)
                .as("Error rate must be under 1%% — got %.2f%%", result.errorRate)
                .isLessThan(1.0);

        assertThat(result.p95ResponseTime)
                .as("P95 response time must be under %d ms — got %d ms", P95_THRESHOLD_MS, result.p95ResponseTime)
                .isLessThan(P95_THRESHOLD_MS);

        assertThat(result.throughput)
                .as("Throughput must exceed %.0f req/s — got %.1f req/s", THROUGHPUT_THRESHOLD_PER_S, result.throughput)
                .isGreaterThan(THROUGHPUT_THRESHOLD_PER_S);

        log.info("Health check load test: {}", result.getSummary());
    }
}
