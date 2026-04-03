package listeners;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import config.FeatureToggle;

/**
 * Automatically applies RetryAnalyzer to ALL tests in the suite (when enabled).
 * No need to manually add retryAnalyzer attribute to each @Test annotation.
 *
 * RetryAnalyzer is only applied if retry is enabled via FeatureToggle.
 * Disable with: -Dretry.enabled=false
 *
 * Register this listener in your testng.xml file:
 * <listeners>
 *     <listener class-name="listeners.AnnotationTransformer"/>
 *     <listener class-name="listeners.TestListenerTestNG"/>
 * </listeners>
 */
public class AnnotationTransformer implements IAnnotationTransformer {

    static final Logger log = getLogger(lookup().lookupClass());

    @Override
    @SuppressWarnings("rawtypes") // TestNG interface uses raw types
    public void transform(ITestAnnotation annotation, Class testClass,
                         Constructor testConstructor, Method testMethod) {

        // Only apply to test methods (not configuration methods)
        if (testMethod != null) {
            // Only apply RetryAnalyzer if retry is enabled
            if (FeatureToggle.isRetryEnabled()) {
                annotation.setRetryAnalyzer(RetryAnalyzer.class);
                log.debug("RetryAnalyzer applied to: " + testMethod.getName());
            } else {
                log.debug("Retry disabled - skipping RetryAnalyzer for: " + testMethod.getName());
            }
        }
    }
}