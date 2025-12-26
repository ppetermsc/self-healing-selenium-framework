package framework.strategies;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import framework.core.HealingResult;

/**
 * Interface defining a healing strategy for web element recovery.
 * <p>
 * Healing strategies are algorithms that attempt to recover from web element location
 * failures (e.g., NoSuchElementException, StaleElementReferenceException).
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public interface HealingStrategy {

    /**
     * Returns the unique name of this healing strategy.
     *
     * @return strategy name
     */
    String getName();

    /**
     * Attempts to heal (recover) a web element that could not be found.
     *
     * @param context the search context (WebDriver or WebElement)
     * @param originalLocator the original locator that failed
     * @param originalError the exception that was thrown
     * @return healing result containing success/failure information
     */
    HealingResult attemptHeal(SearchContext context, By originalLocator, Throwable originalError);

    /**
     * Checks if this strategy can handle the given error type.
     * <p>
     * Default implementation returns true for all error types.
     * </p>
     *
     * @param error the exception to check
     * @return true if this strategy can handle the error
     */
    default boolean canHandle(Throwable error) {
        return true;
    }

    /**
     * Returns the priority of this strategy.
     * <p>
     * Lower numbers indicate higher priority. Strategies are tried in ascending
     * priority order (1, 2, 3...).
     * </p>
     *
     * @return strategy priority (default: 10)
     */
    default int getPriority() {
        return 10;
    }
}
