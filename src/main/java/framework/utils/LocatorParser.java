package framework.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;

/**
 * Utility class for parsing locator strings into Selenium By objects.
 * <p>
 * Supports various locator formats including those from @SmartFindBy annotations.
 * </p>
 *
 * @author Peter Petermsc
 * @version 1.0
 */
public class LocatorParser {

    private LocatorParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a locator string into a Selenium By object.
     * <p>
     * Supported formats:
     * - Standard: "id=username", "css=.btn", "xpath=//button"
     * - Shorthand: "#username" (CSS ID), ".btn-primary" (CSS class)
     * - Direct: By object (returned as-is)
     * </p>
     *
     * @param locatorString the locator string to parse
     * @return By object for element location
     * @throws IllegalArgumentException if locator format is invalid
     */
    public static By parse(String locatorString) {
        if (locatorString == null || locatorString.trim().isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty");
        }

        String locator = locatorString.trim();

        // Check for shorthand CSS selectors
        if (locator.startsWith("#")) {
            return By.cssSelector(locator);
        }

        if (locator.startsWith(".")) {
            return By.cssSelector(locator);
        }

        if (locator.startsWith("//") || locator.startsWith("./") ||
                locator.startsWith("(") || locator.startsWith("/")) {
            return By.xpath(locator);
        }

        // Check for type:value format
        if (locator.contains("=")) {
            int separatorIndex = locator.indexOf('=');
            String type = locator.substring(0, separatorIndex).trim().toLowerCase();
            String value = locator.substring(separatorIndex + 1).trim();

            switch (type) {
                case "id":
                    return By.id(value);
                case "name":
                    return By.name(value);
                case "class":
                case "classname":
                    return By.className(value);
                case "css":
                case "cssselector":
                    return By.cssSelector(value);
                case "xpath":
                    return By.xpath(value);
                case "tagname":
                case "tag":
                    return By.tagName(value);
                case "linktext":
                    return By.linkText(value);
                case "partiallinktext":
                    return By.partialLinkText(value);
                default:
                    throw new InvalidSelectorException(
                            String.format("Unknown locator type: '%s' in '%s'", type, locatorString));
            }
        }

        // Default to CSS selector
        return By.cssSelector(locator);
    }

    /**
     * Parses an array of fallback locator strings.
     *
     * @param fallbacks array of fallback locator strings
     * @return array of By objects
     */
    public static By[] parseFallbacks(String[] fallbacks) {
        if (fallbacks == null || fallbacks.length == 0) {
            return new By[0];
        }

        By[] parsedFallbacks = new By[fallbacks.length];
        for (int i = 0; i < fallbacks.length; i++) {
            parsedFallbacks[i] = parse(fallbacks[i]);
        }

        return parsedFallbacks;
    }

    /**
     * Creates a By object from @SmartFindBy annotation values.
     *
     * @param annotation the @SmartFindBy annotation
     * @return By object for the primary locator
     * @throws IllegalArgumentException if no valid locator is specified
     */
    public static By fromAnnotation(framework.annotations.SmartFindBy annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Annotation cannot be null");
        }

        // Check each locator type in order of preference
        if (!annotation.id().isEmpty()) {
            return By.id(annotation.id());
        }

        if (!annotation.name().isEmpty()) {
            return By.name(annotation.name());
        }

        if (!annotation.css().isEmpty()) {
            return By.cssSelector(annotation.css());
        }

        if (!annotation.xpath().isEmpty()) {
            return By.xpath(annotation.xpath());
        }

        if (!annotation.className().isEmpty()) {
            return By.className(annotation.className());
        }

        if (!annotation.tagName().isEmpty()) {
            return By.tagName(annotation.tagName());
        }

        if (!annotation.linkText().isEmpty()) {
            return By.linkText(annotation.linkText());
        }

        if (!annotation.partialLinkText().isEmpty()) {
            return By.partialLinkText(annotation.partialLinkText());
        }

        throw new IllegalArgumentException(
                "@SmartFindBy annotation must specify at least one locator type");
    }
}

