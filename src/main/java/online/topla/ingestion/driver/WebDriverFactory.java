package online.topla.ingestion.driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import online.topla.ingestion.config.AppConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Creates configured WebDriver instances (headless, timeouts). Extend with proxy / custom caps later.
 */
public final class WebDriverFactory {

    private final AppConfig config;

    public WebDriverFactory(AppConfig config) {
        this.config = config;
    }

    public WebDriver createWebDriver() {
        BrowserType type = parseBrowser(config.getBrowser());
        WebDriver driver = switch (type) {
            case CHROME -> createChrome();
            case FIREFOX -> createFirefox();
            case EDGE -> createEdge();
        };
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(config.getPageLoadTimeoutMs()));
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(config.getImplicitWaitMs()));
        return driver;
    }

    public WebDriverWait createWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofMillis(config.getPageLoadTimeoutMs()));
    }

    private static BrowserType parseBrowser(String name) {
        if (name == null) {
            return BrowserType.CHROME;
        }
        return switch (name.trim().toLowerCase()) {
            case "firefox", "ff" -> BrowserType.FIREFOX;
            case "edge", "msedge" -> BrowserType.EDGE;
            default -> BrowserType.CHROME;
        };
    }

    private WebDriver createChrome() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        return new ChromeDriver(options);
    }

    private WebDriver createFirefox() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (config.isHeadless()) {
            options.addArguments("-headless");
        }
        return new FirefoxDriver(options);
    }

    private WebDriver createEdge() {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return new EdgeDriver(options);
    }
}
