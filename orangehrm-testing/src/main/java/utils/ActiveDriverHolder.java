package utils;

import org.openqa.selenium.WebDriver;

/**
 * Thread-safe holder for the active WebDriver when tests manage their own driver
 * outside of DriverFactory (e.g. BaseOrangeHRMTest).
 *
 * <p>Lives in main scope so both the test base class (test scope) and listeners
 * (main scope) can access it without circular compile-scope dependencies.</p>
 */
public class ActiveDriverHolder {

    private static final ThreadLocal<WebDriver> active = new ThreadLocal<>();

    private ActiveDriverHolder() {}

    public static void set(WebDriver driver) { active.set(driver); }
    public static WebDriver get()            { return active.get(); }
    public static void remove()              { active.remove(); }
}
