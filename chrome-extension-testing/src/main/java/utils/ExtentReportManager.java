package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Singleton manager for Extent Reports.
 * Handles report initialisation, test creation, and report generation.
 */
public class ExtentReportManager {

    static final Logger log = getLogger(lookup().lookupClass());

    private static ExtentReports extent;
    private static final String REPORT_DIR = System.getProperty("user.dir") + "/reports";
    private static String reportPath;
    private static String reportName = "Extension_Test_Report";
    private static String reportTitle = "Chrome Extension Test Report";

    private static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    private ExtentReportManager() {}

    public static void setReportName(String name) {
        reportName = name.replace(" ", "_");
    }

    public static void setReportTitle(String title) {
        reportTitle = title;
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            createInstance();
        }
        return extent;
    }

    private static void createInstance() {
        String sysReportName = System.getProperty("reportName");
        if (sysReportName != null && !sysReportName.isEmpty()) {
            reportName = sysReportName.replace(" ", "_");
        }

        String sysReportTitle = System.getProperty("reportTitle");
        if (sysReportTitle != null && !sysReportTitle.isEmpty()) {
            reportTitle = sysReportTitle;
        }

        File reportDir = new File(REPORT_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        reportPath = REPORT_DIR + "/" + reportName + "_" + timestamp + ".html";

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle(reportTitle);
        sparkReporter.config().setReportName(reportTitle);
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        sparkReporter.config().setEncoding("UTF-8");

        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        extent.setSystemInfo("Project", "Chrome Extension Testing Showcase");
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("User", System.getProperty("user.name"));

        log.info("Extent Reports initialised: {}", reportPath);
    }

    public static ExtentTest createTest(String testName) {
        ExtentTest test = getInstance().createTest(testName);
        extentTest.set(test);
        return test;
    }

    public static ExtentTest createTest(String testName, String description) {
        ExtentTest test = getInstance().createTest(testName, description);
        extentTest.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return extentTest.get();
    }

    public static synchronized void flushReports() {
        if (extent != null) {
            extent.flush();
            log.info("Extent Report generated: {}", reportPath);
        }
    }

    public static String getReportPath() {
        return reportPath;
    }

    public static String getReportName() {
        return reportName;
    }

    public static void removeTest() {
        extentTest.remove();
    }

    public static void setDeviceInfo(String deviceName) {
        if (extent != null && deviceName != null && !deviceName.isEmpty()) {
            extent.setSystemInfo("Device", deviceName);
        }
    }

    public static synchronized void reset() {
        if (extent != null) {
            extent.flush();
        }
        extent = null;
        reportName = "Extension_Test_Report";
        reportTitle = "Chrome Extension Test Report";
        extentTest.remove();
    }
}
