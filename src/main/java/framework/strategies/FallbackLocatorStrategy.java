package framework.strategies;

import framework.annotations.SmartFindBy;
import framework.core.HealingResult;
import framework.utils.LocatorParser;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Healing strategy that attempts to find elements using fallback locators
 * specified in @SmartFindBy annotations.
 * <p>
 * This strategy reads fallback locators from the annotation metadata and tries
 * each one sequentially until the element is found or all fallbacks are exhausted.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public class FallbackLocatorStrategy implements HealingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FallbackLocatorStrategy.class);

    // Cache for annotation metadata to avoid reflection overhead
    private final Map<String, By[]> fallbackCache = new HashMap<>();
    private final Map<String, SmartFindBy> annotationCache = new HashMap<>();

    @Override
    public String getName() {
        return "FallbackLocatorStrategy";
    }

    @Override
    public int getPriority() {
        return 2; // Try after WaitStrategy but before others
    }

    @Override
    public boolean canHandle(Throwable error) {
        // This strategy specifically handles NoSuchElementException
        // when we have fallback locators available
        return error instanceof org.openqa.selenium.NoSuchElementException;
    }

    @Override
    public HealingResult attemptHeal(SearchContext context, By originalLocator, Throwable originalError) {
        long startTime = System.currentTimeMillis();
        var attempts = new ArrayList<HealingResult.HealingAttempt>();

        try {
            logger.info("Attempting {} for locator: {}", getName(), originalLocator);

            // 1. Get fallback locators for this element
            By[] fallbackLocators = getFallbackLocators(originalLocator);

            if (fallbackLocators.length == 0) {
                String message = "No fallback locators available for: " + originalLocator;
                attempts.add(new HealingResult.HealingAttempt(getName(), false, message, Duration.ZERO));
                return HealingResult.failed(attempts, originalError.toString());
            }

            logger.debug("Found {} fallback locators for: {}", fallbackLocators.length, originalLocator);

            // 2. Try each fallback locator sequentially
            for (int i = 0; i < fallbackLocators.length; i++) {
                By fallbackLocator = fallbackLocators[i];
                long attemptStart = System.currentTimeMillis();

                try {
                    logger.debug("Trying fallback {}: {}", i + 1, fallbackLocator);

                    List<WebElement> elements = context.findElements(fallbackLocator);

                    if (!elements.isEmpty()) {
                        Duration attemptTime = Duration.ofMillis(System.currentTimeMillis() - attemptStart);
                        Duration totalTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

                        attempts.add(new HealingResult.HealingAttempt(
                                getName(), true,
                                String.format("Found with fallback %d: %s", i + 1, fallbackLocator),
                                attemptTime
                        ));

                        logger.info("{} SUCCESS: element found with fallback {} in {}ms",
                                getName(), i + 1, attemptTime.toMillis());

                        return HealingResult.success(elements.get(0), getName(), totalTime, attempts);
                    }

                    Duration attemptTime = Duration.ofMillis(System.currentTimeMillis() - attemptStart);
                    attempts.add(new HealingResult.HealingAttempt(
                            getName(), false,
                            String.format("Fallback %d failed: %s", i + 1, fallbackLocator),
                            attemptTime
                    ));

                } catch (Exception e) {
                    Duration attemptTime = Duration.ofMillis(System.currentTimeMillis() - attemptStart);
                    attempts.add(new HealingResult.HealingAttempt(
                            getName(), false,
                            String.format("Fallback %d error: %s", i + 1, e.getMessage()),
                            attemptTime
                    ));
                }
            }

            // 3. All fallbacks failed
            Duration totalTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            String message = String.format("All %d fallback locators failed", fallbackLocators.length);
            attempts.add(new HealingResult.HealingAttempt(getName(), false, message, totalTime));

            logger.warn("{} FAILED: all {} fallback locators exhausted", getName(), fallbackLocators.length);
            return HealingResult.failed(attempts, originalError.toString());

        } catch (Exception e) {
            Duration totalTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            attempts.add(new HealingResult.HealingAttempt(
                    getName(), false,
                    "Strategy error: " + e.getMessage(),
                    totalTime
            ));

            logger.error("{} ERROR: {}", getName(), e.getMessage(), e);
            return HealingResult.failed(attempts, originalError.toString());
        }
    }

    /**
     * Gets fallback locators for the original locator.
     * In a real implementation, this would read from annotation metadata.
     * For MVP, we simulate this with a simple mapping.
     */
    private By[] getFallbackLocators(By originalLocator) {
        String locatorKey = originalLocator.toString();

        // Check cache first
        if (fallbackCache.containsKey(locatorKey)) {
            return fallbackCache.get(locatorKey);
        }

        // IMPORTANT: If we reach here, it means this locator wasn't registered
        // via registerAnnotation(). This shouldn't happen in normal flow.
        // Return empty array to indicate no fallbacks available.
        logger.warn("No fallback locators found in cache for: {}. " +
                "Was the field properly registered with @SmartFindBy?", originalLocator);

        By[] emptyFallbacks = new By[0];
        fallbackCache.put(locatorKey, emptyFallbacks); // Cache empty result

        return emptyFallbacks;
    }

    /**
     * Registers a field with @SmartFindBy annotation for fallback support.
     * This method should be called by PageFactory during initialization.
     *
     * @param fieldName the name of the field
     * @param annotation the @SmartFindBy annotation
     * @param originalLocator the primary locator for this field
     */
    public void registerAnnotation(String fieldName, SmartFindBy annotation, By originalLocator) {
        String locatorKey = originalLocator.toString();

        // Cache the annotation
        annotationCache.put(fieldName, annotation);

        // Parse and cache fallback locators
        By[] fallbacks = LocatorParser.parseFallbacks(annotation.fallbacks());
        fallbackCache.put(locatorKey, fallbacks);

        logger.debug("Registered fallbacks for {}: {} fallback(s)",
                fieldName, fallbacks.length);
    }

    /**
     * Gets the registered annotation for a field.
     */
    public SmartFindBy getAnnotation(String fieldName) {
        return annotationCache.get(fieldName);
    }

    /**
     * Clears all cached annotations and fallback locators.
     */
    public void clearCache() {
        fallbackCache.clear();
        annotationCache.clear();
        logger.info("Cleared FallbackLocatorStrategy cache");
    }
}

