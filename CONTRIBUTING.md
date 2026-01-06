# 🤝 Contributing to Self-Healing Selenium Framework

First off, thank you for considering contributing to our project! We welcome contributions that help make UI testing more reliable for everyone.

## 📋 Table of Contents
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Pull Request Process](#pull-request-process)

## 🎯 How Can I Contribute?

### 🐛 Reporting Bugs
1. **Check existing issues** to avoid duplicates.
2. **Provide a clear title** (e.g., "WaitStrategy doesn't respect custom timeout").
3. **Include detailed steps** to reproduce the issue.
4. **Share logs, code snippets, and environment details** (OS, Java version, browser).

### 💡 Suggesting Enhancements
1. **Describe the problem** you want to solve.
2. **Explain your proposed solution** with examples if possible.
3. **Discuss alternatives** you've considered.

### 🔧 Your First Code Contribution
Look for issues labeled `good-first-issue` or `documentation` if you're new to the project.

## 🛠️ Development Setup

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Git

### Steps
1. **Fork the repository** on GitHub.
2. **Clone your fork** locally:
```bash
  git clone https://github.com/YOUR_USERNAME/self-healing-selenium-framework.git
  cd self-healing-selenium-framework
```

3. **Set up upstream remote:**
```bash
  git remote add upstream https://github.com/ppetermsc/self-healing-selenium-framework.git
```

4. **Install dependencies and verify setup:**
```bash
  mvn clean install
  mvn test
```

5. **Create a feature branch:**
```bash
  git checkout -b feature/your-amazing-feature
```

## 📝 Coding Standards
### Java Style Guide
We follow the **Google Java Style Guide** with these key points:

- **Indentation:** 4 spaces (no tabs).

- **Line length:** 100 characters.

- **Naming:**

    - Classes: PascalCase

    - Methods/Variables: camelCase

    - Constants: UPPER_SNAKE_CASE

### Code Structure
```java
  // 1. Package statement
  package framework.core;

  // 2. Grouped imports (static, java, javax, org, com, framework)
  import static imports;
  import java.time.Duration;
  import org.openqa.selenium.*;
  import framework.annotations.*;

  // 3. Class JavaDoc
  /**
   * Brief description.
   * <p>
   * Detailed description with examples.
   * </p>
   */
  public class ClassName {
  // 4. Constants
  // 5. Fields
  // 6. Constructors
  // 7. Methods (public → private)
  }
```

### Documentation Requirements
All public classes and methods must have complete JavaDoc:

```java
  /**
   * Attempts to heal a web element that could not be found.
   *
   * @param context the search context (WebDriver or WebElement)
   * @param failingLocator the locator that failed
   * @param error the exception that was thrown
   * @return healing result containing outcome
   * @throws IllegalArgumentException if context is null
   * @since 1.0
   */
  public HealingResult attemptHealing(SearchContext context, By failingLocator, Throwable error) {
    // implementation
  }
```

### Testing Requirements
- **Minimum 80% line coverage** for new code.

- All public methods must have tests.

- Include edge cases and integration tests.

Run tests with:
```bash
  mvn test
```

## 🔄 Pull Request Process
### Before Submitting
1. Sync with upstream:
```bash
  git fetch upstream
  git merge upstream/develop
```

2. Run all tests:
```bash
  mvn clean test
```

3. **Update documentation** if your changes affect the public API.

### Creating the Pull Request
1. **Push to your fork:**
```bash
  git push origin feature/your-amazing-feature
```

2. **Create PR on GitHub** using the template.
3. **Use proper PR title format:**

  - [FEAT] - New feature

  - [FIX] - Bug fix

  - [DOCS] - Documentation

  - [TEST] - Tests

  - [REFACTOR] - Code refactoring

### PR Review
  - Automated checks must pass.

  - At least one maintainer must approve.

  - Address review comments promptly.

  - Keep PR focused (one feature/fix per PR).

## ❓ Getting Help
  - **GitHub Issues:** For bugs and feature requests.

  - **GitHub Discussions:** For questions and ideas.

  - **Stack Overflow:** Tag questions with self-healing-selenium.

## 📄 License
By contributing, you agree that your contributions will be licensed under the project's MIT License.

**Thank you for helping make UI testing more reliable!** 🚀



