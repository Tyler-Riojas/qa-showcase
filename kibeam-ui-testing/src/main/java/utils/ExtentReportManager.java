package utils;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/**
 * Singleton manager for Extent Reports
 * Handles report initialization, test creation, and report generation
 * Supports custom report names via system property or setter
 */
public class ExtentReportManager {

    static final Logger log = getLogger(lookup().lookupClass());

    private static ExtentReports extent;
    private static final String REPORT_DIR = System.getProperty("user.dir") + "/reports";
    private static String reportPath;
    private static String reportName = "Test_Report";
    private static String reportTitle = "Test Automation Report";

    // ThreadLocal to support parallel test execution
    private static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    private ExtentReportManager() {
        // Prevent instantiation
    }

    /**
     * Set custom report name (call before getInstance)
     * @param name Report file name prefix (e.g., "Chrome_Tests", "All_Browsers_Tests")
     */
    public static void setReportName(String name) {
        reportName = name.replace(" ", "_");
    }

    /**
     * Set custom report title (shown in the HTML report header)
     * @param title Report title (e.g., "Kibeam Chrome Test Report")
     */
    public static void setReportTitle(String title) {
        reportTitle = title;
    }

    /**
     * Initialize with custom name and title
     * @param name Report file name prefix
     * @param title Report title for HTML header
     */
    public static void initializeReport(String name, String title) {
        setReportName(name);
        setReportTitle(title);
    }

    /**
     * Initialize Extent Reports with default configuration
     */
    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            createInstance();
        }
        return extent;
    }

    /**
     * Create and configure the Extent Reports instance
     */
    private static void createInstance() {
        // Check for system property override
        String sysReportName = System.getProperty("reportName");
        if (sysReportName != null && !sysReportName.isEmpty()) {
            reportName = sysReportName.replace(" ", "_");
        }

        String sysReportTitle = System.getProperty("reportTitle");
        if (sysReportTitle != null && !sysReportTitle.isEmpty()) {
            reportTitle = sysReportTitle;
        }

        // Create reports directory
        File reportDir = new File(REPORT_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
            log.debug("Created reports directory: " + REPORT_DIR);
        }

        // Generate timestamp for unique report name
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        reportPath = REPORT_DIR + "/" + reportName + "_" + timestamp + ".html";

        // Configure Spark Reporter (modern HTML reporter)
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle(reportTitle);
        sparkReporter.config().setReportName(reportTitle);
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        sparkReporter.config().setEncoding("UTF-8");

        // Create Extent Reports instance
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        // Set system info
        extent.setSystemInfo("Project", "Kibeam Test Automation");
        extent.setSystemInfo("Report Type", reportName.replace("_", " "));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("OS Version", System.getProperty("os.version"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("User", System.getProperty("user.name"));

        log.info("Extent Reports initialized: " + reportPath);
        log.info("Report Title: " + reportTitle);
    }

    /**
     * Create a new test in the report
     */
    public static ExtentTest createTest(String testName) {
        ExtentTest test = getInstance().createTest(testName);
        extentTest.set(test);
        return test;
    }

    /**
     * Create a new test with description
     */
    public static ExtentTest createTest(String testName, String description) {
        ExtentTest test = getInstance().createTest(testName, description);
        extentTest.set(test);
        return test;
    }

    /**
     * Get current test for the thread
     */
    public static ExtentTest getTest() {
        return extentTest.get();
    }

    /**
     * Flush and write the report to disk
     */
    public static synchronized void flushReports() {
        if (extent != null) {
            extent.flush();
            log.info("Extent Report generated: " + reportPath);
        }
    }

    /**
     * Get the path to the current report
     */
    public static String getReportPath() {
        return reportPath;
    }

    /**
     * Get the current report name
     */
    public static String getReportName() {
        return reportName;
    }

    /**
     * Remove current test from thread local (cleanup)
     */
    public static void removeTest() {
        extentTest.remove();
    }

    /**
     * Set browser info for the current test
     */
    public static void setBrowserInfo(String browserName) {
        if (extent != null) {
            extent.setSystemInfo("Browser", browserName);
        }
    }

    /**
     * Set device info for the current test
     */
    public static void setDeviceInfo(String deviceName) {
        if (extent != null && deviceName != null && !deviceName.isEmpty()) {
            extent.setSystemInfo("Device", deviceName);
        }
    }

    /**
     * Reset the manager (for starting fresh between suite runs)
     */
    public static synchronized void reset() {
        if (extent != null) {
            extent.flush();
        }
        extent = null;
        reportName = "Test_Report";
        reportTitle = "Test Automation Report";
        extentTest.remove();
    }
}
