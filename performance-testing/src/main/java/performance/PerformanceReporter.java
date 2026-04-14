package performance;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.Markup;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import utils.ExtentReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Robust performance test reporter for Extent Reports integration.
 *
 * <p>Provides rich visualizations for performance metrics:</p>
 * <ul>
 *   <li>Load test dashboards with KPI cards</li>
 *   <li>Response time percentile breakdowns</li>
 *   <li>Network analysis with resource breakdown</li>
 *   <li>Device performance comparison</li>
 *   <li>UI timing metrics (FCP, LCP, TTFB)</li>
 *   <li>Lighthouse audit scores</li>
 * </ul>
 */
public class PerformanceReporter {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String REPORTS_DIR = System.getProperty("user.dir") + "/reports/performance";

    private PerformanceReporter() {
        // Utility class
    }

    // ==================== LOAD TEST REPORTING ====================

    /**
     * Log JMeter load test results to Extent Report with rich visualization.
     */
    public static void logLoadTestResults(JMeterRunner.LoadTestResult result) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) {
            log.debug("No active test context for reporting");
            return;
        }

        // Add test info label
        test.info(MarkupHelper.createLabel("🚀 LOAD TEST: " + result.testName, ExtentColor.CYAN));

        // Summary dashboard
        String summaryHtml = buildLoadTestDashboard(result);
        test.info(createMarkup(summaryHtml));

        // Response time breakdown
        String responseTimeHtml = buildResponseTimeTable(result);
        test.info(createMarkup(responseTimeHtml));

        // Pass/Fail indicator
        if (result.errorRate < 5 && result.p95ResponseTime < 2000) {
            test.pass(MarkupHelper.createLabel("✓ Performance thresholds met", ExtentColor.GREEN));
        } else if (result.errorRate > 20 || result.p95ResponseTime > 5000) {
            test.warning(MarkupHelper.createLabel("⚠ Performance issues detected", ExtentColor.ORANGE));
        }
    }

    /**
     * Build load test dashboard HTML.
     */
    private static String buildLoadTestDashboard(JMeterRunner.LoadTestResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        // Title row
        sb.append("<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;'>");
        sb.append("<span style='color: #4fc3f7; font-size: 16px; font-weight: bold;'>📊 Load Test Dashboard</span>");
        sb.append("<span style='color: #888; font-size: 12px;'>Duration: ").append(result.duration.toSeconds()).append("s | Threads: ").append(result.threads).append("</span>");
        sb.append("</div>");

        // KPI Cards row
        sb.append("<div style='display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px;'>");
        sb.append(buildKpiCard("Total Requests", String.valueOf(result.totalRequests), "📈", "#4fc3f7"));
        sb.append(buildKpiCard("Error Rate", String.format("%.2f%%", result.errorRate), "❌",
                result.errorRate > 5 ? "#f44336" : result.errorRate > 1 ? "#ff9800" : "#4caf50"));
        sb.append(buildKpiCard("Throughput", String.format("%.1f/s", result.throughput), "⚡", "#9c27b0"));
        sb.append(buildKpiCard("P95 Response", result.p95ResponseTime + "ms", "⏱️",
                result.p95ResponseTime > 2000 ? "#f44336" : result.p95ResponseTime > 1000 ? "#ff9800" : "#4caf50"));
        sb.append("</div>");

        // Progress bars for percentiles
        sb.append("<div style='margin-top: 20px;'>");
        sb.append("<div style='color: #888; font-size: 12px; margin-bottom: 10px;'>Response Time Distribution</div>");
        sb.append(buildProgressBar("P50 (Median)", result.p50ResponseTime, result.maxResponseTime, "#4caf50"));
        sb.append(buildProgressBar("P90", result.p90ResponseTime, result.maxResponseTime, "#2196f3"));
        sb.append(buildProgressBar("P95", result.p95ResponseTime, result.maxResponseTime, "#ff9800"));
        sb.append(buildProgressBar("P99", result.p99ResponseTime, result.maxResponseTime, "#f44336"));
        sb.append("</div>");

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Build response time details table.
     */
    private static String buildResponseTimeTable(JMeterRunner.LoadTestResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #1e1e1e; padding: 15px; border-radius: 8px; margin: 10px 0;'>");
        sb.append("<div style='color: #4fc3f7; font-weight: bold; margin-bottom: 10px;'>📋 Response Time Details</div>");

        sb.append("<table style='width: 100%; border-collapse: collapse; color: #fff;'>");
        sb.append("<tr style='background: #333;'>");
        sb.append("<th style='padding: 10px; text-align: left; border-radius: 4px 0 0 0;'>Metric</th>");
        sb.append("<th style='padding: 10px; text-align: right; border-radius: 0 4px 0 0;'>Value</th>");
        sb.append("</tr>");

        sb.append(buildTableRowStyled("Minimum", result.minResponseTime + " ms", "#4caf50"));
        sb.append(buildTableRowStyled("Mean (Average)", result.meanResponseTime + " ms", "#2196f3"));
        sb.append(buildTableRowStyled("Median (P50)", result.p50ResponseTime + " ms", "#4caf50"));
        sb.append(buildTableRowStyled("P90", result.p90ResponseTime + " ms", "#ff9800"));
        sb.append(buildTableRowStyled("P95", result.p95ResponseTime + " ms", "#ff9800"));
        sb.append(buildTableRowStyled("P99", result.p99ResponseTime + " ms", "#f44336"));
        sb.append(buildTableRowStyled("Maximum", result.maxResponseTime + " ms", "#f44336"));
        sb.append(buildTableRowStyled("Data Received", formatBytes(result.receivedBytes), "#9c27b0"));
        sb.append(buildTableRowStyled("Data Sent", formatBytes(result.sentBytes), "#9c27b0"));

        sb.append("</table>");
        sb.append("</div>");
        return sb.toString();
    }

    // ==================== K6 REPORTING ====================

    /**
     * Log k6 test results to Extent Report with rich visualization.
     */
    public static void logK6Results(K6Runner.K6Result result) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) {
            log.debug("No active test context for reporting");
            return;
        }

        ExtentColor labelColor = result.passed ? ExtentColor.CYAN : ExtentColor.RED;
        test.info(MarkupHelper.createLabel("⚡ K6 LOAD TEST", labelColor));

        test.info(createMarkup(buildK6Dashboard(result)));

        if (result.jsonReportPath != null) {
            test.info("📄 JSON report: " + result.jsonReportPath);
        }

        if (result.passed) {
            test.pass(MarkupHelper.createLabel("✓ All k6 thresholds passed", ExtentColor.GREEN));
        } else {
            test.warning(MarkupHelper.createLabel("⚠ One or more k6 thresholds failed", ExtentColor.ORANGE));
        }
    }

    /**
     * Build k6 dashboard HTML.
     */
    private static String buildK6Dashboard(K6Runner.K6Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        // Title row
        sb.append("<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;'>");
        sb.append("<span style='color: #4fc3f7; font-size: 16px; font-weight: bold;'>⚡ k6 Test Dashboard</span>");
        String statusBadge = result.passed
                ? "<span style='background: #4caf50; color: #fff; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold;'>PASSED</span>"
                : "<span style='background: #f44336; color: #fff; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold;'>FAILED</span>";
        sb.append(statusBadge);
        sb.append("</div>");

        // KPI cards
        sb.append("<div style='display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px;'>");

        sb.append(buildKpiCard("Total Requests", String.valueOf(result.totalRequests), "📈", "#4fc3f7"));

        String errorColor = result.httpReqFailed > 0.05 ? "#f44336"
                : result.httpReqFailed > 0.01 ? "#ff9800" : "#4caf50";
        sb.append(buildKpiCard("Error Rate",
                String.format("%.2f%%", result.httpReqFailed * 100), "❌", errorColor));

        String p95Color = result.p95ResponseTime > 1000 ? "#f44336"
                : result.p95ResponseTime > 500 ? "#ff9800" : "#4caf50";
        sb.append(buildKpiCard("P95 Response", result.p95ResponseTime + "ms", "⏱️", p95Color));

        sb.append(buildKpiCard("Avg Response",
                String.format("%.0fms", result.httpReqDurationAvg), "📊", "#9c27b0"));

        sb.append("</div>");

        // Summary row
        sb.append("<div style='margin-top: 15px; padding: 10px; background: rgba(0,0,0,0.3); border-radius: 6px;'>");
        sb.append("<span style='color: #888; font-size: 12px;'>VUs (max): </span>");
        sb.append("<span style='color: #fff; font-size: 12px;'>").append(result.vusMax).append("</span>");
        sb.append("<span style='color: #888; font-size: 12px; margin-left: 20px;'>Iterations: </span>");
        sb.append("<span style='color: #fff; font-size: 12px;'>").append(result.iterations).append("</span>");
        sb.append("</div>");

        sb.append("</div>");
        return sb.toString();
    }

    // ==================== NETWORK METRICS REPORTING ====================

    /**
     * Log network profiler metrics to Extent Report.
     */
    public static void logNetworkMetrics(NetworkProfiler.NetworkMetrics metrics) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) {
            log.debug("No active test context for reporting");
            return;
        }

        test.info(MarkupHelper.createLabel("🌐 NETWORK ANALYSIS", ExtentColor.BLUE));

        String dashboardHtml = buildNetworkDashboard(metrics);
        test.info(createMarkup(dashboardHtml));

        if (!metrics.sizeByType.isEmpty()) {
            String resourceHtml = buildResourceBreakdown(metrics);
            test.info(createMarkup(resourceHtml));
        }

        if (!metrics.slowestRequests.isEmpty()) {
            String slowestHtml = buildSlowestRequests(metrics);
            test.info(createMarkup(slowestHtml));
        }

        if (!metrics.statusCodes.isEmpty()) {
            String statusHtml = buildStatusCodeDistribution(metrics);
            test.info(createMarkup(statusHtml));
        }
    }

    /**
     * Build network dashboard HTML.
     */
    private static String buildNetworkDashboard(NetworkProfiler.NetworkMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        sb.append("<div style='display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px;'>");
        sb.append(buildKpiCard("Requests", String.valueOf(metrics.totalRequests), "📡", "#4fc3f7"));
        sb.append(buildKpiCard("Total Size", metrics.getTotalSizeFormatted(), "💾", "#9c27b0"));
        sb.append(buildKpiCard("Total Time", metrics.totalCaptureTime + "ms", "⏱️", "#ff9800"));
        sb.append(buildKpiCard("P95 Response", metrics.p95ResponseTime + "ms", "📊",
                metrics.p95ResponseTime > 1000 ? "#f44336" : "#4caf50"));
        sb.append("</div>");

        // Network profile info
        if (metrics.networkProfile != null && !metrics.networkProfile.equals("None")) {
            sb.append("<div style='margin-top: 15px; padding: 10px; background: rgba(79, 195, 247, 0.1); border-radius: 6px; border-left: 3px solid #4fc3f7;'>");
            sb.append("<span style='color: #4fc3f7;'>🔗 Network Profile:</span> ");
            sb.append("<span style='color: #fff;'>").append(metrics.networkProfile).append("</span>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Build resource breakdown table.
     */
    private static String buildResourceBreakdown(NetworkProfiler.NetworkMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #1e1e1e; padding: 15px; border-radius: 8px; margin: 10px 0;'>");
        sb.append("<div style='color: #4fc3f7; font-weight: bold; margin-bottom: 10px;'>📦 Resource Breakdown</div>");

        sb.append("<table style='width: 100%; border-collapse: collapse; color: #fff;'>");
        sb.append("<tr style='background: #333;'>");
        sb.append("<th style='padding: 10px; text-align: left;'>Type</th>");
        sb.append("<th style='padding: 10px; text-align: center;'>Count</th>");
        sb.append("<th style='padding: 10px; text-align: right;'>Size</th>");
        sb.append("<th style='padding: 10px; text-align: right;'>% of Total</th>");
        sb.append("</tr>");

        for (Map.Entry<String, Long> entry : metrics.sizeByType.entrySet()) {
            String type = entry.getKey();
            long size = entry.getValue();
            int count = metrics.countByType.getOrDefault(type, 0);
            double percentage = metrics.totalSize > 0 ? (size * 100.0 / metrics.totalSize) : 0;

            sb.append("<tr style='border-bottom: 1px solid #333;'>");
            sb.append("<td style='padding: 10px;'>").append(getResourceIcon(type)).append(" ").append(type).append("</td>");
            sb.append("<td style='padding: 10px; text-align: center;'>").append(count).append("</td>");
            sb.append("<td style='padding: 10px; text-align: right;'>").append(formatBytes(size)).append("</td>");
            sb.append("<td style='padding: 10px; text-align: right;'>");
            sb.append("<div style='display: flex; align-items: center; justify-content: flex-end;'>");
            sb.append("<div style='width: 60px; height: 6px; background: #333; border-radius: 3px; margin-right: 8px;'>");
            sb.append("<div style='width: ").append(Math.min(percentage, 100)).append("%; height: 100%; background: #4fc3f7; border-radius: 3px;'></div>");
            sb.append("</div>");
            sb.append(String.format("%.1f%%", percentage));
            sb.append("</div>");
            sb.append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Build slowest requests section.
     */
    private static String buildSlowestRequests(NetworkProfiler.NetworkMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #1e1e1e; padding: 15px; border-radius: 8px; margin: 10px 0;'>");
        sb.append("<div style='color: #ff9800; font-weight: bold; margin-bottom: 10px;'>🐢 Slowest Requests (Top 5)</div>");

        for (int i = 0; i < Math.min(5, metrics.slowestRequests.size()); i++) {
            NetworkProfiler.RequestTiming req = metrics.slowestRequests.get(i);
            sb.append("<div style='background: #2a2a2a; padding: 10px; margin: 8px 0; border-radius: 6px; display: flex; justify-content: space-between; align-items: center;'>");
            sb.append("<div style='flex: 1; overflow: hidden;'>");
            sb.append("<span style='color: #888; margin-right: 10px;'>#").append(i + 1).append("</span>");
            sb.append("<span style='color: #fff; font-size: 12px;'>").append(req.getShortUrl()).append("</span>");
            sb.append("</div>");
            sb.append("<div style='display: flex; gap: 15px;'>");
            sb.append("<span style='color: #ff9800; font-weight: bold;'>").append(req.time).append("ms</span>");
            sb.append("<span style='color: #9c27b0;'>").append(formatBytes(req.size)).append("</span>");
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Build HTTP status code distribution table.
     */
    private static String buildStatusCodeDistribution(NetworkProfiler.NetworkMetrics metrics) {
        int total = metrics.statusCodes.values().stream().mapToInt(Integer::intValue).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #1e1e1e; padding: 15px; border-radius: 8px; margin: 10px 0;'>");
        sb.append("<div style='color: #4caf50; font-weight: bold; margin-bottom: 10px;'>📋 HTTP Status Code Distribution</div>");

        sb.append("<table style='width: 100%; border-collapse: collapse; color: #fff;'>");
        sb.append("<tr style='background: #333;'>");
        sb.append("<th style='padding: 10px; text-align: left;'>Status Code</th>");
        sb.append("<th style='padding: 10px; text-align: center;'>Count</th>");
        sb.append("<th style='padding: 10px; text-align: right;'>% of Requests</th>");
        sb.append("</tr>");

        metrics.statusCodes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int status = entry.getKey();
                    int count = entry.getValue();
                    double pct = total > 0 ? (count * 100.0 / total) : 0;
                    String color = status < 300 ? "#4caf50" : status < 400 ? "#ff9800" : "#f44336";
                    String label = status < 300 ? "✅" : status < 400 ? "↪️" : "❌";

                    sb.append("<tr style='border-bottom: 1px solid #333;'>");
                    sb.append("<td style='padding: 10px;'>");
                    sb.append("<span style='color: ").append(color).append("; font-weight: bold;'>")
                            .append(label).append(" HTTP ").append(status).append("</span>");
                    sb.append("</td>");
                    sb.append("<td style='padding: 10px; text-align: center; color: #fff;'>").append(count).append("</td>");
                    sb.append("<td style='padding: 10px; text-align: right;'>");
                    sb.append("<div style='display: flex; align-items: center; justify-content: flex-end;'>");
                    sb.append("<div style='width: 60px; height: 6px; background: #333; border-radius: 3px; margin-right: 8px;'>");
                    sb.append("<div style='width: ").append(Math.min(pct, 100))
                            .append("%; height: 100%; background: ").append(color).append("; border-radius: 3px;'></div>");
                    sb.append("</div>");
                    sb.append(String.format("%.1f%%", pct));
                    sb.append("</div>");
                    sb.append("</td>");
                    sb.append("</tr>");
                });

        if (total > 0) {
            int successful = metrics.statusCodes.entrySet().stream()
                    .filter(e -> e.getKey() >= 200 && e.getKey() < 400)
                    .mapToInt(Map.Entry::getValue).sum();
            double successRate = successful * 100.0 / total;
            String rateColor = successRate >= 95 ? "#4caf50" : successRate >= 80 ? "#ff9800" : "#f44336";
            sb.append("<tr style='background: #2a2a2a; font-weight: bold;'>");
            sb.append("<td style='padding: 10px; color: #aaa;'>Success Rate (2xx/3xx)</td>");
            sb.append("<td style='padding: 10px; text-align: center; color: #aaa;'>").append(successful).append("/").append(total).append("</td>");
            sb.append("<td style='padding: 10px; text-align: right; color: ").append(rateColor).append(";'>")
                    .append(String.format("%.1f%%", successRate)).append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</div>");
        return sb.toString();
    }

    // ==================== DEVICE PERFORMANCE REPORTING ====================

    /**
     * Log device performance test results (from a predefined TestScenario).
     */
    public static void logDevicePerformance(DevicePerformanceConfig.TestScenario scenario,
                                            NetworkProfiler.NetworkMetrics metrics,
                                            long pageLoadTime) {
        logDevicePerformance(scenario.getDevice(), scenario.getNetwork(),
                scenario.getDisplayName(), metrics, pageLoadTime);
    }

    /**
     * Log device performance test results (for ad-hoc device/network combinations).
     */
    public static void logDevicePerformance(DevicePerformanceConfig.DeviceProfile device,
                                            DevicePerformanceConfig.NetworkProfile network,
                                            NetworkProfiler.NetworkMetrics metrics,
                                            long pageLoadTime) {
        logDevicePerformance(device, network,
                device.getDisplayName() + " on " + network.getDisplayName(), metrics, pageLoadTime);
    }

    private static void logDevicePerformance(DevicePerformanceConfig.DeviceProfile device,
                                             DevicePerformanceConfig.NetworkProfile network,
                                             String label,
                                             NetworkProfiler.NetworkMetrics metrics,
                                             long pageLoadTime) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) return;

        test.info(MarkupHelper.createLabel("📱 DEVICE PERFORMANCE: " + label, ExtentColor.PURPLE));

        String html = buildDevicePerformanceDashboard(device, network, metrics, pageLoadTime);
        test.info(createMarkup(html));

        // Performance grade
        String grade = getPerformanceGrade(pageLoadTime);
        ExtentColor color = pageLoadTime < 1500 ? ExtentColor.GREEN :
                pageLoadTime < 3000 ? ExtentColor.ORANGE : ExtentColor.RED;
        test.info(MarkupHelper.createLabel("Performance Grade: " + grade, color));
    }

    /**
     * Log viewport responsiveness results for a single device.
     */
    public static void logViewportResult(DevicePerformanceConfig.DeviceProfile viewport,
                                         int targetW, int targetH,
                                         int actualW, int actualH,
                                         long documentWidth) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) return;

        boolean fits = documentWidth <= actualW + 20;
        String html = buildViewportResultCard(viewport, targetW, targetH, actualW, actualH, documentWidth, fits);
        test.info(createMarkup(html));
    }

    /**
     * Build viewport responsiveness result card.
     */
    private static String buildViewportResultCard(DevicePerformanceConfig.DeviceProfile viewport,
                                                   int targetW, int targetH,
                                                   int actualW, int actualH,
                                                   long documentWidth, boolean fits) {
        String fitColor = fits ? "#4caf50" : "#f44336";
        String fitLabel = fits ? "✅ Fits" : "❌ Overflows";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: #1e1e1e; padding: 15px; border-radius: 8px; margin: 6px 0; display: flex; align-items: center; gap: 20px;'>");

        // Device name
        sb.append("<div style='min-width: 160px;'>");
        sb.append("<div style='color: #fff; font-weight: bold; font-size: 13px;'>").append(viewport.getDisplayName()).append("</div>");
        sb.append("<div style='color: #888; font-size: 11px;'>").append(viewport.getType()).append("</div>");
        sb.append("</div>");

        // Target vs Actual
        sb.append("<div style='flex: 1; display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; text-align: center;'>");

        sb.append("<div>");
        sb.append("<div style='color: #888; font-size: 10px; text-transform: uppercase;'>Target</div>");
        sb.append("<div style='color: #4fc3f7; font-size: 13px; font-weight: bold;'>").append(targetW).append(" × ").append(targetH).append("</div>");
        sb.append("</div>");

        sb.append("<div>");
        sb.append("<div style='color: #888; font-size: 10px; text-transform: uppercase;'>Actual</div>");
        sb.append("<div style='color: #fff; font-size: 13px; font-weight: bold;'>").append(actualW).append(" × ").append(actualH).append("</div>");
        sb.append("</div>");

        sb.append("<div>");
        sb.append("<div style='color: #888; font-size: 10px; text-transform: uppercase;'>Doc Width</div>");
        sb.append("<div style='color: #fff; font-size: 13px; font-weight: bold;'>").append(documentWidth).append("px</div>");
        sb.append("</div>");

        sb.append("</div>");

        // Fit status badge
        sb.append("<div style='color: ").append(fitColor).append("; font-weight: bold; font-size: 13px; min-width: 80px; text-align: right;'>").append(fitLabel).append("</div>");

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Build device performance dashboard.
     */
    private static String buildDevicePerformanceDashboard(DevicePerformanceConfig.DeviceProfile device,
                                                          DevicePerformanceConfig.NetworkProfile network,
                                                          NetworkProfiler.NetworkMetrics metrics,
                                                          long pageLoadTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #2d132c 0%, #432371 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        // Device & Network info cards
        sb.append("<div style='display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-bottom: 20px;'>");

        // Device card
        sb.append("<div style='background: rgba(255,255,255,0.1); padding: 15px; border-radius: 8px;'>");
        sb.append("<div style='color: #888; font-size: 11px; text-transform: uppercase; margin-bottom: 5px;'>Device</div>");
        sb.append("<div style='color: #fff; font-size: 18px; font-weight: bold;'>").append(device.getDisplayName()).append("</div>");
        sb.append("<div style='color: #aaa; font-size: 12px; margin-top: 5px;'>");
        sb.append(device.getWidth()).append(" × ").append(device.getHeight()).append(" px");
        sb.append(" • ").append(device.getType());
        sb.append("</div>");
        sb.append("</div>");

        // Network card
        sb.append("<div style='background: rgba(255,255,255,0.1); padding: 15px; border-radius: 8px;'>");
        sb.append("<div style='color: #888; font-size: 11px; text-transform: uppercase; margin-bottom: 5px;'>Network</div>");
        sb.append("<div style='color: #fff; font-size: 18px; font-weight: bold;'>").append(network.getDisplayName()).append("</div>");
        sb.append("<div style='color: #aaa; font-size: 12px; margin-top: 5px;'>");
        sb.append("↓ ").append(network.getDownloadKbps()).append(" Kbps");
        sb.append(" • ↑ ").append(network.getUploadKbps()).append(" Kbps");
        sb.append(" • ").append(network.getLatencyMs()).append("ms latency");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");

        // Performance metrics
        sb.append("<div style='display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px;'>");
        sb.append(buildKpiCard("Page Load", pageLoadTime + "ms", "⏱️",
                pageLoadTime > 3000 ? "#f44336" : pageLoadTime > 1500 ? "#ff9800" : "#4caf50"));
        sb.append(buildKpiCard("Requests", String.valueOf(metrics.totalRequests), "📡", "#4fc3f7"));
        sb.append(buildKpiCard("Page Size", metrics.getTotalSizeFormatted(), "💾", "#9c27b0"));
        sb.append("</div>");

        sb.append("</div>");
        return sb.toString();
    }

    // ==================== UI PERFORMANCE REPORTING ====================

    /**
     * Log UI timing metrics (Navigation Timing API).
     */
    public static void logUITimingMetrics(String pageName, long pageLoadTime, long domContentLoaded,
                                          long ttfb, long dnsLookup, long tcpConnection) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) return;

        test.info(MarkupHelper.createLabel("🌐 UI TIMING: " + pageName, ExtentColor.TEAL));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #134e5e 0%, #71b280 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        // Timing waterfall
        sb.append("<div style='color: #fff; font-weight: bold; margin-bottom: 15px;'>⏱️ Page Load Waterfall</div>");

        long maxTime = pageLoadTime > 0 ? pageLoadTime : 1;

        sb.append(buildWaterfallBar("DNS Lookup", dnsLookup, maxTime, "#4fc3f7"));
        sb.append(buildWaterfallBar("TCP Connection", tcpConnection, maxTime, "#2196f3"));
        sb.append(buildWaterfallBar("Time to First Byte", ttfb, maxTime, "#9c27b0"));
        sb.append(buildWaterfallBar("DOM Content Loaded", domContentLoaded, maxTime, "#ff9800"));
        sb.append(buildWaterfallBar("Page Load Complete", pageLoadTime, maxTime, "#4caf50"));

        sb.append("</div>");

        test.info(createMarkup(sb.toString()));
    }

    /**
     * Log Core Web Vitals metrics.
     */
    public static void logCoreWebVitals(String pageName, Long fcp, Long lcp, Long tbt, Double cls) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) return;

        test.info(MarkupHelper.createLabel("🎯 CORE WEB VITALS: " + pageName, ExtentColor.LIME));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        sb.append("<div style='display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px;'>");

        // FCP
        if (fcp != null) {
            sb.append(buildWebVitalCard("FCP", fcp + "ms", "First Contentful Paint",
                    fcp <= 1800 ? "good" : fcp <= 3000 ? "needs-improvement" : "poor"));
        }

        // LCP
        if (lcp != null) {
            sb.append(buildWebVitalCard("LCP", lcp + "ms", "Largest Contentful Paint",
                    lcp <= 2500 ? "good" : lcp <= 4000 ? "needs-improvement" : "poor"));
        }

        // TBT
        if (tbt != null) {
            sb.append(buildWebVitalCard("TBT", tbt + "ms", "Total Blocking Time",
                    tbt <= 200 ? "good" : tbt <= 600 ? "needs-improvement" : "poor"));
        }

        // CLS
        if (cls != null) {
            sb.append(buildWebVitalCard("CLS", String.format("%.3f", cls), "Cumulative Layout Shift",
                    cls <= 0.1 ? "good" : cls <= 0.25 ? "needs-improvement" : "poor"));
        }

        sb.append("</div>");
        sb.append("</div>");

        test.info(createMarkup(sb.toString()));
    }

    /**
     * Log Lighthouse audit results.
     */
    public static void logLighthouseResults(LighthouseRunner.LighthouseResult result) {
        ExtentTest test = ExtentReportManager.getTest();
        if (test == null) return;

        test.info(MarkupHelper.createLabel("🔦 LIGHTHOUSE AUDIT: " + result.formFactor.toUpperCase(), ExtentColor.AMBER));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 20px; border-radius: 12px; margin: 10px 0;'>");

        // Score gauges
        sb.append("<div style='display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-bottom: 20px;'>");
        sb.append(buildScoreGauge("Performance", result.performanceScore));
        sb.append(buildScoreGauge("Accessibility", result.accessibilityScore));
        sb.append(buildScoreGauge("Best Practices", result.bestPracticesScore));
        sb.append(buildScoreGauge("SEO", result.seoScore));
        sb.append("</div>");

        // Core Web Vitals from Lighthouse
        sb.append("<div style='background: rgba(0,0,0,0.2); padding: 15px; border-radius: 8px;'>");
        sb.append("<div style='color: #fff; font-weight: bold; margin-bottom: 10px;'>Core Web Vitals</div>");
        sb.append("<div style='display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;'>");

        sb.append("<div style='text-align: center;'>");
        sb.append("<div style='color: #aaa; font-size: 11px;'>LCP</div>");
        sb.append("<div style='color: #fff; font-size: 18px; font-weight: bold;'>").append(result.largestContentfulPaint).append("ms</div>");
        sb.append("</div>");

        sb.append("<div style='text-align: center;'>");
        sb.append("<div style='color: #aaa; font-size: 11px;'>FCP</div>");
        sb.append("<div style='color: #fff; font-size: 18px; font-weight: bold;'>").append(result.firstContentfulPaint).append("ms</div>");
        sb.append("</div>");

        sb.append("<div style='text-align: center;'>");
        sb.append("<div style='color: #aaa; font-size: 11px;'>TBT</div>");
        sb.append("<div style='color: #fff; font-size: 18px; font-weight: bold;'>").append(result.totalBlockingTime).append("ms</div>");
        sb.append("</div>");

        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");

        test.info(createMarkup(sb.toString()));

        // Report link
        if (result.htmlReportPath != null) {
            test.info("📄 Full Report: " + result.htmlReportPath);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build KPI card HTML.
     */
    private static String buildKpiCard(String label, String value, String icon, String color) {
        return String.format(
                "<div style='background: rgba(0,0,0,0.3); padding: 15px; border-radius: 8px; text-align: center;'>" +
                        "<div style='font-size: 24px; margin-bottom: 5px;'>%s</div>" +
                        "<div style='color: %s; font-size: 24px; font-weight: bold;'>%s</div>" +
                        "<div style='color: #888; font-size: 11px; text-transform: uppercase; margin-top: 5px;'>%s</div>" +
                        "</div>",
                icon, color, value, label
        );
    }

    /**
     * Build progress bar HTML.
     */
    private static String buildProgressBar(String label, long value, long max, String color) {
        double percentage = max > 0 ? (value * 100.0 / max) : 0;
        return String.format(
                "<div style='margin: 8px 0;'>" +
                        "<div style='display: flex; justify-content: space-between; color: #aaa; font-size: 12px; margin-bottom: 4px;'>" +
                        "<span>%s</span><span>%dms</span>" +
                        "</div>" +
                        "<div style='height: 8px; background: rgba(255,255,255,0.1); border-radius: 4px;'>" +
                        "<div style='width: %.1f%%; height: 100%%; background: %s; border-radius: 4px;'></div>" +
                        "</div>" +
                        "</div>",
                label, value, Math.min(percentage, 100), color
        );
    }

    /**
     * Build waterfall bar HTML.
     * When the bar is too narrow (< 15%), the value is displayed outside the bar to prevent text squishing.
     */
    private static String buildWaterfallBar(String label, long value, long max, String color) {
        double percentage = max > 0 ? (value * 100.0 / max) : 0;
        double displayPercentage = Math.min(percentage, 100);

        // If bar is too narrow, show value outside; otherwise show inside
        boolean showValueOutside = displayPercentage < 15;

        if (showValueOutside) {
            return String.format(
                    "<div style='display: flex; align-items: center; margin: 10px 0;'>" +
                            "<div style='width: 150px; color: #fff; font-size: 12px;'>%s</div>" +
                            "<div style='flex: 1; height: 20px; background: rgba(255,255,255,0.1); border-radius: 4px; margin: 0 10px; position: relative;'>" +
                            "<div style='width: %.1f%%; min-width: 4px; height: 100%%; background: %s; border-radius: 4px;'></div>" +
                            "<span style='position: absolute; left: %.1f%%; margin-left: 8px; top: 50%%; transform: translateY(-50%%); color: #fff; font-size: 11px; font-weight: bold; white-space: nowrap;'>%dms</span>" +
                            "</div>" +
                            "</div>",
                    label, displayPercentage, color, displayPercentage, value
            );
        } else {
            return String.format(
                    "<div style='display: flex; align-items: center; margin: 10px 0;'>" +
                            "<div style='width: 150px; color: #fff; font-size: 12px;'>%s</div>" +
                            "<div style='flex: 1; height: 20px; background: rgba(255,255,255,0.1); border-radius: 4px; margin: 0 10px;'>" +
                            "<div style='width: %.1f%%; height: 100%%; background: %s; border-radius: 4px; display: flex; align-items: center; justify-content: flex-end; padding-right: 8px;'>" +
                            "<span style='color: #fff; font-size: 11px; font-weight: bold;'>%dms</span>" +
                            "</div>" +
                            "</div>" +
                            "</div>",
                    label, displayPercentage, color, value
            );
        }
    }

    /**
     * Build Web Vital card.
     */
    private static String buildWebVitalCard(String name, String value, String description, String status) {
        String bgColor = status.equals("good") ? "rgba(76, 175, 80, 0.3)" :
                status.equals("needs-improvement") ? "rgba(255, 152, 0, 0.3)" : "rgba(244, 67, 54, 0.3)";
        String borderColor = status.equals("good") ? "#4caf50" :
                status.equals("needs-improvement") ? "#ff9800" : "#f44336";

        return String.format(
                "<div style='background: %s; padding: 15px; border-radius: 8px; border-left: 4px solid %s;'>" +
                        "<div style='color: #fff; font-size: 24px; font-weight: bold;'>%s</div>" +
                        "<div style='color: #fff; font-size: 14px; margin: 5px 0;'>%s</div>" +
                        "<div style='color: rgba(255,255,255,0.7); font-size: 11px;'>%s</div>" +
                        "</div>",
                bgColor, borderColor, value, name, description
        );
    }

    /**
     * Build score gauge for Lighthouse.
     */
    private static String buildScoreGauge(String label, int score) {
        String color = score >= 90 ? "#4caf50" : score >= 50 ? "#ff9800" : "#f44336";
        return String.format(
                "<div style='text-align: center;'>" +
                        "<div style='width: 80px; height: 80px; border-radius: 50%%; border: 6px solid %s; display: flex; align-items: center; justify-content: center; margin: 0 auto;'>" +
                        "<span style='color: #fff; font-size: 24px; font-weight: bold;'>%d</span>" +
                        "</div>" +
                        "<div style='color: #fff; font-size: 12px; margin-top: 8px;'>%s</div>" +
                        "</div>",
                color, score, label
        );
    }

    /**
     * Build styled table row.
     */
    private static String buildTableRowStyled(String label, String value, String color) {
        return String.format(
                "<tr style='border-bottom: 1px solid #333;'>" +
                        "<td style='padding: 10px; color: #aaa;'>%s</td>" +
                        "<td style='padding: 10px; text-align: right; color: %s; font-weight: bold;'>%s</td>" +
                        "</tr>",
                label, color, value
        );
    }

    /**
     * Get icon for resource type.
     */
    private static String getResourceIcon(String type) {
        return switch (type.toLowerCase()) {
            case "html" -> "📄";
            case "css" -> "🎨";
            case "javascript" -> "⚡";
            case "images" -> "🖼️";
            case "fonts" -> "🔤";
            case "json" -> "📋";
            case "xml" -> "📰";
            case "video" -> "🎬";
            case "audio" -> "🎵";
            default -> "📦";
        };
    }

    /**
     * Get performance grade based on load time.
     */
    private static String getPerformanceGrade(long loadTime) {
        if (loadTime < 1000) return "A+ (Excellent)";
        if (loadTime < 1500) return "A (Great)";
        if (loadTime < 2500) return "B (Good)";
        if (loadTime < 4000) return "C (Needs Work)";
        if (loadTime < 6000) return "D (Poor)";
        return "F (Critical)";
    }

    /**
     * Format bytes to human readable string.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Create markup from HTML string.
     */
    private static Markup createMarkup(String html) {
        return new Markup() {
            @Override
            public String getMarkup() {
                return html;
            }
        };
    }

    /**
     * Generate standalone HTML report.
     */
    public static void generateHtmlReport(JMeterRunner.LoadTestResult result, String outputPath) throws IOException {
        File reportDir = new File(REPORTS_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Performance Test Report - ").append(result.testName).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; background: #121212; color: #fff; padding: 20px; margin: 0; }\n");
        html.append(".container { max-width: 1200px; margin: 0 auto; }\n");
        html.append("</style>\n</head>\n<body>\n<div class=\"container\">\n");
        html.append("<h1 style=\"color: #4fc3f7;\">🚀 Performance Test Report</h1>\n");
        html.append("<p style=\"color: #888;\">Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</p>\n");
        html.append(buildLoadTestDashboard(result));
        html.append(buildResponseTimeTable(result));
        html.append("</div>\n</body>\n</html>");

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html.toString());
        }

        log.info("Performance report generated: {}", outputPath);
    }

    /**
     * Export results to JSON.
     */
    public static void exportToJson(Object results, String filename) throws IOException {
        File reportDir = new File(REPORTS_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String jsonPath = REPORTS_DIR + "/" + filename + "_" + timestamp + ".json";

        objectMapper.writeValue(new File(jsonPath), results);
        log.info("Results exported to JSON: {}", jsonPath);
    }
}
