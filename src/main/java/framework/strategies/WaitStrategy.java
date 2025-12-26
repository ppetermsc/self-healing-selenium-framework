package framework.strategies;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.core.HealingResult;
import java.time.Duration;
import java.util.ArrayList;

/**
 * Healing strategy that waits for an element to appear using explicit waits.
 * <p>
 * This strategy addresses timing issues where elements load asynchronously
 * or with delays. It's typically the first strategy attempted due to its
 * high success rate for common timing-related failures.
 * </p>
 *
 * @author Peter Petermsc
 * @version 1.0
 */
public class WaitStrategy implements HealingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(WaitStrategy.class);

    private final int timeoutSeconds;

    /**
     * Creates a WaitStrategy with default timeout of 10 seconds.
     */
    public WaitStrategy() {
        this.timeoutSeconds = 10;
    }

    /**
     * Creates a WaitStrategy with custom timeout.
     *
     * @param timeoutSeconds timeout in seconds
     */
    public WaitStrategy(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String getName() {
        return "WaitStrategy";
    }

    @Override
    public int getPriority() {
        return 1; // High priority - usually tried first
    }

    @Override
    public boolean canHandle(Throwable error) {
        // Can handle errors related to element not being found
        String errorMessage = error.getMessage() != null ? error.getMessage() : "";
        String errorClassName = error.getClass().getSimpleName();

        return errorClassName.contains("Element") || errorMessage.contains("element");
    }

    @Override
    public HealingResult attemptHeal(SearchContext context, By originalLocator, Throwable originalError) {
        long startTime = System.currentTimeMillis();
        var attempts = new ArrayList<HealingResult.HealingAttempt>();

        // Wait strategy only works with WebDriver context
        if (!(context instanceof WebDriver)) {
            String message = "WaitStrategy requires WebDriver context, got: " + context.getClass().getName();
            attempts.add(new HealingResult.HealingAttempt(getName(), false, message, Duration.ZERO));
            return HealingResult.failed(attempts, originalError.toString());
        }

        WebDriver driver = (WebDriver) context;

        try {
            logger.warn("Attempting {} for locator: {} (timeout: {}s)",
                    getName(), originalLocator, timeoutSeconds);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(originalLocator));

            Duration healingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            attempts.add(new HealingResult.HealingAttempt(
                    getName(), true,
                    String.format("Element found after waiting %d seconds", timeoutSeconds),
                    healingTime
            ));

            logger.info("{} SUCCESS: element found after {}ms", getName(), healingTime.toMillis());
            return HealingResult.success(element, getName(), healingTime, attempts);

        } catch (Exception e) {
            Duration healingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            attempts.add(new HealingResult.HealingAttempt(
                    getName(), false,
                    String.format("Timeout after %d seconds: %s", timeoutSeconds, e.getMessage()),
                    healingTime
            ));

            logger.warn("{} FAILED: {}", getName(), e.getMessage());
            return HealingResult.failed(attempts, originalError.toString());
        }
    }
}
