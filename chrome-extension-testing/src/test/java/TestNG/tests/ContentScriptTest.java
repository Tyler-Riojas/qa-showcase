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
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests the extension's content script, which injects a blue banner into every
 * web page the user visits.
 *
 * <p>Each test navigates to a real URL and waits briefly for the content script
 * to execute before asserting on the injected DOM elements.</p>
 */
@Feature("Content Script")
public class ContentScriptTest extends BaseExtensionTest {

    private static final Logger log = LoggerFactory.getLogger(ContentScriptTest.class);

    @Story("Banner Injection")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Verify the QA demo banner is injected on page load",
          groups = {"regression", "content-script"})
    public void testBannerInjectedOnLoad() throws InterruptedException {
        Allure.step("Navigate to https://example.com");
        driver.get("https://example.com");
        Thread.sleep(1000); // allow content script to inject

        Allure.step("Find banner element by id");
        WebElement banner = driver.findElement(By.id("qa-demo-banner"));
        assertNotNull(banner, "Banner element should be injected by the content script");
        log.info("Banner injected successfully on example.com");
    }

    @Story("Banner Content")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the injected banner displays the correct text",
          groups = {"regression", "content-script"})
    public void testBannerText() throws InterruptedException {
        Allure.step("Navigate to https://example.com");
        driver.get("https://example.com");
        Thread.sleep(1000);

        Allure.step("Assert banner text equals 'QA Demo Extension Active'");
        assertEquals(driver.findElement(By.id("qa-demo-banner")).getText(),
                "QA Demo Extension Active",
                "Banner should display 'QA Demo Extension Active'");
    }

    @Story("Banner Idempotency")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the banner is not injected more than once on the same page",
          groups = {"regression", "content-script"})
    public void testBannerNotDuplicated() throws InterruptedException {
        Allure.step("Navigate to https://example.com (first visit)");
        driver.get("https://example.com");
        Thread.sleep(1000);

        Allure.step("Navigate to https://example.com again (second visit)");
        driver.get("https://example.com");
        Thread.sleep(500);

        Allure.step("Assert only one banner element exists");
        List<WebElement> banners = driver.findElements(By.id("qa-demo-banner"));
        assertEquals(banners.size(), 1, "Banner should not be duplicated");
    }

    @Story("Multi-Page Injection")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the banner is injected on multiple different pages",
          groups = {"regression", "content-script"})
    public void testBannerOnMultiplePages() throws InterruptedException {
        Allure.step("Navigate to https://example.com");
        driver.get("https://example.com");
        Thread.sleep(1000);
        assertTrue(driver.findElement(By.id("qa-demo-banner")).isDisplayed(),
                "Banner should be visible on example.com");

        Allure.step("Navigate to https://httpbin.org/get");
        driver.get("https://httpbin.org/get");
        Thread.sleep(1500);
        assertTrue(driver.findElement(By.id("qa-demo-banner")).isDisplayed(),
                "Banner should be visible on httpbin.org");

        log.info("Banner confirmed on multiple pages");
    }

    @Story("Banner Visibility")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the injected banner is visible (not hidden)",
          groups = {"regression", "content-script"})
    public void testBannerIsVisible() throws InterruptedException {
        Allure.step("Navigate to https://example.com");
        driver.get("https://example.com");
        Thread.sleep(1000);

        Allure.step("Assert banner is displayed");
        assertTrue(driver.findElement(By.id("qa-demo-banner")).isDisplayed(),
                "Banner should be visible");
    }
}
