package base;

import org.openqa.selenium.WebDriver;

/**
 * Minimal base interface used by {@link listeners.ExtentTestNGListener} to retrieve
 * the WebDriver instance for screenshot capture on failure.
 */
public abstract class BaseTestTestNG {

    /**
     * Returns the WebDriver instance managed by this test class.
     * Subclasses must implement or override this method.
     *
     * @return the active WebDriver, or {@code null} if not available
     */
    public abstract WebDriver getDriver();
}
