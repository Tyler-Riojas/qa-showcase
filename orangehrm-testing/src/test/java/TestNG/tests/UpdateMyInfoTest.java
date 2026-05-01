package TestNG.tests;

import base.BaseOrangeHRMTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import pages.MyInfoPage;

import java.time.Instant;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the logged-in user can view and update their personal info
 * via the My Info module.
 */
@Feature("Employee Management")
public class UpdateMyInfoTest extends BaseOrangeHRMTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    @Test(description = "Update nickname and verify the success toast appears")
    @Story("Update My Info")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdateNickname() {
        String nickname = "AutoTest_" + Instant.now().getEpochSecond();

        dashboardPage.navigateToMyInfo();
        MyInfoPage myInfoPage = new MyInfoPage(driver);
        myInfoPage.isLoaded();
        myInfoPage.updateNickname(nickname);
        myInfoPage.clickSave();

        boolean success = myInfoPage.isUpdateSuccessful();
        log.info("Nickname updated to '{}' — success toast: {}", nickname, success);

        assertThat(success)
                .as("Success toast should appear after updating the nickname")
                .isTrue();
    }

    @Test(description = "Personal info fields (firstName, lastName) are present and editable")
    @Story("Update My Info")
    @Severity(SeverityLevel.MINOR)
    public void testPersonalInfoFieldsEditable() {
        dashboardPage.navigateToMyInfo();
        MyInfoPage myInfoPage = new MyInfoPage(driver);
        myInfoPage.isLoaded();

        assertThat(myInfoPage.isFirstNameFieldEditable())
                .as("First name field should be present and editable")
                .isTrue();
        assertThat(myInfoPage.isLastNameFieldEditable())
                .as("Last name field should be present and editable")
                .isTrue();
    }
}
