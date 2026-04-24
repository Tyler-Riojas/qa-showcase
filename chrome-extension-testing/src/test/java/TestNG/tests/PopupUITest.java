package TestNG.tests;

import base.BaseExtensionTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Tests the extension's popup UI by navigating directly to the
 * {@code chrome-extension://} popup URL and interacting with its elements.
 */
@Feature("Popup UI")
public class PopupUITest extends BaseExtensionTest {

    private static final Logger log = LoggerFactory.getLogger(PopupUITest.class);

    /**
     * Navigate to the popup before each test instead of about:blank,
     * overriding the base class reset.
     */
    @Override
    @BeforeMethod(alwaysRun = true)
    public void resetState() {
        if (driver != null && popupUrl != null) {
            log.info("Navigating to popup: {}", popupUrl);
            driver.get(popupUrl);
        }
    }

    @Story("Popup Loads")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "Verify the popup page title contains the extension name",
          groups = {"regression", "popup"})
    public void testPopupLoads() {
        Allure.step("Assert page title contains 'QA Demo Extension'");
        assertTrue(driver.getTitle().contains("QA Demo Extension"),
                "Popup title should contain 'QA Demo Extension', was: " + driver.getTitle());
    }

    @Story("Popup Elements")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Verify the status element displays the correct default text",
          groups = {"regression", "popup"})
    public void testStatusElement() {
        Allure.step("Find status element by data-testid");
        WebElement status = driver.findElement(By.cssSelector("[data-testid='status']"));

        Allure.step("Assert status text equals 'Extension is active'");
        assertEquals(status.getText(), "Extension is active",
                "Status element should display 'Extension is active'");
    }

    @Story("Popup Elements")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the action button is present and visible",
          groups = {"regression", "popup"})
    public void testActionButtonPresent() {
        Allure.step("Find action button by data-testid");
        WebElement btn = driver.findElement(By.cssSelector("[data-testid='action-btn']"));

        Allure.step("Assert button is displayed");
        assertTrue(btn.isDisplayed(), "Action button should be visible");
    }

    @Story("Popup Interaction")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Click the action button and verify the result element updates",
          groups = {"regression", "popup"})
    public void testButtonClickUpdatesResult() {
        Allure.step("Click the action button");
        driver.findElement(By.cssSelector("[data-testid='action-btn']")).click();

        Allure.step("Assert result element shows 'Check complete!'");
        WebElement result = driver.findElement(By.cssSelector("[data-testid='result']"));
        assertEquals(result.getText(), "Check complete!",
                "Result element should show 'Check complete!' after button click");
    }

    @Story("Popup Elements")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify all expected data-testid elements are present in the popup",
          groups = {"regression", "popup"})
    public void testAllPopupElements() {
        Allure.step("Collect all elements with data-testid attributes");
        List<WebElement> elements = driver.findElements(By.cssSelector("[data-testid]"));
        List<String> testIds = elements.stream()
                .map(e -> e.getAttribute("data-testid"))
                .collect(Collectors.toList());

        Allure.step("Assert all expected test IDs are present");
        assertTrue(testIds.contains("popup-title"), "popup-title should be present");
        assertTrue(testIds.contains("status"),      "status should be present");
        assertTrue(testIds.contains("action-btn"),  "action-btn should be present");
        assertTrue(testIds.contains("result"),      "result should be present");

        log.info("Found data-testid elements: {}", testIds);
    }
}
