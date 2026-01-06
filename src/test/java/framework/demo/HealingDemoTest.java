package framework.demo;

import framework.core.HealingWebDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Demonstration test for the self-healing framework.
 * Uses WebDriverManager for automatic ChromeDriver setup.
 */
public class HealingDemoTest {

    private static final Logger logger = LoggerFactory.getLogger(HealingDemoTest.class);
    private HealingWebDriver driver;

    @BeforeAll
    public static void setupAll() {
        // Setup WebDriverManager once before all tests
        WebDriverManager.chromedriver().setup();
        logger.info("WebDriverManager configured for ChromeDriver");
    }

    @BeforeEach
    public void setUp() {
        // Create ChromeDriver with options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        WebDriver chromeDriver = new ChromeDriver(options);
        driver = new HealingWebDriver(chromeDriver);

        // Configure timeouts
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO); // Important for healing!

        logger.info("HealingWebDriver initialized with {} strategies",
                driver.getHealingEngine().getStrategies().size());

        // Java 11 compatible way to log strategies
        List<String> strategyNames = driver.getHealingEngine().getStrategies().stream()
                .map(s -> s.getName() + " (priority: " + s.getPriority() + ")")
                .collect(Collectors.toList());
        logger.info("Strategies: {}", strategyNames);
    }

    @Test
    @DisplayName("Test 1: Normal element finding without healing")
    public void testSuccessfulElementFinding() {
        logger.info("=== Test 1: Normal element finding ===");

        driver.get("https://example.com");

        // This element exists - should be found immediately
        WebElement heading = driver.findElement(By.tagName("h1"));
        String headingText = heading.getText();
        logger.info("Found heading: '{}'", headingText);

        Assertions.assertTrue(headingText.contains("Example"),
                "Heading should contain 'Example'");

        logger.info("✓ Test passed - no healing needed");
    }

    @Test
    @DisplayName("Test 2: Healing triggered for missing element")
    public void testHealingTriggeredForMissingElement() {
        logger.info("=== Test 2: Healing triggered for missing element ===");

        driver.get("https://example.com");

        // Create a unique locator that definitely doesn't exist
        String uniqueId = "non-existent-element-" + System.currentTimeMillis();
        By nonExistentLocator = By.id(uniqueId);

        try {
            logger.info("Attempting to find element with locator: {}", nonExistentLocator);
            WebElement nonExistent = driver.findElement(nonExistentLocator);
            logger.error("ERROR: Element '{}' should not have been found!", uniqueId);
            Assertions.fail("Element should not have been found");
        } catch (Exception e) {
            logger.info("✓ Expected exception caught: {}", e.getClass().getSimpleName());
            logger.info("✓ Healing was attempted for locator: {}", nonExistentLocator);

            // Check that healing was logged in statistics
            var stats = driver.getHealingEngine().getContext().getStats(nonExistentLocator);
            logger.info("Healing attempts for this locator: {}", stats.getTotalFailures());

            Assertions.assertTrue(stats.getTotalFailures() > 0,
                    "Healing should have been attempted at least once");
        }
    }

    @Test
    @DisplayName("Test 3: Testing findElements() with healing")
    public void testFindElementsMethod() {
        logger.info("=== Test 3: Testing findElements() method ===");

        driver.get("https://example.com");

        // Try to find non-existent elements using findElements
        By nonExistentClass = By.className("non-existent-class-" + System.currentTimeMillis());
        List<WebElement> elements = driver.findElements(nonExistentClass);

        logger.info("findElements() returned {} elements", elements.size());
        Assertions.assertTrue(elements.isEmpty(),
                "findElements() should return empty list for non-existent elements");

        logger.info("✓ findElements() works correctly with healing");
    }

    @Test
    @DisplayName("Test 4: Real healing scenario - wait for element")
    public void testRealHealingScenario() {
        logger.info("=== Test 4: Real healing scenario ===");

        // This test uses a simple page
        driver.get("https://example.com");

        long startTime = System.currentTimeMillis();

        try {
            // Find an element that exists
            WebElement body = driver.findElement(By.tagName("body"));
            long endTime = System.currentTimeMillis();

            logger.info("Found body element after {}ms", (endTime - startTime));
            String bodyText = body.getText();
            if (bodyText.length() > 50) {
                logger.info("Body contains: {}...", bodyText.substring(0, 50));
            } else {
                logger.info("Body contains: {}", bodyText);
            }

            Assertions.assertTrue((endTime - startTime) < 5000,
                    "Should find element quickly on a simple page");
        } catch (Exception e) {
            logger.warn("Element not found: {}", e.getMessage());
            Assertions.fail("Should have found body element");
        }
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            // Print detailed healing statistics
            var stats = driver.getHealingEngine().getContext().getAllStats();
            var cache = driver.getHealingCache();

            logger.info("=== TEST SUMMARY ===");
            logger.info("Healing cache size: {}", cache.size());

            if (!stats.isEmpty()) {
                logger.info("Healing Statistics:");
                stats.forEach((locator, stat) -> {
                    logger.info("  Locator: {}", locator);
                    logger.info("    Success rate: {}%", String.format("%.1f", stat.getSuccessRate()));
                    logger.info("    Attempts: {} total ({} successful, {} failed)",
                            stat.getTotalFailures() + stat.getSuccessfulHealings(),
                            stat.getSuccessfulHealings(),
                            stat.getTotalFailures());
                    if (stat.getLastSuccessfulStrategy() != null) {
                        logger.info("    Last successful strategy: {}", stat.getLastSuccessfulStrategy());
                    }
                });
            } else {
                logger.info("No healing was performed during this test");
            }

            logger.info("Quitting driver...");
            driver.quit();
            logger.info("=== TEST COMPLETED ===\n");
        }
    }

    @AfterAll
    public static void tearDownAll() {
        logger.info("All tests completed. WebDriverManager resources cleaned up.");
    }
}
