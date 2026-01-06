package framework.core;

import lombok.Getter;
import lombok.ToString;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a healing attempt for a web element.
 * <p>
 * Contains information about whether healing was successful, which strategy was used,
 * healing duration, and detailed attempt history.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
@Getter
@ToString
public class HealingResult {

    private final boolean healed;
    private final WebElement element;
    private final String strategyUsed;
    private final Duration healingTime;
    private final List<HealingAttempt> attempts;
    private final String originalError;

    private HealingResult(boolean healed, WebElement element, String strategyUsed,
                          Duration healingTime, List<HealingAttempt> attempts, String originalError) {
        this.healed = healed;
        this.element = element;
        this.strategyUsed = strategyUsed;
        this.healingTime = healingTime;
        this.attempts = attempts != null ? attempts : new ArrayList<>();
        this.originalError = originalError;
    }

    /**
     * Creates a successful healing result.
     *
     * @param element the healed web element
     * @param strategyUsed the name of the strategy that succeeded
     * @param healingTime the duration of the healing process
     * @param attempts list of all healing attempts
     * @return successful healing result
     */
    public static HealingResult success(WebElement element, String strategyUsed,
                                        Duration healingTime, List<HealingAttempt> attempts) {
        return new HealingResult(true, element, strategyUsed, healingTime, attempts, null);
    }

    /**
     * Creates a failed healing result with detailed attempt history.
     *
     * @param attempts list of all healing attempts
     * @param originalError the original error that triggered healing
     * @return failed healing result
     */
    public static HealingResult failed(List<HealingAttempt> attempts, String originalError) {
        return new HealingResult(false, null, null, Duration.ZERO, attempts, originalError);
    }

    /**
     * Creates a failed healing result without attempt history.
     *
     * @param originalError the original error that triggered healing
     * @return failed healing result
     */
    public static HealingResult failed(String originalError) {
        return failed(new ArrayList<>(), originalError);
    }

    /**
     * Represents a single healing attempt by a specific strategy.
     */
    @Getter
    @ToString
    public static class HealingAttempt {

        private final String strategyName;
        private final boolean success;
        private final String details;
        private final Duration duration;

        /**
         * Constructs a healing attempt record.
         *
         * @param strategyName name of the healing strategy
         * @param success whether the attempt was successful
         * @param details detailed description of the attempt
         * @param duration duration of the attempt
         */
        public HealingAttempt(String strategyName, boolean success, String details, Duration duration) {
            this.strategyName = strategyName;
            this.success = success;
            this.details = details;
            this.duration = duration;
        }
    }
}
