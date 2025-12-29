package framework.pages;

import framework.annotations.SmartFindBy;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Example Page Object for login functionality.
 * Demonstrates @SmartFindBy usage with fallback locators.
 *
 * @author Peter Pestriakov
 * @version 1.0
 */
public class LoginPage {

    @SmartFindBy(
            id = "username",
            fallbacks = {"name=username", "css=.login-input", "xpath=//input[@placeholder='Username']"},
            timeout = 10
    )
    public WebElement usernameField;

    @SmartFindBy(
            id = "password",
            fallbacks = {"name=password", "css=.password-input"}
    )
    public WebElement passwordField;

    @SmartFindBy(
            id = "submit",
            fallbacks = {"css=button[type='submit']", "xpath=//button[text()='Login']", "class=btn-primary"}
    )
    public WebElement submitButton;

    @SmartFindBy(
            css = ".error-message",
            fallbacks = {"css=.alert-danger", "class=error"}
    )
    public WebElement errorMessage;

    @SmartFindBy(
            css = ".remember-me",
            fallbacks = {"name=remember", "id=rememberMe"}
    )
    public WebElement rememberMeCheckbox;

    @SmartFindBy(
            css = "a.forgot-password",
            fallbacks = {"linkText=Forgot Password?", "partialLinkText=Forgot"}
    )
    public WebElement forgotPasswordLink;

    // Example of List<WebElement> field
    @SmartFindBy(
            css = ".social-login button",
            fallbacks = {"css=.oauth-buttons button"}
    )
    public List<WebElement> socialLoginButtons;

    /**
     * Business logic method for logging in.
     */
    public void login(String username, String password) {
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        submitButton.click();
    }

    /**
     * Business logic method for logging in with remember me.
     */
    public void loginWithRememberMe(String username, String password) {
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        rememberMeCheckbox.click();
        submitButton.click();
    }

    /**
     * Gets error message text if present.
     */
    public String getErrorMessage() {
        try {
            return errorMessage.getText();
        } catch (Exception e) {
            return ""; // No error message displayed
        }
    }

    /**
     * Clicks on a social login button by index.
     */
    public void clickSocialLogin(int index) {
        if (socialLoginButtons != null && index < socialLoginButtons.size()) {
            socialLoginButtons.get(index).click();
        } else {
            throw new IndexOutOfBoundsException("Social login button index out of bounds: " + index);
        }
    }

    /**
     * Gets the number of social login options available.
     */
    public int getSocialLoginCount() {
        return socialLoginButtons != null ? socialLoginButtons.size() : 0;
    }
}
