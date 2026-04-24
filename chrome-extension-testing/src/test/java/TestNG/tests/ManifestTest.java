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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

/**
 * Validates the demo extension's {@code manifest.json} without launching a browser.
 *
 * <p>All tests in this class parse the manifest using Jackson and assert on its
 * structural properties. These tests can run in CI without a display/browser.</p>
 */
@Feature("Manifest Validation")
public class ManifestTest extends BaseExtensionTest {

    private static final Logger log = LoggerFactory.getLogger(ManifestTest.class);

    private JsonNode manifest;

    @BeforeClass(alwaysRun = true)
    public void loadManifest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        manifest = mapper.readTree(new File("demo-extension/manifest.json"));
        log.info("Manifest loaded for validation");
    }

    @Story("Manifest File")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "Verify that manifest.json exists on disk",
          groups = {"smoke", "manifest"})
    public void testManifestExists() {
        Allure.step("Check demo-extension/manifest.json exists");
        assertTrue(new File("demo-extension/manifest.json").exists(),
                "manifest.json should exist in demo-extension/");
    }

    @Story("Manifest Version")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Verify the manifest declares Manifest Version 3",
          groups = {"smoke", "manifest"})
    public void testManifestVersion3() {
        Allure.step("Assert manifest_version == 3");
        assertEquals(manifest.get("manifest_version").asInt(), 3,
                "Extension must use Manifest V3");
    }

    @Story("Permissions")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the manifest declares required permissions: activeTab and scripting",
          groups = {"smoke", "manifest"})
    public void testRequiredPermissions() {
        Allure.step("Parse permissions array from manifest");
        JsonNode permissions = manifest.get("permissions");
        assertNotNull(permissions, "permissions array should be present");

        boolean hasActiveTab = false;
        boolean hasScripting = false;
        for (JsonNode perm : permissions) {
            if ("activeTab".equals(perm.asText()))  hasActiveTab = true;
            if ("scripting".equals(perm.asText()))  hasScripting = true;
        }

        Allure.step("Assert 'activeTab' permission present");
        assertTrue(hasActiveTab, "Manifest should declare 'activeTab' permission");
        Allure.step("Assert 'scripting' permission present");
        assertTrue(hasScripting, "Manifest should declare 'scripting' permission");
    }

    @Story("Popup Action")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the manifest declares popup.html as the default_popup",
          groups = {"smoke", "manifest"})
    public void testHasPopupAction() {
        Allure.step("Read action.default_popup from manifest");
        JsonNode action = manifest.get("action");
        assertNotNull(action, "action field should be present in manifest");

        String defaultPopup = action.get("default_popup").asText();
        Allure.step("Assert default_popup == 'popup.html'");
        assertEquals(defaultPopup, "popup.html",
                "action.default_popup should be 'popup.html'");
    }

    @Story("Content Scripts")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Verify the manifest declares at least one content script",
          groups = {"smoke", "manifest"})
    public void testHasContentScript() {
        Allure.step("Read content_scripts array from manifest");
        JsonNode contentScripts = manifest.get("content_scripts");
        assertNotNull(contentScripts, "content_scripts should be present in manifest");

        Allure.step("Assert content_scripts array has at least one entry");
        assertTrue(contentScripts.size() > 0,
                "Manifest should declare at least one content script");
    }
}
