package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for smart element location with self-healing capabilities.
 * <p>
 * This annotation allows declaring web elements with primary locators, fallback locators,
 * and custom timeout settings. The framework will attempt to heal elements when standard
 * location fails.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SmartFindBy {

    /**
     * Primary element locator using HTML id attribute.
     *
     * @return the element id
     */
    String id() default "";

    /**
     * Primary element locator using HTML name attribute.
     *
     * @return the element name
     */
    String name() default "";

    /**
     * Primary element locator using CSS class name.
     *
     * @return the CSS class name
     */
    String className() default "";

    /**
     * Primary element locator using CSS selector.
     *
     * @return the CSS selector
     */
    String css() default "";

    /**
     * Primary element locator using XPath expression.
     *
     * @return the XPath expression
     */
    String xpath() default "";

    /**
     * Primary element locator using HTML tag name.
     *
     * @return the HTML tag name
     */
    String tagName() default "";

    /**
     * Primary element locator using exact link text.
     *
     * @return the exact link text
     */
    String linkText() default "";

    /**
     * Primary element locator using partial link text.
     *
     * @return the partial link text
     */
    String partialLinkText() default "";

    /**
     * Array of fallback locators to try if primary locator fails.
     * <p>
     * Format: "type:value" (e.g., "css:.btn-primary", "xpath://button[text()='Submit']")
     * </p>
     *
     * @return array of fallback locator definitions
     */
    String[] fallbacks() default {};

    /**
     * Custom timeout in seconds for this specific element.
     * <p>
     * Overrides the default timeout setting for healing strategies.
     * </p>
     *
     * @return custom timeout in seconds
     */
    int timeout() default 10;
}