import pages.LoggedOutMainPage;
import pages.LoginResultPage;
import pages.LogoutPage;
import pages.PreferencesPage;
import pages.WikipediaLoginPage;
import pages.searchResultPages.ArticlePage;
import pages.searchResultPages.ListOfSearchResultsPage;
import pages.searchResultPages.SearchResultPageHandler;
import utils.DateFormatType;
import utils.DateValidator;
import utils.RandomStringGenerator;

import java.net.URL;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.MalformedURLException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WikipediaTest {    
    private WebDriver driver;
    private String correctUsername;
    private String correctPassword;
    
    @BeforeAll
    public void setup() throws MalformedURLException, InterruptedException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito", "--delete-cookies");
        this.driver = new RemoteWebDriver(new URL("http://selenium:4444/wd/hub"), options);
        this.driver.manage().window().maximize();

        Dotenv dotenv = Dotenv.load();
        correctUsername = dotenv.get("USERNAME");
        correctPassword = dotenv.get("PASSWORD");
    }
    
    @Test
    public void validAndInvalidLoginTest() {
        WikipediaLoginPage loginPage;
        LoginResultPage loginResultPage;
        LogoutPage logoutPage;

        LoggedOutMainPage loggedOutMainPage = new LoggedOutMainPage(this.driver);

        assertTrue(loggedOutMainPage.getWelcomeTitleMessage().contains("Welcome to Wikipedia"));

        loginPage = loggedOutMainPage
            .clickLinkToLoginPage();
        loginResultPage = loginPage
            .clickCorner()
            .writeToUsernameInput("WRONGUSERNAME")
            .writeToPasswordInput("WRONGPASSWORD")
            .hold(8) // You might have to solve Recaptcha
            .clickLoginButton();

        assertTrue(loginResultPage.getBodyText().contains("Incorrect username or password entered"));

        loginResultPage = loginPage
            .clickCorner()
            .writeToUsernameInput(this.correctUsername)
            .writeToPasswordInput(this.correctPassword)
            .hold(8) // You might have to solve Recaptcha
            .clickLoginButton();
        
        assertTrue(
            loginResultPage.userSpanElementExists() &&
            loginResultPage.getUserSpanText().contains(this.correctUsername)
        );

        logoutPage = loginResultPage
            .openUserMenu()
            .logout();

        assertTrue(logoutPage.getBodyText().contains("You are now logged out."));

        loggedOutMainPage = logoutPage.clickMainPageLink();

        assertTrue(loggedOutMainPage.getWelcomeTitleMessage().contains("Welcome to Wikipedia"));
        assertTrue(loggedOutMainPage.getPageTitle().contains("Wikipedia, the free encyclopedia"));
    }  
    
    @ParameterizedTest
    @CsvFileSource(resources = "/searchTestData.csv", numLinesToSkip = 1)
    @DisplayName("Searching: '{0}'; Expected Article Title: '{1}'")
    public void searchTest(String searchText, String expectedArticleTitle) {
        ArticlePage article;
        LoggedOutMainPage loggedOutMainPage = new LoggedOutMainPage(this.driver);

        article = loggedOutMainPage.seachAndGoToFirstResult(searchText);

        assertTrue(article.getArticleTitle().equals(expectedArticleTitle));
    }

    @Test
    public void browserBackButtonTest() {
        LoggedOutMainPage loggedOutMainPage = new LoggedOutMainPage(this.driver);
        loggedOutMainPage.clickLinkToLoginPage().pressBrowserBackButton();
        loggedOutMainPage.ensure();
        assertTrue(loggedOutMainPage.getWelcomeTitleMessage().contains("Welcome to Wikipedia"));
    }

    @ParameterizedTest
    @ValueSource(ints = {18, 19, 20})
    public void testSearchingLongRandomStrings(int length) {
        String randomsearchtext = RandomStringGenerator.generateRandomString(length);

        LoggedOutMainPage loggedOutMainPage = new LoggedOutMainPage(driver);
        SearchResultPageHandler searchResultPageHandler = loggedOutMainPage.search(randomsearchtext);

        assertTrue(searchResultPageHandler.isListOfSearchResults());

        ListOfSearchResultsPage listOfSearchResultsPage = searchResultPageHandler.listOfSearchResultsPage();

        assertTrue(
            listOfSearchResultsPage
                .getListFooterText()
                .toLowerCase()
                .contains(String.format("the page \"%s\" does not exist.", randomsearchtext))
        );
    }

    @Test
    public void userApperancePreferencesFormTest() {
        DateFormatType[] dateFormatTypeWrapper = new DateFormatType[]{DateFormatType.NO_PREFERENCE};
        WikipediaLoginPage loginPage;
        LoginResultPage loginResultPage;
        PreferencesPage preferencesPage;
        
        LoggedOutMainPage loggedOutMainPage = new LoggedOutMainPage(this.driver);

        loginPage = loggedOutMainPage
            .clickLinkToLoginPage();
        loginResultPage = loginPage
            .clickCorner()
            .writeToUsernameInput(this.correctUsername)
            .writeToPasswordInput(this.correctPassword)
            .clickLoginButton();

        preferencesPage = loginResultPage
            .openUserMenu()
            .goToPreferences();

        ArticlePage article = preferencesPage
            .selectApperancePreferencesTab()
            .changeDateFormatPreference(dateFormatTypeWrapper)
            .savePreferences()
            .search("Hungary")
            .articlePage();

        String lastModificationDate = DateValidator.extractDate(
            article.getLastModificationMessage()
        );

        if (dateFormatTypeWrapper[0] == DateFormatType.DAY_MONTH_YEAR) {
            assertTrue(DateValidator.isDataInFormatDayMonthYear(lastModificationDate));
        } else if (dateFormatTypeWrapper[0] == DateFormatType.MONTH_DAY_YEAR) {
            assertTrue(DateValidator.isDataInFormatMonthDayYear(lastModificationDate));
        } else {
            assertTrue(false, "Preferences not changed");
        }

        loginResultPage
            .openUserMenu()
            .logout();
    }

    @AfterAll
    public void close() throws InterruptedException {
        if (this.driver != null) {
            this.driver.quit();
        }
    }
}
