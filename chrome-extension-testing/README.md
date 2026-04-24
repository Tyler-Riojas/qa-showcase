# Chrome Extension Testing Showcase

A complete Maven/TestNG project demonstrating how to load, interact with, and test a Chrome extension using Selenium WebDriver.

---

## What This Demonstrates

| Skill | How It's Demonstrated |
|---|---|
| Loading unpacked extension | `ChromeOptions --load-extension` pointing to `demo-extension/` directory |
| Extension ID discovery | Shadow DOM JavaScript traversal on `chrome://extensions` |
| Popup testing | Navigating directly to `chrome-extension://<id>/popup.html` |
| Content script testing | Banner injection verification on real web pages |
| Manifest validation | Jackson JSON parsing of `manifest.json` without a browser |
| chrome://extensions interaction | Multi-level Shadow DOM traversal via `JavascriptExecutor` |

---

## How Extension Testing Differs From Web Testing

**1. chrome-extension:// URLs (not http://)**
Extension pages are served from a special `chrome-extension://` scheme. You cannot navigate to them with a normal URL — you must build the URL using the runtime extension ID.

**2. Extension IDs are random per-installation**
Chrome assigns a unique ID to each extension installation at startup. The same unpacked extension will have a different ID in each Chrome profile, on each machine, and after each reinstall. The ID must be discovered at runtime, not hardcoded.

**3. Shadow DOM required to inspect chrome://extensions**
Chrome's built-in extensions management page uses nested Shadow DOM trees. Standard `driver.findElement()` calls cannot reach into them. You must use JavaScript with `.shadowRoot` traversal to read extension metadata.

**4. Headless mode is incompatible with Chrome extensions**
The `--headless` flag in Chrome disables extension loading entirely. The `--load-extension` argument is silently ignored. Extension tests must open a real (visible) Chrome window. On Linux CI servers this requires a virtual display (Xvfb).

---

## The Demo Extension

The `demo-extension/` directory contains a minimal Manifest V3 Chrome extension built specifically for this testing showcase.

**What it does:**
- Injects a fixed blue banner at the top of every web page (content script)
- Provides a popup with a status label and a "Run Check" button

**Why it was built for testing:**
- Minimal surface area — only two JS files and one HTML file
- Fully predictable, deterministic behaviour
- Exercises all three main extension features: popup, content script, and manifest
- No external dependencies or API calls

---

## Quick Start

**Prerequisites:**
- Java 17+
- Maven 3.8+
- Chrome browser installed (any recent version)
- No headless mode — tests open a real Chrome window

**Run commands:**

```bash
# Fast validation — manifest checks + extension load (opens Chrome briefly)
mvn test -Pextension-smoke

# Full suite — all 20 tests, opens Chrome
mvn test -Pextension-full

# Popup UI tests only
mvn test -Pextension-popup

# Content script injection tests only
mvn test -Pextension-content
```

**Generate Allure report after running:**
```bash
mvn allure:report
open target/site/allure-maven-plugin/index.html
```

---

## Project Structure

```
chrome-extension-testing/
├── demo-extension/                     # Unpacked Chrome extension
│   ├── manifest.json                   # Manifest V3 declaration
│   ├── popup.html                      # Extension popup UI
│   ├── popup.js                        # Popup button click handler
│   └── content.js                      # Banner injection content script
│
├── src/
│   ├── main/java/
│   │   └── core/
│   │       └── ExtensionDriverFactory.java   # Driver creation + ID discovery
│   │
│   └── test/
│       ├── java/
│       │   ├── base/
│       │   │   └── BaseExtensionTest.java    # Suite lifecycle, screenshot on fail
│       │   └── TestNG/tests/
│       │       ├── ExtensionLoadTest.java    # Extension load + manifest parsing
│       │       ├── PopupUITest.java          # Popup UI interaction
│       │       ├── ContentScriptTest.java    # Banner injection verification
│       │       └── ManifestTest.java         # JSON manifest validation
│       │
│       └── resources/
│           ├── testng-extension-full.xml     # All 4 test classes
│           ├── testng-extension-smoke.xml    # Load + Manifest only
│           ├── testng-extension-popup.xml    # PopupUITest only
│           ├── testng-extension-content.xml  # ContentScriptTest only
│           ├── allure.properties             # Allure results directory
│           └── environment.properties        # Allure environment metadata
│
└── pom.xml
```

---

## Test Coverage

| Class | Tests | What's Tested |
|---|---|---|
| `ExtensionLoadTest` | 5 | Extension directory exists, Chrome loads with extension, runtime ID discovered, manifest version, extension name |
| `PopupUITest` | 5 | Popup title, status label text, button visibility, button click updates result, all data-testid elements present |
| `ContentScriptTest` | 5 | Banner injected on load, banner text, banner not duplicated, banner on multiple pages, banner visible |
| `ManifestTest` | 5 | manifest.json exists, manifest_version 3, required permissions, popup action declared, content script declared |

---

## Key Code Patterns

### a) Loading an extension via ChromeOptions

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--load-extension=" + new File("demo-extension").getAbsolutePath());
// Never add --headless here — extensions are disabled in headless mode
WebDriver driver = new ChromeDriver(options);
```

### b) Discovering the extension ID via Shadow DOM JavaScript

```java
driver.get("chrome://extensions");
Thread.sleep(1500); // allow page to fully render

JavascriptExecutor js = (JavascriptExecutor) driver;
String id = (String) js.executeScript(
    "return document.querySelector('extensions-manager')" +
    "  .shadowRoot.querySelector('extensions-item-list')" +
    "  .shadowRoot.querySelector('extensions-item').id;"
);
```

### c) Navigating to the popup

```java
// Build the URL dynamically using the discovered runtime ID
String popupUrl = "chrome-extension://" + extensionId + "/popup.html";
driver.get(popupUrl);

// Now interact with popup elements normally
WebElement btn = driver.findElement(By.cssSelector("[data-testid='action-btn']"));
btn.click();
```

### d) Verifying content script injection

```java
driver.get("https://example.com");
Thread.sleep(1000); // allow content script to execute after page load

WebElement banner = driver.findElement(By.id("qa-demo-banner"));
assertTrue(banner.isDisplayed());
assertEquals(banner.getText(), "QA Demo Extension Active");
```

---

## Why Extensions Cannot Run Headless

Chrome's headless mode (`--headless` or `--headless=new`) strips out the extension loading infrastructure to reduce memory and startup cost. When `--load-extension` is combined with `--headless`, Chrome silently ignores the extension flag — no error is thrown, the extension simply never loads.

This means:
- The `chrome://extensions` page will show no extensions
- The extension ID cannot be discovered
- `chrome-extension://` URLs will return 404
- Content scripts will not be injected into pages

**For CI/CD on Linux servers:** extensions can still run in a non-headless Chrome if a virtual display is available. Use Xvfb:

```bash
Xvfb :99 -screen 0 1280x1024x24 &
export DISPLAY=:99
mvn test -Pextension-full
```

---

## Run Commands Reference

| Command | Description |
|---|---|
| `mvn test` | Run default suite (extension-full) |
| `mvn test -Pextension-full` | All 20 tests, opens Chrome |
| `mvn test -Pextension-smoke` | ExtensionLoadTest + ManifestTest (10 tests) |
| `mvn test -Pextension-popup` | PopupUITest only (5 tests) |
| `mvn test -Pextension-content` | ContentScriptTest only (5 tests) |
| `mvn compile` | Compile without running tests |
| `mvn allure:report` | Generate Allure HTML report |
| `mvn allure:serve` | Serve Allure report in browser |

---

## Tech Stack + Interview Talking Points

1. **Shadow DOM traversal** — Chrome's `chrome://extensions` page uses multiple layers of Shadow DOM (`extensions-manager` > `extensions-item-list` > `extensions-item`). Standard `querySelector` cannot pierce shadow roots, so `JavascriptExecutor` with `.shadowRoot` chaining is required. This is a real-world skill applicable to any modern web component framework (Salesforce Lightning, Angular Material, etc.).

2. **chrome-extension:// protocol** — Extension pages are not served over HTTP. They use a proprietary scheme that requires the runtime-assigned extension ID. This makes static URL configuration impossible and forces runtime discovery — a pattern common in dynamic web app testing.

3. **Runtime ID discovery** — Unlike a deployed web app that always has the same URL, an unpacked Chrome extension is assigned a new random ID on every installation. Tests must discover this ID programmatically rather than assuming a fixed value. This demonstrates understanding of Chrome's extension sandboxing model.

4. **Content script lifecycle** — Content scripts execute at `document_end` (after the DOM is parsed but before all resources load). Tests must account for a brief execution delay with `Thread.sleep()` or explicit waits, demonstrating knowledge of async injection patterns.

5. **Manifest V3** — This showcase uses Manifest V3, Chrome's current extension platform (MV2 was deprecated in 2023). MV3 restricts background pages to service workers and changes how scripting permissions work. Knowing the difference between MV2 and MV3 is increasingly relevant for QA engineers working with browser extension products.

6. **Why no headless** — A common mistake is attempting to run extension tests with `--headless`. Understanding why this fails (Chrome strips extension loading in headless mode) and knowing the workaround (Xvfb virtual display on Linux) shows real operational experience beyond scripting against simple web apps.

7. **Testing unpacked vs packed extensions** — Unpacked extensions (a directory) are used during development and testing. Packed extensions (`.crx` files) are used for distribution. Testing with unpacked extensions is the standard approach because they can be loaded via `--load-extension` without being signed or published to the Chrome Web Store.

8. **chrome://extensions Shadow DOM structure** — The `extensions-manager` custom element contains `extensions-item-list`, which in turn contains one `extensions-item` per installed extension. Each `extensions-item` has its `id` attribute set to the extension's runtime ID. Navigating this structure requires knowing Chrome's internal DOM architecture, which is a differentiating skill for QA engineers who work with browser automation at a deep level.
