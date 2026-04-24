package TestNG.tests;

import base.BaseExtensionTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

/**
 * Validates that the demo extension loads correctly into Chrome and that
 * its structural artefacts (directory, manifest) are present and well-formed.
 */
@Feature("Extension Loading")
public class ExtensionLoadTest extends BaseExtensionTest {

    private static final Logger log = LoggerFactory.getLogger(ExtensionLoadTest.class);

    @Story("Extension Files")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "Verify that the demo-extension directory and manifest exist",
          groups = {"smoke", "extension-load"})
    public void testExtensionDirectoryExists() {
        Allure.step("Check demo-extension directory exists");
        File extDir = new File("demo-extension");
        File manifest = new File("demo-extension/manifest.json");

        assertTrue(extDir.exists() && extDir.isDirectory(),
                "demo-extension directory should exist at: " + extDir.getAbsolutePath());
        assertTrue(manifest.exists(),
                "manifest.json should exist at: " + manifest.getAbsolutePath());
        log.info("Extension directory found: {}", extDir.getAbsolutePath());
    }

    @Story("Extension Loading")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "Verify that Chrome loaded with the extension and driver is active",
          groups = {"smoke", "extension-load"})
    public void testExtensionLoadsInChrome() {
        Allure.step("Verify WebDriver is active after extension load");
        assertNotNull(driver, "Driver should not be null");
        assertNotNull(driver.getWindowHandle(), "Driver should have an active window handle");
        log.info("Chrome is running with extension loaded. Window handle: {}", driver.getWindowHandle());
    }

    @Story("Extension ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Verify that the runtime extension ID was discovered via chrome://extensions",
          groups = {"smoke", "extension-load"})
    public void testExtensionIdDiscovered() {
        Allure.step("Verify extension ID is not null or empty");
        assertNotNull(extensionId, "Extension ID should be discovered");
        assertFalse(extensionId.isEmpty(), "Extension ID should not be empty");
        log.info("Extension ID: {}", extensionId);
    }

    @Story("Manifest Parsing")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Parse manifest.json and verify manifest_version is 3",
          groups = {"smoke", "extension-load"})
    public void testManifestVersion() throws Exception {
        Allure.step("Read and parse demo-extension/manifest.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File("demo-extension/manifest.json"));

        Allure.step("Assert manifest_version == 3");
        assertEquals(root.get("manifest_version").asInt(), 3,
                "Extension should use Manifest V3");
    }

    @Story("Manifest Parsing")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Parse manifest.json and verify extension name",
          groups = {"smoke", "extension-load"})
    public void testExtensionName() throws Exception {
        Allure.step("Read and parse demo-extension/manifest.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File("demo-extension/manifest.json"));

        Allure.step("Assert name == 'QA Demo Extension'");
        assertEquals(root.get("name").asText(), "QA Demo Extension",
                "Extension name should match");
    }
}
