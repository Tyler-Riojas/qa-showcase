package TestNG.tests;

import base.BaseOrangeHRMTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AddEmployeePage;
import pages.EmployeeListPage;
import utils.WaitUtils;

import java.util.List;
import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies employee deletion via the PIM employee list.
 *
 * <p>Each test creates its own unique employee in @BeforeMethod so deletions
 * do not interfere with other tests running concurrently on the shared demo site.</p>
 */
@Feature("Employee Management")
public class DeleteEmployeeTest extends BaseOrangeHRMTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private static final By DELETE_BUTTONS      = By.cssSelector(".bi-trash");
    // Cancel is the first button in the modal footer; .oxd-button--ghost is unreliable
    private static final By CANCEL_DELETE_BTN   = By.cssSelector(".orangehrm-modal-footer button:first-child");
    private static final By CONFIRM_DELETE_BTN  = By.cssSelector(".orangehrm-modal-footer .oxd-button--label-danger");

    private String createdFirstName;
    private String createdLastName;
    private String createdEmployeeId;

    /**
     * Creates a unique test employee before each test.
     * Runs after parent's @BeforeMethod — driver is logged in and ready.
     */
    @BeforeMethod(alwaysRun = true)
    public void createTestEmployee() {
        String uniqueId  = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        createdFirstName  = "Auto" + uniqueId;
        createdLastName   = "Test" + uniqueId;
        createdEmployeeId = uniqueId;

        dashboardPage.navigateToPIM();
        EmployeeListPage listPage = new EmployeeListPage(driver);
        AddEmployeePage  addPage  = new AddEmployeePage(driver);

        listPage.clickAddEmployee();
        addPage.enterFirstName(createdFirstName);
        addPage.enterLastName(createdLastName);
        addPage.enterEmployeeId(createdEmployeeId);
        addPage.clickSave();
        addPage.isEmployeeSaved();

        log.info("Test employee created for deletion: {} {}", createdFirstName, createdLastName);

        // Return to list page
        driver.get(BASE_URL + "/web/index.php/pim/viewEmployeeList");
        WaitUtils.waitForPageLoad(driver);
    }

    @Test(description = "Delete an employee and verify they no longer appear in the list")
    @Story("Delete Employee Record")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteEmployee() {
        EmployeeListPage listPage = new EmployeeListPage(driver);

        // Search and confirm employee exists before deletion
        listPage.searchEmployee(createdFirstName, createdLastName);
        assertThat(listPage.isEmployeeInList(createdFirstName))
                .as("Employee should be in list before deletion")
                .isTrue();

        // Delete via the table row trash icon
        listPage.deleteFirstEmployee();

        // Search again — employee must be gone
        driver.get(BASE_URL + "/web/index.php/pim/viewEmployeeList");
        WaitUtils.waitForPageLoad(driver);
        listPage.searchEmployee(createdFirstName, createdLastName);

        boolean stillExists = listPage.isEmployeeInList(createdFirstName);
        log.info("Employee '{}' still in list after delete: {}", createdFirstName, stillExists);

        assertThat(stillExists)
                .as("Deleted employee should no longer appear in search results")
                .isFalse();
    }

    @Test(description = "Cancel deletion keeps the employee in the list")
    @Story("Delete Employee Record")
    @Severity(SeverityLevel.NORMAL)
    public void testDeleteConfirmationModal() {
        EmployeeListPage listPage = new EmployeeListPage(driver);

        listPage.searchEmployee(createdFirstName, createdLastName);

        // Click trash icon to open confirmation modal — must click the parent button, not the <i> icon
        List<WebElement> delBtns = WaitUtils.waitForAllElementsVisible(driver, DELETE_BUTTONS, 10);
        assertThat(delBtns).as("Delete buttons should be visible").isNotEmpty();
        WebElement trashBtn = delBtns.get(0).findElement(By.xpath("./ancestor::button[1]"));
        trashBtn.click();

        // Modal must show both Cancel and Delete buttons
        WebElement cancelBtn  = WaitUtils.waitForElementVisible(driver, CANCEL_DELETE_BTN);
        WebElement confirmBtn = WaitUtils.waitForElementVisible(driver, CONFIRM_DELETE_BTN);
        assertThat(cancelBtn.isDisplayed()).as("Cancel button should be visible in modal").isTrue();
        assertThat(confirmBtn.isDisplayed()).as("Delete button should be visible in modal").isTrue();

        // Click Cancel — employee must still be in the list
        cancelBtn.click();

        assertThat(listPage.isEmployeeInList(createdFirstName))
                .as("Employee should remain in list after cancelling the deletion")
                .isTrue();
    }
}
