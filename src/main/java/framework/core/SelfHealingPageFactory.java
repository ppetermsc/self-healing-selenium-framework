package framework.core;

import framework.annotations.SmartFindBy;
import framework.strategies.FallbackLocatorStrategy;
import framework.utils.LocatorParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Factory for initializing Page Objects with self-healing capabilities.
 * <p>
 * This factory creates proxy WebElements that delegate to the HealingWebDriver,
 * enabling self-healing functionality for all element interactions.
 * </p>
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public class SelfHealingPageFactory {

    private static final Logger logger = LoggerFactory.getLogger(SelfHealingPageFactory.class);

    /**
     * Initializes all WebElement fields in the page object.
     *
     * @param driver the HealingWebDriver instance
     * @param pageObject the page object to initialize
     * @throws IllegalArgumentException if driver is not a HealingWebDriver
     */
    public static void initElements(WebDriver driver, Object pageObject) {
        if (!(driver instanceof HealingWebDriver)) {
            throw new IllegalArgumentException(
                    "SelfHealingPageFactory requires HealingWebDriver, got: " + driver.getClass().getName());
        }

        HealingWebDriver healingDriver = (HealingWebDriver) driver;

        // Get all fields including inherited ones
        Class<?> pageClass = pageObject.getClass();
        while (pageClass != Object.class) {
            Field[] fields = pageClass.getDeclaredFields();
            for (Field field : fields) {
                if (WebElement.class.isAssignableFrom(field.getType())) {
                    initializeField(healingDriver, pageObject, field);
                } else if (List.class.isAssignableFrom(field.getType())) {
                    // Handle List<WebElement> fields
                    initializeListField(healingDriver, pageObject, field);
                }
            }
            pageClass = pageClass.getSuperclass();
        }

        logger.info("Initialized {} with {} fields",
                pageObject.getClass().getSimpleName(),
                countInitializedFields(pageObject));
    }

    /**
     * Initializes a single WebElement field.
     */
    private static void initializeField(HealingWebDriver driver, Object pageObject, Field field) {
        try {
            field.setAccessible(true);

            SmartFindBy annotation = field.getAnnotation(SmartFindBy.class);
            if (annotation == null) {
                logger.warn("Field {} is WebElement but not annotated with @SmartFindBy", field.getName());
                return;
            }

            // Create primary locator from annotation
            By primaryLocator = LocatorParser.fromAnnotation(annotation);

            // Register annotation with FallbackLocatorStrategy
            registerWithFallbackStrategy(driver, field, annotation, primaryLocator);

            // Create proxy WebElement
            WebElement proxyElement = createWebElementProxy(driver, primaryLocator, annotation);

            // Set the field value
            field.set(pageObject, proxyElement);

            logger.debug("Initialized field: {} with locator: {}", field.getName(), primaryLocator);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize field: " + field.getName(), e);
        }
    }

    /**
     * Initializes a List<WebElement> field.
     */
    private static void initializeListField(HealingWebDriver driver, Object pageObject, Field field) {
        try {
            field.setAccessible(true);

            SmartFindBy annotation = field.getAnnotation(SmartFindBy.class);
            if (annotation == null) {
                logger.warn("Field {} is List<WebElement> but not annotated with @SmartFindBy", field.getName());
                return;
            }

            // Create locator from annotation
            By locator = LocatorParser.fromAnnotation(annotation);

            // Create proxy for List<WebElement>
            List<WebElement> proxyList = createWebElementListProxy(driver, locator);

            // Set the field value
            field.set(pageObject, proxyList);

            logger.debug("Initialized List field: {} with locator: {}", field.getName(), locator);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize List field: " + field.getName(), e);
        }
    }

    /**
     * Registers the annotation with FallbackLocatorStrategy.
     */
    private static void registerWithFallbackStrategy(
            HealingWebDriver driver, Field field, SmartFindBy annotation, By primaryLocator) {

        // Find FallbackLocatorStrategy in the healing engine
        FallbackLocatorStrategy fallbackStrategy = null;
        for (var strategy : driver.getHealingEngine().getStrategies()) {
            if (strategy instanceof FallbackLocatorStrategy) {
                fallbackStrategy = (FallbackLocatorStrategy) strategy;
                break;
            }
        }

        if (fallbackStrategy != null) {
            // Register for fallback support
            String fieldKey = field.getDeclaringClass().getSimpleName() + "." + field.getName();
            fallbackStrategy.registerAnnotation(fieldKey, annotation, primaryLocator);
            logger.debug("Registered field {} with FallbackLocatorStrategy", fieldKey);
        } else {
            logger.warn("FallbackLocatorStrategy not found - fallback functionality disabled");
        }
    }

    /**
     * Creates a proxy WebElement that delegates to HealingWebDriver.
     */
    private static WebElement createWebElementProxy(HealingWebDriver driver, By locator, SmartFindBy annotation) {
        InvocationHandler handler = (proxy, method, args) -> {
            // Delegate all method calls to the driver's findElement
            if (method.getName().equals("equals") ||
                    method.getName().equals("hashCode") ||
                    method.getName().equals("toString")) {
                return method.invoke(proxy, args);
            }

            // Always get fresh element from driver (handles staleness)
            WebElement element = driver.findElement(locator);

            // Special handling for getWrappedDriver
            if (method.getName().equals("getWrappedDriver") && method.getParameterCount() == 0) {
                return driver;
            }

            // Delegate the method call to the actual element
            return method.invoke(element, args);
        };

        return (WebElement) Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class[] { WebElement.class, WrapsDriver.class },
                handler
        );
    }

    /**
     * Creates a proxy for List<WebElement> that delegates to HealingWebDriver.
     */
    @SuppressWarnings("unchecked")
    private static List<WebElement> createWebElementListProxy(HealingWebDriver driver, By locator) {
        InvocationHandler handler = (proxy, method, args) -> {
            // Delegate to driver's findElements method
            if (method.getName().equals("equals") ||
                    method.getName().equals("hashCode") ||
                    method.getName().equals("toString")) {
                return method.invoke(proxy, args);
            }

            List<WebElement> elements = driver.findElements(locator);
            return method.invoke(elements, args);
        };

        return (List<WebElement>) Proxy.newProxyInstance(
                List.class.getClassLoader(),
                new Class[] { List.class },
                handler
        );
    }

    /**
     * Counts initialized fields in a page object.
     */
    private static int countInitializedFields(Object pageObject) {
        int count = 0;
        Class<?> pageClass = pageObject.getClass();

        while (pageClass != Object.class) {
            Field[] fields = pageClass.getDeclaredFields();
            for (Field field : fields) {
                if (WebElement.class.isAssignableFrom(field.getType()) ||
                        List.class.isAssignableFrom(field.getType())) {
                    count++;
                }
            }
            pageClass = pageClass.getSuperclass();
        }

        return count;
    }

    /**
     * Creates a Page Object instance and initializes its fields.
     *
     * @param driver the HealingWebDriver instance
     * @param pageClass the class of the page object to create
     * @return initialized page object instance
     */
    public static <T> T initElements(WebDriver driver, Class<T> pageClass) {
        try {
            T pageObject = pageClass.getDeclaredConstructor().newInstance();
            initElements(driver, pageObject);
            return pageObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create page object: " + pageClass.getName(), e);
        }
    }
}
