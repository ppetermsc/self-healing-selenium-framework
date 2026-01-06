package framework.core;

import lombok.Getter;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context that stores healing statistics and historical data.
 * <p>
 * Maintains success/failure rates for locators and tracks which strategies
 * are most effective for different types of element location failures.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public class HealingContext {

    private static final Logger logger = LoggerFactory.getLogger(HealingContext.class);

    // Statistics per locator
    private final Map<String, LocatorStats> locatorStats = new ConcurrentHashMap<>();

    /**
     * Statistics for a specific locator's healing attempts.
     */
    @Getter
    public static class LocatorStats {

        private int totalFailures = 0;
        private int successfulHealings = 0;
        private String lastSuccessfulStrategy;
        private long lastFailureTime;
        private long lastSuccessTime;

        /**
         * Records a healing failure for this locator.
         */
        public void recordFailure() {
            totalFailures++;
            lastFailureTime = System.currentTimeMillis();
        }

        /**
         * Records a successful healing for this locator.
         *
         * @param strategyName the strategy that succeeded
         */
        public void recordSuccess(String strategyName) {
            successfulHealings++;
            lastSuccessfulStrategy = strategyName;
            lastSuccessTime = System.currentTimeMillis();
        }

        /**
         * Calculates the success rate for this locator.
         *
         * @return success rate as percentage (0.0 to 100.0)
         */
        public double getSuccessRate() {
            int totalAttempts = totalFailures + successfulHealings;
            return totalAttempts > 0 ? (double) successfulHealings / totalAttempts * 100 : 0;
        }
    }

    /**
     * Records a successful healing attempt for a locator.
     *
     * @param locator the locator that was healed
     * @param strategyName the strategy that succeeded
     */
    public void recordHealingSuccess(By locator, String strategyName) {
        String locatorKey = locator.toString();
        locatorStats.computeIfAbsent(locatorKey, k -> new LocatorStats())
                .recordSuccess(strategyName);
        logger.debug("Recorded healing success for {} using {}", locatorKey, strategyName);
    }

    /**
     * Records a failed healing attempt for a locator.
     *
     * @param locator the locator that failed to heal
     */
    public void recordHealingFailure(By locator) {
        String locatorKey = locator.toString();
        locatorStats.computeIfAbsent(locatorKey, k -> new LocatorStats())
                .recordFailure();
        logger.debug("Recorded healing failure for {}", locatorKey);
    }

    /**
     * Gets statistics for a specific locator.
     *
     * @param locator the locator to get statistics for
     * @return locator statistics (never null)
     */
    public LocatorStats getStats(By locator) {
        return locatorStats.getOrDefault(locator.toString(), new LocatorStats());
    }

    /**
     * Gets all healing statistics.
     *
     * @return map of all locator statistics
     */
    public Map<String, LocatorStats> getAllStats() {
        return new HashMap<>(locatorStats);
    }

    /**
     * Clears all healing statistics.
     */
    public void clearStats() {
        locatorStats.clear();
        logger.info("Cleared all healing statistics");
    }
}

