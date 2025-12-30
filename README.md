# 🛡️ Self-Healing Selenium Framework

> **Intelligently recover from flaky UI tests with transparent, strategy-based healing.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Stop wasting time on false failures.** This framework automatically heals common Selenium issues—like timing and locator changes—so your tests are reliable and your CI/CD pipeline stays green.

## ✨ Why This Framework?

UI tests break for reasons beyond your control. Instead of failing immediately, this framework **attempts recovery** using configurable strategies before reporting a real error.

### 💡 Core Idea
```java
// 1. Wrap your WebDriver
WebDriver driver = new HealingWebDriver(new ChromeDriver());

// 2. Use normally. Healing happens automatically!
driver.findElement(By.id("dynamic-button")).click();
```

### 🚀 Quick Start
**1. Add Dependency (Maven)**
```xml
<dependency>
    <groupId>com.github.ppetermsc</groupId>
    <artifactId>self-healing-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**2. Basic Usage**
```java
import framework.core.HealingWebDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

public class QuickExample {
    public static void main(String[] args) {
        WebDriver driver = new HealingWebDriver(new ChromeDriver());
        driver.get("https://your-app.com");
        
        // If this fails, the framework will try to heal it
        WebElement button = driver.findElement(By.cssSelector(".btn-primary"));
        button.click();
        
        driver.quit();
    }
}
```

**3. Advanced: Self-Healing Page Objects**
```java
import framework.annotations.SmartFindBy;
import framework.core.SelfHealingPageFactory;

public class LoginPage {
    @SmartFindBy(
        id = "username",
        fallbacks = {"name=username", "css=.login-input"}
    )
    private WebElement usernameField;
    
    // The framework initializes this with healing support
    public LoginPage(WebDriver driver) {
        SelfHealingPageFactory.initElements(driver, this);
    }
}
```

### 🛠️ How It Works

When an element isn't found, a HealingEngine runs your chosen strategies in order:

1. WaitStrategy - Waits for the element to appear (solves timing issues).

2. FallbackLocatorStrategy - Tries alternative locators (solves HTML refactoring).

3. Your Custom Strategies - Extend the HealingStrategy interface.

**Result:** Your test either finds the element (status: **HEALED**) or fails with a clear reason after all strategies are exhausted.

### 📚 Documentation
For comprehensive guides and examples, visit the documentation:

- **[📖 Complete Usage Guide](docs/USAGE_GUIDE.md)** - Everything from setup to advanced customization.
- **[🎯 Project Philosophy & Comparisons](docs/PHILOSOPHY.md)** - Why we built it and how it compares to other tools.

### 📄 License
Distributed under the MIT License. See LICENSE for more information.
