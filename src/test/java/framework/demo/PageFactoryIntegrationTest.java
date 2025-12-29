package framework.demo;

import framework.core.HealingWebDriver;
import framework.core.SelfHealingPageFactory;
import framework.pages.LoginPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SelfHealingPageFactory.
 */
public class PageFactoryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PageFactoryIntegrationTest.class);
    private HealingWebDriver driver;

    @BeforeAll
    public static void setupAll() {
        WebDriverManager.chromedriver().setup();
        logger.info("WebDriverManager configured for PageFactory tests");
    }

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");

        WebDriver chromeDriver = new ChromeDriver(options);
        driver = new HealingWebDriver(chromeDriver);

        logger.info("HealingWebDriver initialized with {} strategies",
                driver.getHealingEngine().getStrategies().size());
    }

    @Test
    @DisplayName("Test PageFactory initialization")
    public void testPageFactoryInitialization() {
        logger.info("=== Test: PageFactory initialization ===");

        // Create and initialize page object
        LoginPage loginPage = SelfHealingPageFactory.initElements(driver, LoginPage.class);

        assertNotNull(loginPage, "LoginPage should be created");
        assertNotNull(loginPage.usernameField, "usernameField should be initialized");
        assertNotNull(loginPage.passwordField, "passwordField should be initialized");
        assertNotNull(loginPage.submitButton, "submitButton should be initialized");

        logger.info("✓ PageFactory successfully initialized LoginPage");
    }

    @Test
    @DisplayName("Test List<WebElement> field initialization")
    public void testListFieldInitialization() {
        logger.info("=== Test: List<WebElement> field initialization ===");

        LoginPage loginPage = SelfHealingPageFactory.initElements(driver, LoginPage.class);

        assertNotNull(loginPage.socialLoginButtons, "socialLoginButtons should be initialized");
        assertTrue(loginPage.socialLoginButtons instanceof java.util.List,
                "socialLoginButtons should be a List");

        // The list will be empty on non-existent page, but proxy should work
        assertEquals(0, loginPage.getSocialLoginCount(),
                "Social login count should be 0 on empty page");

        logger.info("✓ List<WebElement> field initialized correctly");
    }

    @Test
    @DisplayName("Test PageFactory with manual initialization")
    public void testManualInitialization() {
        logger.info("=== Test: Manual PageFactory initialization ===");

        LoginPage loginPage = new LoginPage();
        SelfHealingPageFactory.initElements(driver, loginPage);

        assertNotNull(loginPage.usernameField, "usernameField should be initialized");
        assertNotNull(loginPage.passwordField, "passwordField should be initialized");

        logger.info("✓ Manual initialization works correctly");
    }

    @Test
    @DisplayName("Test business logic methods")
    public void testBusinessLogicMethods() {
        logger.info("=== Test: Business logic methods ===");

        LoginPage loginPage = SelfHealingPageFactory.initElements(driver, LoginPage.class);

        // Test that methods don't throw exceptions
        assertDoesNotThrow(() -> loginPage.getErrorMessage());
        assertEquals("", loginPage.getErrorMessage(),
                "Error message should be empty on non-existent page");

        // Test social login methods (won't throw on empty list)
        assertEquals(0, loginPage.getSocialLoginCount());

        logger.info("✓ Business logic methods work without exceptions");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            var stats = driver.getHealingEngine().getContext().getAllStats();
            logger.info("PageFactory test completed. Healing stats: {} locators", stats.size());

            driver.quit();
        }
    }

    @AfterAll
    public static void tearDownAll() {
        logger.info("All PageFactory integration tests completed");
    }
}
