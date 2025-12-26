package framework.core;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.strategies.HealingStrategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Core engine that coordinates healing strategies for web element recovery.
 * <p>
 * The HealingEngine manages a collection of healing strategies and attempts them
 * in priority order when element location fails. It maintains execution context
 * and logs all healing attempts.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public class HealingEngine {

    private static final Logger logger = LoggerFactory.getLogger(HealingEngine.class);

    private final List<HealingStrategy> strategies;
    private final HealingContext context;

    /**
     * Creates a HealingEngine with default strategies.
     */
    public HealingEngine() {
        this.strategies = new ArrayList<>();
        this.context = new HealingContext();
        initializeDefaultStrategies();
    }

    /**
     * Creates a HealingEngine with custom strategies.
     *
     * @param strategies list of healing strategies to use
     */
    public HealingEngine(List<HealingStrategy> strategies) {
        this.strategies = new ArrayList<>(strategies);
        this.context = new HealingContext();
        sortStrategiesByPriority();
    }

    private void initializeDefaultStrategies() {
        strategies.add(new framework.strategies.WaitStrategy());
        sortStrategiesByPriority();
        logger.info("Initialized HealingEngine with default strategies");
    }

    private void sortStrategiesByPriority() {
        strategies.sort(Comparator.comparingInt(HealingStrategy::getPriority));
    }

    /**
     * Adds a new healing strategy to the engine.
     *
     * @param strategy the healing strategy to add
     */
    public void addStrategy(HealingStrategy strategy) {
        strategies.add(strategy);
        sortStrategiesByPriority();
        logger.info("Added healing strategy: {}", strategy.getName());
    }

    /**
     * Attempts to heal a web element that could not be found.
     * <p>
     * Executes all applicable healing strategies in priority order until one succeeds
     * or all strategies are exhausted.
     * </p>
     *
     * @param context the search context (WebDriver or WebElement)
     * @param failingLocator the locator that failed to find the element
     * @param error the exception that was thrown
     * @return healing result containing the outcome of the healing attempt
     */
    public HealingResult attemptHealing(SearchContext context, By failingLocator, Throwable error) {
        logger.warn("Healing triggered for locator: {} | Error: {}",
                failingLocator, error.getMessage());

        List<HealingResult.HealingAttempt> allAttempts = new ArrayList<>();

        for (HealingStrategy strategy : strategies) {
            if (!strategy.canHandle(error)) {
                logger.debug("Strategy {} cannot handle error type: {}",
                        strategy.getName(), error.getClass().getSimpleName());
                continue;
            }

            logger.info("Trying strategy: {}", strategy.getName());
            HealingResult result = strategy.attemptHeal(context, failingLocator, error);
            allAttempts.addAll(result.getAttempts());

            if (result.isHealed()) {
                logger.warn("Healing SUCCESS with strategy: {} in {}ms",
                        strategy.getName(), result.getHealingTime().toMillis());

                this.context.recordHealingSuccess(failingLocator, strategy.getName());

                return HealingResult.success(
                        result.getElement(),
                        result.getStrategyUsed(),
                        result.getHealingTime(),
                        allAttempts
                );
            }
        }

        // All strategies failed
        logger.error("All healing strategies FAILED for locator: {}", failingLocator);
        this.context.recordHealingFailure(failingLocator);
        return HealingResult.failed(allAttempts, error.toString());
    }

    /**
     * Returns the healing context containing execution statistics.
     *
     * @return the healing context
     */
    public HealingContext getContext() {
        return context;
    }

    /**
     * Returns a copy of all healing strategies managed by this engine.
     *
     * @return list of healing strategies
     */
    public List<HealingStrategy> getStrategies() {
        return new ArrayList<>(strategies);
    }
}
