package base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import pages.DashboardPage;
import pages.LoginPage;
import utils.ActiveDriverHolder;
import utils.ScreenshotUtils;

import java.time.Duration;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Base class for all OrangeHRM tests.
 *
 * <p>Manages the browser lifecycle independently from {@code BaseTestTestNG} because
 * OrangeHRM requires custom ChromeOptions (suppress password-save bubble) and a
 * longer page-load timeout (30 s) that would affect the rest of the framework if
 * applied globally.</p>
 *
 * <p>Each test method gets a fresh browser session — OrangeHRM's AJAX-heavy UI
 * can leave session state that contaminates subsequent tests when sharing a driver.</p>
 *
 * <p>Credentials are the public demo credentials published by OrangeHRM:</p>
 * <ul>
 *   <li>Username: {@code Admin}</li>
 *   <li>Password: {@code admin123}</li>
 * </ul>
 */
public class BaseOrangeHRMTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    protected static final String BASE_URL   = "https://opensource-demo.orangehrmlive.com";
    protected static final String ADMIN_USER = "Admin";
    protected static final String ADMIN_PASS = "admin123";

    /** Shared driver — safe because each test gets its own instance (no parallel=methods). */
    public WebDriver driver;

    protected LoginPage    loginPage;
    protected DashboardPage dashboardPage;

    @BeforeMethod(alwaysRun = true)
    public void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Suppress password-save popups that interrupt form interactions
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-infobars");
        options.addArguments("--remote-allow-origins=*");
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service",        false,
                "profile.password_manager_enabled",  false
        ));

        if ("true".equalsIgnoreCase(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);
        ActiveDriverHolder.set(driver);
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        loginPage     = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);

        loginPage.navigateTo();
        loginPage.login(ADMIN_USER, ADMIN_PASS);

        log.info("Logged in as Admin — dashboard ready");
    }

    /**
     * Re-login with a different user mid-test.
     * Navigates back to the login page without quitting the browser.
     */
    protected void login(String username, String password) {
        loginPage.navigateTo();
        loginPage.login(username, password);
    }

    @AfterMethod(alwaysRun = true)
    public void teardown(ITestResult result) {
        if (driver != null) {
            if (result.getStatus() == ITestResult.FAILURE) {
                ScreenshotUtils.captureScreenshot(
                        driver,
                        result.getMethod().getMethodName() + "_FAILED"
                );
            }
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Exception quitting driver: {}", e.getMessage());
            } finally {
                driver = null;
                ActiveDriverHolder.remove();
            }
        }
    }
}
