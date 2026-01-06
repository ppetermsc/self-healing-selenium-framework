# 📖 Complete Usage Guide

## Table of Contents
1. [Basic Setup](#basic-setup)
2. [HealingWebDriver Usage](#healingwebdriver-usage)
3. [Page Objects with @SmartFindBy](#page-objects-with-smartfindby)
4. [Built-in Healing Strategies](#built-in-healing-strategies)
5. [Custom Healing Strategies](#custom-healing-strategies)
6. [Monitoring and Analytics](#monitoring-and-analytics)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

## Basic Setup

### Maven Dependency
```xml
<dependency>
    <groupId>com.github.ppetermsc</groupId>
    <artifactId>self-healing-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Basic Usage
```java
import framework.core.HealingWebDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

public class BasicExample {
public static void main(String[] args) {
// Wrap your existing WebDriver
WebDriver driver = new HealingWebDriver(new ChromeDriver());

        // Configure timeouts (IMPORTANT!)
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        
        driver.get("https://your-app.com");
        
        // Healing happens automatically if element not found
        WebElement button = driver.findElement(By.id("submit-button"));
        button.click();
        
        driver.quit();
    }
}
```

## HealingWebDriver Usage
### Configuration Options
```java
// 1. Default configuration
HealingWebDriver driver = new HealingWebDriver(new ChromeDriver());

// 2. Custom healing engine
        HealingEngine customEngine = new HealingEngine();
        customEngine.addStrategy(new WaitStrategy(20)); // 20 second timeout
        customEngine.addStrategy(new FallbackLocatorStrategy());
        HealingWebDriver driver = new HealingWebDriver(new ChromeDriver(), customEngine);

// 3. Disable implicit waits (CRITICAL for healing to work)
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
```

### Working with findElements()
```java
// findElements() also supports healing
List<WebElement> buttons = driver.findElements(By.className("btn"));

        if (buttons.isEmpty()) {
        // Healing was attempted but no elements found
        System.out.println("No buttons found even after healing attempts");
        }
```

## Page Objects with @SmartFindBy
### Basic Page Object

```java
import framework.annotations.SmartFindBy;
import framework.core.SelfHealingPageFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginPage {

    @SmartFindBy(
            id = "username",
            fallbacks = {"name=username", "css=.login-input"},
            timeout = 10
    )
    private WebElement usernameField;

    @SmartFindBy(
            id = "password",
            fallbacks = {"name=password", "css=.password-input"}
    )
    private WebElement passwordField;

    @SmartFindBy(
            id = "submit",
            fallbacks = {"css=button[type='submit']", "xpath=//button[text()='Login']"}
    )
    private WebElement submitButton;

    public LoginPage(WebDriver driver) {
        SelfHealingPageFactory.initElements(driver, this);
    }

    public void login(String username, String password) {
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        submitButton.click();
    }
}
```
### @SmartFindBy Annotation Syntax
```java
@SmartFindBy(
        // Primary locator (choose one):
        id = "elementId",
        name = "elementName",
        css = ".selector",
        xpath = "//div[@class='test']",
        className = "my-class",
        tagName = "button",
        linkText = "Click Here",
        partialLinkText = "Click",

        // Fallback locators (try in order if primary fails):
        fallbacks = {
                "css=.backup-selector",
                "xpath=//button[text()='Submit']",
                "id=alternativeId",
                "name=alternativeName"
        },

        // Custom timeout for this element (seconds):
        timeout = 15
)
private WebElement element;
```

## Built-in Healing Strategies
### WaitStrategy

```java
// Default: waits 10 seconds
WaitStrategy waitStrategy = new WaitStrategy();

// Custom timeout
        WaitStrategy customWait = new WaitStrategy(30); // 30 seconds
```

### FallbackLocatorStrategy

Automatically tries alternative locators from @SmartFindBy.fallbacks() array.

## Custom Healing Strategies
### Implementing HealingStrategy

```java
package com.yourcompany.strategies;

import framework.strategies.HealingStrategy;
import framework.core.HealingResult;
import org.openqa.selenium.*;

import java.time.Duration;
import java.util.ArrayList;

public class RefreshPageStrategy implements HealingStrategy {

    @Override
    public String getName() {
        return "RefreshPageStrategy";
    }

    @Override
    public int getPriority() {
        return 3; // Executes after WaitStrategy(1) and FallbackLocatorStrategy(2)
    }

    @Override
    public boolean canHandle(Throwable error) {
        return error instanceof StaleElementReferenceException ||
                error instanceof NoSuchElementException;
    }

    @Override
    public HealingResult attemptHeal(SearchContext context, By originalLocator, Throwable originalError) {
        long startTime = System.currentTimeMillis();
        var attempts = new ArrayList<HealingResult.HealingAttempt>();

        if (!(context instanceof WebDriver)) {
            return HealingResult.failed("RefreshPageStrategy requires WebDriver context");
        }

        WebDriver driver = (WebDriver) context;

        try {
            // Refresh the page
            driver.navigate().refresh();

            // Wait for page to reload
            Thread.sleep(1000);

            // Try to find element again
            WebElement element = driver.findElement(originalLocator);

            Duration healingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            attempts.add(new HealingResult.HealingAttempt(
                    getName(), true,
                    "Page refreshed and element found",
                    healingTime
            ));

            return HealingResult.success(element, getName(), healingTime, attempts);

        } catch (Exception e) {
            Duration healingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            attempts.add(new HealingResult.HealingAttempt(
                    getName(), false,
                    "Refresh failed: " + e.getMessage(),
                    healingTime
            ));

            return HealingResult.failed(attempts, originalError.toString());
        }
    }
}
```

### Registering Custom Strategies
```java
// Create custom engine
HealingEngine engine = new HealingEngine();
engine.addStrategy(new WaitStrategy());
engine.addStrategy(new FallbackLocatorStrategy());
engine.addStrategy(new RefreshPageStrategy());

// Use with driver
HealingWebDriver driver = new HealingWebDriver(new ChromeDriver(), engine);
```

## Monitoring and Analytics
### Accessing Healing Statistics
```java
HealingContext context = driver.getHealingEngine().getContext();

// Get stats for all locators
Map<String, HealingContext.LocatorStats> stats = context.getAllStats();

stats.forEach((locator, stat) -> {
System.out.println("Locator: " + locator);
System.out.println("  Success rate: " + String.format("%.1f", stat.getSuccessRate()) + "%");
System.out.println("  Attempts: " + (stat.getTotalFailures() + stat.getSuccessfulHealings()));
System.out.println("  Successful healings: " + stat.getSuccessfulHealings());
System.out.println("  Failed healings: " + stat.getTotalFailures());
});
```

### Real Example from Logs
```text
12:14:45.453 [main] WARN  Healing triggered for locator: By.id: submit-button
12:14:45.453 [main] INFO  Trying strategy: WaitStrategy
12:14:55.497 [main] WARN  WaitStrategy FAILED (timeout after 10s)
12:14:55.500 [main] INFO  Trying strategy: FallbackLocatorStrategy
12:14:55.502 [main] INFO  Found with fallback 1: By.cssSelector: .btn-primary
12:14:55.503 [main] WARN  Healing SUCCESS with strategy: FallbackLocatorStrategy
```

## Best Practices
### Design Effective Fallback Locators
```java
// GOOD: Multiple independent strategies
@SmartFindBy(
        id = "login-button",
        fallbacks = {
                "css=button[type='submit']",      // Same function
                "xpath=//button[text()='Sign In']", // Same text
                "css=.primary-button",            // Same style
                "name=submitLogin"                // Form association
        }
)

// BAD: Fragile selectors
@SmartFindBy(
        id = "login-button",
        fallbacks = {
                "css=#container > div > form > div:last-child > button:first-child"
                // Too specific, breaks easily
        }
)
```

### Set Appropriate Timeouts
```java
// Fast-loading elements
@SmartFindBy(id = "notification", timeout = 3)

// Slow widgets
@SmartFindBy(id = "dashboard-chart", timeout = 30)

// Modal dialogs
@SmartFindBy(id = "confirmation-modal", timeout = 60)
```

### Combine with Explicit Waits
```java
// Use healing for basic recovery, explicit waits for complex scenarios
public void waitForDataToLoad() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
        By.cssSelector(".loading-spinner")
        ));

        // Now use self-healing elements
        dataTable.findElement(By.cssSelector(".first-row")).click();
        }
```

## Troubleshooting
### Common Issues
**1. Element Still Not Found After Healing**

**Problem:** All strategies exhausted but element still missing.

**Solution:**
```java
// Check if element actually exists
List<WebElement> elements = driver.findElements(originalLocator);
if (elements.isEmpty()) {
// Element doesn't exist - likely application bug
reportBug("Element missing: " + originalLocator);
} else {
// Element exists but not interactable
// Consider adding visibility/clickability checks to your strategies
}
```

**2. Slow Test Execution**

   **Problem:** Healing adds too much wait time.

   **Solution:**
```java
// Reduce WaitStrategy timeout
@SmartFindBy(id = "element", timeout = 5) // Reduced from default 10

// Or create faster custom strategy
public class QuickWaitStrategy extends WaitStrategy {
    public QuickWaitStrategy() {
        super(3); // 3 second timeout
    }
}
```

**3. False Positives**

   **Problem:** Healing succeeds but masks real bugs.

   **Solution:**
```java
// Monitor healing success rates
if (context.getStats(locator).getSuccessRate() > 50) {
    logger.warn("High healing rate for {} - investigate UI stability", locator);
}
```

### Debug Logging
Enable debug logging in logback-test.xml:
```xml
<logger name="framework" level="DEBUG" />
<logger name="framework.core" level="DEBUG" />
<logger name="framework.strategies" level="DEBUG" />
```
Run tests with:
```bash
mvn test -Dlogback.configurationFile=src/test/resources/logback-debug.xml
```

## Need More Help?
- Check the PHILOSOPHY.md for design decisions

- Review the demo tests for working examples

- Open an issue on GitHub for specific questions
