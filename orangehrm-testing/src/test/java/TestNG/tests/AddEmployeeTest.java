package TestNG.tests;

import base.BaseOrangeHRMTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AddEmployeePage;
import pages.EmployeeListPage;

import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a new employee can be added through the PIM module
 * and subsequently found in the employee list.
 *
 * <p>Uses a timestamp suffix on names to avoid conflicts with other concurrent
 * runs against the shared demo site.</p>
 */
@Feature("Employee Management")
public class AddEmployeeTest extends BaseOrangeHRMTest {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    /** Unique per-test-method to survive parallel class execution. */
    private String testFirstName;
    private String testLastName;
    private String testEmployeeId;

    /** Runs after parent's @BeforeMethod setup() — driver and login are ready. */
    @BeforeMethod(alwaysRun = true)
    public void navigateToPim() {
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        testFirstName  = "Auto" + uniqueId;
        testLastName   = "Test" + uniqueId;
        testEmployeeId = uniqueId;
        dashboardPage.isLoaded();
        dashboardPage.navigateToPIM();
    }

    @Test(description = "Add a new employee and verify the save succeeds")
    @Story("Add New Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testAddEmployee() {
        EmployeeListPage listPage = new EmployeeListPage(driver);
        AddEmployeePage  addPage  = new AddEmployeePage(driver);

        listPage.clickAddEmployee();
        addPage.enterFirstName(testFirstName);
        addPage.enterLastName(testLastName);
        addPage.enterEmployeeId(testEmployeeId);
        addPage.clickSave();

        boolean saved = addPage.isEmployeeSaved();
        log.info("Employee created: {} {} — saved: {}", testFirstName, testLastName, saved);

        assertThat(saved)
                .as("Employee should be saved successfully")
                .isTrue();
    }

    @Test(description = "Newly added employee appears in the employee list search results")
    @Story("Add New Employee")
    @Severity(SeverityLevel.NORMAL)
    public void testAddedEmployeeAppearsInList() {
        EmployeeListPage listPage = new EmployeeListPage(driver);
        AddEmployeePage  addPage  = new AddEmployeePage(driver);

        // Add employee
        listPage.clickAddEmployee();
        addPage.enterFirstName(testFirstName);
        addPage.enterLastName(testLastName);
        addPage.enterEmployeeId(testEmployeeId);
        addPage.clickSave();
        addPage.isEmployeeSaved(); // wait for navigation

        // Navigate to list and search by name
        driver.get(BASE_URL + "/web/index.php/pim/viewEmployeeList");
        listPage.searchEmployee(testFirstName, testLastName);

        boolean found = listPage.isEmployeeInList(testFirstName);
        log.info("Employee '{}' found in list: {}", testFirstName, found);

        assertThat(found)
                .as("Newly added employee '" + testFirstName + "' should appear in search results")
                .isTrue();
    }
}
