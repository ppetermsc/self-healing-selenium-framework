package framework.core;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.FileDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Self-healing WebDriver implementation that automatically recovers from element location failures.
 * <p>
 * This class wraps a standard WebDriver instance and intercepts all element location operations.
 * When an element cannot be found (NoSuchElementException, StaleElementReferenceException, etc.),
 * the healing engine attempts various recovery strategies before failing the test.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public class HealingWebDriver implements WebDriver, TakesScreenshot, JavascriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(HealingWebDriver.class);

    private final WebDriver delegate;
    private final HealingEngine healingEngine;
    private final ConcurrentMap<By, HealingResult> healingCache;

    /**
     * Creates a HealingWebDriver with default healing engine.
     *
     * @param delegate the underlying WebDriver instance (ChromeDriver, FirefoxDriver, etc.)
     */
    public HealingWebDriver(WebDriver delegate) {
        this(delegate, new HealingEngine());
    }

    /**
     * Creates a HealingWebDriver with custom healing engine.
     *
     * @param delegate the underlying WebDriver instance
     * @param healingEngine custom healing engine with specific strategies
     */
    public HealingWebDriver(WebDriver delegate, HealingEngine healingEngine) {
        this.delegate = delegate;
        this.healingEngine = healingEngine;
        this.healingCache = new ConcurrentHashMap<>();

        // Disable implicit waits - we handle waits explicitly in healing strategies
        this.delegate.manage().timeouts().implicitlyWait(Duration.ZERO);

        logger.info("HealingWebDriver initialized with {} strategies",
                healingEngine.getStrategies().size());
    }

    /**
     * Attempts to find an element with self-healing capabilities.
     * <p>
     * This is the core method that provides self-healing functionality. When the standard
     * element location fails, it triggers the healing engine to attempt recovery.
     * </p>
     *
     * @param by the locator strategy
     * @return the found web element
     * @throws NoSuchElementException if element cannot be found even after healing attempts
     */
    @Override
    public WebElement findElement(By by) throws NoSuchElementException {
        try {
            // First, try standard findElement
            return delegate.findElement(by);
        } catch (NoSuchElementException | StaleElementReferenceException error) {
            logger.warn("Element not found with locator: {}. Attempting healing...", by);

            // Check cache for previous successful healing
            if (healingCache.containsKey(by)) {
                HealingResult cachedResult = healingCache.get(by);
                if (cachedResult.isHealed()) {
                    logger.info("Using cached healing result for locator: {}", by);
                    return cachedResult.getElement();
                }
            }

            // Attempt healing
            HealingResult healingResult = healingEngine.attemptHealing(delegate, by, error);

            if (healingResult.isHealed()) {
                logger.info("Element successfully healed using strategy: {}",
                        healingResult.getStrategyUsed());

                // Cache successful healing result
                healingCache.put(by, healingResult);

                // Log healing details
                logHealingDetails(by, healingResult);

                return healingResult.getElement();
            } else {
                logger.error("All healing strategies failed for locator: {}", by);
                throw new NoSuchElementException(
                        String.format("Element not found with locator '%s' after healing attempts. " +
                                "Original error: %s", by, error.getMessage()),
                        error
                );
            }
        }
    }

    /**
     * Finds multiple elements with self-healing capabilities.
     * <p>
     * If no elements are found initially, attempts healing strategies before returning empty list.
     * </p>
     *
     * @param by the locator strategy
     * @return list of found web elements (may be empty if none found)
     */
    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = delegate.findElements(by);

        if (elements.isEmpty()) {
            logger.warn("No elements found with locator: {}. Attempting healing...", by);

            // Create a mock exception for healing engine
            NoSuchElementException error = new NoSuchElementException(
                    "No elements found with locator: " + by);

            HealingResult healingResult = healingEngine.attemptHealing(delegate, by, error);

            if (healingResult.isHealed() && healingResult.getElement() != null) {
                // Return list with the single healed element
                return List.of(healingResult.getElement());
            }
        }

        return elements;
    }

    /**
     * Logs detailed information about a successful healing operation.
     *
     * @param locator the original locator
     * @param result the healing result
     */
    private void logHealingDetails(By locator, HealingResult result) {
        logger.warn("=== HEALING DETAILS ===");
        logger.warn("Locator: {}", locator);
        logger.warn("Strategy used: {}", result.getStrategyUsed());
        logger.warn("Healing time: {}ms", result.getHealingTime().toMillis());
        logger.warn("Attempt history:");

        for (HealingResult.HealingAttempt attempt : result.getAttempts()) {
            logger.warn("  - {}: {} ({}ms)",
                    attempt.getStrategyName(),
                    attempt.isSuccess() ? "SUCCESS" : "FAILED",
                    attempt.getDuration().toMillis());
        }
        logger.warn("======================");
    }

    /**
     * Gets the underlying WebDriver instance.
     *
     * @return the delegated WebDriver
     */
    public WebDriver getDelegate() {
        return delegate;
    }

    /**
     * Gets the healing engine instance.
     *
     * @return the healing engine
     */
    public HealingEngine getHealingEngine() {
        return healingEngine;
    }

    /**
     * Gets healing statistics for all locators.
     *
     * @return map of healing statistics
     */
    public ConcurrentMap<By, HealingResult> getHealingCache() {
        return new ConcurrentHashMap<>(healingCache);
    }

    /**
     * Clears the healing cache.
     */
    public void clearHealingCache() {
        healingCache.clear();
        logger.info("Healing cache cleared");
    }

    // ================================================================
    // Delegated WebDriver methods (standard implementation)
    // ================================================================

    @Override
    public void get(String url) {
        delegate.get(url);
    }

    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getPageSource() {
        return delegate.getPageSource();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void quit() {
        delegate.quit();
        logger.info("HealingWebDriver quit");
    }

    @Override
    public Set<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return delegate.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return delegate.switchTo();
    }

    @Override
    public Navigation navigate() {
        return delegate.navigate();
    }

    @Override
    public Options manage() {
        return delegate.manage();
    }

    // ================================================================
    // TakesScreenshot implementation
    // ================================================================

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        if (delegate instanceof TakesScreenshot) {
            return ((TakesScreenshot) delegate).getScreenshotAs(target);
        }
        throw new UnsupportedOperationException(
                "Underlying WebDriver does not support screenshots");
    }

    // ================================================================
    // JavascriptExecutor implementation
    // ================================================================

    @Override
    public Object executeScript(String script, Object... args) {
        if (delegate instanceof JavascriptExecutor) {
            return ((JavascriptExecutor) delegate).executeScript(script, args);
        }
        throw new UnsupportedOperationException(
                "Underlying WebDriver does not support JavaScript execution");
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        if (delegate instanceof JavascriptExecutor) {
            return ((JavascriptExecutor) delegate).executeAsyncScript(script, args);
        }
        throw new UnsupportedOperationException(
                "Underlying WebDriver does not support async JavaScript execution");
    }

    // ================================================================
    // Deprecated/legacy methods (for compatibility)
    // ================================================================

    /**
     * Sets the file detector (deprecated in Selenium 4 but needed for compatibility).
     *
     * @param detector the file detector to use
     * @deprecated FileDetector is deprecated in Selenium 4. Use sendKeys() for file uploads instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public void setFileDetector(FileDetector detector) {
        // This method is deprecated but we need it for RemoteWebDriver compatibility
        if (delegate instanceof org.openqa.selenium.remote.RemoteWebDriver) {
            ((org.openqa.selenium.remote.RemoteWebDriver) delegate).setFileDetector(detector);
        }
    }
}
