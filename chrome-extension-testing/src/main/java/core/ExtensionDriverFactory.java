package core;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Factory for creating a Chrome WebDriver with the demo extension loaded.
 *
 * <p>Key behaviours:</p>
 * <ul>
 *   <li>Loads the unpacked extension via {@code --load-extension} ChromeOption.</li>
 *   <li>NEVER adds {@code --headless} — Chrome extensions do not work in headless mode.</li>
 *   <li>Google Chrome stable blocks {@code --load-extension} (compile-time
 *       {@code CHROMIUM_BRANDING} check in {@code extension_service.cc}). This factory
 *       automatically downloads <em>Chrome for Testing</em> from Google's CDN, which lifts
 *       the restriction, and caches it under {@code ~/.cache/chrome-for-testing/}.</li>
 *   <li>Discovers the extension ID from {@code chrome://extensions-internals} JSON output,
 *       cross-referencing the demo-extension directory path. Falls back to filesystem and
 *       shadow-DOM scans.</li>
 *   <li>Exposes the extension ID and popup URL as static getters for use in tests.</li>
 * </ul>
 */
public class ExtensionDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(ExtensionDriverFactory.class);

    /** Absolute path to the unpacked demo-extension directory. */
    private static final String EXTENSION_PATH = new File("demo-extension").getAbsolutePath();

    /** Root cache directory for Chrome for Testing binaries. */
    private static final Path CFT_CACHE = Paths.get(System.getProperty("user.home"), ".cache", "chrome-for-testing");

    /** Chrome for Testing version/downloads JSON from Google. */
    private static final String CFT_API_URL =
        "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json";

    /** Matches exactly 32 lowercase letters (Chrome extension ID format). */
    private static final Pattern EXT_ID_PATTERN = Pattern.compile("^[a-z]{32}$");

    /** Matches a 32-lowercase-letter extension ID within a larger string. */
    private static final Pattern EXT_ID_IN_TEXT = Pattern.compile("(?<![a-z])[a-z]{32}(?![a-z])");

    /** Discovered runtime extension ID — populated by {@link #discoverExtensionId(WebDriver)}. */
    private static String extensionId;

    /** Chrome for Testing version resolved during setup (used to match ChromeDriver version). */
    private static volatile String cftVersion;

    // ==================== Public API ====================

    /**
     * Creates a ChromeDriver with the demo extension loaded and discovers its runtime ID.
     *
     * <p>Automatically downloads Chrome for Testing if it is not already cached, so that
     * {@code --load-extension} is honoured. Falls back to system Chrome or a user-supplied
     * binary path ({@code CHROME_FOR_TESTING_PATH} env var or
     * {@code -Dchrome.for.testing.path} system property) if the auto-download fails.</p>
     *
     * @return a ready-to-use {@link WebDriver} instance
     */
    public static WebDriver createDriver() {
        // Resolve Chrome for Testing binary FIRST so we know its version,
        // then set up the matching ChromeDriver.
        String cftBinary = resolveChromeForTestingBinary();

        log.info("Setting up ChromeDriver via WebDriverManager");
        WebDriverManager wdm = WebDriverManager.chromedriver();
        if (cftVersion != null) {
            log.info("Using ChromeDriver version {} to match Chrome for Testing", cftVersion);
            wdm.browserVersion(cftVersion);
        }
        wdm.setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--load-extension=" + EXTENSION_PATH);
        options.addArguments("--disable-gpu");

        if (cftBinary != null) {
            log.info("Using Chrome for Testing binary: {}", cftBinary);
            options.setBinary(cftBinary);
        } else {
            log.warn("Chrome for Testing binary not available — system Chrome will be used. " +
                     "--load-extension may be blocked by Chrome stable. " +
                     "Set CHROME_FOR_TESTING_PATH env var to override.");
        }

        // Enable Developer Mode in the ChromeDriver profile (belt-and-suspenders)
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("extensions.ui.developer_mode", true);
        options.setExperimentalOption("prefs", prefs);

        log.info("Loading extension from: {}", EXTENSION_PATH);
        WebDriver driver = new ChromeDriver(options);

        // Give Chrome time to fully register the extension
        try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        extensionId = discoverExtensionId(driver);
        return driver;
    }

    /**
     * Quits the driver if it is not null.
     *
     * @param driver the WebDriver to quit (may be null)
     */
    public static void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver quit successfully");
            } catch (Exception e) {
                log.warn("Error quitting WebDriver: {}", e.getMessage());
            }
        }
    }

    /** Returns the discovered runtime extension ID. */
    public static String getExtensionId() { return extensionId; }

    /** Returns the {@code chrome-extension://} URL for the popup page. */
    public static String getPopupUrl() { return "chrome-extension://" + extensionId + "/popup.html"; }

    // ==================== Chrome for Testing resolution ====================

    /**
     * Resolves the Chrome for Testing binary path in priority order:
     * <ol>
     *   <li>System property {@code chrome.for.testing.path}</li>
     *   <li>Environment variable {@code CHROME_FOR_TESTING_PATH}</li>
     *   <li>Local cache ({@code ~/.cache/chrome-for-testing/})</li>
     *   <li>Auto-download from Google's CDN</li>
     * </ol>
     *
     * @return absolute path to the binary, or {@code null} if unavailable
     */
    private static String resolveChromeForTestingBinary() {
        // 1. Explicit override via system property or env var
        String prop = System.getProperty("chrome.for.testing.path");
        if (prop != null && !prop.isBlank() && Files.exists(Paths.get(prop))) {
            log.info("Chrome for Testing binary (from system property): {}", prop);
            return prop;
        }
        String env = System.getenv("CHROME_FOR_TESTING_PATH");
        if (env != null && !env.isBlank() && Files.exists(Paths.get(env))) {
            log.info("Chrome for Testing binary (from env var): {}", env);
            return env;
        }

        // 2. Check cache, then download if not present
        try {
            String platform = detectPlatform();
            Path binaryPath = cachedBinaryPath(platform);
            if (Files.exists(binaryPath)) {
                log.info("Chrome for Testing already cached: {}", binaryPath);
                // Determine version from the path (parent dir structure: chrome-mac-arm64/{version}/...)
                // or from running the binary; simpler: read version from CFT_CACHE marker file
                Path versionFile = CFT_CACHE.resolve("VERSION");
                if (Files.exists(versionFile)) {
                    cftVersion = Files.readString(versionFile).trim();
                    log.info("Chrome for Testing version from cache: {}", cftVersion);
                }
                return binaryPath.toString();
            }
            return downloadChromeForTesting(platform, binaryPath);
        } catch (Exception e) {
            log.warn("Chrome for Testing resolution failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Detects the current platform string used by Google's Chrome for Testing downloads.
     *
     * @return one of {@code mac-arm64}, {@code mac-x64}, {@code linux64}
     * @throws UnsupportedOperationException if the platform is not supported
     */
    private static String detectPlatform() {
        String os   = System.getProperty("os.name", "").toLowerCase();
        String arch  = System.getProperty("os.arch", "").toLowerCase();
        if (os.contains("mac")) {
            return (arch.contains("aarch64") || arch.contains("arm")) ? "mac-arm64" : "mac-x64";
        }
        if (os.contains("linux")) return "linux64";
        throw new UnsupportedOperationException("Unsupported platform for auto-download: " + os + "/" + arch);
    }

    /**
     * Returns the expected binary path within the cache directory for the given platform.
     *
     * <p>After extracting the Chrome for Testing zip, the binary is at:</p>
     * <ul>
     *   <li>macOS: {@code chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing}</li>
     *   <li>Linux: {@code chrome-linux64/chrome}</li>
     * </ul>
     */
    private static Path cachedBinaryPath(String platform) {
        if (platform.startsWith("mac")) {
            return CFT_CACHE.resolve("chrome-" + platform)
                            .resolve("Google Chrome for Testing.app")
                            .resolve("Contents").resolve("MacOS")
                            .resolve("Google Chrome for Testing");
        }
        return CFT_CACHE.resolve("chrome-" + platform).resolve("chrome");
    }

    /**
     * Downloads Chrome for Testing from Google's CDN, extracts the zip, and fixes permissions.
     *
     * @param platform   the platform string (e.g. {@code mac-arm64})
     * @param binaryPath the expected binary path after extraction
     * @return the binary path as a string, or {@code null} if download failed
     */
    private static String downloadChromeForTesting(String platform, Path binaryPath) {
        try {
            log.info("Fetching Chrome for Testing version info from {}", CFT_API_URL);
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String versionJson = http.send(
                    HttpRequest.newBuilder(URI.create(CFT_API_URL)).build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();

            // Extract version and download URL for our platform from the JSON
            Pattern verPat = Pattern.compile("\"Stable\"\\s*:\\s*\\{[^}]*?\"version\"\\s*:\\s*\"([^\"]+)\"");
            Matcher vm = verPat.matcher(versionJson);
            if (vm.find()) {
                cftVersion = vm.group(1);
                log.info("Chrome for Testing stable version: {}", cftVersion);
            }

            Pattern urlPat = Pattern.compile(
                    "\"platform\"\\s*:\\s*\"" + Pattern.quote(platform) + "\"" +
                    "[^}]*?\"url\"\\s*:\\s*\"(https://[^\"]+chrome-" + Pattern.quote(platform) + "\\.zip)\"");
            Matcher um = urlPat.matcher(versionJson);
            if (!um.find()) {
                log.warn("Could not find download URL for platform '{}' in CfT JSON", platform);
                return null;
            }
            String downloadUrl = um.group(1);
            log.info("Downloading Chrome for Testing from: {}", downloadUrl);

            byte[] zipBytes = http.send(
                    HttpRequest.newBuilder(URI.create(downloadUrl)).build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            ).body();
            log.info("Downloaded {} MB", zipBytes.length / 1_048_576);

            Files.createDirectories(CFT_CACHE);
            extractZip(zipBytes, CFT_CACHE);

            // Fix execute permissions on macOS/Linux
            // On macOS the .app bundle contains many helper executables — use chmod -R +x
            if (platform.startsWith("mac")) {
                Path appDir = CFT_CACHE.resolve("chrome-" + platform)
                                       .resolve("Google Chrome for Testing.app");
                chmodRecursive(appDir);
            } else if (!platform.equals("win64")) {
                makeExecutable(binaryPath);
            }

            // Remove macOS Gatekeeper quarantine (prevents "app can't be opened" errors)
            if (platform.startsWith("mac")) {
                removeQuarantine(CFT_CACHE.resolve("chrome-" + platform));
            }

            // Save version to a marker file for future cache hits
            if (cftVersion != null) {
                Files.writeString(CFT_CACHE.resolve("VERSION"), cftVersion);
            }

            if (Files.exists(binaryPath)) {
                log.info("Chrome for Testing ready: {}", binaryPath);
                return binaryPath.toString();
            }
            log.warn("Binary not found after extraction: {}", binaryPath);
            return null;

        } catch (Exception e) {
            log.warn("Failed to download Chrome for Testing: {}", e.getMessage());
            return null;
        }
    }

    /** Extracts a zip archive byte array to the given directory. */
    private static void extractZip(byte[] zipBytes, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = destDir.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(destDir)) continue; // zip-slip guard
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    Files.write(outPath, zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Recursively sets the executable bit on all files under {@code dir} using {@code chmod -R +x}.
     * Required on macOS where the Chrome for Testing {@code .app} bundle contains many helper
     * executables (crashpad handler, GPU process, etc.) that all need execute permission.
     */
    private static void chmodRecursive(Path dir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("chmod", "-R", "+x", dir.toAbsolutePath().toString());
            int exit = pb.start().waitFor();
            log.info("chmod -R +x {} (exit={})", dir.getFileName(), exit);
        } catch (Exception e) {
            log.debug("chmod recursive failed: {}", e.getMessage());
        }
    }

    /**
     * Removes the macOS quarantine extended attribute from a directory tree.
     * Without this, macOS Gatekeeper blocks the downloaded Chrome for Testing binary.
     */
    private static void removeQuarantine(Path appDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "xattr", "-rd", "com.apple.quarantine", appDir.toAbsolutePath().toString());
            Process p = pb.inheritIO().start();
            int exit = p.waitFor();
            log.info("Removed macOS quarantine from {} (exit={})", appDir.getFileName(), exit);
        } catch (Exception e) {
            log.debug("Could not remove quarantine (may be OK if not on macOS): {}", e.getMessage());
        }
    }

    /** Sets executable permission on a file (POSIX systems). */
    private static void makeExecutable(Path path) {
        try {
            if (Files.exists(path)) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(path, perms);
            }
        } catch (Exception e) {
            log.debug("Could not set executable permission on {}: {}", path, e.getMessage());
        }
    }

    // ==================== Extension ID discovery ====================

    /**
     * Discovers the runtime extension ID using multiple strategies in order:
     * <ol>
     *   <li>Preferences JSON scan</li>
     *   <li>Extensions directory scan</li>
     *   <li>{@code chrome://extensions-internals} JSON page</li>
     *   <li>Recursive shadow-DOM scan of {@code chrome://extensions}</li>
     * </ol>
     */
    public static String discoverExtensionId(WebDriver driver) {
        String userDataDir = getUserDataDir(driver);

        if (userDataDir != null) {
            String id = discoverFromPreferences(userDataDir);
            if (!"unknown".equals(id)) return id;

            id = discoverFromFilesystem(userDataDir);
            if (!"unknown".equals(id)) return id;
        }

        String id = discoverFromInternals(driver);
        if (!"unknown".equals(id)) return id;

        return discoverFromDom(driver);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static String getUserDataDir(WebDriver driver) {
        try {
            Capabilities caps = ((HasCapabilities) driver).getCapabilities();
            Map<String, Object> chromeData = (Map<String, Object>) caps.getCapability("chrome");
            if (chromeData == null) return null;
            String dir = (String) chromeData.get("userDataDir");
            if (dir != null) log.info("Chrome user data dir: {}", dir);
            return (dir != null && !dir.isEmpty()) ? dir : null;
        } catch (Exception e) {
            log.debug("Could not get userDataDir: {}", e.getMessage());
            return null;
        }
    }

    private static String discoverFromPreferences(String userDataDir) {
        try {
            Path prefs = Paths.get(userDataDir, "Default", "Preferences");
            if (!Files.exists(prefs)) return "unknown";
            String json = new String(Files.readAllBytes(prefs), StandardCharsets.UTF_8);
            log.debug("Preferences file size: {} chars", json.length());
            Matcher m = EXT_ID_IN_TEXT.matcher(json);
            if (m.find()) {
                log.info("Preferences strategy: found ID: {}", m.group());
                return m.group();
            }
        } catch (Exception e) {
            log.debug("Preferences strategy failed: {}", e.getMessage());
        }
        return "unknown";
    }

    private static String discoverFromFilesystem(String userDataDir) {
        try {
            Path extensionsDir = Paths.get(userDataDir, "Default", "Extensions");
            if (!Files.exists(extensionsDir)) return "unknown";
            try (Stream<Path> children = Files.list(extensionsDir)) {
                return children
                        .filter(Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .filter(name -> EXT_ID_PATTERN.matcher(name).matches())
                        .findFirst()
                        .orElse("unknown");
            }
        } catch (Exception e) {
            log.debug("Filesystem strategy failed: {}", e.getMessage());
            return "unknown";
        }
    }

    private static String discoverFromInternals(WebDriver driver) {
        try {
            driver.get("chrome://extensions-internals");
            Thread.sleep(1000);
            String body = driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
            log.debug("Internals page: {} chars", body.length());
            // Find the ID nearest to our extension's path
            int pathIdx = body.indexOf("demo-extension");
            if (pathIdx >= 0) {
                String before = body.substring(0, pathIdx);
                Matcher m = EXT_ID_IN_TEXT.matcher(before);
                String lastId = null;
                while (m.find()) lastId = m.group();
                if (lastId != null) {
                    log.info("Internals strategy: found ID near path: {}", lastId);
                    return lastId;
                }
            }
            // Fallback: first ID in the page that isn't a known built-in
            Matcher m = EXT_ID_IN_TEXT.matcher(body);
            while (m.find()) {
                String candidate = m.group();
                // Skip known Chrome built-ins (Web Store, PDF Viewer, etc.)
                if (!"ahfgeienlihckogmohjhadlkjgocpleb".equals(candidate) &&
                    !"mhjfbmdgcfjbbpaeojofohoefgiehjai".equals(candidate)) {
                    log.info("Internals strategy: found user extension ID: {}", candidate);
                    return candidate;
                }
            }
            log.debug("Internals strategy: no user extension ID found");
        } catch (Exception e) {
            log.debug("Internals strategy failed: {}", e.getMessage());
        }
        return "unknown";
    }

    private static String discoverFromDom(WebDriver driver) {
        try {
            log.info("DOM strategy: navigating to chrome://extensions");
            driver.get("chrome://extensions");
            Thread.sleep(2000);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String script =
                "function ids(root) {" +
                "  var r=[];" +
                "  root.querySelectorAll('[id]').forEach(function(e){if(/^[a-z]{32}$/.test(e.id))r.push(e.id);});" +
                "  root.querySelectorAll('*').forEach(function(e){if(e.shadowRoot)r=r.concat(ids(e.shadowRoot));});" +
                "  return r;" +
                "}" +
                "var r=ids(document);return r.length>0?r[0]:null;";
            for (int i = 1; i <= 20; i++) {
                Object result = js.executeScript(script);
                if (result != null && !result.toString().isEmpty() && !"null".equals(result.toString())) {
                    log.info("DOM strategy: found ID on attempt {}: {}", i, result);
                    return result.toString();
                }
                Thread.sleep(500);
            }
            log.warn("DOM strategy: extension ID not found after 20 attempts");
        } catch (Exception e) {
            log.error("DOM strategy failed: {}", e.getMessage());
        }
        return "unknown";
    }
}
